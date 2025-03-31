package org.catrobat.catroid.content.actions

import android.app.Activity
import android.os.Build
import android.view.View
import android.view.WindowInsets
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula

class TestAction(private val activity: Activity) : TemporalAction() {
    var scope: Scope? = null

    override fun update(percent: Float) {
        // Для Android 11 и выше (API 30+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            activity.window.insetsController?.show(
                WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars()
            )
        } else {
            // Для старых версий Android (до API 30)
            activity.window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
        }
    }
}

