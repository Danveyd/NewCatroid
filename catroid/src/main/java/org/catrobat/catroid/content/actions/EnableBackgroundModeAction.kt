package org.catrobat.catroid.content.actions

import android.content.Context
import android.content.Intent
import android.os.Build
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.CatroidApplication
import org.catrobat.catroid.common.ChEngineBackgroundService
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.stage.StageActivity

class EnableBackgroundModeAction : TemporalAction() {
    var scope: Scope? = null
    var title: Formula? = null
    var text: Formula? = null

    override fun update(percent: Float) {
        val context = CatroidApplication.getAppContext() ?: return
        val titleStr = title?.interpretString(scope) ?: "Работа в фоне"
        val textStr = text?.interpretString(scope) ?: "Скрипты активны"

        StageActivity.getActiveStageListener()?.isBackgroundModeEnabled = true

        val intent = Intent(context, ChEngineBackgroundService::class.java).apply {
            putExtra("TITLE", titleStr)
            putExtra("TEXT", textStr)
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
