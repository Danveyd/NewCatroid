package org.catrobat.catroid.content.actions

import android.content.Context
import android.content.Intent
import android.os.Build
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.CatroidApplication
import org.catrobat.catroid.R
import org.catrobat.catroid.common.NewCatroidBackgroundService
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.stage.StageActivity

class EnableBackgroundModeAction : TemporalAction() {
    var scope: Scope? = null
    var title: Formula? = null
    var text: Formula? = null

    override fun update(percent: Float) {
        val context = CatroidApplication.getAppContext() ?: return
        val titleStr = title?.interpretString(scope) ?: context.getString(R.string.bgm_default_title)
        val textStr = text?.interpretString(scope) ?: context.getString(R.string.bgm_default_text)

        StageActivity.getActiveStageListener()?.isBackgroundModeEnabled = true

        val intent = Intent(context, NewCatroidBackgroundService::class.java).apply {
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
