package org.catrobat.catroid.ui.fragment

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.InputType
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import org.catrobat.catroid.ProjectManager
import org.catrobat.catroid.databinding.DialogCommandPromptBinding
import org.catrobat.catroid.python.CommandOutputListener
import org.catrobat.catroid.python.PythonCommandManager
import org.catrobat.catroid.python.PythonEngine
import org.catrobat.catroid.ui.MainMenuActivity
import java.io.File
import java.util.regex.Pattern

object SyntaxHighlighter {
    private val COLOR_ERROR = Color.parseColor("#f24e50")
    private val COLOR_WARNING = Color.parseColor("#E68B00")
    private val COLOR_SUCCESS = Color.parseColor("#00FF00")
    private val COLOR_COMMAND = Color.parseColor("#A8DFF4")

    private val RULES = listOf(
        Rule(Pattern.compile("\\b(error|traceback|exception|failed|fatal|failure|none)\\b", Pattern.CASE_INSENSITIVE), COLOR_ERROR, isBold = true),
        Rule(Pattern.compile("\\b(warning|downloading|collecting|looking in indexes)\\b", Pattern.CASE_INSENSITIVE), COLOR_WARNING),
        Rule(Pattern.compile("\\b(success|successfully|installed|complete)\\b", Pattern.CASE_INSENSITIVE), COLOR_SUCCESS),
        Rule(Pattern.compile("^> .*$", Pattern.MULTILINE), COLOR_COMMAND, isBold = true)
    )

    fun highlight(text: String): SpannableStringBuilder {
        val spannable = SpannableStringBuilder(text)
        for (rule in RULES) {
            val matcher = rule.pattern.matcher(spannable)
            while (matcher.find()) {
                spannable.setSpan(ForegroundColorSpan(rule.color), matcher.start(), matcher.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                if (rule.isBold) {
                    spannable.setSpan(StyleSpan(Typeface.BOLD), matcher.start(), matcher.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            }
        }
        return spannable
    }

    private data class Rule(val pattern: Pattern, val color: Int, val isBold: Boolean = false)
}

class CommandPromptDialogFragment : DialogFragment(), CommandOutputListener {

    private var _binding: DialogCommandPromptBinding? = null
    private val binding get() = _binding!!

    private lateinit var pythonEngine: PythonEngine
    private lateinit var commandManager: PythonCommandManager
    private lateinit var projectFilesDir: File


    private val commandHistory = mutableListOf<String>()
    private var historyIndex = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, android.R.style.Theme_DeviceDefault_Light_NoActionBar)

        val engine = MainMenuActivity.pythonEngine
        if (engine == null) {
            dismiss()
            return
        }
        pythonEngine = engine
        pythonEngine.initialize()

        val project = ProjectManager.getInstance().currentProject
        projectFilesDir = project.filesDir
        if (!projectFilesDir.exists()) projectFilesDir.mkdirs()

        commandManager = PythonCommandManager(pythonEngine, project)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogCommandPromptBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (!this::commandManager.isInitialized) return

        commandManager.outputListener = this

        binding.sendButton.setOnClickListener { submitCommand() }
        binding.terminalInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                submitCommand()
                return@setOnEditorActionListener true
            }
            false
        }

        binding.btnHistoryUp.setOnClickListener { navigateHistory(-1) }
        binding.btnHistoryDown.setOnClickListener { navigateHistory(1) }

        binding.terminalOutput.typeface = Typeface.MONOSPACE

        val welcomeMsg = "Shell 1.2\nLinux coreutils available (ls, cd, cp, rm, cat, nano, etc.)\n\n"
        binding.terminalOutput.text = SyntaxHighlighter.highlight(welcomeMsg)
        updatePrompt()
    }

    private fun navigateHistory(direction: Int) {
        if (commandHistory.isEmpty()) return
        historyIndex += direction
        historyIndex = historyIndex.coerceIn(-1, commandHistory.size - 1)

        if (historyIndex == -1) {
            binding.terminalInput.setText("")
        } else {
            val cmd = commandHistory[commandHistory.size - 1 - historyIndex]
            binding.terminalInput.setText(cmd)
            binding.terminalInput.setSelection(cmd.length)
        }
    }

    private fun submitCommand() {
        val command = binding.terminalInput.text.toString()
        if (command.isBlank()) return

        if (commandHistory.isEmpty() || commandHistory.last() != command) {
            commandHistory.add(command)
        }
        historyIndex = -1

        binding.terminalInput.text.clear()
        showLoading(true)
        commandManager.processCommand(command)
    }

    override fun onOutput(output: String) {
        val styledOutput = SyntaxHighlighter.highlight(output)
        binding.terminalOutput.append(styledOutput)
        binding.scrollView.post { binding.scrollView.fullScroll(View.FOCUS_DOWN) }
    }

    override fun onComplete() {
        showLoading(false)
        updatePrompt()
    }


    override fun onOpenEditor(file: File) {
        val dialog = android.app.Dialog(requireContext(), android.R.style.Theme_DeviceDefault_NoActionBar_Fullscreen)
        dialog.setContentView(org.catrobat.catroid.R.layout.dialog_nano_editor)

        val titleView = dialog.findViewById<android.widget.TextView>(org.catrobat.catroid.R.id.nano_title)
        val editorInput = dialog.findViewById<EditText>(org.catrobat.catroid.R.id.nano_editor_input)
        val btnSave = dialog.findViewById<android.widget.Button>(org.catrobat.catroid.R.id.nano_btn_save)
        val btnExit = dialog.findViewById<android.widget.Button>(org.catrobat.catroid.R.id.nano_btn_exit)

        titleView.text = "nano - ${file.name}"

        if (file.exists()) {
            editorInput.setText(file.readText())
        }

        btnSave.setOnClickListener {
            try {
                file.writeText(editorInput.text.toString())
                onOutput("Saved ${file.name}\n")
                android.widget.Toast.makeText(requireContext(), getString(org.catrobat.catroid.R.string.cpd_file_saved, file.name), android.widget.Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                onOutput("Error saving file: ${e.message}\n")
                android.widget.Toast.makeText(requireContext(), getString(org.catrobat.catroid.R.string.cpd_save_error), android.widget.Toast.LENGTH_SHORT).show()
            }
        }

        btnExit.setOnClickListener {
            onOutput("Editor exited\n")
            onComplete()
            dialog.dismiss()
        }

        dialog.setCancelable(false)
        dialog.show()
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.terminalInput.isEnabled = !isLoading
        binding.sendButton.isEnabled = !isLoading
        if (!isLoading) {
            binding.terminalInput.requestFocus()
            binding.scrollView.post { binding.scrollView.fullScroll(View.FOCUS_DOWN) }
        }
    }

    private val displayPath: String
        get() {
            val currentPath = commandManager.currentWorkingDirectory
            val relPath = currentPath.absolutePath.removePrefix(projectFilesDir.absolutePath)
            return if (relPath.isEmpty()) "~" else "~$relPath"
        }

    private fun updatePrompt() {
        binding.terminalPrompt.text = "$displayPath $ "
    }

    override fun onDestroyView() {
        super.onDestroyView()
        commandManager.outputListener = null
        _binding = null
    }

    companion object {
        val TAG: String = CommandPromptDialogFragment::class.java.simpleName
        private const val ARG_PROJECT_PATH = "project_path"

        fun newInstance(projectPath: String): CommandPromptDialogFragment {
            val args = Bundle().apply { putString(ARG_PROJECT_PATH, projectPath) }
            return CommandPromptDialogFragment().apply { arguments = args }
        }
    }
}
