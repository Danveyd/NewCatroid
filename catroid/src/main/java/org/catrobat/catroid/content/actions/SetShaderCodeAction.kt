package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.stage.StageActivity

class SetShaderCodeAction : TemporalAction() {
    var scope: Scope? = null
    var vertexCode: Formula? = null
    var fragmentCode: Formula? = null

    override fun update(percent: Float) {
        val manager = StageActivity.activeStageActivity.get()?.stageListener?.threeDManager ?: return
        val vert = vertexCode?.interpretString(scope)
        val frag = fragmentCode?.interpretString(scope)
        if (vert != "" && frag != "") {
            manager.setShaderCode(vert, frag)
        } else {
            manager.resetSceneShader()
        }
    }
}