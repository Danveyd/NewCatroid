package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import android.util.Log
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.particles.ParticleManager

class SetParticleTransformAction : TemporalAction() {
    var scope: Scope? = null
    var instanceIdFormula: Formula? = null
    var xFormula: Formula? = null
    var yFormula: Formula? = null
    var scaleXFormula: Formula? = null
    var scaleYFormula: Formula? = null
    var rotationFormula: Formula? = null

    override fun update(percent: Float) {
        try {
            val instanceId = instanceIdFormula?.interpretString(scope) ?: return
            if (instanceId.isBlank()) return

            val pInstance = ParticleManager.getInstance().getInstance(instanceId) ?: return

            xFormula?.let { pInstance.x = it.interpretFloat(scope) }
            yFormula?.let { pInstance.y = it.interpretFloat(scope) }
            scaleXFormula?.let { pInstance.scaleX = it.interpretFloat(scope) }
            scaleYFormula?.let { pInstance.scaleY = it.interpretFloat(scope) }
            rotationFormula?.let { pInstance.rotation = it.interpretFloat(scope) }
        } catch (e: Exception) {
            Log.e("SetParticleTransform", "Error in update()", e)
        }
    }
}
