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
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Environment
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import com.badlogic.gdx.utils.ScreenUtils
import kotlinx.coroutines.GlobalScope
import org.catrobat.catroid.CatroidApplication
import org.catrobat.catroid.ProjectManager
import org.catrobat.catroid.common.LookData
import org.catrobat.catroid.common.ScreenValues
import org.catrobat.catroid.content.MyActivityManager
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.io.StorageOperations
import org.catrobat.catroid.stage.ScreenshotSaver
import org.catrobat.catroid.stage.ScreenshotSaverCallback
import org.catrobat.catroid.stage.StageActivity
import org.catrobat.paintroid.common.PERMISSION_EXTERNAL_STORAGE_SAVE
import org.catrobat.paintroid.common.PERMISSION_EXTERNAL_STORAGE_SAVE_COPY
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.jar.Manifest
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

open class ZipAction : TemporalAction() {
    var scope: Scope? = null
    var name: Formula? = null
    var files: Formula? = null

    override fun update(percent: Float) {
        val activity = StageActivity.activeStageActivity.get()
        activity?.runOnUiThread {
            if (ContextCompat.checkSelfPermission(
                    CatroidApplication.getAppContext(),
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    PERMISSION_EXTERNAL_STORAGE_SAVE
                )
            }
            if (ContextCompat.checkSelfPermission(
                    CatroidApplication.getAppContext(),
                    android.Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE),
                    PERMISSION_EXTERNAL_STORAGE_SAVE_COPY
                )
            }
        }

        val fileName = getName(name) ?: "myZip.zip"
        val paths = getFilePaths(files?.interpretString(scope) ?: "")

        // Получаем директорию загрузок
        val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val file = File(dir, fileName)
        val path = file.absolutePath

        // Создаем ZIP-архив
        zipFiles(paths, path)
    }


    fun getName(inputName: Formula?): String? {
        inputName?.let { inname ->
            var name = inname.interpretString(scope)
            val lastDotIndex = name.lastIndexOf('.')
            if(lastDotIndex <= 0 && lastDotIndex >= name.length - 1) {
                name += ".zip"
            }
            return name
        }
        return null
    }

    fun getFilePaths(input: String): List<String> {
        val delimiter = ","
        val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

        // Разделяем входную строку на строки, удаляем пробелы и фильтруем пустые строки
        return input.split(delimiter).map { it.trim() }
            .filter { it.isNotEmpty() } // Убираем пустые строки
            .map { fileName ->
                File(dir, fileName).absolutePath // Формируем полный путь к каждому файлу
            }
    }

    fun zipFiles(fileList: List<String>, zipFilePath: String) {
        try {
            ZipOutputStream(FileOutputStream(zipFilePath)).use { zos ->
                for (filePath in fileList) {
                    val file = File(filePath)
                    val zipEntry = ZipEntry(file.name)
                    zos.putNextEntry(zipEntry)

                    file.inputStream().use { input ->
                        input.copyTo(zos)
                    }
                    zos.closeEntry()
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}
