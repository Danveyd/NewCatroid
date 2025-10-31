package org.catrobat.catroid.content.actions

import android.content.Context
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.CatroidApplication
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import java.io.File
import java.io.IOException

class LoadFromInternalStorageAction : TemporalAction() {
    var scope: Scope? = null
    var internalStoragePath: Formula? = null

    override fun update(percent: Float) {
        val project = scope?.project ?: return
        val context = CatroidApplication.getAppContext() ?: return

        val internalPathStr = internalStoragePath?.interpretString(scope)
        if (internalPathStr.isNullOrBlank()) {
            return
        }

        val sourceFile = File(context.filesDir, internalPathStr)
        if (!sourceFile.exists()) {
            return
        }

        val destinationFile = File(project.filesDir, sourceFile.name)

        try {
            project.filesDir.mkdirs()

            sourceFile.copyTo(destinationFile, overwrite = true)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}