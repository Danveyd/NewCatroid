// Находится в пакете: org.catrobat.catroid.content.actions
package org.catrobat.catroid.content.actions

import android.util.Log
import android.widget.Toast
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.CatroidApplication
import org.catrobat.catroid.R
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import java.io.IOException

class CopyProjectFileAction : TemporalAction() {
    var scope: Scope? = null
    var sourceFileName: Formula? = null
    var newFileName: Formula? = null

    override fun update(percent: Float) {
        val project = scope?.project ?: return
        val context = CatroidApplication.getAppContext()

        val sourceName = sourceFileName?.interpretString(scope)
        val newName = newFileName?.interpretString(scope)

        if (sourceName.isNullOrEmpty() || newName.isNullOrEmpty()) {
            Toast.makeText(context, context.getString(R.string.cpf_empty_names), Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val sourceFile = project.getFile(sourceName)
            if (!sourceFile.exists() || sourceFile.isDirectory) {
                Toast.makeText(context, context.getString(R.string.cpf_source_not_found, sourceName), Toast.LENGTH_SHORT).show()
                return
            }

            val newFile = project.getFile(newName)

            // Копируем файл, перезаписывая, если он уже существует
            sourceFile.copyTo(newFile, overwrite = true)

            //Toast.makeText(context, "Файл скопирован: $newName", Toast.LENGTH_SHORT).show()
        } catch (e: IOException) {
            Log.e("CopyProjectFileAction", "Ошибка при копировании файла '$sourceName' в '$newName'", e)
            Toast.makeText(context, context.getString(R.string.cpf_copy_error), Toast.LENGTH_SHORT).show()
        }
    }
}