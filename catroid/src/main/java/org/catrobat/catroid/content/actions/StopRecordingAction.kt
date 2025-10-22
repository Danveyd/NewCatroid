package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import java.io.File
import android.util.Log
import org.catrobat.catroid.content.AudioRecordingManager

class StopRecordingAction : TemporalAction() {
    var scope: Scope? = null
    var fileNameFormula: Formula? = null

    override fun update(percent: Float) {
        val project = scope?.project ?: return
        val fileName = fileNameFormula?.interpretString(scope)
        if (fileName.isNullOrEmpty()) {
            Log.e("StopRecordingAction", "File name is empty.")
            return
        }

        val finalFileName = if (fileName.contains('.')) fileName else "$fileName.m4a"

        val targetDirectory = project.filesDir

        if (!targetDirectory.exists()) {
            if (!targetDirectory.mkdirs()) {
                Log.e("StopRecordingAction", "Failed to create directory: ${targetDirectory.absolutePath}")
                return
            }
        }

        val finalFile = File(targetDirectory, finalFileName)

        AudioRecordingManager.getInstance().stopRecordingAndSave(finalFile)
    }
}