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
    var variableName: Formula? = null // Новое поле

    override fun update(percent: Float) {
        val scriptContent = script?.interpretString(scope)
        val varName = variableName?.interpretString(scope) ?: "myVar"

        if (scriptContent.isNullOrEmpty()) {
            return // Ничего не делаем, если скрипт пуст
        }

        val pythonEngine = MainMenuActivity.pythonEngine
        if (pythonEngine == null) {
            Log.e("RunPythonScript", "PythonEngine not available.")
            return
        }

        // УБИРАЕМ СОЗДАНИЕ ЛИШНЕГО ПОТОКА!
        // Просто добавляем задачу в очередь. Движок сам разберется с потоком.
        // Если varName пустой, логи будут просто проигнорированы, что нормально.
        pythonEngine.runScriptAsync(scriptContent, {output: String ->
            UserVarsManager.setVar(varName, output)
        })
    }
}