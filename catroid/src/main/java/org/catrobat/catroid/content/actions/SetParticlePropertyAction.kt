package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import android.util.Log
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.particles.ParticleManager

class SetParticlePropertyAction : TemporalAction() {
    var scope: Scope? = null
    var instanceIdFormula: Formula? = null
    var propertyIndex: Int = 0
    var valueFormula: Formula? = null

    override fun update(percent: Float) {
        try {
            val instanceId = instanceIdFormula?.interpretString(scope) ?: return
            if (instanceId.isBlank()) return

            val pInstance = ParticleManager.getInstance().getInstance(instanceId) ?: return
            val valFloat = valueFormula?.interpretFloat(scope) ?: 0f

            when (propertyIndex) {
                0 -> pInstance.model.emissionRate = valFloat
                1 -> pInstance.model.speed = valFloat
                2 -> pInstance.model.angle = valFloat
                3 -> pInstance.model.gravityX = valFloat
                4 -> pInstance.model.gravityY = valFloat
                5 -> pInstance.model.radialAccel = valFloat
                6 -> pInstance.model.tangentialAccel = valFloat
                7 -> pInstance.model.friction = valFloat
                8 -> pInstance.model.startA = valFloat
                9 -> pInstance.model.startR = valFloat
                10 -> pInstance.model.startG = valFloat
                11 -> pInstance.model.startB = valFloat
                12 -> ParticleManager.getInstance().setZIndex(instanceId, valFloat.toInt())
                13 -> pInstance.particleScaleX = valFloat
                14 -> pInstance.particleScaleY = valFloat
            }
        } catch (e: Exception) {
            Log.e("SetParticleProperty", "Error in update()", e)
        }
    }
}
