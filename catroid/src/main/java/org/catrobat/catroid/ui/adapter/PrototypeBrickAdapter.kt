package org.catrobat.catroid.ui.adapter

import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.BaseAdapter
import android.widget.LinearLayout
import org.catrobat.catroid.content.bricks.Brick
import org.catrobat.catroid.ui.recyclerview.util.IndentedBrickLayout

class PrototypeBrickAdapter(private var brickList: List<Brick>) : BaseAdapter() {

    private val viewCache = HashMap<Int, View>()
    private val itemsToAnimate = HashMap<Brick, Long>()
    private var boundListView: ViewGroup? = null

    var isScrolling: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                if (value) {
                    itemsToAnimate.clear()
                }
            }
        }

    init {
        var newItemsCount = 0
        for (brick in brickList) {
            itemsToAnimate[brick] = minOf(newItemsCount * 30L, 250L)
            newItemsCount++
        }
    }

    override fun getCount(): Int = brickList.size

    override fun getItem(position: Int): Brick = brickList[position]

    override fun getItemId(position: Int): Long = brickList[position].hashCode().toLong()

    override fun hasStableIds(): Boolean = true

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View? {
        if (parent != null && parent !== boundListView) {
            viewCache.clear()
            boundListView = parent
        }

        val brick = brickList[position]
        val cacheKey = brick.hashCode()

        var cachedView = viewCache[cacheKey]
        if (cachedView == null) {
            cachedView = brick.getPrototypeView(parent?.context)
            if (cachedView != null) {
                viewCache[cacheKey] = cachedView
            }
        } else {
            val viewParent = cachedView.parent as? ViewGroup
            if (viewParent != null && viewParent !is android.widget.AdapterView<*>) {
                viewParent.removeView(cachedView)
            }
        }

        if (cachedView != null) {
            if (!isScrolling && itemsToAnimate.containsKey(brick)) {
                val delay = itemsToAnimate.remove(brick) ?: 0L

                cachedView.alpha = 0f
                cachedView.scaleX = 0.93f
                cachedView.scaleY = 0.93f

                cachedView.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setStartDelay(delay)
                    .setDuration(200)
                    .setInterpolator(DecelerateInterpolator())
                    .start()
            } else {
                cachedView.animate().cancel()
                cachedView.alpha = 1f
                cachedView.scaleX = 1f
                cachedView.scaleY = 1f
                cachedView.translationX = 0f
                cachedView.translationY = 0f
                itemsToAnimate.remove(brick)
            }
        }

        val context = parent?.context ?: return cachedView

        if (cachedView != null) {
            val depth = getBrickDepth(position)
            if (depth > 0) {
                val existingParent = cachedView.parent
                if (existingParent is IndentedBrickLayout) {
                    existingParent.setDepth(depth)
                    return existingParent
                } else {
                    val parentGroup = existingParent as? ViewGroup
                    if (parentGroup != null && parentGroup !is android.widget.AdapterView<*>) {
                        parentGroup.removeView(cachedView)
                    }

                    val indentedLayout = IndentedBrickLayout(context, depth)
                    indentedLayout.layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    indentedLayout.addView(
                        cachedView,
                        LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        )
                    )
                    return indentedLayout
                }
            }
        }

        if (cachedView != null) {
            val existingParent = cachedView.parent
            if (existingParent is IndentedBrickLayout) {
                existingParent.removeView(cachedView)
            }
        }

        return cachedView
    }

    fun replaceList(newList: List<Brick>, visibleBricksBefore: Set<Brick> = emptySet()) {
        itemsToAnimate.clear()
        var newItemsCount = 0

        if (!isScrolling) {
            for (brick in newList) {
                if (!visibleBricksBefore.contains(brick)) {
                    val delay = minOf(newItemsCount * 30L, 250L)
                    itemsToAnimate[brick] = delay
                    newItemsCount++
                }
            }
        }

        brickList = newList
        notifyDataSetChanged()
    }

    fun clearCache() {
        viewCache.clear()
        itemsToAnimate.clear()
        boundListView = null
    }

    private fun getBrickDepth(position: Int): Int {
        val currentBrick = brickList[position]
        if (currentBrick is org.catrobat.catroid.content.bricks.SubCategoryHeaderBrick) {
            return 0
        }

        for (i in position - 1 downTo 0) {
            if (brickList[i] is org.catrobat.catroid.content.bricks.SubCategoryHeaderBrick) {
                return 1
            }
        }
        return 0
    }
}
