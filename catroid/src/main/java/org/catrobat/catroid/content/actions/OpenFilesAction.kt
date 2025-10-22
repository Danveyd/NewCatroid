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

import android.Manifest
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
import org.catrobat.catroid.utils.ErrorLog
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

    private fun getMimeType(url: String): String {
        val extension = MimeTypeMap.getFileExtensionFromUrl(url)?.lowercase()
        var mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)

        if (mimeType == null) {
            mimeType = when (extension) {
                "doc" -> "application/msword"
                "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                "xls" -> "application/vnd.ms-excel"
                "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                "ppt" -> "application/vnd.ms-powerpoint"
                "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
                "pdf" -> "application/pdf"
                "txt" -> "text/plain"
                "html" -> "text/html"
                "htm" -> "team/html"
                "gif" -> "image/gif"
                "bin" -> "application/octet-stream"
                else -> "*/*"
            }
        }
        return mimeType
    }

    override fun update(percent: Float) {
        val activity = StageActivity.activeStageActivity.get()
        val context = CatroidApplication.getAppContext()
        if (activity == null || context == null) return

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                PERMISSION_EXTERNAL_STORAGE_SAVE_COPY
            )
            return
        }

        val fileName = file?.interpretString(scope)
        if (fileName.isNullOrEmpty()) {
            toast("File name is empty")
            return
        }

        if (scope == null) return

        scope?.project?.let { proj ->
            val fileToOpen = proj.getFile(fileName)
            Log.d("OpenFileAction", "Trying to open file: ${fileToOpen.absolutePath}")

            if (!fileToOpen.exists()) {
                toast("File not found: $fileName")
                Log.e("OpenFileAction", "File does not exist at path: ${fileToOpen.absolutePath}")
                return
            }

            try {
                val uri: Uri = FileProvider.getUriForFile(
                    context,
                    context.packageName + ".fileProvider",
                    fileToOpen
                )

                val intent = Intent(Intent.ACTION_VIEW)

                val mimeType = getMimeType(fileToOpen.name)
                intent.setDataAndType(uri, mimeType)
                Log.d("OpenFileAction", "URI: $uri, MIME Type: $mimeType")

                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

                val chooser = Intent.createChooser(intent, "Open with")

                if (intent.resolveActivity(context.packageManager) != null) {
                    activity.startActivity(chooser)
                } else {
                    toast("No application found to open this file type")
                    Log.e(
                        "OpenFileAction",
                        "No activity found to handle Intent with MIME type: $mimeType"
                    )
                }
            } catch (e: Exception) {
                ErrorLog.log(e.message ?: "**message not provided :(**")
                Log.e("OpenFileAction", "Error creating URI or starting activity", e)
                toast("Error opening file: ${e.localizedMessage}")
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
