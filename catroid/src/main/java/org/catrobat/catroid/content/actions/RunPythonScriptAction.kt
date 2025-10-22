package org.catrobat.catroid.content.actions

import android.util.Log
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.CatroidApplication
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.content.UserVarsManager
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.ui.MainMenuActivity

class RunPythonScriptAction : TemporalAction() {
    var scope: Scope? = null
    var script: Formula? = null
    var variableName: Formula? = null

    override fun update(percent: Float) {
        val scriptContent = script?.interpretString(scope)
        val varName = variableName?.interpretString(scope) ?: "myVar"

        if (scriptContent.isNullOrEmpty()) {
            return
        }

        val pythonEngine = MainMenuActivity.pythonEngine
        if (pythonEngine == null) {
            Log.e("RunPythonScript", "PythonEngine not available.")
            return
        }

        pythonEngine.runScriptAsync(scriptContent, {output: String ->
            UserVarsManager.setVar(varName, output)
        })
    }
}