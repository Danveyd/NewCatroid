package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.stage.StageActivity
import org.catrobat.catroid.content.WebViewController

class DeleteWebAction : TemporalAction() {
    var scope: Scope? = null
    var name: Formula? = null

    override fun update(percent: Float) {
        var namev = name?.interpretObject(scope)?.toString() ?: ""

        StageActivity.activeStageActivity.get()?.removeView(namev);
        StageActivity.activeStageActivity.get()?.setWebViewCallback(namev, null)
    }
}