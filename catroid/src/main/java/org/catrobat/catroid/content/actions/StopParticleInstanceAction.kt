package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import android.util.Log
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.particles.ParticleManager

class StopParticleInstanceAction : TemporalAction() {
    var scope: Scope? = null
    var instanceIdFormula: Formula? = null
    var modeSelection: Int = 0

    override fun update(percent: Float) {
        try {
            val instanceId = instanceIdFormula?.interpretString(scope) ?: return
            if (instanceId.isBlank()) return

            ParticleManager.getInstance().stopInstance(instanceId, modeSelection == 1)
        } catch (e: Exception) {
            Log.e("StopParticleAction", "Error in update()", e)
        }
    }
}
