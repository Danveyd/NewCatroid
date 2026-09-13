package org.catrobat.catroid.ui.dialogs

import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import android.text.Editable
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.catrobat.catroid.ProjectManager
import org.catrobat.catroid.R
import org.catrobat.catroid.common.DefaultProjectHandler
import org.catrobat.catroid.common.FlavoredConstants
import org.catrobat.catroid.common.Nameable
import org.catrobat.catroid.databinding.DialogNewProjectBinding
import org.catrobat.catroid.merge.NewProjectNameTextWatcher
import org.catrobat.catroid.ui.ProjectActivity
import org.catrobat.catroid.ui.recyclerview.dialog.ReplaceExistingProjectDialogFragment.projectExistsInDirectory
import org.catrobat.catroid.ui.recyclerview.util.UniqueNameProvider
import org.catrobat.catroid.utils.FileMetaDataExtractor
import org.catrobat.catroid.utils.ToastUtil
import org.catrobat.catroid.utils.git.GitController
import org.catrobat.catroid.utils.git.GitResult
import org.koin.android.ext.android.inject
import java.io.File
import java.io.IOException

class NewProjectDialogFragment : DialogFragment() {
    private val projectManager: ProjectManager by inject()
    private var _binding: DialogNewProjectBinding? = null
    private val binding get() = _binding!!

    private var isPcMode = false
    private var isCloningInProgress = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogNewProjectBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isPcMode = PreferenceManager.getDefaultSharedPreferences(requireContext())
            .getBoolean("pref_pc_mode_enabled", false)

