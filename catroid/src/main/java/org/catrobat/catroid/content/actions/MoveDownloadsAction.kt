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
import android.content.ContentUris
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import org.catrobat.catroid.stage.StageActivity
import org.catrobat.catroid.stage.StageActivity.IntentListener
import android.util.Log
import androidx.annotation.RequiresApi
import org.catrobat.catroid.CatroidApplication
import org.catrobat.catroid.R

import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import java.io.File
import java.io.IOException
import java.util.ArrayList

class MoveDownloadsAction() : TemporalAction() {
    private var contextt: Context? = null
    var scope: Scope? = null
    var fileName: Formula? = null

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun update(percent: Float) {
        val fileName_s = scope?.project?.checkExtension(fileName?.interpretString(scope), "txt") ?: "variable.txt"
        scope?.project?.let {
            val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName_s)

            copyFileFromDownloads(CatroidApplication.getAppContext(), fileName_s, it.filesDir)
        }
    }

    private fun copyFileToDir(file: File, dir: File): File {
        val newFile = File(dir, file.name)
        file.copyTo(newFile, overwrite = true)
        return newFile
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun copyFileFromDownloads(context: Context, fileName: String, destinationDir: File): File? {
        val resolver = context.contentResolver

        val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
        val projection = arrayOf(MediaStore.Downloads._ID, MediaStore.Downloads.DISPLAY_NAME)
        val selection = "${MediaStore.Downloads.DISPLAY_NAME} = ?"
        val selectionArgs = arrayOf(fileName)
        val sortOrder = "${MediaStore.Downloads.DATE_ADDED} DESC"

        try {
            resolver.query(collection, projection, selection, selectionArgs, sortOrder)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Downloads._ID)
                    val id = cursor.getLong(idColumn)

                    val contentUri = ContentUris.withAppendedId(MediaStore.Downloads.EXTERNAL_CONTENT_URI, id)

                    resolver.openInputStream(contentUri)?.use { inputStream ->
                        val destinationFile = File(destinationDir, fileName)

                        destinationFile.outputStream().use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                        return destinationFile
                    }
                }
            }
        } catch (e: IOException) {
            Log.e("MoveDownloadsAction", "Error copying file from Downloads: ${e.message}")
            return null
        }

        Log.w("MoveDownloadsAction", "File '$fileName' not found in Downloads.")
        return null
    }
}
