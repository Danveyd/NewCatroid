// в org.catrobat.catroid.content.actions

package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.stage.StageActivity

class SetCCDAction : TemporalAction() {
    var scope: Scope? = null
    var objectId: Formula? = null
    var enabled: Formula? = null // Будет "true" или "false"

    override fun update(percent: Float) {
        val manager = StageActivity.stageListener?.threeDManager ?: return
        val id = objectId?.interpretString(scope)
        if (id.isNullOrEmpty()) return

        val ccdEnabled = enabled?.interpretBoolean(scope) ?: false

        manager.setContinuousCollisionDetection(id, ccdEnabled)
    }
}