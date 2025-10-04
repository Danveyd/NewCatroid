package org.catrobat.catroid.python

// Файл: PythonCommandManager.kt
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import org.catrobat.catroid.CatroidApplication
import org.catrobat.catroid.content.Project
import java.io.File

/**
 * Интерфейс для UI, который будет слушать события от командного менеджера.
 * Любой класс (например, ваш TerminalView) может его реализовать.
 */
interface CommandOutputListener {
    /**
     * Вызывается, когда команда вернула какой-то результат.
     * ПРИМЕЧАНИЕ: С текущим PythonEngine этот метод получит весь вывод сразу.
     * Для "построчного" вывода в реальном времени потребуется модификация C++ части.
     */
    fun onOutput(output: String)

    /**
     * Вызывается, когда команда полностью завершила выполнение.
     */
    fun onComplete()
}

/**
 * Управляет выполнением команд, поддерживает состояние (текущую директорию)
 * и общается с UI через CommandOutputListener.
 */
class PythonCommandManager(
    private val pythonEngine: PythonEngine,
    private val project: Project
) {

    private val defaultLibsPath: String = project.filesDir.absolutePath
    // "Слушатель" из мира UI.
    var outputListener: CommandOutputListener? = null

    // Состояние: текущая рабочая директория терминала.
    var currentWorkingDirectory: File = project.filesDir

    // Директория для библиотек, установленных через pip.
    private val sitePackagesDir = project.filesDir

    init {
        if (!sitePackagesDir.exists()) {
            sitePackagesDir.mkdirs()
        }
    }

    /**
     * Выполняет команду и возвращает результат в виде строки через callback.
     * Идеально подходит для использования в коде, например, в блоках Catroid.
     *
     * @param command Строка с командой для выполнения (например, "pip install requests --files").
     * @param onResult Лямбда-функция, которая будет вызвана с результатом (String) после выполнения команды.
     */
    fun executeCommandForResult(command: String, onResult: (String) -> Unit) {
        val trimmedCommand = command.trim()
        if (trimmedCommand.isEmpty()) {
            mainThreadHandler.post { onResult("") } // Возвращаем пустую строку для пустой команды
            return
        }

        val parts = trimmedCommand.split("\\s+".toRegex())
        val commandName = parts.first()
        val args = parts.drop(1)

        // Эта логика частично дублирует processCommand, но адаптирована для возврата результата
        when (commandName) {
            "pip" -> {
                val script = buildPipScript(args) // Используем хелпер, чтобы не дублировать код
                executePythonScriptForResult(script, onResult)
            }
            "ls", "dir" -> executePythonScriptForResult("import os; print('\\n'.join(sorted(os.listdir('.'))))", onResult)
            "pwd" -> executePythonScriptForResult("import os; print(os.getcwd())", onResult)
            "cd" -> {
                // Команда cd не выполняет Python, обрабатываем ее здесь же
                val path = args.firstOrNull() ?: project.filesDir.absolutePath
                val newDir = if (path.startsWith("/")) File(path) else File(currentWorkingDirectory, path)

                if (newDir.exists() && newDir.isDirectory) {
                    currentWorkingDirectory = newDir.canonicalFile
                    mainThreadHandler.post { onResult("New directory: ${currentWorkingDirectory.absolutePath}") }
                } else {
                    mainThreadHandler.post { onResult("Error: no such file or directory: $path") }
                }
            }
            "clear" -> {
                pythonEngine.clearEnvironment()
                mainThreadHandler.post { onResult("Python environment has been reset.") }
            }
            else -> {
                // Любая другая команда считается Python-скриптом
                executePythonScriptForResult(trimmedCommand, onResult)
            }
        }
    }

    /**
     * Приватный хелпер для выполнения Python-скрипта, который возвращает результат в callback.
     */
    private fun executePythonScriptForResult(script: String, onResult: (String) -> Unit) {
        val wrappedScript = """
import os
try:
    os.chdir('${currentWorkingDirectory.absolutePath}')
    # ---> ИСПРАВЛЕНИЕ ЗДЕСЬ <---
    # Добавляем отступ в 4 пробела к каждой строке вставляемого скрипта,
    # чтобы он корректно вписался в блок 'try'.
${script.prependIndent("    ")}
except Exception as e:
    import traceback
    traceback.print_exc()
    """.trimIndent()

        pythonEngine.runScriptAsync(wrappedScript) { output ->
            mainThreadHandler.post { onResult(output) }
        }
    }

    /**
     * Главный метод. Принимает строку от пользователя, разбирает и выполняет ее.
     */
    fun processCommand(command: String) {
        val trimmedCommand = command.trim()
        if (trimmedCommand.isEmpty()) {
            outputListener?.onComplete()
            return
        }

        // Добавляем команду в "историю" вывода UI
        outputListener?.onOutput("> $trimmedCommand\n")

        val parts = trimmedCommand.split("\\s+".toRegex())
        val commandName = parts.first()
        val args = parts.drop(1)

        when (commandName) {
            "pip" -> handlePipCommand(args)
            "ls", "dir" -> executePythonScript("import os; print('\\n'.join(sorted(os.listdir('.'))))")
            "pwd" -> executePythonScript("import os; print(os.getcwd())")
            "cd" -> handleChangeDirectory(args)
            "clear" -> {
                // Команда для очистки Python окружения
                pythonEngine.clearEnvironment()
                mainThreadHandler.post {
                    outputListener?.onOutput("Python environment has been reset.\n")
                    outputListener?.onComplete()
                }
            }
            else -> {
                // Если это не специальная команда, считаем ее Python-кодом
                executePythonScript(trimmedCommand)
            }
        }
    }

    /**
     * Обрабатывает команду 'cd'. Выполняется полностью в Kotlin, т.к. меняет состояние менеджера.
     */
    private fun handleChangeDirectory(args: List<String>) {
        val path = args.firstOrNull() ?: CatroidApplication.getAppContext().filesDir.absolutePath // 'cd' без аргументов -> домой

        val newDir = if (path.startsWith("/")) {
            File(path) // Абсолютный путь
        } else {
            File(currentWorkingDirectory, path) // Относительный путь
        }

        if (newDir.exists() && newDir.isDirectory) {
            currentWorkingDirectory = newDir.canonicalFile // .canonicalFile убирает '..' и '.'
            Log.i("CMD", "New CWD: ${currentWorkingDirectory.absolutePath}")
        } else {
            mainThreadHandler.post {
                outputListener?.onOutput("cd: no such file or directory: $path\n")
            }
        }

        mainThreadHandler.post {
            outputListener?.onComplete()
        }
    }

    /**
     * Формирует и выполняет Python-скрипт для установки пакетов через pip.
     */
    private fun handlePipCommand(args: List<String>) {
        val pipScript = buildPipScript(args)

        // Проверяем, не вернул ли хелпер ошибку
        if (pipScript.startsWith("Error:")) {
            mainThreadHandler.post {
                outputListener?.onOutput(pipScript + "\n")
                outputListener?.onComplete()
            }
        } else {
            executePythonScript(pipScript) // executePythonScript - это ваша старая функция
        }
    }

    private fun buildPipScript(args: List<String>): String {
        val showOnlyFiles = args.contains("--files")
        val filteredArgs = args.filter { it != "--files" }
        val command = filteredArgs.firstOrNull()
        val packageName = if (filteredArgs.size > 1) filteredArgs[1] else return "Error: No package name specified."

        if (command != "install" && command != "download") {
            return "Error: use 'pip install <package> [--files]'."
        }

        val targetDir = currentWorkingDirectory.absolutePath
        val chaquopyIndex = "https://chaquo.com/pypi-13.1/"
        val pypiIndex = "https://pypi.org/simple"

        var scriptTemplate = if (showOnlyFiles) {
            // РЕЖИМ --files: АБСОЛЮТНАЯ ТИШИНА
            """
        import os, sys, io
        
        target_dir = '$targetDir'
        files_before = set(os.listdir(target_dir))
        
        # 1. Полностью перенаправляем stdout и stderr в "черную дыру",
        #    чтобы скрыть ВЕСЬ вывод от pip, включая WARNING'и.
        original_stdout = sys.stdout
        original_stderr = sys.stderr
        sys.stdout = io.StringIO()
        sys.stderr = io.StringIO()
        
        try:
            from pip._internal.cli.main import main as pip_main
            cli_args = [
                'download', '--only-binary=:all:', '-d', target_dir,
                '--find-links', '$defaultLibsPath', '--index-url', '$chaquopyIndex',
                '--extra-index-url', '$pypiIndex', '$packageName'
            ]
            result = pip_main(cli_args)
        finally:
            # 2. Гарантированно возвращаем потоки вывода в исходное состояние
            sys.stdout = original_stdout
            sys.stderr = original_stderr
            
        if result == 0:
            files_after = set(os.listdir(target_dir))
            new_files = sorted(list(files_after - files_before))
            # 3. Выводим ТОЛЬКО то, что нам нужно, в чистый stdout.
            if new_files:
                print('\\n'.join(new_files))
        # В случае ошибки ничего не выводим, сохраняя тишину.

        """.trimIndent()
        } else {
            // --- НАЧАЛО ИЗМЕНЕНИЙ В СТАНДАРТНОМ РЕЖИМЕ ---
            """
        import os, sys, io
        from pip._internal.cli.main import main as pip_main

        # 1. Создаем буферы для stdout и stderr, чтобы перехватить ВЕСЬ вывод
        stdout_capture = io.StringIO()
        stderr_capture = io.StringIO()
        
        original_stdout = sys.stdout
        original_stderr = sys.stderr
        
        sys.stdout = stdout_capture
        sys.stderr = stderr_capture
        
        result = -1 # Инициализируем кодом ошибки
        try:
            # 2. Формируем аргументы для pip
            cli_args = [
                'download', '--only-binary=:all:', '-d', '$targetDir',
                '--find-links', '$defaultLibsPath', '--index-url', '$chaquopyIndex',
                '--extra-index-url', '$pypiIndex', '$packageName'
            ]
            
            # 3. Выполняем pip. Весь его вывод теперь попадает в наши буферы
            result = pip_main(cli_args)
            
        finally:
            # 4. ОБЯЗАТЕЛЬНО возвращаем стандартные потоки вывода
            sys.stdout = original_stdout
            sys.stderr = original_stderr

        # 5. Получаем перехваченный вывод из буферов
        pip_stdout = stdout_capture.getvalue()
        pip_stderr = stderr_capture.getvalue()

        # 6. Выводим результат пользователю, как в настоящем терминале
        # Сначала выводим стандартный вывод pip...
        if pip_stdout:
            print(pip_stdout, end='')
            
        # ...а затем ошибки, если они были.
        if pip_stderr:
            print(pip_stderr, file=sys.stderr, end='')

        # 7. Добавляем финальное сообщение о статусе
        if result == 0:
            print(f"\n--- Pip finished successfully (Code: {result}) ---")
        else:
            print(f"\n--- Pip finished with an error (Code: {result}) ---")
            
        """.trimIndent()
            // --- КОНЕЦ ИЗМЕНЕНИЙ ---
        }

        return scriptTemplate
            .replace("\$targetDir", targetDir)
            .replace("\$defaultLibsPath", defaultLibsPath)
            .replace("\$packageName", packageName)
    }

    /**
     * Обертка для выполнения любого Python-скрипта.
     * Гарантирует, что скрипт будет запущен в правильной рабочей директории.
     */
    // В файле PythonCommandManager.kt

    /**
     * Обертка для выполнения любого Python-скрипта.
     * Гарантирует, что скрипт будет запущен в правильной рабочей директории.
     */
    private fun executePythonScript(script: String) {
        // Оборачиваем скрипт в код, который сначала меняет директорию
        val wrappedScript = """
import os
try:
    os.chdir('${currentWorkingDirectory.absolutePath}')
    # ---> ИСПРАВЛЕНИЕ ЗДЕСЬ <---
    # Добавляем отступ в 4 пробела к каждой строке вставляемого скрипта,
    # чтобы он корректно вписался в блок 'try'.
${script.prependIndent("    ")}
except Exception as e:
    import traceback
    traceback.print_exc()
    """.trimIndent()

        // Используем наш PythonEngine с callback'ом
        pythonEngine.runScriptAsync(wrappedScript) { output ->
            // Результат выполнения приходит сюда, в фоновом потоке.
            // Передаем его в UI в главном потоке.
            mainThreadHandler.post {
                outputListener?.onOutput(output)
                outputListener?.onComplete()
            }
        }
    }

    // Handler для безопасной отправки результатов в главный (UI) поток
    private val mainThreadHandler = Handler(Looper.getMainLooper())
}