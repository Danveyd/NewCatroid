package org.catrobat.catroid.python

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.util.Log
import org.catrobat.catroid.content.UserVarsManager
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.ConcurrentLinkedQueue

class PythonEngine(private val context: Context) {

    private data class PythonTask2(val script: String, val variableName: String? = null)

    private data class PythonTask(
        val script: String,
        val onComplete: ((output: String) -> Unit)? = null // Вместо variableName
    )

    // Потокобезопасная очередь для задач
    private val taskQueue = ConcurrentLinkedQueue<PythonTask>()
    // Ссылка на наш единственный рабочий поток
    private var workerThread: Thread? = null

    private val loadedNativeLibs = mutableSetOf<String>()

    private var isSupportedArchitecture = false

    init {
        Log.d("PythonEngine", "Initializing engine...")

        // 1. ПРОВЕРЯЕМ АРХИТЕКТУРУ
        val supportedAbi = "arm64-v8a"
        if (Build.SUPPORTED_ABIS.contains(supportedAbi)) {
            isSupportedArchitecture = true
            Log.d("PythonEngine", "Architecture is supported ($supportedAbi). Loading native libraries...")

            // 2. ЗАГРУЖАЕМ БИБЛИОТЕКИ ТОЛЬКО ЕСЛИ АРХИТЕКТУРА ПОДХОДИТ
            try {
                // Весь наш runtime
                System.loadLibrary("crypto")
                System.loadLibrary("ssl")
                System.loadLibrary("z")
                System.loadLibrary("expat")
                System.loadLibrary("openblas")
                // System.loadLibrary("gfortran") // Мы выяснили, что он не нужен для NumPy
                System.loadLibrary("jpeg")
                System.loadLibrary("png")

                // Ядро
                System.loadLibrary("python3.12")
                System.loadLibrary("catroid")

                Log.d("PythonEngine", "All native libraries loaded successfully.")
            } catch (e: UnsatisfiedLinkError) {
                Log.e("PythonEngine", "FATAL: Could not load a native library even on a supported architecture.", e)
                isSupportedArchitecture = false // Если что-то пошло не так, отключаемся
            }
        } else {
            isSupportedArchitecture = false
            Log.w("PythonEngine", "!!! WARNING: Unsupported CPU architecture found: ${Build.SUPPORTED_ABIS.joinToString()}. Python functionality will be disabled. !!!")
        }
    }

    var isInitialized = false

    // Объявление нативных функций
    private external fun nativeInitPython(modulePaths: Array<String>)
    private external fun nativeRunScript(script: String): String
    private external fun nativeFinalizePython()
    private external fun nativeForceStopPythonScript()

    private val cleanSlateScript = """
        import sys
        if '_initial_modules' not in globals():
            _initial_modules = set(sys.modules.keys())
            _initial_path = list(sys.path)
            _initial_globals = set(globals().keys())
        else:
            modules_to_delete = [name for name in sys.modules if name not in _initial_modules]
            for mod_name in modules_to_delete:
                try: del sys.modules[mod_name]
                except KeyError: pass
            
            globals_to_delete = [name for name in globals() if name not in _initial_globals]
            for var_name in globals_to_delete:
                try: del globals()[var_name]
                except KeyError: pass
            
            sys.path[:] = _initial_path
        print("--- Python Environment has been reset ---")
    """.trimIndent()

    fun initialize() {
        if (!isSupportedArchitecture || isInitialized) return

        workerThread = Thread {
            Log.i("PythonWorker", "Worker thread started.")
            // 1. Инициализация Python ВНУТРИ этого потока
            val pythonHome = File(context.filesDir, "python3.12")
            if (!pythonHome.exists()) copyAssets("python3.12", pythonHome)
            val defaultLibsDir = File(context.filesDir, "default_pylibs")
            copyAssets("default_pylibs", defaultLibsDir)
            val dynloadDir = File(pythonHome, "lib-dynload")
            val modulePaths = arrayOf(
                pythonHome.absolutePath,
                defaultLibsDir.absolutePath,
                dynloadDir.absolutePath
            )
            nativeInitPython(modulePaths)
            Log.i("PythonWorker", "Python interpreter initialized in worker thread.")

            nativeRunScript(cleanSlateScript)
            Log.i("PythonWorker", "Initial Python state recorded.")

            // 2. Основной цикл обработки задач
            //UserVarsManager.setVar(task.variableName, output)
            try {
                while (!Thread.currentThread().isInterrupted) {
                    val task = taskQueue.poll()
                    if (task != null) {
                        val output = nativeRunScript(task.script)
                        // Вызываем callback, если он есть
                        task.onComplete?.invoke(output)
                    } else {
                        Thread.sleep(50)
                    }
                }
            } catch (e: InterruptedException) {
                // Это нормальный способ остановить поток
                Thread.currentThread().interrupt()
            } finally {
                // 3. Гарантированная очистка при завершении потока
                nativeFinalizePython()
                Log.i("PythonWorker", "Python interpreter finalized. Worker thread shutting down.")
            }
        }.apply {
            name = "PythonWorkerThread"
            start() // Запускаем поток
        }

        isInitialized = true
    }

    /**
     * Добавляет скрипт в очередь на выполнение. Не блокирует вызывающий поток.
     * @param script Код Python для выполнения.
     * @param variableName (опционально) Имя переменной для сохранения вывода.
     */
    fun runScriptAsync(script: String, onComplete: ((output: String) -> Unit)? = null) {
        if (!isSupportedArchitecture) return
        if (!isInitialized) {
            Log.w("PythonEngine", "Python was not initialized. Initializing now...")
            initialize()
        }
        taskQueue.add(PythonTask(script, onComplete))
    }

