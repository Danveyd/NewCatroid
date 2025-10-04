package org.catrobat.catroid.ui.fragment

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.DialogFragment
import org.catrobat.catroid.ProjectManager
import org.catrobat.catroid.databinding.DialogCommandPromptBinding
import org.catrobat.catroid.python.CommandOutputListener
import org.catrobat.catroid.python.PythonCommandManager
import org.catrobat.catroid.python.PythonEngine
import org.catrobat.catroid.ui.MainMenuActivity
import java.io.File
import java.util.regex.Pattern

// ▼▼▼ НАЧАЛО: НАШ НОВЫЙ КЛАСС ДЛЯ ПОДСВЕТКИ СИНТАКСИСА ▼▼▼
object SyntaxHighlighter {

    // Определяем цвета (можешь настроить их как угодно)
    private val COLOR_ERROR = Color.RED
    private val COLOR_WARNING = Color.rgb(255, 165, 0) // Оранжевый
    private val COLOR_SUCCESS = Color.rgb(0, 180, 0)   // Темно-зеленый
    private val COLOR_INFO = Color.CYAN

    // Определяем правила подсветки с помощью регулярных выражений
    private val RULES = listOf(
        // Ошибки (ключевые слова, нечувствительные к регистру)
        Rule(Pattern.compile("\\b(error|traceback|exception|failed|fatal|failure|none)\\b", Pattern.CASE_INSENSITIVE), COLOR_ERROR, isBold = true),
        // Предупреждения и загрузка
        Rule(Pattern.compile("\\b(warning|downloading|collecting|looking in indexes|looking in links)\\b", Pattern.CASE_INSENSITIVE), COLOR_WARNING),
        // Успешное завершение
        Rule(Pattern.compile("\\b(success|successfully|installed|complete)\\b", Pattern.CASE_INSENSITIVE), COLOR_SUCCESS),
        // Числа, версии и размеры файлов (например, 1.2.3, 50kB, 100)
        Rule(Pattern.compile("\\b(\\d+(\\.\\d+)*[kKmM]?[bB]?)\\b"), COLOR_SUCCESS)
    )

    /**
     * Главный метод, который принимает строку и возвращает стилизованный Spannable.
     */
    fun highlight(text: String): SpannableStringBuilder {
        val spannable = SpannableStringBuilder(text)
        for (rule in RULES) {
            val matcher = rule.pattern.matcher(spannable)
            while (matcher.find()) {
                // Применяем цвет
                spannable.setSpan(
                    ForegroundColorSpan(rule.color),
                    matcher.start(),
                    matcher.end(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                // Применяем жирный шрифт, если нужно
                if (rule.isBold) {
                    spannable.setSpan(
                        StyleSpan(Typeface.BOLD),
                        matcher.start(),
                        matcher.end(),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            }
        }
        return spannable
    }

    // Вспомогательный класс для хранения правил
    private data class Rule(val pattern: Pattern, val color: Int, val isBold: Boolean = false)
}
// ▲▲▲ КОНЕЦ: НАШ НОВЫЙ КЛАСС ДЛЯ ПОДСВЕТКИ СИНТАКСИСА ▲▲▲


class CommandPromptDialogFragment : DialogFragment(), CommandOutputListener {

    private var _binding: DialogCommandPromptBinding? = null
    private val binding get() = _binding!!

    private lateinit var pythonEngine: PythonEngine
    private lateinit var commandManager: PythonCommandManager
    private lateinit var projectFilesDir: File

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, android.R.style.Theme_DeviceDefault_Light_NoActionBar)

        pythonEngine = MainMenuActivity.pythonEngine
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
        commandManager.outputListener = this

        binding.sendButton.setOnClickListener { submitCommand() }
        binding.terminalInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                submitCommand()
                return@setOnEditorActionListener true
            }
            false
        }

        // Устанавливаем моноширинный шрифт для вывода, как в настоящих терминалах
        binding.terminalOutput.typeface = Typeface.MONOSPACE

        binding.terminalOutput.text = "Shell initialized.\n"
        updatePrompt()
    }

    private fun submitCommand() {
        val command = binding.terminalInput.text.toString()
        if (command.isBlank()) return

        binding.terminalInput.text.clear()
        showLoading(true)
        commandManager.processCommand(command)
    }

    // --- Реализация интерфейса CommandOutputListener ---

    override fun onOutput(output: String) {
        // ▼▼▼ ГЛАВНОЕ ИЗМЕНЕНИЕ ЗДЕСЬ ▼▼▼
        // 1. Пропускаем полученный текст через наш хайлайтер
        val styledOutput = SyntaxHighlighter.highlight(output)

        // 2. Добавляем уже стилизованный текст в поле вывода
        binding.terminalOutput.append(styledOutput)
        // ▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲

        binding.scrollView.post { binding.scrollView.fullScroll(View.FOCUS_DOWN) }
    }

    override fun onComplete() {
        showLoading(false)
        updatePrompt()
    }

    // --- Вспомогательные функции ---

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.terminalInput.isEnabled = !isLoading
        binding.terminalPrompt.text = if (isLoading) "Executing..." else "$displayPath > "
        if (!isLoading) {
            binding.terminalInput.requestFocus()
        }
    }

    // Сделаем displayPath свойством класса для удобства
    private val displayPath: String
        get() {
            val currentPath = commandManager.currentWorkingDirectory
            return currentPath.absolutePath.removePrefix(projectFilesDir.parentFile?.absolutePath ?: "")
        }

    private fun updatePrompt() {
        binding.terminalPrompt.text = "$displayPath > "
    }

    override fun onDestroyView() {
        super.onDestroyView()
        commandManager.outputListener = null
        _binding = null
    }

    companion object {
        val TAG: String = CommandPromptDialogFragment::class.java.simpleName
        private const val ARG_PROJECT_PATH = "project_path"

        // Фабричный метод для безопасного создания диалога с передачей аргументов
        fun newInstance(projectPath: String): CommandPromptDialogFragment {
            val args = Bundle().apply {
                putString(ARG_PROJECT_PATH, projectPath)
            }
            return CommandPromptDialogFragment().apply {
                arguments = args
            }
        }
    }
}