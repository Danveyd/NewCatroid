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
import org.catrobat.catroid.stage.StageActivity
import org.catrobat.catroid.stage.StageActivity.IntentListener
import android.util.Log
import okhttp3.Call
import org.catrobat.catroid.CatroidApplication
import org.catrobat.catroid.R
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okhttp3.Callback
import org.catrobat.catroid.content.MyActivityManager
import java.io.IOException
import org.catrobat.catroid.formulaeditor.FormulaElement
import org.catrobat.catroid.formulaeditor.UserVariable

import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import java.util.ArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class PostWebRequestAction() : TemporalAction() {
    var scope: Scope? = null
    var rurl: Formula? = null
    var header: Formula? = null
    var body: Formula? = null
    var userVariable: UserVariable? = null

    fun setVariable(userVariable: UserVariable?) {
        this.userVariable = userVariable ?: return
    }

    override fun update(percent: Float) {
        val client = OkHttpClient.Builder()
            .connectTimeout(0, TimeUnit.SECONDS) // Устанавливаем таймаут подключения в 0 секунд
            .readTimeout(0, TimeUnit.SECONDS) // Устанавливаем таймаут чтения в 0 секунд
            .writeTimeout(0, TimeUnit.SECONDS) // Устанавливаем таймаут записи в 0 секунд
            .build()


        var urlVal = rurl?.interpretObject(scope) ?: ""
        var urlText = urlVal.toString()
        var headerVal = header?.interpretObject(scope) ?: ""
        var bodyVal = body?.interpretObject(scope) ?: ""

        // Извлекаем только тип контента из заголовка
        var headerText = headerVal.toString().trim()
        var mediaTypeString = headerText.removePrefix("Content-Type:").trim() // Убираем префикс

        var json = bodyVal.toString()

        if (userVariable == null) {
            return
        }

        val mediaType = MediaType.parse(mediaTypeString) ?: MediaType.get("application/json") // Фолбэк на JSON, если тип не валиден
        //val latch = CountDownLatch(1) // Создаем CountDownLatch с 1
        var responseBody: String? = null
        var errorMessage: String? = null
        val bodyn = RequestBody.create(mediaType, json)
        val request = Request.Builder()
            .url(urlText)
            .post(bodyn)
            .build()
        Thread {
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: okhttp3.Call, e: IOException) {
                    // Обновляем UI в основном потоке
                    MyActivityManager.stage_activity?.runOnUiThread {
                        userVariable?.value = "Response error: ${e.message}"
                    }
                }

                override fun onResponse(call: okhttp3.Call, response: Response) {
                    if (response.isSuccessful) {
                        val bodyStr = response.body()?.string() ?: "Empty response"
                        // Обновляем UI в основном потоке
                        MyActivityManager.stage_activity?.runOnUiThread {
                            userVariable?.value = bodyStr
                        }
                    } else {
                        val errorMessage = "Error ${response.code()}: ${response.message()}"
                        // Обновляем UI в основном потоке
                        MyActivityManager.stage_activity?.runOnUiThread {
                            userVariable?.value = errorMessage
                        }
                    }
                }
            })
        }.start() // Запускаем поток
    }
}