/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2022 The Catrobat Team
 * (<http://developer.catrobat.org/credits>)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * An additional term exception under section 7 of the GNU Affero
 * General Public License, version 3, is available at
 * http://developer.catrobat.org/license_additional_term
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.catrobat.catroid.ui.dragndrop

import android.animation.Animator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.ListAdapter
import android.widget.ListView
import androidx.annotation.VisibleForTesting
import androidx.core.view.OneShotPreDrawListener
import org.catrobat.catroid.CatroidApplication
import org.catrobat.catroid.content.bricks.Brick
import org.catrobat.catroid.content.bricks.CompositeBrick
import org.catrobat.catroid.content.bricks.EndBrick
import org.catrobat.catroid.ui.settingsfragments.SettingsFragment
import java.util.ArrayList
import kotlin.math.abs
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toDrawable
import androidx.core.graphics.withTranslation

private const val SMOOTH_SCROLL_BY = 15
private const val SMOOTH_SCROLL_BY_NEW = 20
private const val ANIMATION_DURATION = 250
private const val SWAP_ANIMATION_DURATION = 150
private const val TRANSLUCENT_BLACK_ALPHA = 128
private const val OBJECT_ANIMATOR_VALUE = 255
private const val ANIMATION_REPEAT_COUNT = 5
private const val MINIMUM_ANIMATED_OFFSET = 1f
private const val MINIMUM_ROWS_FOR_SCROLL_ESTIMATE = 4

enum class DragMode {
    NEW,
    LEGACY
}

class BrickListView : ListView {
    private var upperScrollBound = 0
    private var lowerScrollBound = 0
    private var hoveringDrawable: BitmapDrawable? = null
    private val viewBounds = Rect()
    private var currentPositionOfHoveringBrick = 0
    private var brickToMove: Brick? = null
    private var motionEventId = -1
    private var downY = 0f
    private var offsetToCenter = 0
    private var invalidateHoveringItem = false
    private val runningSwapAnimators = HashMap<View, ObjectAnimator>()
    private val itemPositionsBeforeSwap = HashMap<Any, Float>()
    private val swapInterpolator = DecelerateInterpolator()
    private var brickAdapterInterface: BrickAdapterInterface? = null
    private val translucentBlack = Color.argb(TRANSLUCENT_BLACK_ALPHA, 0, 0, 0)
    var dragMode: DragMode = DragMode.NEW

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attributes: AttributeSet?) : super(context, attributes)
    constructor(context: Context?, attributes: AttributeSet?, defStyle: Int) : super(
        context,
        attributes,
        defStyle
    )

    val brickPositionsToHighlight: MutableList<Int> = ArrayList()

    val isCurrentlyMoving: Boolean
        get() = hoveringDrawable != null

    val isCurrentlyHighlighted: Boolean
        get() = brickPositionsToHighlight.isNotEmpty()

    fun highlightMovingItem() {
        val animator = ObjectAnimator.ofInt(hoveringDrawable!!, "alpha", OBJECT_ANIMATOR_VALUE, 0)
        animator.duration = ANIMATION_DURATION.toLong()
        animator.repeatMode = ValueAnimator.REVERSE
        animator.repeatCount = ANIMATION_REPEAT_COUNT
        animator.start()
        animator.addUpdateListener { invalidate() }
    }

    fun cancelHighlighting() {
        brickPositionsToHighlight.clear()
        invalidate()
    }

    fun highlightControlStructureBricks(positions: Collection<Int>) {
        cancelHighlighting()
        brickPositionsToHighlight.addAll(positions)
        invalidate()
    }

    fun startMoving(brickToMove: Brick?) {
        dragMode = if (SettingsFragment.isOldDragEnabled(CatroidApplication.getAppContext())) {
            DragMode.LEGACY
        } else {
            DragMode.NEW
        }
        cancelMove()
        val flatList: MutableList<Brick> = ArrayList()
        brickToMove?.addToFlatList(flatList)
        if (brickToMove?.parent is CompositeBrick && brickToMove is EndBrick) {
            this.brickToMove = brickToMove
            flatList.clear()
        } else if (brickToMove !== flatList[0]) {
            return
        } else {
            this.brickToMove = flatList[0]
            flatList.removeAt(0)
        }

        upperScrollBound = height / 4
        lowerScrollBound = height * 3 / 4
        currentPositionOfHoveringBrick = brickAdapterInterface!!.getPosition(this.brickToMove)
        invalidateHoveringItem = true

        prepareHoveringItem(getChildAtVisiblePosition(currentPositionOfHoveringBrick))
        brickAdapterInterface?.setItemVisible(currentPositionOfHoveringBrick, false)

        if (!brickAdapterInterface!!.removeItems(flatList)) {
            invalidateViews()
        }
    }

    fun stopMoving() {
        brickAdapterInterface?.moveItemTo(currentPositionOfHoveringBrick, brickToMove)
        cancelMove()
    }

    fun cancelMove() {
        brickAdapterInterface?.setAllPositionsVisible()
        brickToMove = null
        hoveringDrawable = null
        motionEventId = -1
        finishRunningSwapAnimations()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (hoveringDrawable == null) {
            return super.onTouchEvent(event)
        }
        when (event.action) {
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> stopMoving()
            MotionEvent.ACTION_DOWN -> {
                downY = event.y
                motionEventId = event.getPointerId(0)
            }
            MotionEvent.ACTION_MOVE -> {
                val dY = event.y - downY
                downY += dY
                downY -= offsetToCenter.toFloat()
                viewBounds.offsetTo(viewBounds.left, downY.toInt())
                hoveringDrawable?.bounds = viewBounds
                invalidate()
                swapListItems()
                scrollWhileDragging()
            }
            MotionEvent.ACTION_POINTER_UP -> {
                val pointerIndex =
                    event.action and MotionEvent.ACTION_POINTER_INDEX_MASK shr MotionEvent.ACTION_POINTER_INDEX_SHIFT
                if (event.getPointerId(pointerIndex) == motionEventId) {
                    stopMoving()
                }
            }
        }
        return true
    }

    public override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        if (brickToMove != null || brickPositionsToHighlight.isNotEmpty()) {
            canvas.drawColor(translucentBlack)
        }
        if (invalidateHoveringItem) {
            val childAtVisiblePosition = getChildAtVisiblePosition(currentPositionOfHoveringBrick)
            if (childAtVisiblePosition != null) {
                invalidateHoveringItem = false
                prepareHoveringItem(childAtVisiblePosition)
            }
        }

        hoveringDrawable?.draw(canvas)

        for (pos in brickPositionsToHighlight) {
            if (pos in firstVisiblePosition..lastVisiblePosition) {
                drawHighlightedItem(getChildAtVisiblePosition(pos), canvas)
            }
        }
    }

    @VisibleForTesting
    fun drawHighlightedItem(view: View?, canvas: Canvas?) {
        if (view == null || canvas == null) {
            return
        }
        canvas.withTranslation(view.left.toFloat(), view.top.toFloat()) {
            view.draw(canvas)
        }
    }

    private fun prepareHoveringItem(view: View?) {
        if (view == null) {
            return
        }
        val bitmap = createBitmap(view.width, view.height)
        view.draw(Canvas(bitmap))

        viewBounds[view.left, view.top, view.right] = view.bottom
        val drawable = bitmap.toDrawable(resources)
        drawable.bounds = viewBounds
        hoveringDrawable = drawable
        setOffsetToCenter(viewBounds)
    }

    private fun setOffsetToCenter(viewBounds: Rect) {
        offsetToCenter = viewBounds.height() / 2
    }

    @Suppress("ComplexMethod")
    private fun swapListItems() {
        val itemPositionAbove = currentPositionOfHoveringBrick - 1
        val itemPositionBelow = currentPositionOfHoveringBrick + 1
        val itemBelow: View? = if (isPositionValid(itemPositionBelow)) getChildAtVisiblePosition(itemPositionBelow) else null
        val itemAbove: View? = if (isPositionValid(itemPositionAbove)) getChildAtVisiblePosition(itemPositionAbove) else null

        val hoverCenter = downY + viewBounds.height() / 2f
        val isAbove = itemBelow != null &&
            hoverCenter > itemBelow.top + itemBelow.translationY + itemBelow.height / 2f
        val isBelow = itemAbove != null &&
            hoverCenter < itemAbove.top + itemAbove.translationY + itemAbove.height / 2f

        if (isAbove || isBelow) {
            val swapWith = if (isAbove) itemPositionBelow else itemPositionAbove

            snapshotVisibleItemPositions()
            if (brickAdapterInterface?.onItemMove(currentPositionOfHoveringBrick, swapWith) == true) {
                brickAdapterInterface?.setItemVisible(currentPositionOfHoveringBrick, true)
                currentPositionOfHoveringBrick = swapWith
                brickAdapterInterface?.setItemVisible(currentPositionOfHoveringBrick, false)

                invalidateViews()
                animateItemsToTheirNewPositions()
            } else {
                itemPositionsBeforeSwap.clear()
            }
        }
    }

    private fun snapshotVisibleItemPositions() {
        itemPositionsBeforeSwap.clear()
        val listAdapter = adapter ?: return
        for (childIndex in 0 until childCount) {
            val position = firstVisiblePosition + childIndex
            if (position < 0 || position >= listAdapter.count) {
                continue
            }
            val child = getChildAt(childIndex) ?: continue
            val item = listAdapter.getItem(position) ?: continue
            itemPositionsBeforeSwap[item] = child.top + child.translationY
        }
    }

    private fun animateItemsToTheirNewPositions() {
        if (itemPositionsBeforeSwap.isEmpty()) {
            return
        }
        OneShotPreDrawListener.add(this) {
            val listAdapter = adapter
            if (listAdapter != null) {
                val movedChildren = ArrayList<View>()
                val offsets = ArrayList<Float>()
                for (childIndex in 0 until childCount) {
                    val position = firstVisiblePosition + childIndex
                    if (position < 0 || position >= listAdapter.count) {
                        continue
                    }
                    val child = getChildAt(childIndex) ?: continue
                    val item = listAdapter.getItem(position) ?: continue
                    val positionBeforeSwap = itemPositionsBeforeSwap[item] ?: continue
                    movedChildren.add(child)
                    offsets.add(positionBeforeSwap - child.top)
                }

                val scrollShift = estimateCommonOffset(offsets)
                for (index in movedChildren.indices) {
                    val offset = offsets[index] - scrollShift
                    if (abs(offset) >= MINIMUM_ANIMATED_OFFSET) {
                        animateChildFromOffset(movedChildren[index], offset)
                    }
                }
            }
            itemPositionsBeforeSwap.clear()
        }
    }

    private fun estimateCommonOffset(offsets: List<Float>): Float {
        if (offsets.size < MINIMUM_ROWS_FOR_SCROLL_ESTIMATE) {
            return 0f
        }
        var commonOffset = 0f
        var bestCount = 0
        for (candidate in offsets) {
            val count = offsets.count { abs(it - candidate) < MINIMUM_ANIMATED_OFFSET }
            if (count > bestCount || (count == bestCount && abs(candidate) < abs(commonOffset))) {
                commonOffset = candidate
                bestCount = count
            }
        }
        return if (bestCount > 2) commonOffset else 0f
    }

    private fun animateChildFromOffset(child: View, offset: Float) {
        stopSwapAnimation(child)

        child.translationY = offset
        child.setHasTransientState(true)

        val animator = ObjectAnimator.ofFloat(child, TRANSLATION_Y, 0f)
        animator.duration = SWAP_ANIMATION_DURATION.toLong()
        animator.interpolator = swapInterpolator
        animator.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) = Unit
            override fun onAnimationEnd(animation: Animator) {
                if (runningSwapAnimators[child] === animation) {
                    runningSwapAnimators.remove(child)
                }
                child.translationY = 0f
                child.setHasTransientState(false)
            }

            override fun onAnimationCancel(animation: Animator) = Unit
            override fun onAnimationRepeat(animation: Animator) = Unit
        })
        runningSwapAnimators[child] = animator
        animator.start()
    }

    private fun stopSwapAnimation(child: View) {
        runningSwapAnimators.remove(child)?.let { animator ->
            animator.cancel()
            child.translationY = 0f
            child.setHasTransientState(false)
        }
    }

    private fun finishRunningSwapAnimations() {
        if (runningSwapAnimators.isEmpty()) {
            itemPositionsBeforeSwap.clear()
            return
        }
        for (child in ArrayList(runningSwapAnimators.keys)) {
            stopSwapAnimation(child)
        }
        runningSwapAnimators.clear()
        itemPositionsBeforeSwap.clear()
    }

    override fun onDetachedFromWindow() {
        finishRunningSwapAnimations()
        super.onDetachedFromWindow()
    }

    private fun scrollWhileDragging() {
        val scrollSpeed: Int

        val referenceY = downY + viewBounds.height() / 2f

        when (dragMode) {
            DragMode.NEW -> {
                val scrollZoneSize = height / 5

                if (referenceY < upperScrollBound) {
                    val distance = (upperScrollBound - referenceY) / scrollZoneSize
                    scrollSpeed = (-SMOOTH_SCROLL_BY_NEW * distance).toInt()
                } else if (referenceY > lowerScrollBound) {
                    val distance = (referenceY - lowerScrollBound) / scrollZoneSize
                    scrollSpeed = (SMOOTH_SCROLL_BY_NEW * distance).toInt()
                } else {
                    scrollSpeed = 0
                }
            }
            DragMode.LEGACY -> {
                scrollSpeed = when {
                    referenceY > lowerScrollBound -> SMOOTH_SCROLL_BY
                    referenceY < upperScrollBound -> -SMOOTH_SCROLL_BY
                    else -> 0
                }
            }
        }

        if (scrollSpeed != 0) {
            smoothScrollBy(scrollSpeed, 0)
        }
    }

    private fun getChildAtVisiblePosition(positionInAdapter: Int): View? =
        getChildAt(positionInAdapter - firstVisiblePosition)

    private fun isPositionValid(position: Int): Boolean = position in 0 until count

    override fun setAdapter(adapter: ListAdapter) {
        require(adapter is BrickAdapterInterface) { "Adapter has to implement the BrickListView.AdapterInterface." }
        super.setAdapter(adapter)
        brickAdapterInterface = adapter
    }
}
