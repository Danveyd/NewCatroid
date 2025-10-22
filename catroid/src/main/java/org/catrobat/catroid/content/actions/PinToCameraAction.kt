package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.stage.StageActivity

class PinToCameraAction : TemporalAction() {
    var scope: Scope? = null

    override fun update(percent: Float) {
        val listener = StageActivity.getActiveStageListener() ?: return
        val sprite = scope?.sprite ?: return
        listener.pinSpriteToCamera(sprite)
    }
}