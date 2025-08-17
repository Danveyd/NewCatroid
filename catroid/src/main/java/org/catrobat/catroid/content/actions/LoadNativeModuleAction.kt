package org.catrobat.catroid.content.actions

import android.util.Log
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.CatroidApplication
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.ui.MainMenuActivity
import java.io.File

class LoadNativeModuleAction : TemporalAction() {
    var scope: Scope? = null
    var fileName: Formula? = null

    override fun update(percent: Float) {
        val fileNameStr = fileName?.interpretString(scope)
        if (fileNameStr.isNullOrEmpty()) {
            return
        }

        val projectFile: File? = scope?.project?.getFile(fileNameStr)
        if (projectFile == null || !projectFile.exists()) {
            Log.e("LoadNativeModule", "Native module file not found: $fileNameStr")
            return
        }

        val pythonEngine = MainMenuActivity.pythonEngine
        pythonEngine.loadNativeModule(projectFile.absolutePath)
    }
}