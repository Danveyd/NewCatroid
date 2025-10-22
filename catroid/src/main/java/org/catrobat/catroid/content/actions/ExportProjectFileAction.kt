package org.catrobat.catroid.content.actions

import android.widget.Toast
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.CatroidApplication
import org.catrobat.catroid.R
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.stage.StageActivity
import java.io.File

class ExportProjectFileAction : TemporalAction() {
    var scope: Scope? = null
    var projectFileName: Formula? = null

    override fun update(percent: Float) {
        val project = scope?.project ?: return
        val context = CatroidApplication.getAppContext()
        val activity = StageActivity.activeStageActivity?.get()

        if (activity == null) {
            Toast.makeText(context, "Ошибка: не удалось получить доступ к Activity", Toast.LENGTH_SHORT).show()
            return
        }

        val fileName = projectFileName?.interpretString(scope)
        if (fileName.isNullOrEmpty()) {
            return
        }

        val sourceFile: File = project.getFile(fileName)
        if (!sourceFile.exists()) {
            return
        }

        activity.runOnUiThread {
            activity.launchExportFilePicker(sourceFile.absolutePath, fileName)
        }
    }
}