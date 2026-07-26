package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import android.util.Log
import com.google.gson.Gson
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.particles.ParticleEffectModel
import org.catrobat.catroid.particles.ParticleManager

class CreateParticleEffectAction : TemporalAction() {
    var scope: Scope? = null
    var fileFormula: Formula? = null

    override fun update(percent: Float) {
        try {
            val fileName = fileFormula?.interpretString(scope) ?: return
            if (fileName.isBlank()) return

            var model: ParticleEffectModel? = ParticleManager.getInstance().loadModelFromProjectFile(fileName)

            if (model == null) {
                model = ParticleEffectModel()
                model.effectId = fileName
            }

            ParticleManager.getInstance().registerEffectTemplate(fileName, model)
        } catch (e: Exception) {
            Log.e("CreateParticleAction", "Error in update()", e)
        }
    }
}
