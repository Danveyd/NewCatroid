package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.stage.StageActivity

class EvalWebAction : TemporalAction() {
    //private lateinit var rootLayout: FrameLayout // Контейнер для WebView
    var scope: Scope? = null
    var code: Formula? = null
    var name: Formula? = null

    override fun update(percent: Float) {
        val activity: StageActivity = StageActivity.activeStageActivity.get() ?: return;

        activity.runOnUiThread(Runnable {
            activity.executeJavaScript(
                name?.interpretString(scope) ?: "",
                code?.interpretString(scope) ?: ""
            )
        })
    }
}