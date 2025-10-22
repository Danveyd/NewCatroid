package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.stage.StageActivity

class SetShaderUniformAction : TemporalAction() {
    lateinit var scope: Scope
    var uniformName: Formula? = null
    var valueX: Formula? = null
    var valueY: Formula? = null
    var valueZ: Formula? = null

    override fun update(percent: Float) {
        val manager = StageActivity.activeStageActivity.get()?.stageListener?.threeDManager ?: return
        val name = uniformName?.interpretString(scope)
        if (name.isNullOrEmpty()) return

        val x = valueX?.interpretFloat(scope) ?: 0f

        if (valueY != null && valueZ != null) {
            val y = valueY?.interpretFloat(scope) ?: 0f
            val z = valueZ?.interpretFloat(scope) ?: 0f
            manager.setShaderUniform(name, x, y, z)
        } else {
            manager.setShaderUniform(name, x)
        }
    }
}