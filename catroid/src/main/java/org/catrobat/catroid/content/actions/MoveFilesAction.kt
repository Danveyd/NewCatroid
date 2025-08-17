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
import android.content.ContentValues
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

class MoveFilesAction() : TemporalAction() {
    private var contextt: Context? = null
    var scope: Scope? = null
    var fileName: Formula? = null

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun update(percent: Float) {
        val fileName_s = scope?.project?.checkExtension(fileName?.interpretString(scope), "txt") ?: "variable.txt"
        scope?.project?.let {
            val file = it.getFile(fileName_s)

            copyFileToDownloads(CatroidApplication.getAppContext(), file)
        }
    }

    private fun copyFileToDir(file: File, dir: File): File {
        val newFile = File(dir, file.name)
        if (newFile.exists()) newFile.delete()
        file.copyTo(newFile, overwrite = true)
        return newFile
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun copyFileToDownloads(context: Context, sourceFile: File): File? {
        // MediaStore работает с ContentResolver, а не напрямую с файлами
        val resolver = context.contentResolver

        // 1. Описываем наш файл с помощью ContentValues
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, sourceFile.name) // Имя файла
            // Можно указать MIME-тип, если он известен, например "application/zip"
            // put(MediaStore.MediaColumns.MIME_TYPE, "application/zip")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }

        try {
            // 2. Просим систему создать для нас пустой файл в Downloads и дать нам URI
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                ?: throw IOException("Failed to create new MediaStore record.")

            // 3. Открываем OutputStream в этот новый файл
            resolver.openOutputStream(uri)?.use { outputStream ->
                // 4. Открываем InputStream из нашего исходного файла
                sourceFile.inputStream().use { inputStream ->
                    // 5. Копируем байты из источника в место назначения
                    inputStream.copyTo(outputStream)
                }
            }
            // Возвращаем File-представление, хотя лучше работать с Uri
            return File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), sourceFile.name)
        } catch (e: IOException) {
            // Если файл уже существует, resolver.insert может выдать ошибку.
            // В некоторых версиях Android нужно сначала удалить старый файл через MediaStore.
            Log.e("MoveFilesAction", "Error copying file to Downloads: ${e.message}")

            // Попытка удаления существующего файла через MediaStore (более сложный вариант)
            // val selection = "${MediaStore.MediaColumns.RELATIVE_PATH}=? AND ${MediaStore.MediaColumns.DISPLAY_NAME}=?"
            // val selectionArgs = arrayOf(Environment.DIRECTORY_DOWNLOADS + "/", sourceFile.name)
            // resolver.delete(MediaStore.Downloads.EXTERNAL_CONTENT_URI, selection, selectionArgs)
            // ... и потом снова пробовать insert ...

            return null
        }
    }
}
