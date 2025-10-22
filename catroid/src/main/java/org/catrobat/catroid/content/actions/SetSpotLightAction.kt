/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2024 The Catrobat Team
 * (<http://developer.catrobat.org/credits>)
 */

package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.stage.StageActivity

class SetSpotLightAction : TemporalAction() {
    var scope: Scope? = null
    var lightId: Formula? = null
    var posX: Formula? = null
    var posY: Formula? = null
    var posZ: Formula? = null
    var dirX: Formula? = null
    var dirY: Formula? = null
    var dirZ: Formula? = null
    var colorR: Formula? = null
    var colorG: Formula? = null
    var colorB: Formula? = null
    var intensity: Formula? = null
    var angle: Formula? = null
    var range: Formula? = null
    var exponent: Formula? = null

    override fun update(percent: Float) {
        val threeDManager = StageActivity.activeStageActivity.get()?.stageListener?.threeDManager ?: return
        try {
            val id = lightId?.interpretString(scope) ?: ""
            if (id.isEmpty()) return

            val pX = posX?.interpretFloat(scope) ?: 0f
            val pY = posY?.interpretFloat(scope) ?: 0f
            val pZ = posZ?.interpretFloat(scope) ?: 0f
            val dX = dirX?.interpretFloat(scope) ?: 0f
            val dY = dirY?.interpretFloat(scope) ?: -1f
            val dZ = dirZ?.interpretFloat(scope) ?: 0f
            val r = (colorR?.interpretInteger(scope) ?: 255) / 255f
            val g = (colorG?.interpretInteger(scope) ?: 255) / 255f
            val b = (colorB?.interpretInteger(scope) ?: 255) / 255f
            val i = intensity?.interpretFloat(scope) ?: 1000f
            val cutAngle = angle?.interpretFloat(scope) ?: 30f
            val lightRange = range?.interpretFloat(scope) ?: 0f
            val exp = exponent?.interpretFloat(scope) ?: 1f

            threeDManager.setSpotLight(id, pX, pY, pZ, dX, dY, dZ, r, g, b, i, cutAngle, exp, lightRange)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}