        if (!isPcMode) {
            setStyle(STYLE_NORMAL, Window.FEATURE_NO_TITLE)
        } else {
            setStyle(STYLE_NO_TITLE, R.style.NewCatroid_Dialog_Pc)
        }
    }

    override fun onStart() {
        super.onStart()
        if (isPcMode) {
            val width = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 450f, resources.displayMetrics
            ).toInt()
            dialog?.window?.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
        } else {
            dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()

        val uniqueNameProvider: UniqueNameProvider = object : UniqueNameProvider() {
            override fun isUnique(newName: String) = !projectExistsInDirectory(newName)
        }

        binding.input.hint = getString(R.string.project_name_label)
        binding.inputEditText.apply {
            setText(uniqueNameProvider.getUniqueName(getString(R.string.default_project_name), null))
            addTextChangedListener(object : NewProjectNameTextWatcher<Nameable>() {
                override fun afterTextChanged(s: Editable?) {
                    binding.input.error = validateInput(s.toString(), getContext())
                    updateConfirmButtonState()
                }
            })
            requestFocus()
        }

        binding.gitCloneSwitch.setOnCheckedChangeListener { _, isChecked ->
            binding.gitInputContainer.visibility = if (isChecked) View.VISIBLE else View.GONE
            binding.standardSettingsContainer.visibility = if (isChecked) View.GONE else View.VISIBLE
            updateConfirmButtonState()
        }

        binding.gitUrlEditText.addTextChangedListener { updateConfirmButtonState() }

        updateConfirmButtonState()
    }

    private fun setupToolbar() {
        if (isPcMode) {
            binding.toolbar.toolbar.setTitle(R.string.new_project_title)
            binding.toolbar.toolbar.inflateMenu(R.menu.menu_confirm)
            binding.toolbar.toolbar.setOnMenuItemClickListener { item ->
                if (item.itemId == R.id.confirm) {
                    handleConfirmClick()
                    true
                } else false
            }
        } else {
            setHasOptionsMenu(true)
            (requireActivity() as AppCompatActivity).setSupportActionBar(binding.toolbar.toolbar)
            (requireActivity() as AppCompatActivity).supportActionBar?.setTitle(R.string.new_project_title)
        }
        binding.toolbar.toolbar.setNavigationIcon(R.drawable.ic_close)
        binding.toolbar.toolbar.setNavigationOnClickListener { dismiss() }
    }

    private fun updateConfirmButtonState() {
        val confirm = if (isPcMode) binding.toolbar.toolbar.menu.findItem(R.id.confirm)
        else binding.toolbar.toolbar.menu?.findItem(R.id.confirm)
        confirm ?: return

        var isOk = binding.input.error == null && !isCloningInProgress

        if (binding.gitCloneSwitch.isChecked) {
            val url = binding.gitUrlEditText.text.toString().trim()
            if (url.isEmpty()) isOk = false
        }

        if (isOk) {
            confirm.setIcon(R.drawable.ic_done)
            confirm.isEnabled = true
        } else {
            confirm.setIcon(R.drawable.ic_done_disabled)
            confirm.isEnabled = false
        }
    }

    private fun handleConfirmClick() {
        if (binding.gitCloneSwitch.isChecked) {
            cloneProjectFromGit()
        } else {
            createStandardProject()
            dismiss()
        }
    }

    private fun createStandardProject() {
        val projectName = binding.inputEditText.text.toString().trim()
        val landscapeMode = binding.orientationToggleGroup.checkedButtonId == R.id.btn_landscape
        val projectCreatorType = DefaultProjectHandler.ProjectCreatorType.PROJECT_CREATOR_DEFAULT

        try {
            when (binding.exampleProjectSwitch.isChecked) {
                true -> projectManager.createNewExampleProject(projectName, projectCreatorType, landscapeMode)
                false -> projectManager.createNewEmptyProject(projectName, landscapeMode, false)
            }
            activity?.startActivity(Intent(activity, ProjectActivity::class.java))
        } catch (_: IOException) {
            ToastUtil.showError(activity, R.string.error_new_project)
        }
    }

    private fun cloneProjectFromGit() {
        val projectName = binding.inputEditText.text.toString().trim()
        val gitUrl = binding.gitUrlEditText.text.toString().trim()
        val gitToken = binding.gitTokenEditText.text.toString().trim()

        isCloningInProgress = true
        updateConfirmButtonState()
        binding.gitProgressBar.visibility = View.VISIBLE
        binding.contentContainer.alpha = 0.5f
        binding.inputEditText.isEnabled = false
        binding.gitUrlEditText.isEnabled = false
        binding.gitTokenEditText.isEnabled = false

        val targetDir = File(
            FlavoredConstants.DEFAULT_ROOT_DIRECTORY,
            FileMetaDataExtractor.encodeSpecialCharsForFileSystem(projectName)
        )

        lifecycleScope.launch(Dispatchers.IO) {
            val gitController = GitController(targetDir)
            val result = gitController.cloneRepository(gitUrl, gitToken, targetDir)

            withContext(Dispatchers.Main) {
                isCloningInProgress = false

                when (result) {
                    is GitResult.Success -> {
                        try {
                            projectManager.loadProject(targetDir)
                            ToastUtil.showSuccess(activity, getString(R.string.npd_clone_success))
                            activity?.startActivity(Intent(activity, ProjectActivity::class.java))
                            dismiss()
                        } catch (e: Exception) {
                            ToastUtil.showError(activity, getString(R.string.npd_clone_not_readable, e.message ?: ""))
                            resetUIState()
                        }
                    }
                    is GitResult.Error -> {
                        ToastUtil.showError(activity, getString(R.string.npd_clone_error, result.message))
                        resetUIState()
                    }

                    else -> {
                        ToastUtil.showError(activity, getString(R.string.npd_clone_error, result.toString()))
                        resetUIState()
                    }
                }
            }
        }
    }

    private fun resetUIState() {
        binding.gitProgressBar.visibility = View.GONE
        binding.contentContainer.alpha = 1.0f
        binding.inputEditText.isEnabled = true
        binding.gitUrlEditText.isEnabled = true
        binding.gitTokenEditText.isEnabled = true
        updateConfirmButtonState()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_confirm, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        for (index in 0 until menu.size()) {
            menu.getItem(index).isVisible = false
        }
        val confirm = menu.findItem(R.id.confirm)
        confirm.isVisible = true
        super.onPrepareOptionsMenu(menu)
        updateConfirmButtonState()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == R.id.confirm) {
            handleConfirmClick()
            true
        } else {
            super.onOptionsItemSelected(item)
        }
    }

    companion object {
        val TAG: String = NewProjectDialogFragment::class.java.simpleName
    }
}