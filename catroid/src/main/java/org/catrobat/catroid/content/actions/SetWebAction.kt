package org.catrobat.catroid.content.actions

import android.widget.Toast
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.formulaeditor.UserVariable
import org.catrobat.catroid.stage.StageActivity
import org.catrobat.catroid.stage.StageActivity.WebViewCallback


class SetWebAction : TemporalAction() {
    var scope: Scope? = null
    var userVariable: UserVariable? = null
    var name: Formula? = null

    override fun update(percent: Float) {
        val activity: StageActivity = StageActivity.activeStageActivity.get() ?: return;

        activity.setWebViewCallback(name?.interpretString(scope) ?: "",
            WebViewCallback { message -> // Здесь переменная 'message' будет в точности равна "Привет из WebView!"
                userVariable?.value = message
            })
    }
}