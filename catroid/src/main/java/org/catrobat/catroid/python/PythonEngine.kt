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
        val onComplete: ((output: String) -> Unit)? = null
    )

    private val taskQueue = ConcurrentLinkedQueue<PythonTask>()
    private var workerThread: Thread? = null

    private val loadedNativeLibs = mutableSetOf<String>()

    private var isSupportedArchitecture = false

    init {
        Log.d("PythonEngine", "Initializing engine...")

        val supportedAbi = "arm64-v8a"
        if (Build.SUPPORTED_ABIS.contains(supportedAbi)) {
            isSupportedArchitecture = true
            Log.d("PythonEngine", "Architecture is supported ($supportedAbi). Loading native libraries...")

            try {
                System.loadLibrary("crypto")
                System.loadLibrary("ssl")
                System.loadLibrary("z")
                System.loadLibrary("expat")
                System.loadLibrary("openblas")
                System.loadLibrary("jpeg")
                System.loadLibrary("png")

                System.loadLibrary("python3.12")
                System.loadLibrary("catroid")

                Log.d("PythonEngine", "All native libraries loaded successfully.")
            } catch (e: UnsatisfiedLinkError) {
                Log.e("PythonEngine", "FATAL: Could not load a native library even on a supported architecture.", e)
                isSupportedArchitecture = false
            }
        } else {
            isSupportedArchitecture = false
            Log.w("PythonEngine", "!!! WARNING: Unsupported CPU architecture found: ${Build.SUPPORTED_ABIS.joinToString()}. Python functionality will be disabled. !!!")
        }
    }

    var isInitialized = false

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

            try {
                while (!Thread.currentThread().isInterrupted) {
                    val task = taskQueue.poll()
                    if (task != null) {
                        val output = nativeRunScript(task.script)
                        task.onComplete?.invoke(output)
                    } else {
                        Thread.sleep(50)
                    }
                }
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
            } finally {
                nativeFinalizePython()
                Log.i("PythonWorker", "Python interpreter finalized. Worker thread shutting down.")
            }
        }.apply {
            name = "PythonWorkerThread"
            start()
        }

        isInitialized = true
    }

    fun runScriptAsync(script: String, onComplete: ((output: String) -> Unit)? = null) {
        if (!isSupportedArchitecture) return
        if (!isInitialized) {
            Log.w("PythonEngine", "Python was not initialized. Initializing now...")
            initialize()
        }
        taskQueue.add(PythonTask(script, onComplete))
    }

    fun clearEnvironment() {
        if (!isInitialized) return
        Log.i("PythonEngine", "Clearing environment: stopping script and queueing reset task...")

        nativeForceStopPythonScript()

        runScriptAsync(cleanSlateScript)
    }

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

    private fun copyAssets(assetPath: String, destDir: File) {
        try {
            val assetManager = context.assets
            val assets = assetManager.list(assetPath)
            if (assets.isNullOrEmpty()) {
                if (!destDir.exists()) {
                    destDir.mkdirs()
                }
                return
            }

            if (!destDir.exists()) {
                destDir.mkdirs()
            }

            for (assetName in assets) {
                val sourcePath = if (assetPath.isEmpty()) assetName else "$assetPath/$assetName"
                val destFile = File(destDir, assetName)

                val isDir = assetManager.list(sourcePath)?.isNotEmpty() == true

                if (isDir) {
                    destFile.mkdirs()
                    copyAssets(sourcePath, destFile)
                } else {
                    assetManager.open(sourcePath).use { inputStream ->
                        java.io.FileOutputStream(destFile).use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("PythonEngine", "FATAL ERROR in copyAssets for path: $assetPath", e)
        }
    }

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