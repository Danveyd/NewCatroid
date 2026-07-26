package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import android.util.Log
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.particles.ParticleInstance
import org.catrobat.catroid.particles.ParticleManager

class SetParticleBufferRenderAction : TemporalAction() {
    var scope: Scope? = null
    var instanceIdFormula: Formula? = null
    var bufferNameFormula: Formula? = null
    var modeSelection: Int = 0

    override fun update(percent: Float) {
        try {
            val instanceId = instanceIdFormula?.interpretString(scope) ?: return
            val bufferName = bufferNameFormula?.interpretString(scope) ?: ""

            if (instanceId.isBlank()) return

            val pInstance = ParticleManager.getInstance().getInstance(instanceId) ?: return
            pInstance.targetBufferName = bufferName

            pInstance.bufferMode = when (modeSelection) {
                1 -> ParticleInstance.BufferRenderMode.BUFFER_ONLY
                2 -> ParticleInstance.BufferRenderMode.SCREEN_ONLY
                else -> ParticleInstance.BufferRenderMode.SCREEN_AND_BUFFER
            }
        } catch (e: Exception) {
            Log.e("BufferRenderAction", "Error in update()", e)
        }
    }
}
