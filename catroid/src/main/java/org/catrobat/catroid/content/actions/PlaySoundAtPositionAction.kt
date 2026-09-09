package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.stage.StageActivity

class PlaySoundAtPositionAction : TemporalAction() {
    var scope: Scope? = null
    var soundName: Formula? = null
    var instanceName: Formula? = null
    var posX: Formula? = null
    var posY: Formula? = null
    var posZ: Formula? = null
    var volume: Formula? = null
    var pitch: Formula? = null
    var loop: Boolean = false

    override fun update(percent: Float) {
        val engine = StageActivity.getActiveStageListener()?.threeDManager ?: return

        val sound = soundName?.interpretString(scope)
        val instance = instanceName?.interpretString(scope)
        if (sound.isNullOrEmpty() || instance.isNullOrEmpty()) return

        val x = posX?.interpretFloat(scope) ?: 0f
        val y = posY?.interpretFloat(scope) ?: 0f
        val z = posZ?.interpretFloat(scope) ?: 0f

        val vol = (volume?.interpretFloat(scope) ?: 100f) / 100f
        val pit = (pitch?.interpretFloat(scope) ?: 100f) / 100f

        engine.playSoundAt(instance, sound, x, y, z, vol, pit, loop, true, 250f)
    }
}
