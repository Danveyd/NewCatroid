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
package org.catrobat.catroid.ui.recyclerview.fragment

import android.Manifest.permission
import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import androidx.appcompat.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.preference.PreferenceManager
import android.provider.DocumentsContract
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.annotation.PluralsRes
import androidx.annotation.RequiresApi
import androidx.lifecycle.lifecycleScope
import io.noties.markwon.Markwon
import io.noties.markwon.image.ImagesPlugin
import io.noties.markwon.image.file.FileSchemeHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.catrobat.catroid.CatroidApplication
import org.catrobat.catroid.ProjectManager
import org.catrobat.catroid.R
import org.catrobat.catroid.common.Constants
import org.catrobat.catroid.common.FlavoredConstants
import org.catrobat.catroid.common.ProjectData
import org.catrobat.catroid.common.SharedPreferenceKeys
import org.catrobat.catroid.content.Project
import org.catrobat.catroid.content.backwardcompatibility.ProjectMetaDataParser
import org.catrobat.catroid.exceptions.LoadingProjectException
import org.catrobat.catroid.io.StorageOperations
import org.catrobat.catroid.io.XstreamSerializer
import org.catrobat.catroid.io.asynctask.ProjectCopier
import org.catrobat.catroid.io.asynctask.ProjectLoader
import org.catrobat.catroid.io.asynctask.ProjectLoader.ProjectLoadListener
import org.catrobat.catroid.io.asynctask.ProjectRenamer
import org.catrobat.catroid.io.asynctask.ProjectUnZipperAndImporter
import org.catrobat.catroid.libraries.LibraryManager
import org.catrobat.catroid.libraryeditor.data.LibraryEditorActivity
import org.catrobat.catroid.stage.StageActivity
import org.catrobat.catroid.ui.BottomBar
import org.catrobat.catroid.ui.ProjectActivity
import org.catrobat.catroid.ui.ProjectListActivity
import org.catrobat.catroid.ui.UiUtils
import org.catrobat.catroid.ui.filepicker.FilePickerActivity
import org.catrobat.catroid.ui.fragment.ProjectFilesFragment
import org.catrobat.catroid.ui.fragment.ProjectLibsFragment
import org.catrobat.catroid.ui.fragment.ProjectOptionsFragment
import org.catrobat.catroid.ui.recyclerview.adapter.ProjectAdapter
import org.catrobat.catroid.ui.recyclerview.adapter.RVAdapter
import org.catrobat.catroid.ui.recyclerview.adapter.multiselection.MultiSelectionManager
import org.catrobat.catroid.ui.recyclerview.viewholder.CheckableViewHolder
import org.catrobat.catroid.ui.runtimepermissions.RequiresPermissionTask
import org.catrobat.catroid.utils.ToastUtil
import org.koin.android.ext.android.inject
import java.io.File
import java.io.IOException

@SuppressLint("NotifyDataSetChanged")
class ProjectListFragment : RecyclerViewFragment<ProjectData?>(), ProjectLoadListener {
    private var filesForUnzipAndImportTask: ArrayList<File>? = null
    private var hasUnzipAndImportTaskFinished = false

    private val projectManager: ProjectManager by inject()

    private var importDialog: AlertDialog? = null

    override fun onActivityCreated(savedInstance: Bundle?) {
        super.onActivityCreated(savedInstance)
        filesForUnzipAndImportTask = ArrayList()
        hasUnzipAndImportTaskFinished = true
        if (arguments != null) {
            importProject(requireArguments().getParcelable("intent"))
        }
        if (requireActivity().intent?.hasExtra(ProjectListActivity.IMPORT_LOCAL_INTENT) == true) {
            adapter.showSettings = false
            actionModeType = IMPORT_LOCAL
        }
    }

