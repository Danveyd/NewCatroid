package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.io.SoundCacheManager
import android.util.Log

class PrepareSoundAction : TemporalAction() {
    var scope: Scope? = null
    var fileNameFormula: Formula? = null
    var cacheNameFormula: Formula? = null

    override fun update(percent: Float) {
        val fileName = fileNameFormula?.interpretString(scope)
        val cacheName = cacheNameFormula?.interpretString(scope)
        val project = scope?.project

        if (fileName.isNullOrEmpty() || cacheName.isNullOrEmpty() || project == null) {
            return
        }

        try {
            val soundFile = project.getFile(fileName)
            if (soundFile != null && soundFile.exists()) {
                // Вызываем новый менеджер
                SoundCacheManager.getInstance().loadSound(cacheName, soundFile.absolutePath)
            } else {
                Log.e("PrepareSoundAction", "Sound file not found: $fileName")
            }
        } catch (e: Exception) {
            Log.e("PrepareSoundAction", "Error preparing sound", e)
        }
    }
}