package org.catrobat.catroid.content.actions

import android.util.Log
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.CatroidApplication
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.ui.MainMenuActivity
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.zip.ZipInputStream

class LoadPythonLibraryAction : TemporalAction() {
    var scope: Scope? = null
    var fileName: Formula? = null

    override fun update(percent: Float) {
        val fileNameStr = fileName?.interpretString(scope)
        if (fileNameStr.isNullOrEmpty()) return

        val projectFile: File? = scope?.project?.getFile(fileNameStr)
        if (projectFile == null || !projectFile.exists()) {
            Log.e("LoadPythonLibrary", "Library file not found: $fileNameStr")
            return
        }

        val pythonEngine = MainMenuActivity.pythonEngine
        if (pythonEngine == null) {
            Log.e("LoadPythonLibrary", "PythonEngine not available.")
            return
        }

        // 1. Определяем, куда будем распаковывать библиотеку
        val unpackedLibsDir = File(CatroidApplication.getAppContext().filesDir, "pylibs_unpacked")
        unpackedLibsDir.mkdirs() // Создаем папку, если ее нет

        // 2. Распаковываем архив, если он еще не был распакован
        // (проверяем по наличию папки с таким же именем, как у архива)
        val destDir = File(unpackedLibsDir, projectFile.name)
        if (!destDir.exists()) {
            Log.i("LoadPythonLibrary", "Unpacking '${projectFile.name}' to '${destDir.absolutePath}'...")
            try {
                unzip(projectFile, destDir)
                Log.i("LoadPythonLibrary", "Unpacking finished successfully.")
            } catch (e: IOException) {
                Log.e("LoadPythonLibrary", "Failed to unpack library", e)
                return // Если распаковка не удалась, выходим
            }
        } else {
            Log.d("LoadPythonLibrary", "Library '${projectFile.name}' already unpacked. Skipping.")
        }

        // 3. Добавляем путь к РАСПАКОВАННОЙ папке в sys.path
        val libraryPath = destDir.absolutePath.replace("'", "\\'")
        val script = "import sys\nif '$libraryPath' not in sys.path:\n  sys.path.append('$libraryPath')"
        pythonEngine.runScriptAsync(script)

        Log.d("LoadPythonLibrary", "Task to add unpacked library '${projectFile.name}' to sys.path has been queued.")
    }

    /**
     * Вспомогательная функция для распаковки zip-архива.
     */
    @Throws(IOException::class)
    private fun unzip(zipFile: File, targetDirectory: File) {
        ZipInputStream(zipFile.inputStream()).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val newFile = File(targetDirectory, entry.name)
                if (entry.isDirectory) {
                    newFile.mkdirs()
                } else {
                    // Убедимся, что родительские папки существуют
                    File(newFile.parent!!).mkdirs()
                    FileOutputStream(newFile).use { fos ->
                        zis.copyTo(fos)
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    }
}