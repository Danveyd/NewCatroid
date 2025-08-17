package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.ui.MainMenuActivity

class ClearPythonEnvironmentAction : TemporalAction() {
    var scope: Scope? = null

    override fun update(percent: Float) {
        val pythonEngine = MainMenuActivity.pythonEngine
        pythonEngine.clearEnvironment()
    }
}