package org.catrobat.catroid.content.actions

import android.app.Activity
import android.content.Intent
import android.widget.Toast
import com.badlogic.gdx.scenes.scene2d.Action
import org.catrobat.catroid.CatroidApplication
import org.catrobat.catroid.R
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.stage.StageActivity
import java.io.File

class ExportProjectFileAction : Action() {
    var scope: Scope? = null
    var projectFileName: Formula? = null

    private var started = false
    @Volatile private var finished = false

    override fun act(delta: Float): Boolean {
        if (!started) {
            started = true
            runExport()
        }
        return finished
    }

    private fun runExport() {
        val project = scope?.project
        if (project == null) {
            finished = true
            return
        }

        val context = CatroidApplication.getAppContext()
        val activity = StageActivity.activeStageActivity?.get()

        if (activity == null) {
            Toast.makeText(context, context.getString(R.string.epf_activity_unavailable), Toast.LENGTH_SHORT).show()
            finished = true
            return
        }

        val fileName = projectFileName?.interpretString(scope)
        if (fileName.isNullOrEmpty()) {
            finished = true
            return
        }

        val sourceFile: File = project.getFile(fileName)
        if (!sourceFile.exists()) {
            finished = true
            return
        }

        activity.runOnUiThread {
            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
                putExtra(Intent.EXTRA_TITLE, fileName)
            }

            activity.queueIntent(object : StageActivity.IntentListener {
                override fun getTargetIntent(): Intent = intent

                override fun onIntentResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
                    if (resultCode == Activity.RESULT_OK && data != null && data.data != null) {
                        val destinationUri = data.data!!
                        try {
                            activity.contentResolver.openOutputStream(destinationUri)?.use { out ->
                                sourceFile.inputStream().use { input ->
                                    input.copyTo(out)
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    finished = true
                    return true
                }
            })
        }
    }

    override fun restart() {
        super.restart()
        started = false
        finished = false
    }

    override fun reset() {
        super.reset()
        started = false
        finished = false
        scope = null
        projectFileName = null
    }
}
