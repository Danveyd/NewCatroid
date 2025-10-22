package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.AudioRecordingManager
import org.catrobat.catroid.stage.StageActivity

class StartRecordingAction : TemporalAction() {
    override fun update(percent: Float) {
        val stage = StageActivity.activeStageActivity?.get() ?: return
        AudioRecordingManager.getInstance().startRecording(stage.applicationContext)
    }
}