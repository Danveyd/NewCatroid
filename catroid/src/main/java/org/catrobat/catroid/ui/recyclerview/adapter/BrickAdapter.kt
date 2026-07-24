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
 * An additional term exception under section section 3 of the GNU Affero
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
package org.catrobat.catroid.ui.recyclerview.adapter

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.drawable.Drawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RectShape
import android.view.LayoutInflater
import android.graphics.Typeface
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AdapterView.OnItemLongClickListener
import android.widget.BaseAdapter
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.IntDef
import org.catrobat.catroid.R
import org.catrobat.catroid.ai.KoveAutocompleteController
import org.catrobat.catroid.codeanalysis.AnalysisManager
import org.catrobat.catroid.codeanalysis.Severity
import org.catrobat.catroid.content.Script
import org.catrobat.catroid.content.Sprite
import org.catrobat.catroid.content.bricks.Brick
import org.catrobat.catroid.content.bricks.Brick.BrickField
import org.catrobat.catroid.content.bricks.CompositeBrick
import org.catrobat.catroid.content.bricks.EmptyEventBrick
import org.catrobat.catroid.content.bricks.EndBrick
import org.catrobat.catroid.content.bricks.FormulaBrick
import org.catrobat.catroid.content.bricks.ListSelectorBrick
import org.catrobat.catroid.content.bricks.ScriptBrick
import org.catrobat.catroid.content.bricks.SetParticleColorBrick
import org.catrobat.catroid.content.bricks.UserDefinedReceiverBrick
import org.catrobat.catroid.ui.dragndrop.BrickAdapterInterface
import org.catrobat.catroid.ui.recyclerview.adapter.draganddrop.ViewStateManager
import org.catrobat.catroid.ui.recyclerview.adapter.multiselection.MultiSelectionManager
import org.catrobat.catroid.ui.recyclerview.util.IndentedBrickLayout
import java.util.ArrayList
import java.util.Collections

