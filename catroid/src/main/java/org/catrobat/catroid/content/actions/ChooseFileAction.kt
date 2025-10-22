package org.catrobat.catroid.content.actions

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.CatroidApplication
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.UserVariable
import org.catrobat.catroid.stage.StageActivity
import org.catrobat.catroid.utils.ErrorLog
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class ChooseFileAction : TemporalAction() {
    var scope: Scope? = null
    var fileType: Int = 4 // По умолчанию - все файлы
    var variable: UserVariable? = null

    companion object {
        private const val REQUEST_CODE_PICK_FILE = 2013
        private val TAG = ChooseFileAction::class.java.simpleName

        private val MIME_TYPES = arrayOf(
            "image/*",   // 0 - Изображения
            "video/*",   // 1 - Видео
            "audio/*",   // 2 - Аудио
            "application/*", // 3 - Документы (разные форматы)
            "*/*"        // 4 - Любые файлы
        )

        private var TARGET_DIRECTORY = File(CatroidApplication.getAppContext().filesDir, "chosen_files")
    }

    override fun update(percent: Float) {
        scope?.project?.let { proj ->
            TARGET_DIRECTORY = proj.filesDir
            val activity = StageActivity.activeStageActivity.get() ?: CatroidApplication.getAppContext() as? Activity
            activity?.let {
                val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                    type = MIME_TYPES[fileType]
                    addCategory(Intent.CATEGORY_OPENABLE)
                }


                activity.startActivityForResult(
                    Intent.createChooser(intent, "Choose a file"),
                    REQUEST_CODE_PICK_FILE
                )
                StageActivity.addIntentListener(object : StageActivity.IntentListener {
                    override fun onIntentResult(
                        requestCode: Int,
                        resultCode: Int,
                        data: Intent?
                    ): Boolean {
                        if (requestCode == REQUEST_CODE_PICK_FILE && resultCode == Activity.RESULT_OK) {
                            data?.data?.let { uri ->
                                handleFileSelection(activity, uri)
                            }
                            return true
                        }
                        return false
                    }

                    override fun getTargetIntent(): Intent {
                        return Intent()
                    }
                })
            }
        }
    }

    private fun handleFileSelection(context: Context, uri: Uri) {
        val fileName = getFileName(context, uri)
        val destinationFile = File(TARGET_DIRECTORY, fileName)

        if (!TARGET_DIRECTORY.exists()) {
            TARGET_DIRECTORY.mkdirs()
        }

        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                copyFile(inputStream, destinationFile)
                variable?.value = fileName
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при копировании файла: ${e.message}")
        }
    }

    private fun getFileName(context: Context, uri: Uri): String {
        var fileName = ""
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex > -1) {
                        fileName = cursor.getString(nameIndex)
                    }
                }
            }
        }
        if (fileName.isBlank()) {
            fileName = uri.lastPathSegment ?: "chosen_file_${System.currentTimeMillis()}"
        }
        return fileName
    }

    private fun copyFile(inputStream: InputStream, destFile: File) {
        FileOutputStream(destFile).use { outputStream ->
            inputStream.copyTo(outputStream)
        }
    }
}
