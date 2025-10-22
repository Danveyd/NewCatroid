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
import android.content.pm.PackageManager
import android.os.Build
import org.catrobat.catroid.stage.StageActivity
import org.catrobat.catroid.stage.StageActivity.IntentListener
import android.util.Log
import org.catrobat.catroid.CatroidApplication
import org.catrobat.catroid.R

import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import java.util.ArrayList
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import android.os.Environment
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import okhttp3.Callback
import okhttp3.RequestBody
import org.catrobat.catroid.utils.ErrorLog
import org.catrobat.paintroid.common.PERMISSION_EXTERNAL_STORAGE_SAVE
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.CountDownLatch
import java.util.jar.Manifest
import kotlin.concurrent.thread

class FileUrlAction() : TemporalAction() {
    var scope: Scope? = null
    var url: Formula? = null
    var name: Formula? = null

    override fun update(percent: Float) {
        val activity = StageActivity.activeStageActivity.get()
        activity?.runOnUiThread {
            if (ContextCompat.checkSelfPermission(
                    CatroidApplication.getAppContext(),
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    PERMISSION_EXTERNAL_STORAGE_SAVE
                )
            }
        }
        val us = url?.interpretString(scope) ?: "http://e95814zx.beget.tech/map.jpg"
        val fileName = getName(name) ?: "fileFromUrl.jpg"
        downloadFileFromUrl(us, fileName)
    }

    fun getName(inputName: Formula?): String? {
        inputName?.let { inname ->
            var name = inname.interpretString(scope)
            val lastDotIndex = name.lastIndexOf('.')
            if(lastDotIndex <= 0 && lastDotIndex >= name.length - 1) {
                name += ".jpg"
            }
            return name
        }
        return null
    }

    fun showToast(toast: String) {
        val params = ArrayList<Any>(listOf(toast))
        StageActivity.messageHandler.obtainMessage(StageActivity.SHOW_TOAST, params).sendToTarget()
    }

    fun downloadFileFromUrl(fileUrl: String, newFileName: String) {
        Thread {
            var inputStream: InputStream? = null
            var fileOutputStream: FileOutputStream? = null
            try {
                Log.d("DownloadFile", "Connecting to URL: $fileUrl")
                val url = URL(fileUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("User-Agent", "Mozilla/5.0")
                Log.d("DownloadFile", "Connecting...")
                connection.connect()
                Log.d("DownloadFile", "Connection Ended")

                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    Log.e("DownloadFile", "Ошибка: ${connection.responseCode}")
                    showToast("Ошибка: ${connection.responseCode}")
                    return@Thread
                }

                inputStream = connection.inputStream

                val downloadsFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val destFile = File(downloadsFolder, newFileName)

                fileOutputStream = FileOutputStream(destFile)
                inputStream.copyTo(fileOutputStream)

                Log.d("DownloadFile", "Файл скачан: ${destFile.absolutePath}")
            } catch (e: Exception) {
                showToast("Ошибка при загрузке файла: ${e.message}")
                Log.e("DownloadFile", "Ошибка при загрузке файла: ${e.message}", e)
            } finally {
                inputStream?.close()
                fileOutputStream?.close()
            }
        }.start()
    }
}
