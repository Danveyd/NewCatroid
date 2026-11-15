package org.catrobat.catroid.ui.fragment

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.danvexteam.lunoscript_annotations.LunoClass
import com.google.android.material.snackbar.Snackbar
import org.catrobat.catroid.BuildConfig
import org.catrobat.catroid.CatroidApplication
import org.catrobat.catroid.ProjectManager
import org.catrobat.catroid.R
import org.catrobat.catroid.content.Project
import org.catrobat.catroid.databinding.FragmentProjectFilesBinding
import org.catrobat.catroid.io.asynctask.saveProjectSerial
import org.catrobat.catroid.libraries.LibraryManager
import org.catrobat.catroid.stage.StageActivity
import org.catrobat.catroid.ui.BottomBar.hideBottomBar
import org.catrobat.catroid.ui.PROJECT_DIR
import org.catrobat.catroid.ui.ProjectUploadActivity
import org.catrobat.catroid.ui.adapter.FilesAdapter
import org.catrobat.catroid.utils.Utils
import org.koin.android.ext.android.inject
import java.io.File
import kotlin.random.Random

@LunoClass
class ProjectLibsFragment : Fragment() {

    private val projectManager: ProjectManager by inject()
    private var _binding: FragmentProjectFilesBinding? = null
    private val binding get() = _binding!!
    private var project: Project? = null
    private var sceneName: String? = null
    private lateinit var recyclerView: RecyclerView
    private lateinit var filesAdapter: FilesAdapter

    private var filesList = mutableListOf<File>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProjectFilesBinding.inflate(inflater, container, false)
        recyclerView = binding.recyclerViewFiles
        binding.projectFilesCmd.hide()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)
        (requireActivity() as AppCompatActivity).supportActionBar?.setTitle(R.string.project_libs)

        project = projectManager.currentProject
        sceneName = projectManager.currentlyEditedScene.name

        setupAdd()
        setupRecyclerView()

        project?.let { proj ->
            updateFilesList(File(proj.directory, "libs").absoluteFile)
        }

        hideBottomBar(requireActivity())
    }

    private fun setupAdd() {
        binding.projectFilesAdd.setOnClickListener {
            handleAdd()
        }
    }

    private fun setupRecyclerView() {
        filesAdapter = FilesAdapter(project, filesList,
            { file -> deleteFile(file) },
            { file -> copyFile(file) },
            { file -> openFile(file) }
        )
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = filesAdapter
    }

    private fun openFile(file: File) {
        if (!file.exists()) {
            Toast.makeText(requireContext(), getString(R.string.common_file_not_found), Toast.LENGTH_SHORT).show()
            return
        }

        val authority = "${BuildConfig.APPLICATION_ID}.provider"
        val uri = FileProvider.getUriForFile(requireContext(), authority, file)

        val intent = Intent(Intent.ACTION_VIEW)
        val mimeType = context?.contentResolver?.getType(uri) ?: "*/*"
        intent.setDataAndType(uri, mimeType)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(requireContext(), getString(R.string.lib_no_app_to_open), Toast.LENGTH_LONG).show()
        }
    }

    private fun copyToClipboard(text: String) {
        val clipboard = CatroidApplication.getAppContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Response", text)
        clipboard.setPrimaryClip(clip)
    }

    private fun copyFile(file: File) {
        copyToClipboard(file.name)
        Toast.makeText(requireContext(), getString(R.string.lib_name_copied), Toast.LENGTH_SHORT).show()
    }

    private fun deleteFile(file: File) {
        if (file.exists() && file.delete()) {
            updateFilesList(File(project!!.directory, "libs"))
            Toast.makeText(requireContext(), getString(R.string.lib_deleted), Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), getString(R.string.lib_delete_error), Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateFilesList(directory: File) {
        val newFiles = directory.listFiles() ?: emptyArray()
        val oldFiles = filesList.toList()

        newFiles.forEach { file ->
            if (!oldFiles.contains(file)) {
                filesList.add(file)
                filesAdapter.notifyItemInserted(filesList.size - 1)
            }
        }

        oldFiles.forEach { file ->
            if (!newFiles.contains(file)) {
                val position = filesList.indexOf(file)
                if (position != -1) {
                    filesList.removeAt(position)
                    filesAdapter.notifyItemRemoved(position)
                }
            }
        }
    }

    override fun onPause() {
        saveProject()
        super.onPause()
    }

    private fun saveProject() {
        project ?: return
        saveProjectSerial(project, requireContext())
    }

    private fun handleAdd() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        val chooser = Intent.createChooser(intent, getString(R.string.lib_choose_newlib_file))
        startActivityForResult(chooser, ADD_FILE_REQUEST)
    }

    override fun onResume() {
        super.onResume()
        projectManager.currentProject = project
        project?.let {
            LibraryManager.syncAndLoadLibraries(it)
        }
        hideBottomBar(requireActivity())
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        data ?: return
        if (requestCode == ADD_FILE_REQUEST && resultCode == Activity.RESULT_OK) {
            data.data?.let { uri ->
                val directory: File? = project?.directory
                val filesDir = File(directory, "libs")

                if (!filesDir.exists()) {
                    filesDir.mkdirs()
                }
                copyFileToDir(uri, filesDir)
            }
        }
    }

    private fun copyFileToDir(uri: Uri, dir: File) {
        val inputStream = requireActivity().contentResolver.openInputStream(uri)
        val outputFileName = getFileName(uri)
        val outputFile = File(dir, outputFileName)

        inputStream.use { input ->
            outputFile.outputStream().use { output ->
                input?.copyTo(output)
            }
        }
        updateFilesList(dir)
    }

    private fun getFileName(uri: Uri): String {
        var fileName = ""
        if (uri.scheme == "content") {
            val cursor = requireActivity().contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (it.moveToFirst() && nameIndex != -1) {
                    fileName = it.getString(nameIndex)
                }
            }
        } else if (uri.scheme == "file") {
            fileName = File(uri.path).name
        }
        return fileName.ifEmpty { getString(R.string.common_unknown_file) }
    }

    companion object {
        val TAG: String = ProjectLibsFragment::class.java.simpleName
        private const val ADD_FILE_REQUEST = 15
    }
}
