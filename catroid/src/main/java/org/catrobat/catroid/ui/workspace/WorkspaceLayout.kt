package org.catrobat.catroid.ui.workspace

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.LinearLayout.HORIZONTAL
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import org.catrobat.catroid.R
import org.catrobat.catroid.ui.recyclerview.fragment.LookListFragment
import org.catrobat.catroid.ui.recyclerview.fragment.SceneListFragment
import org.catrobat.catroid.ui.recyclerview.fragment.ScriptFragment
import org.catrobat.catroid.ui.recyclerview.fragment.SoundListFragment
import org.catrobat.catroid.ui.recyclerview.fragment.SpriteListFragment
import androidx.core.graphics.toColorInt
import androidx.core.content.edit
import kotlin.math.abs

class WorkspaceLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val sharedPrefs = context.getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
    private val activeWindows = mutableMapOf<String, WindowContainer>()

    private lateinit var sidebar: LinearLayout
    private var isSidebarOpen = false
    private lateinit var menuButton: FrameLayout

    var focusedWindowTag: String? = null
    private var isLayoutRestored = false
    private var isRestoring = false

    private val minWidth get() = dpToPx(240)
    private val minHeight get() = dpToPx(180)

    private var isReplacing = false

    private val ghostPreview: View by lazy {
        View(context).apply {
            val shape = GradientDrawable().apply {
                setColor("#22A8DFF4".toColorInt())
                setStroke(dpToPx(2), ContextCompat.getColor(context, R.color.accent))
                cornerRadius = dpToPx(16).toFloat()
            }
            background = shape
            visibility = GONE
            isClickable = false
            isFocusable = false
        }
    }

    private val fragmentLifecycleCallbacks = object : androidx.fragment.app.FragmentManager.FragmentLifecycleCallbacks() {
        override fun onFragmentDetached(fm: androidx.fragment.app.FragmentManager, f: Fragment) {
            super.onFragmentDetached(fm, f)
            if (isReplacing) return
            val tag = f.tag
            if (tag != null && activeWindows.containsKey(tag)) {
                removeWindowViewOnly(tag)
            }
        }
    }

    var isProgrammaticBackPress = false

    init {
        val activity = context as? androidx.fragment.app.FragmentActivity
        activity?.supportFragmentManager?.registerFragmentLifecycleCallbacks(fragmentLifecycleCallbacks, false)

        setupSidebar()
        addView(ghostPreview)
    }

    private fun getLocalizedString(key: String): String {
        @Suppress("DEPRECATION")
        val isRussian = context.resources.configuration.locale.language == "ru"
        return when (key) {
            "workspace_title" -> if (isRussian) "ИНСТРУМЕНТЫ" else "WORKSPACE TOOLS"
            "scripts" -> if (isRussian) "Скрипты" else "Scripts"
            "looks" -> if (isRussian) "Образы" else "Looks"
            "sounds" -> if (isRussian) "Звуки" else "Sounds"
            "categories" -> if (isRussian) "Категории блоков" else "Block Categories"
            "add_brick" -> if (isRussian) "Добавить блок" else "Add Block"
            "data_selection" -> if (isRussian) "Выбор данных" else "Select Data"
            "formula_editor" -> if (isRussian) "Редактор формул" else "Formula Editor"
            else -> key
        }
    }

    private fun removeWindowViewOnly(tag: String) {
        val window = activeWindows.remove(tag) ?: return
        window.animate()
            .alpha(0f)
            .scaleX(0.85f)
            .scaleY(0.85f)
            .setDuration(150)
            .withEndAction { removeView(window) }
            .start()
    }

    fun bringNavigationToFront() {
        menuButton.bringToFront()
        sidebar.bringToFront()
    }

    private fun setupSidebar() {
        menuButton = FrameLayout(context).apply {
            val btnSize = dpToPx(48)
            layoutParams = LayoutParams(btnSize, btnSize).apply {
                gravity = Gravity.BOTTOM or Gravity.START
                leftMargin = dpToPx(16)
                bottomMargin = dpToPx(16)
            }

            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor("#EE002B4D".toColorInt())
                setStroke(dpToPx(1), "#50FFFFFF".toColorInt())
            }
            elevation = dpToPx(8).toFloat()

            val icon = TextView(context).apply {
                text = "☰"
                setTextColor(Color.WHITE)
                textSize = 20f
                typeface = android.graphics.Typeface.DEFAULT
                gravity = Gravity.CENTER
            }
            addView(icon)

            setOnClickListener {
                toggleSidebar(true)
            }
        }
        addView(menuButton)

        sidebar = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            elevation = dpToPx(8).toFloat()
            visibility = GONE
            setPadding(0, 0, 0, dpToPx(16))
            layoutParams = LayoutParams(dpToPx(240), dpToPx(320)).apply {
                leftMargin = dpToPx(20)
                topMargin = dpToPx(20)
                gravity = Gravity.TOP or Gravity.START
            }
            background = GradientDrawable().apply {
                setColor("#D0003554".toColorInt())
                setStroke(dpToPx(1), "#33FFFFFF".toColorInt())
                cornerRadius = dpToPx(16).toFloat()
            }
        }

        val titleBar = LinearLayout(context).apply {
            orientation = HORIZONTAL
            setBackgroundColor("#B0002B4D".toColorInt())
            setPadding(dpToPx(14), dpToPx(10), dpToPx(14), dpToPx(10))
            gravity = Gravity.CENTER_VERTICAL
        }

        val headerTitle = TextView(context).apply {
            text = getLocalizedString("workspace_title")
            setTextColor(ContextCompat.getColor(context, R.color.solid_white))
            textSize = 14f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
        }

        val closeSidebarBtn = ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(dpToPx(20), dpToPx(20))
            setImageResource(R.drawable.ic_close)
            setColorFilter(ContextCompat.getColor(context, R.color.accent))
            setOnClickListener {
                toggleSidebar(false)
            }
        }

        titleBar.addView(headerTitle)
        titleBar.addView(closeSidebarBtn)
        sidebar.addView(titleBar)

        val spacer = View(context).apply {
            layoutParams = LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, dpToPx(12))
        }
        sidebar.addView(spacer)

        val isProjectLevel = context.javaClass.simpleName == "ProjectActivity"

        if (isProjectLevel) {
            addStyledSidebarRow("Сцены", SceneListFragment.TAG, R.drawable.landscape_2_24px) {
                SceneListFragment()
            }
            addStyledSidebarRow("Спрайты", SpriteListFragment.TAG, R.drawable.ic_stat) {
                SpriteListFragment()
            }

            val divider2 = View(context).apply {
                layoutParams = LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, dpToPx(1)).apply {
                    topMargin = dpToPx(8)
                    bottomMargin = dpToPx(8)
                    leftMargin = dpToPx(12)
                    rightMargin = dpToPx(12)
                }
                setBackgroundColor("#15FFFFFF".toColorInt())
            }
            sidebar.addView(divider2)
        }
        addStyledSidebarRow(getLocalizedString("scripts"), ScriptFragment.TAG, R.drawable.ic_play) {
            ScriptFragment()
        }
        addStyledSidebarRow(getLocalizedString("looks"), LookListFragment.TAG, R.drawable.ic_look_pos) {
            LookListFragment()
        }
        addStyledSidebarRow(getLocalizedString("sounds"), SoundListFragment.TAG, R.drawable.ic_sound_pos) {
            SoundListFragment()
        }

        addView(sidebar)
    }

    private fun addStyledSidebarRow(title: String, tag: String, iconRes: Int, fragmentCreator: () -> Fragment) {
        val row = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dpToPx(16), dpToPx(10), dpToPx(16), dpToPx(10))
            layoutParams = LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = dpToPx(4)
                leftMargin = dpToPx(12)
                rightMargin = dpToPx(12)
            }

            val states = StateListDrawable().apply {
                addState(intArrayOf(android.R.attr.state_pressed), GradientDrawable().apply {
                    setColor("#25A8DFF4".toColorInt())
                    cornerRadius = dpToPx(8).toFloat()
                })
                addState(intArrayOf(), GradientDrawable().apply {
                    setColor(Color.TRANSPARENT)
                })
            }
            background = states

            val icon = ImageView(context).apply {
                layoutParams = LinearLayout.LayoutParams(dpToPx(18), dpToPx(20)).apply {
                    rightMargin = dpToPx(14)
                }
                setImageResource(iconRes)
                setColorFilter(ContextCompat.getColor(context, R.color.accent))
            }

            val text = TextView(context).apply {
                text = title
                setTextColor(Color.WHITE)
                textSize = 14f
                typeface = android.graphics.Typeface.DEFAULT
            }

            addView(icon)
            addView(text)

            setOnClickListener {
                openWindow(tag, title, fragmentCreator)
                toggleSidebar(false)
            }
        }
        sidebar.addView(row)
    }

    fun toggleSidebar(open: Boolean) {
        isSidebarOpen = open
        if (open) {
            sidebar.visibility = VISIBLE
            menuButton.animate().alpha(0f).scaleX(0f).scaleY(0f).setDuration(180).start()

            bringNavigationToFront()

            sidebar.alpha = 0f
            sidebar.scaleX = 0.85f
            sidebar.scaleY = 0.85f
            sidebar.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setInterpolator(OvershootInterpolator(1.05f))
                .setDuration(220)
                .start()
        } else {
            menuButton.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(220).start()

            sidebar.animate()
                .alpha(0f)
                .scaleX(0.85f)
                .scaleY(0.85f)
                .setInterpolator(DecelerateInterpolator())
                .setDuration(180)
                .withEndAction { sidebar.visibility = GONE }
                .start()
        }
    }

    fun requestFocus(targetTag: String) {
        focusedWindowTag = targetTag
        for ((tag, window) in activeWindows) {
            window.setFocusedState(tag == targetTag)
        }
    }

    fun openWindow(tag: String, title: String, fragmentCreator: () -> Fragment) {
        val activity = context as? androidx.fragment.app.FragmentActivity

        if (activeWindows.containsKey(tag)) {
            requestFocus(tag)
            activeWindows[tag]?.bringToFront()
            bringNavigationToFront()
            return
        }

        val window = WindowContainer(context)
        window.initWindow(tag, title)

        val defaultWidth = dpToPx(400)
        val defaultHeight = dpToPx(500)

        window.layoutParams = LayoutParams(defaultWidth, defaultHeight).apply {
            leftMargin = dpToPx(50)
            topMargin = dpToPx(50)
            gravity = Gravity.TOP or Gravity.START
        }

        setupWindowMenu(window, tag, activity)

        window.onCloseClickListener = {
            if (tag == org.catrobat.catroid.ui.fragment.FormulaEditorFragment.FORMULA_EDITOR_FRAGMENT_TAG) {
                val activity = context as? androidx.fragment.app.FragmentActivity
                val fragment = activity?.supportFragmentManager?.findFragmentByTag(tag)
                        as? org.catrobat.catroid.ui.fragment.FormulaEditorFragment

                fragment?.saveFormulaIfPossible()

                removeWindow(tag, force = true)

                val scriptsFragment = activity?.supportFragmentManager
                    ?.findFragmentByTag(ScriptFragment.TAG) as? ScriptFragment
                scriptsFragment?.notifyDataSetChanged()
            } else {
                removeWindow(tag, force = true)
            }
        }

        window.onLayoutChangedListener = { saveLayoutState() }
        addView(window)
        activeWindows[tag] = window

        activity?.supportFragmentManager?.beginTransaction()?.apply {
            replace(window.contentFrame.id, fragmentCreator.invoke(), tag)
            commitAllowingStateLoss()
        }

        bringNavigationToFront()
        requestFocus(tag)
        loadSingleWindowLayout(tag, window)

        if (!isRestoring) {
            saveLayoutState()
        }
    }

    fun removeWindow(tag: String, force: Boolean = false) {
        val window = activeWindows[tag] ?: return
        if (window.isPinned && !force) return

        activeWindows.remove(tag)
        val activity = context as? androidx.fragment.app.FragmentActivity

        val subWindows = listOf(
            org.catrobat.catroid.ui.recyclerview.fragment.DataListFragment.TAG,
            "functionFragment", "logicFragment", "sensorFragment", "objectFragment"
        )

        if (tag in subWindows) {
            val formulaTag = org.catrobat.catroid.ui.fragment.FormulaEditorFragment.FORMULA_EDITOR_FRAGMENT_TAG
            val formulaEditor = activity?.supportFragmentManager?.findFragmentByTag(formulaTag)
                    as? org.catrobat.catroid.ui.fragment.FormulaEditorFragment

            formulaEditor?.view?.post {
                formulaEditor.onHiddenChanged(false)
            }
        }

        val fragment = activity?.supportFragmentManager?.findFragmentByTag(tag)
        if (fragment != null) {
            activity.supportFragmentManager.beginTransaction().remove(fragment).commitAllowingStateLoss()
        }

        window.animate()
            .alpha(0f)
            .scaleX(0.85f)
            .scaleY(0.85f)
            .setDuration(150)
            .withEndAction { removeView(window) }
            .start()

        saveLayoutState()
    }

    fun saveLayoutState() {
        val parentWidth = width.toFloat()
        val parentHeight = height.toFloat()
        if (parentWidth <= 0 || parentHeight <= 0) return

        sharedPrefs.edit {
            val activeTags = activeWindows.keys.joinToString(",")
            putString("active_tags", activeTags)

            for ((tag, window) in activeWindows) {
                val params = window.layoutParams as LayoutParams
                val xPerc = params.leftMargin / parentWidth
                val yPerc = params.topMargin / parentHeight

                val wPx = params.width.toFloat()
                val hPx = params.height.toFloat()

                putString("${tag}_layout", "$xPerc;$yPerc;$wPx;$hPx;${window.isPinned}")
            }
        }
    }

    private fun loadSingleWindowLayout(tag: String, window: WindowContainer) {
        val layoutStr = sharedPrefs.getString("${tag}_layout", null) ?: return
        val parts = layoutStr.split(";")
        if (parts.size < 4) return

        val parentWidth = width
        val parentHeight = height

        if (parentWidth > 0 && parentHeight > 0) {
            val params = window.layoutParams as LayoutParams

            val savedWidthVal = parts[2].toFloat()
            val targetWidth = if (savedWidthVal < 1.0f) {
                (savedWidthVal * parentWidth).toInt()
            } else {
                savedWidthVal.toInt()
            }.coerceIn(minWidth, parentWidth)

            val savedHeightVal = parts[3].toFloat()
            val targetHeight = if (savedHeightVal < 1.0f) {
                (savedHeightVal * parentHeight).toInt()
            } else {
                savedHeightVal.toInt()
            }.coerceIn(minHeight, parentHeight)

            var targetLeft = (parts[0].toFloat() * parentWidth).toInt()
            var targetTop = (parts[1].toFloat() * parentHeight).toInt()

            targetLeft = targetLeft.coerceIn(0, (parentWidth - targetWidth).coerceAtLeast(0))
            targetTop = targetTop.coerceIn(0, (parentHeight - targetHeight).coerceAtLeast(0))

            params.leftMargin = targetLeft
            params.topMargin = targetTop
            params.width = targetWidth
            params.height = targetHeight
            window.layoutParams = params

            if (parts.size == 5) {
                window.isPinned = parts[4].toBoolean()
                window.updatePinIconState()
            }
        } else {
            post {
                val w = width
                val h = height
                if (w > 0 && h > 0) {
                    val params = window.layoutParams as LayoutParams

                    val savedWidthVal = parts[2].toFloat()
                    val targetWidth = if (savedWidthVal < 1.0f) {
                        (savedWidthVal * w).toInt()
                    } else {
                        savedWidthVal.toInt()
                    }.coerceIn(minWidth, w)

                    val savedHeightVal = parts[3].toFloat()
                    val targetHeight = if (savedHeightVal < 1.0f) {
                        (savedHeightVal * h).toInt()
                    } else {
                        savedHeightVal.toInt()
                    }.coerceIn(minHeight, h)

                    var targetLeft = (parts[0].toFloat() * w).toInt()
                    var targetTop = (parts[1].toFloat() * h).toInt()

                    targetLeft = targetLeft.coerceIn(0, (w - targetWidth).coerceAtLeast(0))
                    targetTop = targetTop.coerceIn(0, (h - targetHeight).coerceAtLeast(0))

                    params.leftMargin = targetLeft
                    params.topMargin = targetTop
                    params.width = targetWidth
                    params.height = targetHeight
                    window.layoutParams = params

                    if (parts.size == 5) {
                        window.isPinned = parts[4].toBoolean()
                        window.updatePinIconState()
                    }
                }
            }
        }
    }

    fun restoreLayoutState(fragmentCreators: Map<String, Pair<String, () -> Fragment>>) {
        if (isLayoutRestored) return
        val listener = object : android.view.ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                if (width > 0 && height > 0) {
                    viewTreeObserver.removeOnGlobalLayoutListener(this)
                    if (isLayoutRestored) return
                    isLayoutRestored = true

                    val activeTags = sharedPrefs.getString("active_tags", "") ?: ""
                    if (activeTags.isNotEmpty()) {
                        isRestoring = true
                        val tagsList = activeTags.split(",")
                        for (tag in tagsList) {
                            val creatorData = fragmentCreators[tag] ?: continue
                            openWindow(tag, creatorData.first, creatorData.second)
                        }
                        isRestoring = false
                    }
                }
            }
        }
        viewTreeObserver.addOnGlobalLayoutListener(listener)
    }

    fun startGhostDrag(draggedWindow: WindowContainer) {
        ghostPreview.layoutParams = LayoutParams(draggedWindow.width, draggedWindow.height)
        ghostPreview.visibility = VISIBLE
        ghostPreview.bringToFront()
        draggedWindow.bringToFront()
        bringNavigationToFront()
    }

    fun updateGhostDrag(draggedWindow: WindowContainer, newLeft: Int, newTop: Int): Pair<Int, Int> {
        val (snapLeft, snapTop) = calculateSnap(draggedWindow, newLeft, newTop, draggedWindow.width, draggedWindow.height)
        val params = ghostPreview.layoutParams as LayoutParams
        params.leftMargin = snapLeft
        params.topMargin = snapTop
        ghostPreview.layoutParams = params
        return Pair(snapLeft, snapTop)
    }

    fun endGhostDrag(draggedWindow: WindowContainer, finalLeft: Int, finalTop: Int) {
        ghostPreview.visibility = GONE
        draggedWindow.animateToPosition(finalLeft, finalTop) {
            saveLayoutState()
        }
    }

    fun updateGhostResize(draggedWindow: WindowContainer, left: Int, top: Int, snapWidth: Int, snapHeight: Int) {
        val params = ghostPreview.layoutParams as LayoutParams
        params.leftMargin = left
        params.topMargin = top
        params.width = snapWidth
        params.height = snapHeight
        ghostPreview.layoutParams = params
    }

    fun endGhostResize(draggedWindow: WindowContainer, finalWidth: Int, finalHeight: Int) {
        ghostPreview.visibility = GONE
        draggedWindow.animateToSize(finalWidth, finalHeight) {
            saveLayoutState()
        }
    }

    fun calculateResizeSnap(
        draggedWindow: WindowContainer,
        newWidth: Int,
        newHeight: Int
    ): Pair<Int, Int> {
        val snapThreshold = dpToPx(30)
        var targetWidth = newWidth
        var targetHeight = newHeight

        val params = draggedWindow.layoutParams as LayoutParams
        val left = params.leftMargin
        val top = params.topMargin

        val parentWidth = this.width
        val parentHeight = this.height

        if (abs((left + newWidth) - parentWidth) < snapThreshold) {
            targetWidth = parentWidth - left
        }
        if (abs((top + newHeight) - parentHeight) < snapThreshold) {
            targetHeight = parentHeight - top
        }

        for (other in activeWindows.values) {
            if (other == draggedWindow) continue

            val otherParams = other.layoutParams as LayoutParams
            val otherLeft = otherParams.leftMargin
            val otherTop = otherParams.topMargin

            if (abs((left + newWidth) - otherLeft) < snapThreshold) {
                targetWidth = otherLeft - left
            }
            if (abs((top + newHeight) - otherTop) < snapThreshold) {
                targetHeight = otherTop - top
            }
        }

        return Pair(targetWidth, targetHeight)
    }

    fun calculateSnap(
        draggedWindow: WindowContainer,
        newLeft: Int,
        newTop: Int,
        width: Int,
        height: Int
    ): Pair<Int, Int> {
        val snapThreshold = dpToPx(30)
        var targetLeft = newLeft
        var targetTop = newTop

        val parentWidth = this.width
        val parentHeight = this.height

        if (newLeft < snapThreshold) {
            targetLeft = 0
        } else if (parentWidth - (newLeft + width) < snapThreshold) {
            targetLeft = parentWidth - width
        }

        if (newTop < snapThreshold) {
            targetTop = 0
        } else if (parentHeight - (newTop + height) < snapThreshold) {
            targetTop = parentHeight - height
        }

        for (other in activeWindows.values) {
            if (other == draggedWindow) continue

            val otherParams = other.layoutParams as LayoutParams
            val otherLeft = otherParams.leftMargin
            val otherTop = otherParams.topMargin
            val otherRight = otherLeft + other.width
            val otherBottom = otherTop + other.height

            if (abs(newLeft - otherRight) < snapThreshold) {
                targetLeft = otherRight
            } else if (abs((newLeft + width) - otherLeft) < snapThreshold) {
                targetLeft = otherLeft - width
            }

            if (abs(newTop - otherBottom) < snapThreshold) {
                targetTop = otherBottom
            } else if (abs((newTop + height) - otherTop) < snapThreshold) {
                targetTop = otherTop - height
            }

            if (abs(newLeft - otherLeft) < snapThreshold) {
                targetLeft = otherLeft
            }
            if (abs(newTop - otherTop) < snapThreshold) {
                targetTop = otherTop
            }
        }

        return Pair(targetLeft, targetTop)
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }

    fun openBrickCategoryWindow() {
        val tag = org.catrobat.catroid.ui.fragment.BrickCategoryFragment.BRICK_CATEGORY_FRAGMENT_TAG
        openWindow(tag, getLocalizedString("categories")) {
            org.catrobat.catroid.ui.fragment.BrickCategoryFragment().apply {
                setOnCategorySelectedListener(object : org.catrobat.catroid.ui.fragment.BrickCategoryFragment.OnCategorySelectedListener {
                    override fun onCategorySelected(category: String?) {
                        openAddBrickWindow(category ?: "")
                    }
                })
            }
        }
    }

    fun openAddBrickWindow(category: String) {
        val tag = org.catrobat.catroid.ui.fragment.AddBrickFragment.ADD_BRICK_FRAGMENT_TAG
        openWindow(tag, getLocalizedString("add_brick") + ": $category") {
            val scriptsFragment = (context as? androidx.fragment.app.FragmentActivity)
                ?.supportFragmentManager
                ?.findFragmentByTag(ScriptFragment.TAG)
                    as? ScriptFragment
            org.catrobat.catroid.ui.fragment.AddBrickFragment.newInstance(category, scriptsFragment)
        }
    }

    fun openFormulaEditorWindow(
        formulaBrick: org.catrobat.catroid.content.bricks.FormulaBrick,
        formulaField: org.catrobat.catroid.content.bricks.Brick.FormulaField,
        showCustomView: Boolean
    ) {
        val tag = org.catrobat.catroid.ui.fragment.FormulaEditorFragment.FORMULA_EDITOR_FRAGMENT_TAG
        val title = getLocalizedString("formula_editor")

        val existingWindow = activeWindows[tag]
        if (existingWindow != null) {
            val fragment = (context as? androidx.fragment.app.FragmentActivity)
                ?.supportFragmentManager
                ?.findFragmentByTag(tag) as? org.catrobat.catroid.ui.fragment.FormulaEditorFragment
            if (fragment != null) {
                requestFocus(tag)
                existingWindow.bringToFront()
                fragment.switchToFormula(formulaBrick, formulaField, showCustomView)
                return
            }
            removeWindow(tag, force = true)
        }

        val fragmentManager = (context as? androidx.fragment.app.FragmentActivity)?.supportFragmentManager
        if (fragmentManager != null && !fragmentManager.isStateSaved) {
            fragmentManager.executePendingTransactions()
            val leftover = fragmentManager.findFragmentByTag(tag)
            if (leftover != null) {
                fragmentManager.beginTransaction().remove(leftover).commitNowAllowingStateLoss()
            }
        }

        openWindow(tag, title) {
            org.catrobat.catroid.ui.fragment.FormulaEditorFragment().apply {
                val bundle = android.os.Bundle().apply {
                    putSerializable(org.catrobat.catroid.ui.fragment.FormulaEditorFragment.FORMULA_BRICK_BUNDLE_ARGUMENT, formulaBrick)
                    putSerializable(org.catrobat.catroid.ui.fragment.FormulaEditorFragment.FORMULA_FIELD_BUNDLE_ARGUMENT, formulaField)
                }
                arguments = bundle
                setShowCustomView(showCustomView)
            }
        }
    }

    fun openCategoryListWindow(fragmentTag: String, title: String) {
        openWindow(fragmentTag, title) {
            org.catrobat.catroid.ui.recyclerview.fragment.CategoryListFragment().apply {
                val bundle = android.os.Bundle().apply {
                    putString(org.catrobat.catroid.ui.recyclerview.fragment.CategoryListFragment.ACTION_BAR_TITLE_BUNDLE_ARGUMENT, title)
                    putString(org.catrobat.catroid.ui.recyclerview.fragment.CategoryListFragment.FRAGMENT_TAG_BUNDLE_ARGUMENT, fragmentTag)
                }
                arguments = bundle
            }
        }
    }

    fun openDataListWindow(parentBrick: org.catrobat.catroid.content.bricks.Brick) {
        val tag = org.catrobat.catroid.ui.recyclerview.fragment.DataListFragment.TAG
        openWindow(tag, getLocalizedString("data_selection")) {
            org.catrobat.catroid.ui.recyclerview.fragment.DataListFragment().apply {
                val formulaEditor = (context as? androidx.fragment.app.FragmentActivity)
                    ?.supportFragmentManager
                    ?.findFragmentByTag(org.catrobat.catroid.ui.fragment.FormulaEditorFragment.FORMULA_EDITOR_FRAGMENT_TAG)
                        as? org.catrobat.catroid.ui.fragment.FormulaEditorFragment
                if (formulaEditor != null) {
                    setFormulaEditorDataInterface(formulaEditor)
                }
                val bundle = android.os.Bundle().apply {
                    putSerializable(org.catrobat.catroid.ui.recyclerview.fragment.DataListFragment.PARENT_SCRIPT_BRICK_BUNDLE_ARGUMENT, parentBrick)
                }
                arguments = bundle
            }
        }
    }

    fun handleBackPressed(): Boolean {
        for ((_, window) in activeWindows) {
            if (window.isMaximized) {
                window.toggleMaximize()
                return true
            }
        }

        val subWindows = listOf(
            org.catrobat.catroid.ui.recyclerview.fragment.DataListFragment.TAG,
            "functionFragment", "logicFragment", "sensorFragment", "objectFragment",
            org.catrobat.catroid.ui.fragment.AddBrickFragment.ADD_BRICK_FRAGMENT_TAG,
            org.catrobat.catroid.ui.fragment.BrickCategoryFragment.BRICK_CATEGORY_FRAGMENT_TAG
        )

        for (tag in subWindows) {
            val window = activeWindows[tag]
            if (window != null && !window.isPinned) {
                removeWindow(tag, force = false)
                return true
            }
        }

        val formulaTag = org.catrobat.catroid.ui.fragment.FormulaEditorFragment.FORMULA_EDITOR_FRAGMENT_TAG
        val formulaWindow = activeWindows[formulaTag]
        if (formulaWindow != null && !formulaWindow.isPinned) {
            val activity = context as? androidx.fragment.app.FragmentActivity
            val fragment = activity?.supportFragmentManager?.findFragmentByTag(formulaTag)
                    as? org.catrobat.catroid.ui.fragment.FormulaEditorFragment

            fragment?.saveFormulaIfPossible()

            removeWindow(formulaTag, force = true)

            val scriptsFragment = activity?.supportFragmentManager
                ?.findFragmentByTag(ScriptFragment.TAG) as? ScriptFragment
            scriptsFragment?.notifyDataSetChanged()
            return true
        }

        for ((tag, window) in activeWindows) {
            if (!window.isPinned) {
                removeWindow(tag, force = false)
                return true
            }
        }

        return false
    }

    private fun setupWindowMenu(window: WindowContainer, tag: String, activity: androidx.fragment.app.FragmentActivity?) {
        val menuRes = when (tag) {
            org.catrobat.catroid.ui.fragment.FormulaEditorFragment.FORMULA_EDITOR_FRAGMENT_TAG -> R.menu.menu_formulaeditor
            ScriptFragment.TAG,
            LookListFragment.TAG,
            SoundListFragment.TAG -> R.menu.menu_script_activity
            SpriteListFragment.TAG,
            SceneListFragment.TAG -> R.menu.menu_project_activity
            else -> null
        }

        if (menuRes != null) {
            window.optionsButton.visibility = VISIBLE
            window.optionsButton.setOnClickListener { view ->
                val popup = androidx.appcompat.widget.PopupMenu(context, view)
                popup.inflate(menuRes)

                val fragment = activity?.supportFragmentManager?.findFragmentByTag(tag)
                fragment?.onPrepareOptionsMenu(popup.menu)

                popup.setOnMenuItemClickListener { item ->
                    val projectItems = listOf(
                        R.id.project_options,
                        R.id.project_files,
                        R.id.project_libs,
                        R.id.editor3d,
                        R.id.new_scene
                    )

                    if (item.itemId in projectItems) {
                        activity?.onOptionsItemSelected(item) ?: false
                    } else {
                        val handledByFragment = fragment?.onOptionsItemSelected(item) ?: false
                        if (!handledByFragment) {
                            activity?.onOptionsItemSelected(item) ?: false
                        } else {
                            true
                        }
                    }
                }
                popup.show()
            }
        } else {
            window.optionsButton.visibility = GONE
        }
    }

    fun isWindowOpen(tag: String): Boolean = activeWindows.containsKey(tag)

    fun replaceWindowContent(tag: String, fragmentCreator: () -> Fragment) {
        val window = activeWindows[tag] ?: return
        val activity = context as? androidx.fragment.app.FragmentActivity
        isReplacing = true

        activity?.supportFragmentManager?.beginTransaction()?.apply {
            replace(window.contentFrame.id, fragmentCreator.invoke(), tag)
            commitAllowingStateLoss()
        }

        activity?.supportFragmentManager?.executePendingTransactions()

        isReplacing = false
    }
}
