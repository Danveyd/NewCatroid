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
import androidx.activity.OnBackPressedCallback
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
import org.catrobat.catroid.stage.StageActivity
import org.catrobat.catroid.ui.BottomBar.hideBottomBar
import org.catrobat.catroid.ui.PROJECT_DIR
import org.catrobat.catroid.ui.ProjectUploadActivity
import org.catrobat.catroid.ui.adapter.FilesAdapter
import org.catrobat.catroid.utils.SimpleTextEditorActivity
import org.catrobat.catroid.utils.Utils
import org.koin.android.ext.android.inject
import java.io.File
import java.io.FileOutputStream
import kotlin.random.Random

@LunoClass
class ProjectFilesFragment : Fragment() {

    private val projectManager: ProjectManager by inject()
    private var _binding: FragmentProjectFilesBinding? = null
    private val binding get() = _binding!!
    private var project: Project? = null
    private var sceneName: String? = null
    private lateinit var recyclerView: RecyclerView
    private lateinit var filesAdapter: FilesAdapter

    private var filesList = mutableListOf<File>()
    private lateinit var currentDirectory: File
    private lateinit var rootDirectory: File

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProjectFilesBinding.inflate(inflater, container, false)
        recyclerView = binding.recyclerViewFiles
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)
        (requireActivity() as AppCompatActivity).supportActionBar?.setTitle(R.string.project_files)

        project = projectManager.currentProject
        sceneName = projectManager.currentlyEditedScene.name

        project?.let { proj ->
            rootDirectory = File(proj.directory, "files").absoluteFile
            currentDirectory = rootDirectory
        }

        setupAdd()
        setupRecyclerView()
        setupBackPressed()

        updateFilesList(currentDirectory)

        hideBottomBar(requireActivity())
    }

    private fun setupBackPressed() {
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (currentDirectory != rootDirectory) {
                    currentDirectory = currentDirectory.parentFile ?: rootDirectory
                    updateFilesList(currentDirectory)
                } else {
                    isEnabled = false
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                }
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)
    }

    private fun setupAdd() {
        binding.projectFilesAdd.setOnClickListener {
            handleAdd()
        }
        binding.projectFilesCmd.setOnClickListener {
            handleCmd()
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

    private fun handleCmd() {
        project?.filesDir?.absolutePath?.let { projectPath ->
            val dialog = CommandPromptDialogFragment.newInstance(projectPath)
            dialog.show(parentFragmentManager, CommandPromptDialogFragment.TAG)
        } ?: run {
            Toast.makeText(requireContext(), getString(R.string.pf_dir_not_found), Toast.LENGTH_SHORT).show()
        }
    }

    private fun copyToClipboard(text: String) {
        val clipboard = CatroidApplication.getAppContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Response", text)
        clipboard.setPrimaryClip(clip)
    }

    private fun openFile(file: File) {
        if (file.name == "..") {
            currentDirectory = currentDirectory.parentFile ?: rootDirectory
            updateFilesList(currentDirectory)
            return
        }

        if (file.isDirectory) {
            currentDirectory = file
            updateFilesList(currentDirectory)
            return
        }

        if (!file.exists()) {
            Toast.makeText(requireContext(), getString(R.string.common_file_not_found), Toast.LENGTH_SHORT).show()
            return
        }

        val extension = file.extension.lowercase()
        val editableExtensions = listOf("txt", "py", "json", "xml", "lua", "md", "csv", "log", "rscene", "java", "kt")

        if (extension in editableExtensions) {
            val intent = Intent(requireContext(), SimpleTextEditorActivity::class.java)
            intent.putExtra("FILE_PATH", file.absolutePath)
            startActivity(intent)
        } else {
            try {
                val authority = "${BuildConfig.APPLICATION_ID}.fileProvider"
                val uri = FileProvider.getUriForFile(requireContext(), authority, file)

                val intent = Intent(Intent.ACTION_VIEW)
                val mimeType = context?.contentResolver?.getType(uri) ?: "*/*"
                intent.setDataAndType(uri, mimeType)
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(requireContext(), getString(R.string.pf_no_app_to_open), Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), getString(R.string.pf_open_error, e.localizedMessage), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun copyFile(file: File) {
        copyToClipboard(file.name)
        Toast.makeText(requireContext(), getString(R.string.pf_name_copied), Toast.LENGTH_SHORT).show()
    }

    private fun deleteFile(file: File) {
        val success = if (file.isDirectory) {
            file.deleteRecursively()
        } else {
            file.delete()
        }

        if (success) {
            updateFilesList(currentDirectory)
            Toast.makeText(requireContext(), getString(R.string.pf_deleted), Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), getString(R.string.pf_delete_error), Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateFilesList(directory: File) {
        filesList.clear()

        if (directory != rootDirectory) {
            val parentFile = File(directory.parentFile, "..")
            filesList.add(parentFile)
        }

        val sortedFiles = directory.listFiles()
            ?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
            ?: emptyList()

        filesList.addAll(sortedFiles)
        filesAdapter.notifyDataSetChanged()
    }

    override fun onPause() {
        saveProject()
        super.onPause()
    }

    private fun saveProject() {
        project ?: return
        saveProjectSerial(project, requireContext())
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == ADD_FILE_REQUEST && resultCode == Activity.RESULT_OK) {
            val uri: Uri? = data?.data
            if (uri != null) {
                saveFileToProject(uri)
            }
        }
    }

    private fun saveFileToProject(uri: Uri) {
        val fileName = getFileName(uri)

        val destinationFile = File(currentDirectory, fileName)

        destinationFile.parentFile?.mkdirs()

        try {
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            if (inputStream == null) {
                Toast.makeText(requireContext(), getString(R.string.pf_open_failed), Toast.LENGTH_SHORT).show()
                return
            }

            FileOutputStream(destinationFile).use { outputStream ->
                inputStream.use { input ->
                    input.copyTo(outputStream)
                }
            }

            updateFilesList(currentDirectory)
            Toast.makeText(requireContext(), getRandomMessage(), Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("ProjectFile", "Error saving file", e)
            Toast.makeText(requireContext(), getString(R.string.pf_save_error, e.localizedMessage), Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleAdd() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        val chooser = Intent.createChooser(intent, getString(R.string.common_choose_file))
        startActivityForResult(chooser, ADD_FILE_REQUEST)
    }

    override fun onResume() {
        super.onResume()
        projectManager.currentProject = project
        hideBottomBar(requireActivity())
    }

    private fun handleText() {
        showToast(getRandomError())
    }

    private fun onSaveProjectComplete() {
        val currentProject = projectManager.currentProject

        if (Utils.isDefaultProject(currentProject, activity)) {
            binding.root.apply {
                Snackbar.make(binding.root, R.string.error_upload_default_project, Snackbar.LENGTH_LONG).show()
            }
            return
        }

        val intent = Intent(requireContext(), ProjectUploadActivity::class.java)
        intent.putExtra(PROJECT_DIR, currentProject.directory)
        startActivity(intent)
    }

    fun showToast(toast: String) {
        if (StageActivity.messageHandler != null) {
            val params = ArrayList<Any>(listOf(toast))
            StageActivity.messageHandler.obtainMessage(StageActivity.SHOW_TOAST, params).sendToTarget()
        } else {
            Log.e("ShowToast", "messageHandler is null!")
        }
    }

    fun getRandomMessage(): String {
        val messages = resources.getStringArray(R.array.pf_success_messages)
        return messages[Random.nextInt(messages.size)]
    }

    fun getRandomError(): String {
        val errorMessages = resources.getStringArray(R.array.pf_error_messages)
        return errorMessages[Random.nextInt(errorMessages.size)]
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
        val TAG: String = ProjectFilesFragment::class.java.simpleName
        private const val ADD_FILE_REQUEST = 15
    }
}
