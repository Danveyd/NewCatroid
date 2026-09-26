package org.catrobat.catroid.content.actions

import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.CatroidApplication
import org.catrobat.catroid.ProjectManager
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.stage.StageActivity
import org.catrobat.paintroid.common.PERMISSION_EXTERNAL_STORAGE_SAVE
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

open class SaveLookFilesAction : TemporalAction() {
    var scope: Scope? = null
    var name: Formula? = null

    private var copied = false

    override fun act(delta: Float): Boolean {
        if (copied) return true

        val activity = StageActivity.activeStageActivity.get()
        activity?.runOnUiThread {
            if (ContextCompat.checkSelfPermission(
                    CatroidApplication.getAppContext(),
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    PERMISSION_EXTERNAL_STORAGE_SAVE
                )
            }
        }

        Thread {
            scope?.let {
                val sprite = it.sprite ?: return@let
                val look = sprite.look ?: return@let
                val lookData = look.lookData ?: return@let
                val file = lookData.file ?: return@let

                if (!file.exists()) {
                    Log.e("SaveLookFilesAction", "Source file does not exist: ${file.absolutePath}")
                    return@let
                }

                val fileName = getName(name) ?: file.name
                copyFileToDownloads(file, fileName)
            }
        }.start()

        copied = true
        return true
    }

    override fun update(percent: Float) {
    }

    override fun restart() {
        super.restart()
        copied = false
    }

    override fun reset() {
        super.reset()
        copied = false
    }

    fun getName(inputName: Formula?): String? {
        inputName?.let { inname ->
            var name = inname.interpretString(scope)
            val lastDotIndex = name.lastIndexOf('.')
            if (lastDotIndex <= 0 || lastDotIndex >= name.length - 1) {
                name += ".png"
            }
            return name
        }
        return null
    }

    fun copyFileToDownloads(sourceFile: File, newFileName: String) {
        scope?.project?.let { proj ->
            val downloadsFolder = proj.filesDir
            val destFile = File(downloadsFolder, newFileName)

            try {
                FileInputStream(sourceFile).use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }
                Log.d("SaveLook", "Файл скопирован: ${destFile.absolutePath}")
            } catch (e: IOException) {
                Log.e("SaveLook", "Ошибка при копировании файла")
            }
        }
    }
}
