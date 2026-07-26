package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import android.util.Log
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.particles.ParticleManager

class EmitParticleBurstAction : TemporalAction() {
    var scope: Scope? = null
    var instanceIdFormula: Formula? = null
    var countFormula: Formula? = null

    override fun update(percent: Float) {
        try {
            val instanceId = instanceIdFormula?.interpretString(scope) ?: return
            val count = countFormula?.interpretInteger(scope) ?: 10

            if (instanceId.isBlank() || count <= 0) return

            val pInstance = ParticleManager.getInstance().getInstance(instanceId) ?: return
            pInstance.burst(count)
        } catch (e: Exception) {
            Log.e("EmitParticleBurst", "Error in update()", e)
        }
    }
}
