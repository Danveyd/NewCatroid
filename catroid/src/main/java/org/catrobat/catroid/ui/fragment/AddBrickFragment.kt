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
package org.catrobat.catroid.ui.fragment

import android.content.Context
import android.graphics.Typeface
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.AdapterView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.ListFragment
import com.danvexteam.lunoscript_annotations.LunoClass
import org.catrobat.catroid.ProjectManager
import org.catrobat.catroid.R
import org.catrobat.catroid.cast.CastManager
import org.catrobat.catroid.content.bricks.Brick
import org.catrobat.catroid.ui.SpriteActivity
import org.catrobat.catroid.ui.adapter.PrototypeBrickAdapter
import org.catrobat.catroid.ui.settingsfragments.AccessibilityProfile
import org.catrobat.catroid.utils.SnackbarUtil
import org.catrobat.catroid.utils.ToastUtil
import org.koin.java.KoinJavaComponent.inject

@LunoClass
class AddBrickFragment : ListFragment() {
    private var addBrickListener: OnAddBrickListener? = null
    private var previousActionBarTitle: CharSequence? = null
    private var adapter: PrototypeBrickAdapter? = null

    private var masterBrickList: List<Brick> = emptyList()

    private var previousVisibleListSize = 0

    private var lastBuiltWithBeginnerBricksSetting: Boolean? = null

