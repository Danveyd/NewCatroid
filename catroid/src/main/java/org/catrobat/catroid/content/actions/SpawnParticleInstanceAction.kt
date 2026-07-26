package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import android.util.Log
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.particles.ParticleManager

class SpawnParticleInstanceAction : TemporalAction() {
    var scope: Scope? = null
    var effectFileFormula: Formula? = null
    var instanceIdFormula: Formula? = null
    var xFormula: Formula? = null
    var yFormula: Formula? = null

    override fun update(percent: Float) {
        try {
            val effectFile = effectFileFormula?.interpretString(scope) ?: return
            val instanceId = instanceIdFormula?.interpretString(scope) ?: return
            val x = xFormula?.interpretFloat(scope) ?: 0f
            val y = yFormula?.interpretFloat(scope) ?: 0f

            if (effectFile.isBlank() || instanceId.isBlank()) return

            ParticleManager.getInstance().spawnInstance(effectFile, instanceId, x, y)
        } catch (e: Exception) {
            Log.e("SpawnParticleAction", "Error in update()", e)
        }
    }
}
