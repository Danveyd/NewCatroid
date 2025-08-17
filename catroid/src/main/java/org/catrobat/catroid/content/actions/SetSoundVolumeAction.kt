// В пакете: org.catrobat.catroid.content.actions
package org.catrobat.catroid.content.actions

import android.util.Log
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.common.SoundInfo
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.formulaeditor.InterpretationException
import org.catrobat.catroid.io.SoundManager

class SetSoundVolumeAction : TemporalAction() {
    var scope: Scope? = null
    var sound: SoundInfo? = null
    var volume: Formula? = null

    override fun update(percent: Float) {
        val sprite = scope?.sprite
        val soundInfo = sound

        if (sprite == null || soundInfo == null) {
            return
        }

        try {
            val newVolume = volume?.interpretFloat(scope) ?: 100f
            val soundPath = soundInfo.file.absolutePath

            SoundManager.getInstance().setVolumeForSound(soundPath, sprite, newVolume)

        } catch (e: InterpretationException) {
            Log.d(javaClass.simpleName, "Formula interpretation failed.", e)
        }
    }
}