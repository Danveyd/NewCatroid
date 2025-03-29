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
import com.badlogic.gdx.scenes.scene2d.Action
import org.catrobat.catroid.CatroidApplication
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
import androidx.core.app.ActivityCompat
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WriteVariableToFileAction : TemporalAction(), IntentListener {
    var scope: Scope? = null
    var formula: Formula? = null
    var userVariable: UserVariable? = null

    override fun update(percent: Float) {
        if (userVariable == null || formula == null) {
            return
        }
        createAndWriteToFile()
    }

    @VisibleForTesting
    fun createAndWriteToFile() {
        /*val fileName = getFileName()
        createFile(fileName)?.let {
            val content = userVariable?.value.toString() ?: "0"
            writeToFile(it, content)
        }*/
        writeNew()
        /*val content = userVariable?.value.toString() ?: "0"
        saveToFile(getFileName(), content)*/
    }

    private fun writeUsingSystemFilePicker() {
        StageActivity.messageHandler?.obtainMessage(
            StageActivity.REGISTER_INTENT, arrayListOf(this))?.sendToTarget()
    }

    private fun writeUsingLegacyExternalStorage() {
        val fileName = getFileName()
        createFile(fileName)?.let {
            val content = userVariable?.value.toString() ?: "0"
            writeToFile(it, content)
        }
    }

    fun saveToFile(data: String, fileName: String) {
        // Проверяем, если имя файла уже содержит расширение
        val finalFileName = if (fileName.contains(".")) fileName else "$fileName.txt"

        // Получаем путь до папки "Загрузки" (Downloads)
        val downloadsPath = System.getProperty("user.home") + "/Downloads"

        // Создаем объект File
        val file = File(downloadsPath, finalFileName)

        // Записываем данные в файл
        file.writeText(data) // Записываем текст в файл
        showSuccessMessage(file.name)
        //println("Данные сохранены в файл: ${file.absolutePath}") // Уведомляем о сохранении
    }

    private fun writeNew() {
        showSuccessMessage(formula?.interpretString(scope).toString())
        val fileName = getFileName()
        showSuccessMessage(fileName)
        CoroutineScope(Dispatchers.IO).launch {
            createFile(fileName)?.let { file ->
                val content = userVariable?.value.toString() ?: "0"
                val result = writeToFile2(file, content)
                // Теперь возвращаемся в основной поток для уведомления пользователя
                withContext(Dispatchers.Main) {
                    if (result) {
                        //showSuccessMessage(file.name)
                    } else {
                        Log.e(javaClass.simpleName, "Could not write variable value to storage.")
                    }
                }
            }
        }
    }

    @VisibleForTesting
    fun createFile(fileName: String): File? {
        showSuccessMessage("create: " + fileName)
        val file = File(Constants.DOWNLOAD_DIRECTORY, fileName)
        return if (file.exists() || file.createNewFile()) {
            file
        } else null
    }

    @VisibleForTesting
    fun writeToFile(file: File, content: String) {
        showSuccessMessage("write1")
        try {
            file.writeText(content)
            showSuccessMessage(file.name)
        } catch (e: IOException) {
            Log.e(javaClass.simpleName, "Could not write variable value to storage.")
        }
    }

    @VisibleForTesting
    fun writeToFile2(file: File, content: String): Boolean {
        showSuccessMessage("write2")
        return try {
            file.writeText(content)
            true // Возвращаем true, если запись успешна
        } catch (e: IOException) {
            Log.e(javaClass.simpleName, "Could not write variable value to storage.")
            false // Возвращаем false, если произошла ошибка
        }
    }

    private fun getFileName(): String {
        var fileName = Utils.sanitizeFileName(formula?.interpretString(scope))
        if (!fileName.contains(Regex(Constants.ANY_EXTENSION_REGEX))) {
            fileName += Constants.TEXT_FILE_EXTENSION
        }
        /*val message = fileName
        val params = ArrayList<Any>(listOf(message))
        StageActivity.messageHandler.obtainMessage(StageActivity.SHOW_LONG_TOAST, params).sendToTarget()*/
        //Utils.sanitizeFileName(formula?.interpretString(scope))
        return fileName
    }

    private fun showSuccessMessage(fileName: String) {
        val context = CatroidApplication.getAppContext()
        val message = context.getString(R.string.brick_write_variable_to_file_success, fileName)
        val params = ArrayList<Any>(listOf(message))
        StageActivity.messageHandler.obtainMessage(StageActivity.SHOW_TOAST, params).sendToTarget()
    }

    private fun writeToUri(uri: Uri, content: String) {
        //showSuccessMessage("uri")
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
        showSuccessMessage("getIntent")
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

    override fun onIntentResult(resultCode: Int, data: Intent?) {
        showSuccessMessage("result")
        if (resultCode == Activity.RESULT_OK) {
            data?.data?.let {
                val content: String = when (val value = userVariable?.value ?: 0) {
                    Double -> (value as Double).toBigDecimal().toPlainString()
                    else -> value.toString()
                }
                writeToUri(it, content)
            }
        }
    }
}
