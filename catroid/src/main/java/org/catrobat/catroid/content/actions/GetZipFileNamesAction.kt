// В пакете: org.catrobat.catroid.content.actions
package org.catrobat.catroid.content.actions

import android.widget.Toast
import org.catrobat.catroid.CatroidApplication
import org.catrobat.catroid.R
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.formulaeditor.UserVariable
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import java.util.zip.ZipFile
import java.io.IOException

class GetZipFileNamesAction : TemporalAction() {
    var scope: Scope? = null
    var zipFileName: Formula? = null
    var userVariable: UserVariable? = null

    override fun update(percent: Float) {
        val project = scope?.project
        val variable = userVariable
        if (project == null || variable == null) {
            return
        }

        val context = CatroidApplication.getAppContext()
        val fileName = zipFileName?.interpretString(scope)

        if (fileName.isNullOrEmpty()) {
            variable.value = "Error: ZIP file name is empty"
            return
        }

        try {
            val zipFile = project.getFile(fileName)
            if (!zipFile.exists()) {
                variable.value = "Error: File not found: $fileName"
                return
            }

            // Используем ZipFile для безопасного чтения архива
            val zf = ZipFile(zipFile)
            val fileNames = zf.entries().asSequence()
                .map { it.name } // Получаем имена всех записей
                .joinToString(",") // Объединяем их через запятую

            zf.close() // Обязательно закрываем файл

            variable.value = fileNames

        } catch (e: IOException) {
            // Эта ошибка может возникнуть, если файл поврежден или это не ZIP-архив
            variable.value = "Error: Failed to read ZIP file: ${e.message}"
            //Toast.makeText(context, context.getString(R.string.error_zip_read_failed, fileName), Toast.LENGTH_SHORT).show()
        }
    }
}