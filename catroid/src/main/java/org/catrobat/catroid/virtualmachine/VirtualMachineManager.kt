package org.catrobat.catroid.virtualmachine

import android.content.Context
import android.util.Log
import com.gaurav.avnc.vnc.VncClient
import org.catrobat.catroid.ProjectManager
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object VirtualMachineManager {
    // Загружаем нашу C++ библиотеку
    init {
        System.loadLibrary("catroid")
        VncClient.loadLibrary()
    }

    // Карта для хранения PID запущенных ВМ: Имя ВМ -> PID
    private val runningVMs = mutableMapOf<String, Int>()

    /**
     * Готовит исполняемый файл QEMU и его библиотеки к запуску.
     * Копирует всю папку qemu_x86_64 из assets в приватную директорию приложения.
     * @return Путь к исполняемому файлу QEMU или null в случае ошибки.
     */
    private fun copyAssetFolder(context: Context, srcAssetPath: String, dstPath: String) {
        val assetManager = context.assets
        val files = assetManager.list(srcAssetPath) ?: return

        if (files.isEmpty()) { // Это может быть файл, а не папка
            assetManager.open(srcAssetPath).use { inputStream ->
                File(dstPath).outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
        } else {
            val dir = File(dstPath)
            if (!dir.exists()) dir.mkdirs()
            files.forEach { fileName ->
                copyAssetFolder(context, "$srcAssetPath/$fileName", "$dstPath/$fileName")
            }
        }
    }

    private fun prepareQemuEnvironment(context: Context): String? {
        val assetDirName = "qemu_x86_64"
        val targetDir = File(context.filesDir, assetDirName)
        val qemuSystem = File(targetDir, "qemu-system-x86_64")
        val qemuImg = File(targetDir, "qemu-img") // <-- Добавили qemu-img

        try {
            if (!qemuSystem.exists() || !qemuImg.exists()) { // <-- Проверяем оба
                Log.i("VMManager", "QEMU environment not found. Extracting from assets...")
                if (targetDir.exists()) targetDir.deleteRecursively()
                copyAssetFolder(context, assetDirName, targetDir.absolutePath)
                Log.i("VMManager", "Extraction complete.")
            }

            if (!qemuSystem.canExecute()) {
                qemuSystem.setExecutable(true, true)
            }
            if (!qemuImg.canExecute()) { // <-- Делаем qemu-img исполняемым
                qemuImg.setExecutable(true, true)
            }
            return targetDir.absolutePath // <-- Возвращаем путь к папке
        } catch (e: Exception) {
            Log.e("VMManager", "Failed to prepare QEMU environment", e)
            return null
        }
    }

    // НОВАЯ ФУНКЦИЯ: Создает или находит qcow2 диск
    fun createDiskIfNotExists(baseDir: String, diskPath: String, diskSize: String): Boolean {
        val diskFile = File(diskPath)
        if (diskFile.exists()) {
            Log.i("VMManager", "Disk already exists: $diskPath")
            return true
        }

        Log.i("VMManager", "Disk not found. Creating new disk: $diskPath with size $diskSize")
        val qemuImgPath = File(baseDir, "qemu-img").absolutePath
        val libPath = File(baseDir, "lib").absolutePath

        val command = listOf(
            "/system/bin/linker64",
            qemuImgPath,
            "create",
            "-f", "qcow2",
            diskFile.absolutePath,
            diskSize
        )

        try {
            val processBuilder = ProcessBuilder(command)
            processBuilder.environment()["LD_LIBRARY_PATH"] = libPath
            processBuilder.redirectErrorStream(true) // Объединяем stdout и stderr
            val process = processBuilder.start()

            // Выводим лог создания диска
            process.inputStream.bufferedReader().useLines { lines ->
                lines.forEach { line -> Log.i("QEMU-IMG", line) }
            }

            val exitCode = process.waitFor()
            Log.i("VMManager", "qemu-img process finished with exit code: $exitCode")
            return exitCode == 0
        } catch (e: Exception) {
            Log.e("VMManager", "Failed to create disk", e)
            return false
        }
    }

    // ИЗМЕНЕНО: createVM теперь принимает параметры диска
    fun createVM(context: Context, vmName: String, args: String, diskName: String, diskSize: String) {
        if (runningVMs.containsKey(vmName)) {
            Log.w("VMManager", "VM with name '$vmName' is already running.")
            return
        }

        val qemuBaseDir = prepareQemuEnvironment(context)
        if (qemuBaseDir == null) {
            Log.e("VMManager", "Cannot start VM, QEMU environment is not available.")
            return
        }

        var finalArgs = args

        if (diskName.isNotEmpty()) {
            val disksDir = ProjectManager.getInstance().currentProject.filesDir
            if (!disksDir.exists()) disksDir.mkdirs()
            val diskPath = File(disksDir, diskName).absolutePath

            if (!createDiskIfNotExists(qemuBaseDir, diskPath, diskSize)) {
                Log.e("VMManager", "Failed to create or find disk. Aborting VM start.")
                return
            }

            finalArgs = args.replace("%DISK_PATH%", diskPath)
        }

        // --- ИСПРАВЛЕНИЕ #1: Формируем правильный путь к ИСПОЛНЯЕМОМУ файлу ---
        val qemuSystemPath = File(qemuBaseDir, "qemu-system-x86_64").absolutePath
        val projectFilesPath = ProjectManager.getInstance().currentProject.filesDir.absolutePath

        val argsList = finalArgs.split(Regex(" (?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)")).map {
            it.removeSurrounding("\"").replace("%PROJECT_FILES%", projectFilesPath)
        }

        // Собираем финальную команду, начиная с ПРАВИЛЬНОГО пути к файлу
        val commandWithExe = mutableListOf(qemuSystemPath)
        commandWithExe.addAll(argsList)

        Log.i("VMManager", "Executing command: ${commandWithExe.joinToString(" ")}")

        // --- ИСПРАВЛЕНИЕ #2: Передаем в C++ ПРАВИЛЬНЫЙ путь к корневой папке ---
        // C++ код ожидает именно qemuBaseDir, а не его родителя
        val pid = nativeCreateAndRunVM(vmName, commandWithExe.toTypedArray(), qemuBaseDir)

        if (pid != -1) {
            runningVMs[vmName] = pid
        }
    }

    fun createVM(context: Context, vmName: String, args: String) {
        // Просто вызываем полную версию, передавая пустые строки для диска.
        createVM(context, vmName, args, "", "")
    }

    /**
     * Останавливает виртуальную машину.
     */
    fun stopVM(vmName: String) {
        if (!runningVMs.containsKey(vmName)) {
            Log.w("VMManager", "No running VM found with name '$vmName'.")
            return
        }
        nativeStopVM(vmName)
        runningVMs.remove(vmName)
    }

    // --- Объявления нативных функций ---
    @JvmStatic
    private external fun nativeCreateAndRunVM(vmName: String, command: Array<String>, libraryPath: String): Int

    @JvmStatic
    private external fun nativeStopVM(vmName: String): Int
}