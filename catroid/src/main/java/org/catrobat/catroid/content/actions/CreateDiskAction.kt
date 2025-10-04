package org.catrobat.catroid.content.actions
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.stage.StageActivity
class CreateDiskAction : TemporalAction() {
    var scope: Scope? = null
    var diskName: Formula? = null
    var diskSize: Formula? = null
    override fun update(percent: Float) {
        val nameStr = diskName?.interpretString(scope) ?: "disk.qcow2"
        val sizeStr = diskSize?.interpretString(scope) ?: "2G"

        val stageActivity = StageActivity.activeStageActivity.get()
        stageActivity?.createHardDisk(nameStr, sizeStr)
    }
}