    /*private fun onImportProjectFinished(success: Boolean) {
        setAdapterItems(adapter.projectsSorted)
        if (success && filesForUnzipAndImportTask?.size == 1) {
            val importedFile = filesForUnzipAndImportTask!!.first()
            val projectName = importedFile.nameWithoutExtension
            showReadmeForProject(projectName)
        } else if (success) {
            ToastUtil.showSuccess(
                requireContext(),
                resources.getQuantityString(
                    R.plurals.imported_projects,
                    filesForUnzipAndImportTask?.size ?: 0,
                    filesForUnzipAndImportTask?.size ?: 0
                )
            )
        } else {
            ToastUtil.showError(requireContext(), R.string.error_import_project)
        }
        filesForUnzipAndImportTask?.clear()
        setShowProgressBar(false)
    }*/

    private fun onImportProjectFinished(result: org.catrobat.catroid.io.asynctask.ImportResult) {
        val currentContext = context ?: return

        dismissImportProgressDialog()
        filesForUnzipAndImportTask?.clear()
        setShowProgressBar(false)

        when (result) {
            is org.catrobat.catroid.io.asynctask.ImportResult.Success -> {
                ProjectListFragment.clearCache()
                setAdapterItems(adapter.projectsSorted)
            }

            is org.catrobat.catroid.io.asynctask.ImportResult.Failure -> {
                ToastUtil.showError(currentContext, R.string.error_import_project)
            }

            is org.catrobat.catroid.io.asynctask.ImportResult.BakedProject -> {
                ToastUtil.showSuccess(currentContext, currentContext.getString(R.string.plf_launching_baked_project))
                launchBakedProject(currentContext, result.projectDir)
            }
        }
    }

    private fun launchBakedProject(context: android.content.Context, projectDir: File) {
        val intent = Intent(context, StageActivity::class.java)
        intent.putExtra(StageActivity.EXTRA_PROJECT_PATH, projectDir.absolutePath)
        intent.putExtra("IS_BAKED_LAUNCH", true)
        startActivity(intent)
    }

    private fun showImportProgressDialog() {
        val context = context ?: return

        val layout = android.widget.LinearLayout(context).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            val padding = (24 * resources.displayMetrics.density).toInt()
            setPadding(padding, padding, padding, padding)
            gravity = android.view.Gravity.CENTER_VERTICAL
        }

        val progressBar = android.widget.ProgressBar(context).apply {
            isIndeterminate = true
        }

        val textView = android.widget.TextView(context).apply {
            text = getString(R.string.importing_project_dialog_msg)
            val marginStart = (16 * resources.displayMetrics.density).toInt()
            setPadding(marginStart, 0, 0, 0)
            textSize = 16f
            setTextColor(android.graphics.Color.WHITE)
        }

        layout.addView(progressBar)
        layout.addView(textView)

        importDialog = AlertDialog.Builder(context)
            .setView(layout)
            .setCancelable(false)
            .create()