    /**
     * Останавливает фоновый поток и очищает ресурсы.
     */
    /*fun clearEnvironment() {
        // Проверяем, есть ли вообще что останавливать
        if (!isInitialized || workerThread == null) {
            return
        }

        Log.i("PythonEngine", "Shutdown sequence initiated...")

        // --- НОВАЯ, БЕЗОПАСНАЯ ПОСЛЕДОВАТЕЛЬНОСТЬ ---

        // Шаг 1: Попросить Python-скрипт остановиться (если он запущен).
        // Это прервет блокирующие операции вроде bot.polling().
        nativeForceStopPythonScript()

        // Шаг 2: Прервать Java-поток. Это выведет его из цикла while или Thread.sleep().
        workerThread?.interrupt()

        // Шаг 3 (КЛЮЧЕВОЙ): Дождаться, пока рабочий поток полностью умрет.
        // Метод join() заблокирует текущий поток (GLThread) до тех пор,
        // пока workerThread не закончит выполнение, включая свой блок `finally`.
        try {
            Log.d("PythonEngine", "Waiting for worker thread to terminate...")
            workerThread?.join(2000) // Ждем до 2 секунд
        } catch (e: InterruptedException) {
            // Если этот поток тоже прервали, восстанавливаем флаг и выходим
            Thread.currentThread().interrupt()
            Log.w("PythonEngine", "Interrupted while waiting for worker thread.")
        }

        // Шаг 4: Теперь мы на 100% уверены, что рабочий поток мертв и Python финализирован.
        // Можно безопасно очищать состояние в Kotlin.
        if (workerThread?.isAlive == true) {
            Log.e("PythonEngine", "Worker thread failed to stop within the timeout!")
        } else {
            Log.i("PythonEngine", "Worker thread terminated successfully.")
        }

        taskQueue.clear()
        workerThread = null
        isInitialized = false
        Log.i("PythonEngine", "Shutdown sequence complete.")
    }*/
    fun clearEnvironment() {
        if (!isInitialized) return
        Log.i("PythonEngine", "Clearing environment: stopping script and queueing reset task...")

        // Шаг 1: Принудительно прерываем текущий скрипт.
        // Это заставит nativeRunScript завершиться с исключением SystemExit.
        nativeForceStopPythonScript()

        // Шаг 2: Ставим в очередь задачу на очистку.
        // Так как у нас один рабочий поток, эта задача гарантированно
        // выполнится ПОСЛЕ того, как прерванный скрипт завершится.
        runScriptAsync(cleanSlateScript)
    }

    /**
     * Полностью останавливает движок Python.
     * Вызывать ТОЛЬКО при полном закрытии приложения. 
     */
    fun shutdown() {
        if (!isInitialized || workerThread == null) return

        Log.i("PythonEngine", "Shutdown sequence initiated...")
        workerThread?.interrupt()
        try {
            workerThread?.join(1000)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        }

        workerThread = null
        isInitialized = false
        Log.i("PythonEngine", "Python Engine has been shut down.")
    }

    // Вспомогательная функция для копирования файлов из assets
    // В файле PythonEngine.kt

    private fun copyAssets(assetPath: String, destDir: File) {
        try {
            val assetManager = context.assets
            val assets = assetManager.list(assetPath)
            if (assets.isNullOrEmpty()) {
                // Если список пуст, это может быть пустая папка или файл.
                // Но наша логика вызывает рекурсию только для папок с содержимым,
                // поэтому сюда мы попадем только для пустых папок.
                // На всякий случай создадим ее.
                if (!destDir.exists()) {
                    destDir.mkdirs()
                }
                return
            }

            // Убедимся, что директория назначения существует
            if (!destDir.exists()) {
                destDir.mkdirs()
            }

            for (assetName in assets) {
                val sourcePath = if (assetPath.isEmpty()) assetName else "$assetPath/$assetName"
                val destFile = File(destDir, assetName)

                // ---> НОВАЯ, НАДЕЖНАЯ ЛОГИКА ПРОВЕРКИ <---
                // Если мы можем получить список дочерних элементов, это точно папка.
                val isDir = assetManager.list(sourcePath)?.isNotEmpty() == true

                if (isDir) {
                    // Если это папка, создаем ее и запускаем рекурсию
                    destFile.mkdirs()
                    copyAssets(sourcePath, destFile)
                } else {
                    // Если это файл, просто копируем его
                    assetManager.open(sourcePath).use { inputStream ->
                        java.io.FileOutputStream(destFile).use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Ловим любые исключения, чтобы увидеть, если что-то пойдет не так
            Log.e("PythonEngine", "FATAL ERROR in copyAssets for path: $assetPath", e)
        }
    }

    /**
     * Динамически загружает нативный .so модуль из файла проекта.
     * @param filePath Полный путь к .so файлу.
     * @return true, если загрузка прошла успешно или модуль уже был загружен.
     */
    @SuppressLint("UnsafeDynamicallyLoadedCode")
    fun loadNativeModule(filePath: String): Boolean {
        if (!isSupportedArchitecture) {
            Log.w("PythonEngine", "Cannot load native module on unsupported architecture.")
            return false
        }
        if (loadedNativeLibs.add(filePath)) {
            try {
                Log.d("PythonEngine", "Dynamically loading native module: $filePath")
                System.load(filePath)
                return true
            } catch (e: UnsatisfiedLinkError) {
                Log.e("PythonEngine", "Failed to load native module: $filePath", e)
                loadedNativeLibs.remove(filePath)
                return false
            }
        } else {
            Log.d("PythonEngine", "Native module already loaded: $filePath")
            return true
        }
    }
}