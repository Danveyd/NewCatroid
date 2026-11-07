package org.catrobat.catroid.content.actions

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.badlogic.gdx.scenes.scene2d.Action
import org.catrobat.catroid.CatroidApplication
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.UserVariable
import org.catrobat.catroid.io.StorageOperations
import org.catrobat.catroid.stage.StageActivity
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class ChooseFileAction : Action() {
    var scope: Scope? = null
    var fileType: Int = 4
    var variable: UserVariable? = null

    private var started = false
    @Volatile private var finished = false

    companion object {
        private val TAG = ChooseFileAction::class.java.simpleName

        private val MIME_TYPES = arrayOf(
            "image/*",   // 0 - Image
            "video/*",   // 1 - Video
            "audio/*",   // 2 - Audio
            "application/*", // 3 - Document
            "*/*"        // 4 - Any
        )

        private var TARGET_DIRECTORY = File(CatroidApplication.getAppContext().filesDir, "chosen_files")
    }

    override fun act(delta: Float): Boolean {
        if (!started) {
            started = true
            runAsyncChoose()
        }
        return finished
    }

    private fun runAsyncChoose() {
        val proj = scope?.project
        if (proj == null) {
            finished = true
            return
        }

        TARGET_DIRECTORY = proj.filesDir

        val activity = StageActivity.activeStageActivity.get()
        if (activity == null) {
            finished = true
            return
        }

        activity.runOnUiThread {
            val safeType = if (fileType in MIME_TYPES.indices) fileType else MIME_TYPES.size - 1
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = MIME_TYPES[safeType]
                addCategory(Intent.CATEGORY_OPENABLE)
            }

            activity.queueIntent(object : StageActivity.IntentListener {
                override fun getTargetIntent(): Intent {
                    return Intent.createChooser(intent, "Choose a file")
                }

                override fun onIntentResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
                    if (resultCode == Activity.RESULT_OK && data != null && data.data != null) {
                        val uri = data.data
                        addSpriteObjectFromUri(uri)
                    }
                    finished = true
                    return true
                }
            })
        }
    }

    private fun addSpriteObjectFromUri(uri: Uri?, extension: String = "png") {
        val contentResolver = CatroidApplication.getAppContext().contentResolver
        val resolvedFileName = StorageOperations.resolveFileName(contentResolver, uri)
        val destFileName = resolvedFileName ?: ("file_" + System.currentTimeMillis() + "." + extension)

        try {
            val file = StorageOperations.copyUriToDir(contentResolver, uri, scope?.project?.getFilesDir(), destFileName)
            if (file != null && file.exists()) {
                variable?.value = file.name
            }
        } catch (e: Exception) {
            e.printStackTrace()
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
        variable = null
    }
}
