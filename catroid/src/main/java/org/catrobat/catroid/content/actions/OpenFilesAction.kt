/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2024 The Catrobat Team
 * (<http://developer.catrobat.org/credits>)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * An additional term exception under section 7 of the GNU Affero
 * General Public License, version 3, is available at
 * http://developer.catrobat.org/license_additional_term
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.catrobat.catroid.content.actions

import android.widget.Toast
import android.content.Context
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import org.catrobat.catroid.stage.StageActivity
import org.catrobat.catroid.stage.StageActivity.IntentListener
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import org.catrobat.catroid.CatroidApplication
import org.catrobat.catroid.R

import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.paintroid.common.PERMISSION_EXTERNAL_STORAGE_SAVE_COPY
import java.io.File
import java.util.ArrayList

class OpenFilesAction() : TemporalAction() {
    private var contextt: Context? = null
    var scope: Scope? = null
    var file: Formula? = null

    private fun toast(toast: String) {
        val params = ArrayList<Any>(listOf(toast))
        StageActivity.messageHandler.obtainMessage(StageActivity.SHOW_TOAST, params).sendToTarget()
    }

    override fun update(percent: Float) {
        val activity = StageActivity.activeStageActivity.get()
        val context = CatroidApplication.getAppContext() ?: return

        // Запрос разрешений
        activity?.runOnUiThread {
            if (ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE
                )
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE),
                    PERMISSION_EXTERNAL_STORAGE_SAVE_COPY
                )
            }
        }

        val fileName = file?.interpretString(scope) ?: return
        //val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        scope?.let { scop ->
            scop.project?.let{ proj ->//File(downloadsDir, fileName)
                val fileToOpen = proj.getFile(fileName)
                Log.d(
                    "OpenFileAction",
                    "File path: ${fileToOpen.absolutePath}, Exists: ${fileToOpen.exists()}"
                )
                Log.d("FileProvider", "Opening file: ${fileToOpen.absolutePath}")
                Log.d("FileProvider", "Authority: ${context.packageName}.fileProvider")

                if (fileToOpen.exists()) {
                    val uri: Uri = FileProvider.getUriForFile(
                        context,
                        context.packageName + ".fileProvider",
                        fileToOpen
                    )

                    val intent = Intent(Intent.ACTION_VIEW)

                    val fileExtension = fileToOpen.extension.lowercase()
                    var mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension)
                    if (mimeType == null) {
                        mimeType = "*/*" // Тип по умолчанию, если не удалось определить
                    }
                    intent.setDataAndType(uri, mimeType)

                    intent.flags =
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK

                    if (intent.resolveActivity(context.packageManager) != null) {
                        activity?.runOnUiThread {
                            activity.startActivity(Intent.createChooser(intent, "Open with"))
                        }
                    } else {
                        toast("No application found to open this file")
                    }
                } else {
                    toast("File not found")
                }
            }
        }
    }

    private fun copyFileToNewCatroidDir(file: File): File {
        val newCatroidDir = File(Environment.getExternalStorageDirectory(), "NewCatroid")
        if (!newCatroidDir.exists()) {
            newCatroidDir.mkdirs()
        }

        val newFile = File(newCatroidDir, file.name)
        file.copyTo(newFile)
        return newFile
    }

}