    private fun onlyBeginnerBricks(): Boolean = PreferenceManager.getDefaultSharedPreferences(activity).getBoolean(AccessibilityProfile.BEGINNER_BRICKS, false)
    private val projectManager: ProjectManager by inject(ProjectManager::class.java)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_brick_add, container, false)
        previousActionBarTitle = (activity as? AppCompatActivity)?.supportActionBar?.title
        (activity as? AppCompatActivity)?.supportActionBar?.title = arguments?.getString(BUNDLE_ARGUMENTS_SELECTED_CATEGORY)
        setupSelectedBrickCategory()
        return view
    }

    private fun setupSelectedBrickCategory() {
        val context: Context? = activity
        val sprite = projectManager.currentSprite
        val backgroundSprite = projectManager.currentlyEditedScene.backgroundSprite
        val selectedCategory = arguments?.getString(BUNDLE_ARGUMENTS_SELECTED_CATEGORY)
        val categoryBricksFactory: CategoryBricksFactory = when {
            onlyBeginnerBricks() -> CategoryBeginnerBricksFactory()
            else -> CategoryBricksFactory()
        }
        val brickList = selectedCategory?.let { context?.let { it1 ->
            categoryBricksFactory.getBricks(it, backgroundSprite == sprite,
                it1
            )
        } } ?: emptyList()

        masterBrickList = brickList
        lastBuiltWithBeginnerBricksSetting = onlyBeginnerBricks()

        for (brick in masterBrickList) {
            if (brick is org.catrobat.catroid.content.bricks.SubCategoryHeaderBrick) {
                brick.isExpanded = org.catrobat.catroid.utils.SubCategoryStateManager.isExpanded(
                    requireContext(),
                    brick.title
                )
            }
        }

        updateVisibleBricks()
    }

    override fun onStart() {
        super.onStart()
        if (listIndexToFocus != -1) {
            listView.setSelection(listIndexToFocus)
            listIndexToFocus = -1
        }

        listView.onItemClickListener = AdapterView.OnItemClickListener { parent: AdapterView<*>?, view: View?, position: Int, id: Long ->
            adapter?.getItem(position)?.let { brick ->
                if (brick is org.catrobat.catroid.content.bricks.SubCategoryHeaderBrick) {

                    val transition = android.transition.TransitionSet().apply {
                        ordering = android.transition.TransitionSet.ORDERING_TOGETHER
                        addTransition(android.transition.ChangeBounds())
                        duration = 250
                        interpolator = androidx.interpolator.view.animation.FastOutSlowInInterpolator()
                    }
                    android.transition.TransitionManager.beginDelayedTransition(listView, transition)

                    brick.isExpanded = !brick.isExpanded
                    view?.let { clickedView ->
                        val arrowView = clickedView.findViewById<android.widget.TextView>(R.id.brick_header_arrow)
                        arrowView?.text = if (brick.isExpanded) "▼" else "▶"
                    }
                    org.catrobat.catroid.utils.SubCategoryStateManager.setExpanded(requireContext(), brick.title, brick.isExpanded)
                    updateVisibleBricks()
                } else {
                    addBrickToScript(brick, requireActivity(), addBrickListener, parentFragmentManager, ADD_BRICK_FRAGMENT_TAG)
                }
            }
        }

        listView.onItemLongClickListener = AdapterView.OnItemLongClickListener { _, _, position, _ ->
            adapter?.getItem(position)?.let { brick ->
                if (brick !is org.catrobat.catroid.content.bricks.SubCategoryHeaderBrick) {
                    showAddBrickPreviewDialog(brick)
                }
            }
            true
        }

        listView.setOnScrollListener(object : AbsListView.OnScrollListener {
            override fun onScrollStateChanged(view: AbsListView?, scrollState: Int) {
                val isScrollingNow = scrollState == AbsListView.OnScrollListener.SCROLL_STATE_FLING ||
                        scrollState == AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL
                adapter?.isScrolling = isScrollingNow
            }

            override fun onScroll(
                view: AbsListView?,
                firstVisibleItem: Int,
                visibleItemCount: Int,
                totalItemCount: Int
            ) {}
        })
    }

    private fun toggleHeaderState(position: Int, brick: org.catrobat.catroid.content.bricks.SubCategoryHeaderBrick) {

        val transition = android.transition.TransitionSet().apply {
            ordering = android.transition.TransitionSet.ORDERING_TOGETHER
            addTransition(android.transition.ChangeBounds())
            addTransition(android.transition.Fade(android.transition.Fade.OUT).apply {
                duration = 150
            })
            duration = 250
            interpolator = androidx.interpolator.view.animation.FastOutSlowInInterpolator()
        }
        android.transition.TransitionManager.beginDelayedTransition(listView, transition)

        brick.isExpanded = !brick.isExpanded

        val childIndex = position - listView.firstVisiblePosition
        if (childIndex in 0 until listView.childCount) {
            val clickedView = listView.getChildAt(childIndex)
            val arrowView = clickedView?.findViewById<android.widget.TextView>(R.id.brick_header_arrow)
            arrowView?.text = if (brick.isExpanded) "▼" else "▶"
        }

        org.catrobat.catroid.utils.SubCategoryStateManager.setExpanded(
            requireContext(),
            brick.title,
            brick.isExpanded
        )
        updateVisibleBricks()
    }

    private fun updateVisibleBricks() {
        val visibleList = mutableListOf<Brick>()
        var isCurrentSubcategoryExpanded = true
        var previousWasHeader = true

        for (brick in masterBrickList) {
            if (brick is org.catrobat.catroid.content.bricks.SubCategoryHeaderBrick) {
                brick.isStacked = previousWasHeader
                visibleList.add(brick)
                isCurrentSubcategoryExpanded = brick.isExpanded
                previousWasHeader = true
            } else {
                if (isCurrentSubcategoryExpanded) {
                    visibleList.add(brick)
                    previousWasHeader = false
                }
            }
        }

        previousVisibleListSize = visibleList.size

        val visibleBricksBefore = mutableSetOf<Brick>()
        if (view != null) {
            try {
                val list = listView
                val first = list.firstVisiblePosition
                val last = list.lastVisiblePosition
                val count = list.count
                if (first >= 0 && last >= 0) {
                    val start = maxOf(0, first)
                    val end = minOf(count - 1, last)
                    if (start <= end) {
                        for (i in start..end) {
                            adapter?.getItem(i)?.let {
                                visibleBricksBefore.add(it)
                            }
                        }
                    }
                }
            } catch (_: Exception) {
            }
        }

        if (adapter == null) {
            adapter = PrototypeBrickAdapter(visibleList)
            listAdapter = adapter
        } else {
            adapter?.replaceList(visibleList, visibleBricksBefore)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, menuInflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, menuInflater)
        menu.findItem(R.id.comment_in_out)?.isVisible = false
    }

    override fun onDestroy() {
        adapter?.clearCache()
        val actionBar = (activity as? AppCompatActivity)?.supportActionBar
        val isRestoringPreviouslyDestroyedActivity = actionBar == null
        if (!isRestoringPreviouslyDestroyedActivity) {
            actionBar?.title = previousActionBarTitle
        }
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        if (masterBrickList.isEmpty() || lastBuiltWithBeginnerBricksSetting != onlyBeginnerBricks()) {
            setupSelectedBrickCategory()
        }
        SnackbarUtil.showHintSnackbar(activity, R.string.hint_bricks)
    }

    interface OnAddBrickListener {
        fun addBrick(brick: Brick?)
    }

    companion object {
        @JvmField
        val ADD_BRICK_FRAGMENT_TAG = AddBrickFragment::class.java.simpleName
        private const val BUNDLE_ARGUMENTS_SELECTED_CATEGORY = "selected_category"
        private var listIndexToFocus = -1
        @JvmStatic
        fun newInstance(selectedCategory: String?, addBrickListener: OnAddBrickListener?): AddBrickFragment {
            val fragment = AddBrickFragment()
            val arguments = Bundle()
            arguments.putString(BUNDLE_ARGUMENTS_SELECTED_CATEGORY, selectedCategory)
            fragment.arguments = arguments
            fragment.addBrickListener = addBrickListener
            return fragment
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.search) {
            (addBrickListener as BrickCategoryFragment.OnCategorySelectedListener).onCategorySelected(arguments?.getString(BUNDLE_ARGUMENTS_SELECTED_CATEGORY))
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showAddBrickPreviewDialog(brick: Brick) {
        val context = requireContext()
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_brick_context, null)

        val brickView =
            (brick as org.catrobat.catroid.content.bricks.BrickBaseType).getFreshDetachedView(context)

        val maxBrickHeight = (200 * context.resources.displayMetrics.density).toInt()
        val wrapperScrollView = object : android.widget.ScrollView(context) {
            override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
                var measuredHeightSpec = heightMeasureSpec
                val heightMode = android.view.View.MeasureSpec.getMode(heightMeasureSpec)
                val heightSize = android.view.View.MeasureSpec.getSize(heightMeasureSpec)
                if (heightSize > maxBrickHeight || heightMode == android.view.View.MeasureSpec.UNSPECIFIED) {
                    measuredHeightSpec = android.view.View.MeasureSpec.makeMeasureSpec(maxBrickHeight, android.view.View.MeasureSpec.AT_MOST)
                }
                super.onMeasure(widthMeasureSpec, measuredHeightSpec)
            }
        }
        wrapperScrollView.addView(brickView)

        val brickContainer = dialogView.findViewById<ViewGroup>(R.id.brick_view_container)
        brickContainer.addView(wrapperScrollView)

        val descriptionView = dialogView.findViewById<TextView>(R.id.brick_description)
        descriptionView.text = org.catrobat.catroid.content.BrickInfo.getDescription(brick)

        val density = context.resources.displayMetrics.density
        val parentLayout = descriptionView?.parent as? ViewGroup
        if (parentLayout != null) {
            val infoButton = TextView(context).apply {
                text = context.getString(R.string.dialog_add_brick_system_info_button)
                textSize = 13f
                setTextColor(context.resources.getColor(R.color.accent))
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                setPadding(0, (12 * density).toInt(), 0, (12 * density).toInt())
                gravity = android.view.Gravity.CENTER
                setOnClickListener {
                    showDetailedSystemInfoDialog(brick)
                }
            }
            val index = parentLayout.indexOfChild(descriptionView)
            parentLayout.addView(infoButton, index + 1)
        }

        val warningLayout = dialogView.findViewById<View>(R.id.warning_layout)
        warningLayout?.visibility = View.GONE

        val isFav = org.catrobat.catroid.utils.FavoriteBricksManager.isFavorite(context, brick)
        val favButtonText = if (isFav) {
            context.getString(R.string.dialog_add_brick_remove_from_favorites)
        } else {
            context.getString(R.string.dialog_add_brick_add_to_favorites)
        }

        androidx.appcompat.app.AlertDialog.Builder(context)
            .setTitle(R.string.dialog_add_brick_title)
            .setView(dialogView)
            .setPositiveButton(R.string.dialog_add_brick_add) { dialog, _ ->
                addBrickToScript(
                    brick,
                    requireActivity(),
                    addBrickListener,
                    parentFragmentManager,
                    ADD_BRICK_FRAGMENT_TAG
                )
                dialog.dismiss()
            }
            .setNeutralButton(favButtonText) { dialog, _ ->
                if (isFav) {
                    org.catrobat.catroid.utils.FavoriteBricksManager.removeFavorite(context, brick)
                    ToastUtil.showSuccess(activity, context.getString(R.string.toast_removed_from_favorites))
                } else {
                    org.catrobat.catroid.utils.FavoriteBricksManager.addFavorite(context, brick)
                    ToastUtil.showSuccess(activity, context.getString(R.string.toast_added_to_favorites))
                }
                setupSelectedBrickCategory()
                dialog.dismiss()
            }
            .setNegativeButton(R.string.dialog_add_brick_cancel) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun showDetailedSystemInfoDialog(brick: Brick) {
        val context = requireContext()
        val density = context.resources.displayMetrics.density

        val scrollView = android.widget.ScrollView(context).apply {
            setPadding((16 * density).toInt(), (12 * density).toInt(), (16 * density).toInt(), (12 * density).toInt())
        }

        val infoTextView = TextView(context).apply {
            text = getBrickSystemInfo(brick)
            setTextColor(context.resources.getColor(R.color.solid_white))
            textSize = 13f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
        }
        scrollView.addView(infoTextView)

        androidx.appcompat.app.AlertDialog.Builder(context)
            .setTitle(R.string.dialog_add_brick_system_info_title)
            .setView(scrollView)
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun getBrickSystemInfo(brick: Brick): String {
        val sb = StringBuilder()
        sb.append("Class Name:\n")
        sb.append(brick.javaClass.name).append("\n")
        sb.append("---------------------------------------\n\n")

        val superclassName = brick.javaClass.superclass?.simpleName ?: "None"
        sb.append("Superclass:\n")
        sb.append(superclassName).append("\n")
        sb.append("---------------------------------------\n\n")

        if (brick is org.catrobat.catroid.content.bricks.FormulaBrick) {
            val formulas = brick.allFormulaFieldsWithFormulas
            if (formulas.isNotEmpty()) {
                sb.append("Formula Fields (BrickFields):\n")
                formulas.forEach { (field, formula) ->
                    sb.append("  * $field = ${formula.getTrimmedFormulaString(requireContext())}\n")
                }
                sb.append("---------------------------------------\n\n")
            }
        }

        if (brick is org.catrobat.catroid.content.bricks.UserDataBrick) {
            val data = brick.allBrickDataWithValues
            if (data.isNotEmpty()) {
                sb.append("Data Fields (UserData):\n")
                data.forEach { (field, value) ->
                    sb.append("  * $field = $value\n")
                }
                sb.append("---------------------------------------\n\n")
            }
        }

        sb.append("Declared Fields:\n")
        val fields = brick.javaClass.declaredFields
        if (fields.isNotEmpty()) {
            fields.forEach { field ->
                try {
                    field.isAccessible = true
                    val name = field.name
                    if (!name.startsWith("$") && name != "serialVersionUID" && name != "Companion") {
                        val type = field.type.simpleName
                        val value = field.get(brick) ?: "null"
                        sb.append("  * $name ($type) = $value\n")
                    }
                } catch (_: Exception) {
                }
            }
        } else {
            sb.append("  * No declared fields found\n")
        }

        return sb.toString()
    }
}

fun addBrickToScript(brick: Brick, activity: android.app.Activity, addBrickListener: AddBrickFragment.OnAddBrickListener?, parentFragmentManager: FragmentManager, tag: String) {
    if (ProjectManager.getInstance().currentProject.isCastProject && CastManager.unsupportedBricks.contains(brick.javaClass)) {
        ToastUtil.showError(activity, R.string.error_unsupported_bricks_chromecast)
        return
    }
    try {
        val brickToAdd = brick.clone()
        addBrickListener?.addBrick(brickToAdd)
        SnackbarUtil.showHintSnackbar(activity, R.string.hint_scripts)
        val workspace = activity.findViewById<View>(R.id.workspace_layout)
        if (workspace != null && workspace.visibility == View.VISIBLE) {
            val workspaceLayout = workspace as? org.catrobat.catroid.ui.workspace.WorkspaceLayout
            if (workspaceLayout != null) {
                workspaceLayout.removeWindow(tag, false)
                workspaceLayout.removeWindow(
                    BrickCategoryFragment.BRICK_CATEGORY_FRAGMENT_TAG,
                    false
                )
            }
        } else {
            val fragmentTransaction = parentFragmentManager.beginTransaction()
            val categoryFragment =
                parentFragmentManager.findFragmentByTag(BrickCategoryFragment.BRICK_CATEGORY_FRAGMENT_TAG)
            if (categoryFragment != null) {
                fragmentTransaction.remove(categoryFragment)
                parentFragmentManager.popBackStack()
            }
            val fragment = parentFragmentManager.findFragmentByTag(tag)
            if (fragment != null) {
                fragmentTransaction.remove(fragment)
                parentFragmentManager.popBackStack()
            }
            fragmentTransaction.commit()
        }
    } catch (e: CloneNotSupportedException) {
        Log.e(tag, e.localizedMessage)
        ToastUtil.showError(activity, R.string.error_adding_brick)
    }
}
