// Поместите этот файл в пакет org.catrobat.catroid.ui.fragment
package org.catrobat.catroid.ui.fragment

import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.DialogFragment
import org.catrobat.catroid.ProjectManager
import org.catrobat.catroid.databinding.DialogCommandPromptBinding
import org.catrobat.catroid.python.PythonEngine
import org.catrobat.catroid.python.PythonCommandManager
import org.catrobat.catroid.python.CommandOutputListener
import org.catrobat.catroid.ui.MainMenuActivity
import java.io.File

class CommandPromptDialogFragment : DialogFragment(), CommandOutputListener {

    private var _binding: DialogCommandPromptBinding? = null
    private val binding get() = _binding!!

    // Эти два класса - ядро нашей системы
    private lateinit var pythonEngine: PythonEngine
    private lateinit var commandManager: PythonCommandManager

    // Директория "files" текущего проекта
    private lateinit var projectFilesDir: File

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Устанавливаем стиль, чтобы диалог был полноэкранным
        setStyle(STYLE_NO_TITLE, android.R.style.Theme_DeviceDefault_Light_NoActionBar)

        // Инициализируем Python движок
        pythonEngine = MainMenuActivity.pythonEngine
        pythonEngine.initialize()

        // Получаем путь к файлам проекта из аргументов
        val project = ProjectManager.getInstance().currentProject
        projectFilesDir = project.filesDir
        if (!projectFilesDir.exists()) projectFilesDir.mkdirs()

        // Создаем менеджер команд
        commandManager = PythonCommandManager(pythonEngine, project)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogCommandPromptBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        commandManager.outputListener = this

        // --- ИЗМЕНЕНИЯ ЗДЕСЬ ---

        // 1. Назначаем слушателя для новой кнопки
        binding.sendButton.setOnClickListener {
            submitCommand()
        }

        // 2. Улучшаем обработку нажатия "Enter" на клавиатуре
        binding.terminalInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                submitCommand()
                return@setOnEditorActionListener true
            }
            false
        }

        // Код ниже остается без изменений
        binding.terminalOutput.text = "Shell initialized.\n"
        updatePrompt()
    }

    /**
     * Новая функция, которая содержит всю логику отправки команды.
     */
    private fun submitCommand() {
        val command = binding.terminalInput.text.toString()
        if (command.isBlank()) { // Не отправляем пустые команды
            return
        }

        binding.terminalInput.text.clear()

        // Показываем индикатор загрузки и блокируем ввод
        showLoading(true)

        // Отправляем команду на выполнение
        commandManager.processCommand(command)
    }

    // --- Реализация интерфейса CommandOutputListener ---

    override fun onOutput(output: String) {
        // Добавляем новый текст в поле вывода
        binding.terminalOutput.append(output)
        // Автоматически прокручиваем вниз, чтобы видеть последний результат
        binding.scrollView.post { binding.scrollView.fullScroll(View.FOCUS_DOWN) }
    }

    override fun onComplete() {
        // Команда выполнена: скрываем индикатор и разблокируем ввод
        showLoading(false)
        updatePrompt()
    }

    // --- Вспомогательные функции ---

    private fun showLoading(isLoading: Boolean) {
        if (isLoading) {
            binding.progressBar.visibility = View.VISIBLE
            binding.terminalInput.isEnabled = false
            binding.terminalPrompt.text = "Executing..."
        } else {
            binding.progressBar.visibility = View.GONE
            binding.terminalInput.isEnabled = true
            binding.terminalInput.requestFocus() // Возвращаем фокус на поле ввода
        }
    }

    private fun updatePrompt() {
        // Получаем текущий путь из менеджера и форматируем его
        val currentPath = commandManager.currentWorkingDirectory
        // Отображаем путь относительно папки 'files' для краткости
        val displayPath = currentPath.absolutePath.removePrefix(projectFilesDir.parentFile?.absolutePath ?: "")

        binding.terminalPrompt.text = "$displayPath > "
    }

    override fun onDestroyView() {
        super.onDestroyView()
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