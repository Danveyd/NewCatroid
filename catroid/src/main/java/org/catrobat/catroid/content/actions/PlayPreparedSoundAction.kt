package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.io.SoundCacheManager
import android.util.Log

class PlayPreparedSoundAction : TemporalAction() {
    var scope: Scope? = null
    var cacheNameFormula: Formula? = null

    override fun update(percent: Float) {
        val cacheName = cacheNameFormula?.interpretString(scope)
        if (cacheName.isNullOrEmpty()) {
            return
        }
        // Вызываем новый менеджер
        SoundCacheManager.getInstance().playSound(cacheName)
    }
}