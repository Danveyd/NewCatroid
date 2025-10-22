package org.catrobat.catroid.content.actions

import android.widget.Toast
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.android.AndroidFileHandle
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.CatroidApplication
import org.catrobat.catroid.R
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.raptor.SceneManager
import org.catrobat.catroid.stage.StageActivity

class LoadSceneAction : TemporalAction() {
    var scope: Scope? = null
    var fileName: Formula? = null

    override fun update(percent: Float) {
        val sceneFileName = fileName?.interpretString(scope)
        if (sceneFileName.isNullOrEmpty()) {
            return
        }

        val sceneFile = scope?.project?.getFile(sceneFileName)

        if (sceneFile == null || !sceneFile.exists()) {
            return
        }

        val threeDManager = StageActivity.activeStageActivity.get()?.stageListener?.threeDManager
        val sceneManager = SceneManager(threeDManager)

        val fileHandle = Gdx.files.absolute(sceneFile.absolutePath)
        sceneManager.loadAndReplaceScene(fileHandle)
    }
}