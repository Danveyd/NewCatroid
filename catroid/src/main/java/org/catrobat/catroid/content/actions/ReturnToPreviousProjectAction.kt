package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.ProjectManager
import org.catrobat.catroid.stage.StageActivity
import android.util.Log
import java.io.File

class ReturnToPreviousProjectAction : TemporalAction() {
    override fun update(percent: Float) {
        val stage = StageActivity.activeStageActivity?.get() ?: return

        val previousProjectPath = ProjectManager.popProjectHistory()

        if (previousProjectPath != null) {
            try {
                ProjectManager.getInstance().loadProject(File(previousProjectPath))
            } catch (e: Exception) {
                Log.e("ReturnAction", "Failed to reload previous project. App might be in an unstable state.", e)
            }
        }

        stage.finish()
    }
}