package org.catrobat.catroid.content.actions

import android.util.Log
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.CatroidApplication
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.content.UserVarsManager
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.formulaeditor.UserVariable
import org.catrobat.catroid.python.PythonCommandManager
import org.catrobat.catroid.ui.MainMenuActivity

class RunShellAction : TemporalAction() {
    var scope: Scope? = null
    var command: Formula? = null
    var userVariable: UserVariable? = null // Новое поле

    override fun update(percent: Float) {
        val commandContent = command?.interpretString(scope)

        if (scope?.project == null) return

        if (commandContent.isNullOrEmpty()) {
            return
        }

        val pythonEngine = MainMenuActivity.pythonEngine

        val commandmanager = PythonCommandManager(pythonEngine, scope!!.project!!)

        commandmanager.executeCommandForResult(commandContent) { result: String ->
            userVariable?.value = result
        }
    }
}