class BrickAdapter(private val sprite: Sprite) :
    BaseAdapter(),
    BrickAdapterInterface,
    AdapterView.OnItemClickListener,
    OnItemLongClickListener {

    @Retention(AnnotationRetention.SOURCE)
    @IntDef(NONE, ALL, SCRIPTS_ONLY, CONNECTED_ONLY)
    internal annotation class CheckBoxMode

    interface OnScriptChangedListener {
        fun onScriptChanged()
    }

    private var scriptChangedListener: OnScriptChangedListener? = null

    fun setOnScriptChangedListener(listener: OnScriptChangedListener?) {
        this.scriptChangedListener = listener
    }

    @CheckBoxMode
    private var checkBoxMode = NONE

    private var scripts: MutableList<Script> = ArrayList()
    private var firstConnectedItem = -1
    private var lastConnectedItem = -1

    private val selectionManager = MultiSelectionManager()
    private val viewStateManager = ViewStateManager()

    private var onItemClickListener: OnBrickClickListener? = null
    private var selectionListener: SelectionListener? = null

    val items: MutableList<Brick> = ArrayList()

    private var useIndentationCached: Boolean? = null

    private val brickDepthCache = HashMap<Int, Int>()

    init {
        updateItems(sprite)
    }

    companion object {
        const val DISABLED_BRICK_ALPHA = .8f
        const val NONE = 0
        const val ALL = 1
        const val SCRIPTS_ONLY = 2
        const val CONNECTED_ONLY = 3

        @JvmStatic
        fun colorAsCommentedOut(background: Drawable) {
            val matrix = ColorMatrix()
            matrix.setSaturation(0f)
            val filter = ColorMatrixColorFilter(matrix)
            background.mutate()
            background.colorFilter = filter
        }
    }

    private class BrickViewHolder(val itemView: View) {
        val textViews: List<TextView> by lazy {
            val list = ArrayList<TextView>()
            findTextViewsRecursive(itemView, list)
            list
        }

        var toggleWrapper: View? = null
        var collapseToggle: TextView? = null
        var originalContainer: View? = null
        var lastAppliedSeverity: Severity? = null
        var lastIsCommentedOutOrEmpty: Boolean? = null
        var lastIsCollapsed: Boolean? = null

        private fun findTextViewsRecursive(view: View, list: MutableList<TextView>) {
            if (view is TextView) {
                list.add(view)
            } else if (view is ViewGroup) {
                for (i in 0 until view.childCount) {
                    findTextViewsRecursive(view.getChildAt(i), list)
                }
            }
        }
    }

    fun setOnItemClickListener(onItemClickListener: OnBrickClickListener?) {
        this.onItemClickListener = onItemClickListener
    }

    fun setSelectionListener(selectionListener: SelectionListener?) {
        this.selectionListener = selectionListener
    }

    fun setCheckBoxMode(checkBoxMode: Int) {
        this.checkBoxMode = checkBoxMode
        notifyDataSetChanged()
    }

    fun updateItems(sprite: Sprite?) {
        sprite?.scriptList?.let { scripts = it }
        updateItemsFromCurrentScripts()
    }

    private fun updateItemsFromCurrentScripts() {
        items.clear()
        sprite.removeAllEmptyScriptBricks()

        val tempItems = ArrayList<Brick>()
        for (script in scripts) {
            script.setParents()
            script.addToFlatList(tempItems)
        }

        for (brick in tempItems) {
            if (isBrickVisibleInCollapsedHierarchy(brick)) {
                items.add(brick)
            }
        }

        notifyDataSetChanged()
        scriptChangedListener?.onScriptChanged()
    }

    private fun isBrickVisibleInCollapsedHierarchy(brick: Brick): Boolean {
        var currentParent = brick.parent
        while (currentParent != null) {
            if (org.catrobat.catroid.utils.BrickCollapseManager.isCollapsed(currentParent)) {
                return false
            }
            currentParent = currentParent.parent
        }
        return true
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val item = items[position]

        if (item is SetParticleColorBrick) {
            val colorFormula = item.getFormulaWithBrickField(BrickField.COLOR, true)
                ?: return createUnknownView("NoneBrick", parent)
        }

        val itemView = item.getView(parent.context) as ViewGroup

        val holder = itemView.tag as? BrickViewHolder ?: BrickViewHolder(itemView).also { itemView.tag = it }

        val result = AnalysisManager.getResultFor(item)
        val currentSeverity = result?.severity

        if (holder.lastAppliedSeverity != currentSeverity) {
            if (currentSeverity == null) {
                clearHighlightsOptimized(holder)
            } else {
                applyHighlightOptimized(holder, currentSeverity)
            }
            holder.lastAppliedSeverity = currentSeverity
        }

        val baseAlpha = if (viewStateManager.isEnabled(position)) 1F else DISABLED_BRICK_ALPHA
        val targetAlpha = if (item.isPhantom) baseAlpha * 0.5f else baseAlpha
        if (itemView.alpha != targetAlpha) {
            itemView.alpha = targetAlpha
        }

        var brickViewContainer = itemView.getChildAt(1)
        if (item is UserDefinedReceiverBrick) {
            brickViewContainer = (itemView.getChildAt(1) as ViewGroup).getChildAt(0)
        }

        val isCommentedOutOrEmpty = item.isCommentedOut || item is EmptyEventBrick
        if (holder.lastIsCommentedOutOrEmpty != isCommentedOutOrEmpty) {
            val background = brickViewContainer.background
            if (isCommentedOutOrEmpty) {
                colorAsCommentedOut(background)
            } else {
                background.clearColorFilter()
            }
            holder.lastIsCommentedOutOrEmpty = isCommentedOutOrEmpty
        }

        if (item is CompositeBrick) {
            val itemViewViewGroup = itemView as? ViewGroup
            if (itemViewViewGroup != null) {
                val isCollapsed = org.catrobat.catroid.utils.BrickCollapseManager.isCollapsed(item)
                val density = parent.context.resources.displayMetrics.density

                var toggleWrapper = holder.toggleWrapper
                var toggleBtn = holder.collapseToggle

                if (toggleWrapper == null) {
                    toggleWrapper = itemViewViewGroup.findViewWithTag<View>("toggle_wrapper")
                    if (toggleWrapper != null) {
                        holder.toggleWrapper = toggleWrapper
                        toggleBtn = toggleWrapper.findViewWithTag<TextView>("collapse_toggle")
                        holder.collapseToggle = toggleBtn
                    }
                }

                if (toggleWrapper == null) {
                    val originalContainer = itemViewViewGroup.getChildAt(1)

                    originalContainer.setPadding(
                        originalContainer.paddingLeft,
                        originalContainer.paddingTop,
                        originalContainer.paddingRight + (42 * density).toInt(),
                        originalContainer.paddingBottom
                    )

                    toggleWrapper = android.widget.RelativeLayout(parent.context).apply {
                        tag = "toggle_wrapper"
                        layoutParams = originalContainer.layoutParams
                    }

                    itemViewViewGroup.removeViewAt(1)

                    originalContainer.layoutParams = android.widget.RelativeLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )

                    val toggleView = TextView(parent.context).apply {
                        tag = "collapse_toggle"
                        textSize = 16f
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        setTextColor(Color.WHITE)
                        gravity = Gravity.CENTER

                        layoutParams = android.widget.RelativeLayout.LayoutParams(
                            (70 * density).toInt(),
                            ViewGroup.LayoutParams.MATCH_PARENT
                        ).apply {
                            addRule(android.widget.RelativeLayout.ALIGN_PARENT_RIGHT)
                            addRule(android.widget.RelativeLayout.CENTER_VERTICAL)
                        }

                        setOnClickListener {
                            org.catrobat.catroid.utils.BrickCollapseManager.toggleCollapsed(item)
                            updateItemsFromCurrentScripts()
                        }
                    }

                    toggleWrapper.addView(originalContainer)
                    toggleWrapper.addView(toggleView)

                    itemViewViewGroup.addView(toggleWrapper, 1)

                    holder.toggleWrapper = toggleWrapper
                    holder.collapseToggle = toggleView
                    holder.originalContainer = originalContainer
                    toggleBtn = toggleView
                }

                if (holder.lastIsCollapsed != isCollapsed) {
                    toggleBtn?.text = if (isCollapsed) "[＋]" else "[－]"
                    itemView.alpha = if (isCollapsed) baseAlpha * 0.82f else baseAlpha
                    holder.lastIsCollapsed = isCollapsed
                }
            }
        }

        checkBoxClickListener(item, itemView as ViewGroup, position)
        item.checkBox.isChecked = selectionManager.isPositionSelected(position)
        item.checkBox.isEnabled = viewStateManager.isEnabled(position)

        val useIndentation = getUseIndentation(parent.context)
        var rootView: View = itemView

        if (useIndentation) {
            val depth = getBrickDepthCached(item)
            val existingParent = itemView.parent
            if (existingParent is IndentedBrickLayout) {
                val lastDepth = existingParent.tag as? Int
                if (lastDepth != depth) {
                    existingParent.setDepth(depth)
                    existingParent.tag = depth
                }
                rootView = existingParent
            } else {
                (existingParent as? ViewGroup)?.takeIf { it !is AdapterView<*> }?.removeView(itemView)

                val indentedLayout = IndentedBrickLayout(parent.context, depth)
                indentedLayout.layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                indentedLayout.tag = depth
                indentedLayout.addView(
                    itemView,
                    LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                )

                if (indentedLayout.measuredWidth == 0 || indentedLayout.measuredHeight == 0) {
                    val parentWidth = if (parent.width > 0) parent.width else parent.resources.displayMetrics.widthPixels
                    val widthSpec = View.MeasureSpec.makeMeasureSpec(parentWidth, View.MeasureSpec.EXACTLY)
                    val heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
                    indentedLayout.measure(widthSpec, heightSpec)
                    indentedLayout.layout(0, 0, indentedLayout.measuredWidth, indentedLayout.measuredHeight)
                }

                rootView = indentedLayout
            }
        } else {
            val existingParent = itemView.parent
            if (existingParent is IndentedBrickLayout) {
                existingParent.removeView(itemView)
            }
        }

        if (itemView.translationY != 0f || itemView.translationX != 0f) {
            itemView.animate().cancel()
            itemView.translationY = 0f
            itemView.translationX = 0f
        }
        if (rootView !== itemView && (rootView.translationY != 0f || rootView.translationX != 0f)) {
            rootView.animate().cancel()
            rootView.translationY = 0f
            rootView.translationX = 0f
        }

        itemView.visibility = View.VISIBLE
        rootView.visibility = if (viewStateManager.isVisible(position)) View.VISIBLE else View.INVISIBLE

        return rootView
    }

    private fun clearHighlightsOptimized(holder: BrickViewHolder) {
        val textViews = holder.textViews
        for (i in textViews.indices) {
            textViews[i].foreground = null
        }
    }

    private fun applyHighlightOptimized(holder: BrickViewHolder, severity: Severity) {
        val textViews = holder.textViews
        val context = holder.itemView.context
        val color = if (severity == Severity.ERROR) 0xFFFF4444.toInt() else 0xFFFFBB33.toInt()
        val density = context.resources.displayMetrics.density

        for (i in textViews.indices) {
            val child = textViews[i]
            val underline = object : ShapeDrawable(RectShape()) {
                init {
                    paint.color = color
                }

                override fun draw(canvas: android.graphics.Canvas) {
                    val b = bounds
                    canvas.drawRect(
                        b.left.toFloat(),
                        (b.bottom - (2f * density)),
                        b.right.toFloat(),
                        b.bottom.toFloat(),
                        paint
                    )
                }
            }
            child.foreground = underline
        }
    }

    private fun createUnknownView(className: String, container: ViewGroup): View {
        return LayoutInflater.from(container.context).inflate(R.layout.brick_none, container, false)
    }

    private fun checkBoxClickListener(item: Brick, itemView: ViewGroup, position: Int) {
        item.checkBox.setOnClickListener { onCheckBoxClick(position) }

        if (item.isPhantom) {
            item.checkBox.visibility = View.GONE
            if (item is FormulaBrick) {
                item.disableSpinners()
            }
            return
        }

        when (checkBoxMode) {
            NONE -> handleCheckBoxModeNone(item)
            CONNECTED_ONLY -> handleCheckBoxModeConnectedOnly(item, itemView, position)
            ALL -> handleCheckBoxModeAll(item)
            SCRIPTS_ONLY -> handleCheckBoxModeScriptsOnly(item)
        }
    }

    private fun handleCheckBoxModeScriptsOnly(item: Brick) {
        val isScriptBrick = item is ScriptBrick
        item.checkBox.visibility = if (isScriptBrick) View.VISIBLE else View.INVISIBLE
        item.disableSpinners()
    }

    private fun handleCheckBoxModeAll(item: Brick) {
        item.checkBox.visibility = View.VISIBLE
        item.disableSpinners()
    }

    private fun handleCheckBoxModeNone(item: Brick) {
        item.checkBox.visibility = View.GONE
        if (item is FormulaBrick) {
            item.setClickListeners()
        } else if (item is ListSelectorBrick) {
            item.setClickListeners()
        }
    }

    private fun handleCheckBoxModeConnectedOnly(item: Brick, itemView: ViewGroup, position: Int) {
        if (item is UserDefinedReceiverBrick) {
            viewStateManager.setEnabled(false, position)
            itemView.alpha = DISABLED_BRICK_ALPHA
        }
        item.checkBox.visibility = View.VISIBLE
        item.disableSpinners()
    }

    override fun onItemClick(parent: AdapterView<*>?, view: View, position: Int, id: Long) {
        if (checkBoxMode == NONE) {
            val item = items[position]

            if (item.isPhantom) {
                KoveAutocompleteController.getInstance().acceptSuggestions()
                return
            }

            onItemClickListener?.onBrickClick(item, position)
        }
    }

    override fun onItemLongClick(
        parent: AdapterView<*>?,
        view: View,
        position: Int,
        id: Long
    ): Boolean {
        if (checkBoxMode == NONE) {
            val item = items[position]
            if (item.isPhantom) {
                return true
            }
            onItemClickListener?.onBrickLongClick(item, position)
            return true
        }
        return false
    }

    private fun onCheckBoxClick(position: Int) {
        val selected = !selectionManager.isPositionSelected(position)
        setSelectionTo(selected, position)
        selectionListener?.onSelectionChanged(selectionManager.selectedPositions.size)
        notifyDataSetChanged()
    }

    private fun setSelectionTo(selected: Boolean, position: Int) {
        val item = items[position]

        val flatItems: List<Brick> = ArrayList()
        item.addToFlatList(flatItems)

        val scriptSelected = item is ScriptBrick
        var adapterPosition = -1

        if (selected && noConnectedItemsSelected()) {
            firstConnectedItem = position - 1
            lastConnectedItem = position + 1
        }

        val positionIndex = buildPositionIndex()
        for (i in flatItems.indices) {
            adapterPosition = positionIndex[flatItems[i]] ?: items.indexOf(flatItems[i])
            selectionManager.setSelectionTo(selected, adapterPosition)
            if (i > 0) {
                viewStateManager.setEnabled(!selected, adapterPosition)
            }
        }

        if (checkBoxMode == CONNECTED_ONLY) {
            val firstFlatListPosition = positionIndex[flatItems[0]] ?: items.indexOf(flatItems[0])
            updateConnectedItems(
                position,
                firstFlatListPosition,
                adapterPosition,
                selected,
                scriptSelected
            )
        }
    }

    private fun buildPositionIndex(): Map<Brick, Int> {
        val map = HashMap<Brick, Int>(items.size)
        for (i in items.indices) {
            map[items[i]] = i
        }
        return map
    }

    private fun updateConnectedItems(
        selectedPosition: Int,
        firstFlatListPosition: Int,
        lastFlatListPosition: Int,
        selected: Boolean,
        scriptSelected: Boolean
    ) {
        if (selected) {
            if (lastFlatListPosition >= lastConnectedItem) {
                lastConnectedItem = lastFlatListPosition + 1
            }
            if (firstFlatListPosition <= firstConnectedItem) {
                firstConnectedItem = firstFlatListPosition - 1
            }
        } else {
            if (selectedPosition == firstConnectedItem + 1) {
                firstConnectedItem = firstFlatListPosition
            }
            if (selectedPosition == lastConnectedItem - 1) {
                lastConnectedItem = firstFlatListPosition
            }
            if (selectionManager.selectedPositions.isEmpty()) {
                clearConnectedItems()
            }
        }
        for (brickPosition in items.indices) {
            viewStateManager.setEnabled(
                selectableForCopy(brickPosition, scriptSelected),
                brickPosition
            )
        }
    }

    private fun selectableForCopy(brickPosition: Int, scriptSelected: Boolean): Boolean =
        noConnectedItemsSelected() || isItemWithinConnectedRange(
            brickPosition,
            scriptSelected
        ) && !isItemOfNewScript(brickPosition, scriptSelected)

    private fun isItemWithinConnectedRange(brickPosition: Int, scriptSelected: Boolean): Boolean {
        return brickPosition >= firstConnectedItem && brickPosition <= firstConnectedItem + 1 ||
                brickPosition <= lastConnectedItem && brickPosition >= lastConnectedItem - 1 && !scriptSelected
    }

    private fun isItemOfNewScript(brickPosition: Int, scriptSelected: Boolean): Boolean {
        return lastConnectedItem == brickPosition && items[brickPosition] is ScriptBrick ||
                scriptSelected && brickPosition <= firstConnectedItem
    }

    private fun noConnectedItemsSelected(): Boolean =
        firstConnectedItem == -1 && lastConnectedItem == -1

    private fun clearConnectedItems() {
        firstConnectedItem = -1
        lastConnectedItem = -1
    }

    val selectedItems: List<Brick>
        get() {
            val selectedItems: MutableList<Brick> = ArrayList()
            for (position in selectionManager.selectedPositions) {
                selectedItems.add(items[position])
            }
            return selectedItems
        }

    fun clearSelection() {
        selectionManager.clearSelection()
        viewStateManager.clearDisabledPositions()
        clearConnectedItems()
        notifyDataSetChanged()
    }

    override fun setItemVisible(position: Int, visible: Boolean) {
        viewStateManager.setVisible(position, visible)
    }

    override fun setAllPositionsVisible() {
        viewStateManager.setAllPositionsVisible()
    }

    fun selectAllCommentedOutBricks() {
        for (i in items.indices) {
            setSelectionTo(items[i].isCommentedOut, i)
        }
        notifyDataSetChanged()
    }

    fun addItem(position: Int, item: Brick?) {
        item?.let { items.add(position, it) }
        notifyDataSetChanged()
    }

    override fun getItem(position: Int): Brick = items[position]

    fun findByHash(hashCode: Int): Brick? {
        for (item in items) {
            if (item.hashCode() == hashCode) {
                return item
            }
        }
        return null
    }

    override fun removeItems(items: List<Brick>): Boolean {
        if (this.items.removeAll(items)) {
            notifyDataSetChanged()
            return true
        }
        return false
    }

    private fun getUseIndentation(context: android.content.Context): Boolean {
        if (useIndentationCached == null) {
            val prefs = android.preference.PreferenceManager.getDefaultSharedPreferences(context)
            useIndentationCached = prefs.getBoolean("pref_enable_brick_indentation", false)
        }
        return useIndentationCached!!
    }

    private fun cacheBrickProperties() {
        brickDepthCache.clear()
        for (item in items) {
            brickDepthCache[item.hashCode()] = calculateBrickDepth(item)
        }
    }

    private fun getBrickDepthCached(brick: Brick): Int {
        return brickDepthCache[brick.hashCode()] ?: calculateBrickDepth(brick)
    }

    private fun calculateBrickDepth(brick: Brick): Int {
        var depth = 0
        var currentParent = brick.parent
        while (currentParent != null) {
            if (!isElseBrick(currentParent)) {
                depth++
            }
            currentParent = currentParent.parent
        }

        if (depth > 0 && isEndOrElseBrick(brick)) {
            depth--
        }
        return depth
    }

    override fun notifyDataSetChanged() {
        useIndentationCached = null
        cacheBrickProperties()
        super.notifyDataSetChanged()
    }

    override fun getPosition(brick: Brick?): Int = items.indexOf(brick)

    override fun onItemMove(sourcePosition: Int, targetPosition: Int): Boolean {
        if (sourcePosition < 0 || sourcePosition >= items.size) return false
        if (targetPosition < 0 || targetPosition >= items.size) return false

        val source = items[sourcePosition]
        if (source !is ScriptBrick && targetPosition == 0) {
            return false
        }
        if (source !is EndBrick && source.allParts.contains(items[targetPosition])) {
            return false
        }
        Collections.swap(items, sourcePosition, targetPosition)
        return true
    }

    private fun getParentBrickInDragAndDropList(
        brickAboveTarget: Brick,
        enclosureBrick: Brick
    ): Pair<Brick, Int>? {

        if (brickAboveTarget == enclosureBrick) {
            return brickAboveTarget to 0
        }

        var brickInEnclosure = brickAboveTarget
        while (brickInEnclosure.parent !== null &&
            brickInEnclosure.parent !in enclosureBrick.allParts &&
            brickInEnclosure !in enclosureBrick.dragAndDropTargetList
        ) {

            brickInEnclosure = brickInEnclosure.parent
        }

        if (brickInEnclosure.parent !== enclosureBrick &&
            brickInEnclosure !in enclosureBrick.dragAndDropTargetList
        ) {
            return null
        }

        return brickInEnclosure to
                enclosureBrick.dragAndDropTargetList.indexOf(brickInEnclosure) + 1
    }

    private fun moveEndIntoExtendedSection(
        position: Int,
        endBrick: Brick,
        brickAboveTargetPosition: Brick
    ): Boolean {
        var tmpParent = brickAboveTargetPosition
        val firstPart = endBrick.parent
        while (tmpParent.parent != null) {
            if (tmpParent is CompositeBrick) {
                moveItemTo(position, firstPart)
                return true
            }
            tmpParent = tmpParent.parent
            if (tmpParent !is CompositeBrick || tmpParent == firstPart.parent) {
                break
            }
        }
        return false
    }

    private fun moveEndTo(position: Int, endBrick: Brick, brickAboveTargetPosition: Brick) {
        if (endBrick.script !== brickAboveTargetPosition.script) {
            return
        }

        var startBrick = endBrick.parent
        var enclosure = (startBrick as CompositeBrick).nestedBricks
        if (startBrick.hasSecondaryList()) {
            enclosure = startBrick.secondaryNestedBricks
            startBrick = startBrick.allParts[1]
        }

        val (parentOfBrickAboveTargetPosition, destinationPosition) =
            getParentBrickInDragAndDropList(brickAboveTargetPosition, startBrick)
                ?: (null to 0)

        val indexStartBrick = getPosition(startBrick)
        if (getPosition(brickAboveTargetPosition) + 1 < indexStartBrick) {
            return
        }

        if (parentOfBrickAboveTargetPosition == null) {
            if (moveEndIntoExtendedSection(position, endBrick, brickAboveTargetPosition)) {
                return
            }

            var (outsideParent, outPosition) =
                getParentBrickInDragAndDropList(
                    brickAboveTargetPosition,
                    endBrick.parent.parent
                )
                    ?: return
            outsideParent = outsideParent.parent

            val startIndex =
                outsideParent.dragAndDropTargetList.indexOf(endBrick.parent) + 1
            for (i in startIndex until outPosition) {
                val brick = outsideParent.dragAndDropTargetList.removeAt(startIndex)
                brick.parent = startBrick
                enclosure.add(brick)
            }
        } else {
            val parentOfCompBrick = endBrick.parent.parent
            val positionInList = parentOfCompBrick.dragAndDropTargetList.indexOf(endBrick.parent)
            for (index in (destinationPosition until enclosure.size).withIndex()) {
                val brick = enclosure.removeAt(destinationPosition)
                brick.parent = parentOfCompBrick
                parentOfCompBrick.dragAndDropTargetList.add(positionInList + index.index + 1, brick)
            }
        }
    }

    override fun moveItemTo(position: Int, itemToMove: Brick?) {
        val brickAboveTargetPosition = getBrickAbovePosition(position)

        if (itemToMove is ScriptBrick) {
            moveScript(itemToMove, brickAboveTargetPosition)
        } else if (itemToMove is EndBrick) {
            moveEndTo(position, itemToMove, brickAboveTargetPosition)
        } else {
            for (script in scripts) {
                script.removeBrick(itemToMove)
            }
            val destinationPosition = brickAboveTargetPosition.positionInDragAndDropTargetList + 1
            val destinationList = brickAboveTargetPosition.dragAndDropTargetList

            if (destinationPosition < destinationList.size) {
                destinationList.add(destinationPosition, itemToMove)
            } else {
                destinationList.add(itemToMove)
            }
        }
        updateItemsFromCurrentScripts()
    }

    private fun moveScript(itemToMove: ScriptBrick, brickAboveTargetPosition: Brick) {
        val scriptToMove = itemToMove.script
        val scriptAtTargetPosition = brickAboveTargetPosition.script
        val bricksInScriptToMove = scriptToMove.brickList
        val bricksInScriptAtTargetPosition = scriptAtTargetPosition.brickList

        val divideScriptAtPositionAndAddBricksToMovingScript =
            bricksInScriptToMove.isEmpty() && bricksInScriptAtTargetPosition.isNotEmpty()

        if (divideScriptAtPositionAndAddBricksToMovingScript) {
            val positionToDivideScriptAt = brickAboveTargetPosition.positionInScript + 1
            val bricksToMove: MutableList<Brick> = ArrayList()

            for (i in positionToDivideScriptAt until bricksInScriptAtTargetPosition.size) {
                bricksToMove.add(bricksInScriptAtTargetPosition[i])
            }

            bricksInScriptToMove.addAll(bricksToMove)
            bricksInScriptAtTargetPosition.removeAll(bricksToMove)
        }

        scripts.remove(scriptToMove)
        val destinationPosition = scripts.indexOf(scriptAtTargetPosition) + 1

        if (destinationPosition == scripts.size) {
            scripts.add(scriptToMove)
        } else {
            scripts.add(destinationPosition, scriptToMove)
        }
    }

    private fun getBrickAbovePosition(position: Int): Brick {
        var position = position
        if (position > 0) {
            position--
        }
        return items[position]
    }

    private fun isElseBrick(brick: Brick): Boolean {
        if (brick is CompositeBrick) return false
        val name = brick.javaClass.simpleName
        return name.endsWith("ElseBrick") || name.contains("Else")
    }

    private fun isEndOrElseBrick(brick: Brick): Boolean {
        if (brick is CompositeBrick) return false
        if (brick is EndBrick) return true
        val name = brick.javaClass.simpleName
        return name.endsWith("EndBrick") || name.endsWith("ElseBrick") || name.endsWith("ElseIfSeparatorBrick")
    }

    override fun getCount(): Int = items.size

    override fun getItemId(position: Int): Long = items[position].hashCode().toLong()

    interface SelectionListener {
        fun onSelectionChanged(selectedItemCnt: Int)
    }

    interface OnBrickClickListener {
        fun onBrickClick(item: Brick, position: Int)
        fun onBrickLongClick(item: Brick, position: Int): Boolean
    }
}
