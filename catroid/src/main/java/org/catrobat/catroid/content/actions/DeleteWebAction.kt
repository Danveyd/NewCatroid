package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.stage.StageActivity
import org.catrobat.catroid.content.WebViewController

class DeleteWebAction : TemporalAction() {
    //private lateinit var rootLayout: FrameLayout // Контейнер для WebView
    var scope: Scope? = null
    var name: Formula? = null

    override fun update(percent: Float) {
        //rootLayout = FrameLayout(CatroidApplication.getAppContext())
        var namev = name?.interpretObject(scope)?.toString() ?: ""

        val activity = StageActivity.activeStageActivity.get()
        //activity?.runOnUiThread {
        //WebViewController.removeWebView(namev)
        //}
    }
}