package org.catrobat.catroid.content.actions

import android.content.Context
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.CatroidApplication
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import java.io.File
import java.io.IOException

class SaveToInternalStorageAction : TemporalAction() {
    var scope: Scope? = null
    var projectFileName: Formula? = null
    var internalStoragePath: Formula? = null

    override fun update(percent: Float) {
        val project = scope?.project ?: return
        val context = CatroidApplication.getAppContext() ?: return

        val projFileNameStr = projectFileName?.interpretString(scope)
        val internalPathStr = internalStoragePath?.interpretString(scope)

        if (projFileNameStr.isNullOrBlank() || internalPathStr.isNullOrBlank()) {
            return
        }

        val sourceFile = project.getFile(projFileNameStr)
        if (sourceFile == null || !sourceFile.exists()) {
            return
        }

        val destinationFile = File(context.filesDir, internalPathStr)

        try {
            destinationFile.parentFile?.mkdirs()

            sourceFile.copyTo(destinationFile, overwrite = true)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}