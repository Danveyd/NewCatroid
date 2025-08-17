/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2022 The Catrobat Team
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

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting
import org.catrobat.catroid.R
import org.catrobat.catroid.common.Constants
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.formulaeditor.UserVariable
import org.catrobat.catroid.stage.StageActivity
import org.catrobat.catroid.stage.StageActivity.IntentListener
import org.catrobat.catroid.utils.Utils
import java.io.File
import java.io.IOException
import java.util.ArrayList
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.pm.PackageManager
import android.provider.MediaStore
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.catrobat.catroid.CatroidApplication


class WriteVarToFileAction : TemporalAction(), IntentListener {
    var scope: Scope? = null
    var formula: Formula? = null
    var userVariable: UserVariable? = null

    fun request(activity: Activity) {
        //showSuccessMessage("requesting...")
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE),
            1001,
        )
        //ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), REQUEST_WRITE_STORAGE)
    }

    override fun update(percent: Float) {
        if (ContextCompat.checkSelfPermission(CatroidApplication.getAppContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            val activity = StageActivity.activeStageActivity.get()
            activity?.runOnUiThread {
                request(activity)
            }
        }
        if (userVariable == null || formula == null) {
            return
        }
        saveOrOverwriteInDownloads(CatroidApplication.getAppContext(), getFileName(), userVariable!!.value.toString())
    }

    /**
     * Сохраняет текстовый контент в файл в общую папку "Загрузки".
     * Если файл с таким именем уже существует, он будет ПЕРЕЗАПИСАН.
     * Если файл не существует, он будет создан.
     *
     * @param context Контекст приложения.
     * @param fileName Имя файла для сохранения (например, "my-data.txt").
     * @param content Новое содержимое файла.
     * @return Uri файла или null в случае ошибки.
     */
    fun saveOrOverwriteInDownloads(context: Context, fileName: String, content: String): Uri? {
        // --- Логика для Android 10 (Q) и новее ---
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentResolver = context.contentResolver

            // 1. Попытка найти существующий файл
            val projection = arrayOf(MediaStore.MediaColumns._ID)
            val selection = "${MediaStore.MediaColumns.RELATIVE_PATH} = ? AND ${MediaStore.MediaColumns.DISPLAY_NAME} = ?"
            val selectionArgs = arrayOf("${Environment.DIRECTORY_DOWNLOADS}/", fileName)

            var existingFileUri: Uri? = null

            // Выполняем запрос
            contentResolver.query(
                MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
                projection,
                selection,
                selectionArgs,
                null
            )?.use { cursor ->
                // Если курсор не пустой и есть хотя бы одна строка - файл найден
                if (cursor.moveToFirst()) {
                    val idColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                    val id = cursor.getLong(idColumn)
                    existingFileUri = ContentUris.withAppendedId(
                        MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
                        id
                    )
                }
            }

            try {
                // 2. Если файл НАЙДЕН - перезаписываем его
                if (existingFileUri != null) {
                    // 'w' означает "write mode", который обрезает файл до нуля перед записью (т.е. перезаписывает)
                    contentResolver.openOutputStream(existingFileUri!!, "w")?.use {
                        it.write(content.toByteArray(Charsets.UTF_8))
                    }
                    println("Файл успешно перезаписан: $existingFileUri")
                    return existingFileUri
                }
                // 3. Если файл НЕ НАЙДЕН - создаем новый
                else {
                    val newValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                        // ИСПРАВЛЕНИЕ: Позволяем системе самой определить тип или используем обобщенный
                        put(MediaStore.MediaColumns.MIME_TYPE, "*/*") // Вместо "text/plain"
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    }
                    val newFileUri = contentResolver.insert(MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), newValues)

                    newFileUri?.let { uri ->
                        contentResolver.openOutputStream(uri)?.use {
                            it.write(content.toByteArray(Charsets.UTF_8))
                        }
                        println("Новый файл успешно создан: $newFileUri")
                    }
                    return newFileUri
                }
            } catch (e: Exception) {
                println("Ошибка при сохранении/перезаписи файла: ${e.message}")
                return null
            }
        }
        // --- Логика для старых версий Android (до 10) ---
        else {
            // Требует разрешения WRITE_EXTERNAL_STORAGE в манифесте для API < 29
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(downloadsDir, fileName)

            try {
                // writeText по умолчанию создает или перезаписывает файл.
                file.writeText(content, Charsets.UTF_8)
                println("Файл успешно сохранен/перезаписан: ${file.absolutePath}")
                // Для старых версий мы не можем легко получить content:// Uri, но можем вернуть файловый Uri.
                // Однако для совместимости лучше просто вернуть null или true/false.
                return Uri.fromFile(file)
            } catch (e: Exception) {
                println("Ошибка при сохранении/перезаписи файла: ${e.message}")
                return null
            }
        }
    }

    private fun getFileName(): String {
        var fileName = Utils.sanitizeFileName(formula?.interpretString(scope))

        // ИСПРАВЛЕНИЕ: Более простая и надежная проверка.
        // Если в имени файла нет точки, значит, пользователь не указал расширение.
        // Тогда добавляем .txt по умолчанию.
        if (!fileName.contains(".")) {
            fileName += Constants.TEXT_FILE_EXTENSION // ".txt"
        }
        // Если точка есть (например, "my_archive.zip"), то ничего не делаем,
        // оставляя расширение пользователя.

        return fileName
    }

    private fun showSuccessMessage(fileName: String) {
        val context = CatroidApplication.getAppContext()
        val message = context.getString(R.string.brick_write_variable_to_file_success, fileName)
        val params = ArrayList<Any>(listOf(message))
        StageActivity.messageHandler.obtainMessage(StageActivity.SHOW_TOAST, params).sendToTarget()
    }

    private fun writeToUri(uri: Uri, content: String) {
        try {
            val context: Context = CatroidApplication.getAppContext()
            val contentResolver = context.contentResolver
            contentResolver.openOutputStream(uri).use {
                it?.write(content.toByteArray())
            }
            showSuccessMessage(getFileName())
        } catch (e: IOException) {
            Log.e(javaClass.simpleName, "Could not write variable value to storage.")
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun getTargetIntent(): Intent {
        val fileName = getFileName()
        val context = StageActivity.activeStageActivity.get()?.context
        val title = context?.getString(R.string.brick_write_variable_to_file_top) ?: ""
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            type = "*/*"
            putExtra(Intent.EXTRA_TITLE, fileName)
            putExtra(DocumentsContract.EXTRA_INITIAL_URI, Environment.DIRECTORY_DOWNLOADS)
        }
        return Intent.createChooser(intent, title)
    }

    override fun onIntentResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        if (resultCode == Activity.RESULT_OK) {
            data?.data?.let {
                val content: String = when (val value = userVariable?.value ?: 0) {
                    Double -> (value as Double).toBigDecimal().toPlainString()
                    else -> value.toString()
                }
                writeToUri(it, content)
            }
            return true
        }
        return false
    }
}