        importDialog?.show()
    }

    private fun dismissImportProgressDialog() {
        try {
            if (importDialog != null && importDialog!!.isShowing) {
                importDialog?.dismiss()
            }
        } catch (e: Exception) {
        }
        importDialog = null
    }

    override fun onDestroy() {
        dismissImportProgressDialog()
        super.onDestroy()
    }

    private fun showReadmeForProject(projectName: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val projectDir = File(FlavoredConstants.DEFAULT_ROOT_DIRECTORY, projectName)
                if (!projectDir.exists()) return@launch

                val project = XstreamSerializer.getInstance().loadProject(projectDir, requireContext())
                val description = project?.description

                if (!description.isNullOrBlank()) {
                    withContext(Dispatchers.Main) {
                        showMarkdownDialog(project, description)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load project for Readme display", e)
            }
        }
    }

    private fun showMarkdownDialog(project: Project, markdownText: String) {
        val context = activity ?: return

        val markdownView = TextView(context).apply {
            val padding = (16 * resources.displayMetrics.density).toInt()
            setPadding(padding, padding, padding, padding)
        }

        val markwon = Markwon.builder(context)
            .usePlugin(ImagesPlugin.create { plugin ->
                plugin.addSchemeHandler(FileSchemeHandler.create())
            })
            .build()

        fun resolveImagePaths(markdown: String): String {
            val projectDir = project.directory
            val imageRegex = Regex("!\\[(.*?)]\\((?!file://)(.*?)\\)")
            return imageRegex.replace(markdown) { matchResult ->
                val altText = matchResult.groupValues[1]
                val imageName = matchResult.groupValues[2]
                val imageFile = project.getFile(imageName)
                if (imageFile != null && imageFile.exists()) {
                    "![${altText}](${imageFile.toURI()})"
                } else {
                    matchResult.value
                }
            }
        }

        markwon.setMarkdown(markdownView, resolveImagePaths(markdownText))

        AlertDialog.Builder(
            android.view.ContextThemeWrapper(context, R.style.Theme_NewCatroid_Dialog)
        )
            .setTitle(getString(R.string.plf_project_info, project.name))
            .setView(markdownView)
            .setPositiveButton(R.string.ok, null)
            .show()
    }

    private fun onRenameFinished(success: Boolean) {
        val currentContext = context ?: return
        if (success) {
            if (hasUnzipAndImportTaskFinished) {
                ToastUtil.showSuccess(
                    currentContext,
                    currentContext.getString(R.string.renamed_project)
                )
                filesForUnzipAndImportTask?.clear()
            }
            setAdapterItems(adapter.projectsSorted)
        } else {
            ToastUtil.showError(currentContext, R.string.error_rename_incompatible_project)
        }
        setShowProgressBar(false)
    }

    override fun onResume() {
        if (actionModeType != IMPORT_LOCAL) {
            projectManager.currentProject = null
        }

        updateAdapterAsync()
        BottomBar.showBottomBar(requireActivity())
        super.onResume()
    }

    override fun initializeAdapter() {
        sharedPreferenceDetailsKey = SharedPreferenceKeys.SHOW_DETAILS_PROJECTS_PREFERENCE_KEY
        adapter = ProjectAdapter(ArrayList<ProjectData?>())
        onAdapterReady()
    }

    private val itemList: List<ProjectData>
        get() {
            val items: MutableList<ProjectData> = ArrayList()
            getLocalProjectList(items)
            items.sortWith(Comparator { project1: ProjectData, project2: ProjectData ->
                project2.lastUsed.compareTo(project1.lastUsed)
            })
            return items
        }

    private val sortedItemList: List<ProjectData>
        get() {
            val items: MutableList<ProjectData> = ArrayList()
            getLocalProjectList(items)
            items.sortWith(Comparator { project1: ProjectData, project2: ProjectData ->
                project1.name.compareTo(
                    project2.name
                )
            })
            return items
        }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.import_project -> showImportChooser()
            R.id.sort_projects -> sortProjects()
            R.id.libs_menu -> libsMenu()
            R.id.menu_trash_bin -> {
                org.catrobat.catroid.utils.ProjectTrashManager.showTrashBinDialog(requireContext()) {
                    updateAdapterAsync()
                }
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    private fun libsMenu() {
        val intent = Intent(context, LibraryEditorActivity::class.java)
        startActivity(intent)
    }

    private fun sortProjects() {
        adapter.projectsSorted = !adapter.projectsSorted
        PreferenceManager.getDefaultSharedPreferences(requireContext())
            .edit()
            .putBoolean(
                SharedPreferenceKeys.SORT_PROJECTS_PREFERENCE_KEY,
                adapter.projectsSorted
            )
            .apply()
        updateAdapterAsync()
    }

    private fun showImportChooser() {
        setShowProgressBar(true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            importUsingSystemFilePicker()
        } else {
            importUsingFilePickerActivity()
        }
    }

    private fun importUsingSystemFilePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            putExtra(DocumentsContract.EXTRA_INITIAL_URI, Environment.DIRECTORY_DOWNLOADS)
        }
        val title = requireContext().resources.getString(R.string.import_project)
        startActivityForResult(Intent.createChooser(intent, title), REQUEST_IMPORT_PROJECT)
    }

    private fun importUsingFilePickerActivity() {
        object : RequiresPermissionTask(
            PERMISSIONS_REQUEST_IMPORT_FROM_EXTERNAL_STORAGE,
            listOf(permission.READ_EXTERNAL_STORAGE),
            R.string.runtime_permission_general
        ) {
            override fun task() {
                startActivityForResult(
                    Intent(requireContext(), FilePickerActivity::class.java),
                    REQUEST_IMPORT_PROJECT
                )
            }
        }.execute(requireActivity())
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMPORT_PROJECT && resultCode == RESULT_OK) {
            importProject(data)
        }
    }

    private fun importProject(data: Intent?) {
        if (data == null) {
            onImportError()
            return
        }
        var uris: ArrayList<Uri> = ArrayList()
        if (data.data == null && !data.hasExtra(Intent.EXTRA_STREAM) && data.clipData == null) {
            onImportError()
            return
        }
        if (data.hasExtra(Intent.EXTRA_STREAM)) {
            val streamExtra = data.extras?.get(Intent.EXTRA_STREAM)
            if (streamExtra is ArrayList<*>) {
                @Suppress("UNCHECKED_CAST")
                uris = streamExtra as ArrayList<Uri>
            } else if (streamExtra is Uri) {
                uris.add(streamExtra)
            }
        } else {
            extractAllUris(data, uris)
        }
        try {
            importProjectUris(uris)
        } catch (e: IOException) {
            Log.e(TAG, "Cannot resolve project to import.", e)
        }
    }

    private fun onImportError() {
        val currentContext = context ?: return
        dismissImportProgressDialog()
        setShowProgressBar(false)
        ToastUtil.showError(currentContext, R.string.error_import_project)
    }

    private fun extractAllUris(data: Intent, uris: ArrayList<Uri>) {
        val singleUri = data.data
        if (singleUri != null) {
            uris.add(singleUri)
        } else {
            val clipData = data.clipData
            if (clipData != null) {
                val itemCount = clipData.itemCount
                for (idx in 0 until itemCount) {
                    uris.add(clipData.getItemAt(idx).uri)
                }
            }
        }
    }

    private fun importProjectUris(uris: ArrayList<Uri>) {
        showImportProgressDialog()
        prepareFilesForImport(uris)
        filesForUnzipAndImportTask?.apply {
            if (isNotEmpty()) {
                val filesToUnzipAndImport = toTypedArray()
                ProjectUnZipperAndImporter({ result -> onImportProjectFinished(result) })
                    .unZipAndImportAsync(filesToUnzipAndImport)
            } else {
                dismissImportProgressDialog()
            }
        }
    }

    private fun prepareFilesForImport(urisToImport: ArrayList<Uri>) {
        for (uri in urisToImport) {
            val contentResolver = requireActivity().contentResolver
            val fileName = StorageOperations.resolveFileName(contentResolver, uri)

            /*if (!fileName.endsWith(Constants.CATROBAT_EXTENSION) && !fileName.endsWith(Constants.NEW_CATROBAT_EXTENSION)) {
                ToastUtil.showError(requireContext(), R.string.only_select_catrobat_files)
                continue
            }*/

            val projectFile = StorageOperations.copyUriToDir(
                contentResolver, uri, Constants.CACHE_DIRECTORY, fileName
            )

            if (fileName.endsWith(Constants.CATROBAT_EXTENSION)) {

                filesForUnzipAndImportTask?.add(projectFile)
            } else if (fileName.endsWith(Constants.NEW_CATROBAT_EXTENSION)) {

                /*val unzippedDir = StorageOperations.unzipFileToDir(projectFile, Constants.CACHE_DIRECTORY)
                if (!isValidNewStructure(unzippedDir)) {
                    ToastUtil.showError(requireContext(), R.string.error_import_project)
                    continue
                }*/
                filesForUnzipAndImportTask?.add(projectFile)
            } else {
                try {
                    filesForUnzipAndImportTask?.add(projectFile)
                } catch (e: Exception) {
                    ToastUtil.showError(requireContext(), R.string.only_select_catrobat_files)
                }
            }

            hasUnzipAndImportTaskFinished = false
        }
    }

    private fun isValidNewStructure(projectDir: File): Boolean {
        val codeXml = File(projectDir, "code.xml")
        val scenesDir = File(projectDir, "scenes")
        val filesDir = File(projectDir, "files")
        return codeXml.exists() && scenesDir.isDirectory && filesDir.isDirectory
    }

    private fun copyFileContentToCacheFile(uri: Uri, fileName: String) {
        val projectFile = StorageOperations.copyUriToDir(
            requireActivity().contentResolver, uri,
            Constants.CACHE_DIRECTORY, fileName
        )
        filesForUnzipAndImportTask?.add(projectFile)
        hasUnzipAndImportTaskFinished = false
    }

    override fun prepareActionMode(@ActionModeType type: Int) {
        if (type == COPY) {
            adapter.selectionMode = RVAdapter.MULTIPLE
        } else if (type == MERGE) {
            adapter.selectionMode = RVAdapter.PAIRS
        }
        super.prepareActionMode(type)
    }

    override fun packItems(selectedItems: MutableList<ProjectData?>?) {
        throw IllegalStateException("$TAG: Projects cannot be backpacked")
    }

    override fun isBackpackEmpty(): Boolean = true

    override fun switchToBackpack() {
        throw IllegalStateException("$TAG: Projects cannot be backpacked")
    }

    override fun copyItems(selectedItems: MutableList<ProjectData?>?) {
        finishActionMode()
        setShowProgressBar(true)
        selectedItems ?: return
        val usedProjectNames = ArrayList(adapter.items)
        for (projectData in selectedItems) {
            projectData ?: continue
            val name = uniqueNameProvider.getUniqueNameInNameables(projectData.name, usedProjectNames)
            usedProjectNames.add(ProjectData(name, null, 0.0, false))
            val projectCopier = ProjectCopier(projectData.directory, name)
            projectCopier.copyProjectAsync({ success: Boolean -> onCopyProjectComplete(success) })
        }
    }

    @PluralsRes
    override fun getDeleteAlertTitleId(): Int = R.plurals.delete_projects

    override fun deleteItems(selectedItems: MutableList<ProjectData?>?) {
        selectedItems ?: return
        val validItems = selectedItems.filterNotNull()
        if (validItems.isEmpty()) return

        val projectDirs = validItems.map { it.directory }

        org.catrobat.catroid.utils.ProjectTrashManager.showDeleteMultipleProjectsDialog(
            requireContext(),
            projectDirs
        ) {
            requireActivity().runOnUiThread {
                setShowProgressBar(true)
                ProjectListFragment.clearCache()
                for (item in validItems) {
                    projectManager.deleteDownloadedProjectInformation(item.name)
                    adapter.remove(item)
                }

                val deletedItemCount = validItems.size
                ToastUtil.showSuccess(
                    requireContext(),
                    resources.getQuantityString(
                        R.plurals.deleted_projects,
                        deletedItemCount,
                        deletedItemCount
                    )
                )

                finishActionMode()
                setAdapterItems(adapter.projectsSorted)
                checkForEmptyList()
                setShowProgressBar(false)
            }
        }
    }

    fun checkForEmptyList() {
        if (adapter.items.isEmpty()) {
            setShowProgressBar(true)
            if (projectManager.initializeDefaultProject()) {
                setAdapterItems(adapter.projectsSorted)
                setShowProgressBar(false)
            } else {
                ToastUtil.showError(requireContext(), R.string.wtf_error)
                requireActivity().finish()
            }
        }
    }

    override fun getRenameDialogTitle(): Int = R.string.rename_project

    override fun getRenameDialogHint(): Int = R.string.project_name_label

    override fun renameItem(item: ProjectData?, name: String?) {
        finishActionMode()
        item ?: return
        name ?: return
        if (name != item.name) {
            setShowProgressBar(true)
            ProjectListFragment.clearCache()
            ProjectRenamer(item.directory, name)
                .renameProjectAsync({ success: Boolean -> onRenameFinished(success) })
        }
    }

    override fun onLoadFinished(success: Boolean) {
        val currentContext = context ?: return
        try {
            if (success) {
                val intent = Intent(currentContext, ProjectActivity::class.java)
                intent.putExtra(
                    ProjectActivity.EXTRA_FRAGMENT_POSITION,
                    ProjectActivity.FRAGMENT_SCENES
                )
                startActivity(intent)
            } else {
                setShowProgressBar(false)
                ToastUtil.showError(currentContext, R.string.error_load_project)
            }
        } catch (e: Exception) {
            ToastUtil.showError(CatroidApplication.getAppContext(), "Something went wrong")
            e.printStackTrace()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: android.view.MenuInflater) {
        inflater.inflate(R.menu.menu_projects_activity, menu)
        menu.findItem(R.id.merge)?.isVisible = org.catrobat.catroid.BuildConfig.FEATURE_MERGE_ENABLED
        super.onCreateOptionsMenu(menu, inflater)
    }

    private fun onCopyProjectComplete(success: Boolean) {
        val currentContext = context ?: return
        if (success) {
            setAdapterItems(adapter.projectsSorted)
        } else {
            ToastUtil.showError(currentContext, R.string.error_copy_project)
        }
        setShowProgressBar(false)
    }

    override fun onItemClick(item: ProjectData?, selectionManager: MultiSelectionManager?) {
        when (actionModeType) {
            RENAME -> {
                super.onItemClick(item, null)
                return
            }
            NONE -> {
                setShowProgressBar(true)
                val directoryFile = item?.directory ?: return
                ProjectLoader(directoryFile, requireContext()).setListener(this).loadProjectAsync()
            }
            IMPORT_LOCAL -> {
                val intent = Intent()
                intent.putExtra(
                    ProjectListActivity.IMPORT_LOCAL_INTENT,
                    item?.directory?.absoluteFile?.absolutePath
                )
                requireActivity().setResult(RESULT_OK, intent)
                requireActivity().finish()
            }
            else -> super.onItemClick(item, selectionManager)
        }
    }

    override fun onItemLongClick(item: ProjectData?, holder: CheckableViewHolder?) {
        onItemClick(item, null)
    }

    override fun onSettingsClick(item: ProjectData?, view: View?) {
        val itemList: MutableList<ProjectData?> = ArrayList()
        itemList.add(item)
        val hiddenMenuOptionIds = intArrayOf(
            R.id.new_group, R.id.new_scene, R.id.show_details,
            R.id.from_library, R.id.from_local, R.id.edit
        )
        try {
            val popupMenu = UiUtils.createSettingsPopUpMenu(
                view, requireContext(),
                R.menu.menu_project_activity, hiddenMenuOptionIds
            )
            popupMenu.setOnMenuItemClickListener { menuItem: MenuItem ->
                when (menuItem.itemId) {
                    R.id.copy -> copyItems(itemList)
                    R.id.rename -> showRenameDialog(item)
                    R.id.delete -> deleteItems(itemList)
                    R.id.project_options -> showProjectOptionsFragment(item)
                    R.id.project_files -> showProjectFilesFragment(item)
                    R.id.project_libs -> showProjectLibsFragment(item)
                }
                true
            }
            popupMenu.show()
        } catch (e: Exception) {
            e.printStackTrace()
            ToastUtil.showError(requireContext(), R.string.error_unknown_error)
        }
    }

    private fun showProjectOptionsFragment(item: ProjectData?) {
        item ?: return
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val project = XstreamSerializer.getInstance().loadProject(
                    item.directory,
                    requireContext()
                )
                withContext(Dispatchers.Main) {
                    projectManager.currentProject = project
                    requireActivity().supportFragmentManager.beginTransaction()
                        .replace(
                            R.id.fragment_container, ProjectOptionsFragment(), ProjectOptionsFragment.TAG
                        )
                        .addToBackStack(ProjectOptionsFragment.TAG)
                        .commit()
                }
            } catch (exception: IOException) {
                withContext(Dispatchers.Main) {
                    ToastUtil.showError(requireContext(), R.string.error_load_project)
                }
                Log.e(TAG, Log.getStackTraceString(exception))
            } catch (exception: LoadingProjectException) {
                withContext(Dispatchers.Main) {
                    ToastUtil.showError(requireContext(), R.string.error_load_project)
                }
                Log.e(TAG, Log.getStackTraceString(exception))
            }
        }
    }

    private fun showProjectLibsFragment(item: ProjectData?) {
        item ?: return
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val project = XstreamSerializer.getInstance().loadProject(
                    item.directory,
                    requireContext()
                )
                withContext(Dispatchers.Main) {
                    projectManager.currentProject = project
                    requireActivity().supportFragmentManager.beginTransaction()
                        .replace(
                            R.id.fragment_container, ProjectLibsFragment(), ProjectOptionsFragment.TAG
                        )
                        .addToBackStack(ProjectOptionsFragment.TAG)
                        .commit()
                }
            } catch (exception: IOException) {
                withContext(Dispatchers.Main) {
                    ToastUtil.showError(requireContext(), R.string.error_load_project)
                }
                Log.e(TAG, Log.getStackTraceString(exception))
            } catch (exception: LoadingProjectException) {
                withContext(Dispatchers.Main) {
                    ToastUtil.showError(requireContext(), R.string.error_load_project)
                }
                Log.e(TAG, Log.getStackTraceString(exception))
            }
        }
    }

    private fun showProjectFilesFragment(item: ProjectData?) {
        item ?: return
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val project = XstreamSerializer.getInstance().loadProject(
                    item.directory,
                    requireContext()
                )
                withContext(Dispatchers.Main) {
                    projectManager.currentProject = project
                    requireActivity().supportFragmentManager.beginTransaction()
                        .replace(
                            R.id.fragment_container, ProjectFilesFragment(), ProjectOptionsFragment.TAG
                        )
                        .addToBackStack(ProjectOptionsFragment.TAG)
                        .commit()
                }
            } catch (exception: IOException) {
                withContext(Dispatchers.Main) {
                    ToastUtil.showError(requireContext(), R.string.error_load_project)
                }
                Log.e(TAG, Log.getStackTraceString(exception))
            } catch (exception: LoadingProjectException) {
                withContext(Dispatchers.Main) {
                    ToastUtil.showError(requireContext(), R.string.error_load_project)
                }
                Log.e(TAG, Log.getStackTraceString(exception))
            }
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        adapter.projectsSorted = PreferenceManager.getDefaultSharedPreferences(requireContext())
            .getBoolean(SharedPreferenceKeys.SORT_PROJECTS_PREFERENCE_KEY, false)
        menu.findItem(R.id.sort_projects)
            ?.setTitle(
                if (adapter.projectsSorted) {
                    R.string.unsort_projects
                } else {
                    R.string.sort_projects
                }
            )
    }

    private fun setAdapterItems(sortProjects: Boolean) {
        if (sortProjects) {
            adapter.setItems(sortedItemList)
        } else {
            adapter.setItems(itemList)
        }
        adapter.notifyDataSetChanged()
    }

    interface ProjectImportFinishedListener {
        fun notifyActivityFinished(success: Boolean)
    }

    companion object {
        @JvmStatic
        val TAG: String = ProjectListFragment::class.java.simpleName
        private const val PERMISSIONS_REQUEST_IMPORT_FROM_EXTERNAL_STORAGE = 801
        private const val REQUEST_IMPORT_PROJECT = 7

        private val projectDataCache = java.util.concurrent.ConcurrentHashMap<String, ProjectData>()

        @JvmStatic
        fun clearCache() {
            projectDataCache.clear()
        }

        @JvmStatic
        fun getLocalProjectList(items: MutableList<ProjectData>) {
            FlavoredConstants.DEFAULT_ROOT_DIRECTORY.listFiles()?.forEach { projectDir ->
                val codeXml = File(projectDir, Constants.CODE_XML_FILE_NAME)

                if (codeXml.exists()) {
                    val key = codeXml.absolutePath
                    val cached = projectDataCache[key]
                    if (cached != null) {
                        items.add(cached)
                    } else {
                        try {
                            val metaDataParser = ProjectMetaDataParser(codeXml)
                            val meta = metaDataParser.projectMetaData
                            projectDataCache[key] = meta
                            items.add(meta)
                        } catch (exception: IOException) {
                            Log.e(TAG, "Could not parse local project.", exception)
                        }
                    }
                }
            }
        }
    }

    private fun updateAdapterAsync() {
        setShowProgressBar(true)

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
            val quickItems: List<ProjectData?> = withContext(Dispatchers.IO) {
                val list = ArrayList<ProjectData>()
                FlavoredConstants.DEFAULT_ROOT_DIRECTORY.listFiles()?.forEach { projectDir ->
                    val codeXml = File(projectDir, Constants.CODE_XML_FILE_NAME)
                    if (codeXml.exists()) {
                        list.add(ProjectData(projectDir.name, projectDir, codeXml.lastModified().toDouble(), false))
                    }
                }
                if (adapter.projectsSorted) {
                    list.sortWith(Comparator { p1, p2 -> p1.name.compareTo(p2.name) })
                } else {
                    list.sortWith(Comparator { p1, p2 -> p2.lastUsed.compareTo(p1.lastUsed) })
                }
                list
            }

            adapter.setItems(quickItems)
            adapter.notifyDataSetChanged()

            val finalItems = withContext(Dispatchers.IO) {
                val projectDirs = FlavoredConstants.DEFAULT_ROOT_DIRECTORY.listFiles()?.filter {
                    File(it, Constants.CODE_XML_FILE_NAME).exists()
                } ?: emptyList()

                val deferredList = projectDirs.map { projectDir ->
                    async(Dispatchers.IO) {
                        val codeXml = File(projectDir, Constants.CODE_XML_FILE_NAME)
                        val key = codeXml.absolutePath
                        val cached = projectDataCache[key]
                        if (cached != null) {
                            cached
                        } else {
                            try {
                                val metaDataParser = ProjectMetaDataParser(codeXml)
                                val meta = metaDataParser.projectMetaData
                                projectDataCache[key] = meta
                                meta
                            } catch (e: Exception) {
                                Log.e(TAG, "Could not parse local project $projectDir", e)
                                ProjectData(projectDir.name, projectDir, codeXml.lastModified().toDouble(), false)
                            }
                        }
                    }
                }

                val list = deferredList.awaitAll().toMutableList()
                if (adapter.projectsSorted) {
                    list.sortWith(Comparator { p1, p2 -> p1.name.compareTo(p2.name) })
                } else {
                    list.sortWith(Comparator { p1, p2 -> p2.lastUsed.compareTo(p1.lastUsed) })
                }
                list
            }

            val currentContext = context ?: return@launch
            val currentActivity = activity ?: return@launch

            adapter.setItems(finalItems)
            adapter.notifyDataSetChanged()

            if (adapter.items.isEmpty()) {
                if (projectManager.initializeDefaultProject()) {
                    updateAdapterAsync()
                } else {
                    ToastUtil.showError(currentContext, R.string.wtf_error)
                    currentActivity.finish()
                }
            } else {
                setShowProgressBar(false)
            }
        }
    }
}
