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
package org.catrobat.catroid.web

import android.util.Log
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.catrobat.catroid.common.Constants
import org.catrobat.catroid.stage.StageActivity
import java.io.IOException
import java.lang.ref.WeakReference

class WebConnection(private val okHttpClient: OkHttpClient, listener: WebRequestListener, private val url: String) {
    private var weakListenerReference: WeakReference<WebRequestListener>? = WeakReference(listener)
    private var call: Call? = null

    constructor(listener: WebRequestListener, url: String) :
            this(StageActivity.stageListener.webConnectionHolder.okHttpClient, listener, url) {
            //this(OkHttpClient(), listener, url) { // Временно используем новый OkHttpClient() с дефолтными настройками
        Log.d("WebConnection", "[${System.identityHashCode(this)}] Constructor called. URL: '$url', Listener: ${System.identityHashCode(listener)}") // ++ Лог конструктора
    }

    companion object {
        private const val EXCEPTION_MESSAGE_TIMEOUT = "timeout"
        private const val EXCEPTION_MESSAGE_CANCELED = "Canceled"
    }

    @Synchronized
    private fun popListener(): WebRequestListener? {
        val listener = weakListenerReference?.get()
        weakListenerReference = null
        return listener
    }

    @Synchronized
    fun sendWebRequest() {
        Log.d("WebConnection", "[${System.identityHashCode(this)}] sendWebRequest CALLED. URL: '$url'") // ++ Лог вызова
        try {
            val request = Request.Builder()
                .url(url) // Убедитесь, что url здесь не null и корректен
                .header("User-Agent", "Mozilla/5.0 (Android 10; Mobile; rv:91.0) Gecko/91.0 Firefox/91.0")
                .build()
            Log.d("WebConnection", "[${System.identityHashCode(this)}] Request object created. Starting enqueue...") // ++ Лог перед enqueue
            call = okHttpClient.newCall(request)
            call?.enqueue(createCallback())
            Log.d("WebConnection", "[${System.identityHashCode(this)}] Enqueue called.") // ++ Лог после enqueue
        } catch (exception: IllegalArgumentException) {
            Log.e("WebConnection", "[${System.identityHashCode(this)}] IllegalArgumentException in sendWebRequest for URL: '$url'", exception) // ++ Лог ошибки
            // Важно: если здесь ошибка, popListener может не быть вызван, а должен бы.
            // И статус в Action не изменится.
            popListener()?.onRequestError(Constants.ERROR_BAD_REQUEST.toString())
            // requestStatus в Action останется WAITING, что может привести к зависанию, если не обрабатывается.
            // Однако, если sendRequest в Action устанавливает WAITING только после успешного addConnection,
            // то при IllegalArgumentException здесь, статус в Action еще будет NOT_SENT, что лучше.
        } catch (e: Exception) { // Перехватываем другие возможные исключения
            Log.e("WebConnection", "[${System.identityHashCode(this)}] Generic Exception in sendWebRequest for URL: '$url'", e)
            popListener()?.onRequestError(Constants.ERROR_SERVER_ERROR.toString()) // или более специфическая ошибка
        }
    }

    private fun createCallback(): Callback {
        return object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.d("WebConnectionCallback", "[${System.identityHashCode(this@WebConnection)}] onFailure ENTERED. URL: $url. Listener ref: ${weakListenerReference?.get()}") // ++ Лог
                popListener()?.apply {
                    when (e.message) {
                        EXCEPTION_MESSAGE_TIMEOUT -> onRequestError(Constants.ERROR_TIMEOUT.toString())
                        EXCEPTION_MESSAGE_CANCELED -> onCancelledCall()
                        else -> onRequestError(Constants.ERROR_SERVER_ERROR.toString())
                    }
                } ?: Log.w("WebConnectionCallback", "onFailure: Listener was null for URL: $url")
            }

            override fun onResponse(call: Call, response: Response) {
                // Добавим логирование для отладки
                Log.d("WebConnectionCallback", "[${System.identityHashCode(this@WebConnection)}] onResponse ENTERED. URL: $url. Code: ${response.code()}. Listener ref: ${weakListenerReference?.get()}") // ++ Лог
                val listener = popListener()
                if (listener == null) {
                    Log.w("WebConnectionCallback", "onResponse: Listener was null for URL: $url. Closing response body.")
                    response.close() // Важно закрыть тело, если слушателя нет
                    return
                }

                if (response.isSuccessful) {
                    // В onRequestSuccess (в WebRequestAction/AskGPTAction) вызывается body().string(),
                    // который считывает и ЗАКРЫВАЕТ тело ответа.
                    listener.onRequestSuccess(response)
                } else {
                    listener.onRequestError(response.code().toString())
                    // Для НЕУСПЕШНЫХ ответов тело ответа НЕ было считано и закрыто слушателем.
                    // Закрываем его здесь, чтобы освободить ресурсы.
                    Log.d("WebConnectionCallback", "Closing response body for unsuccessful request. URL: $url, Code: ${response.code()}")
                    response.close() // <--- ВАЖНОЕ ИСПРАВЛЕНИЕ
                }
            }
        }
    }

    fun cancelCall() {
        okHttpClient.dispatcher()?.executorService()?.execute {
            call?.cancel()
        }
    }

    interface WebRequestListener {
        fun onRequestSuccess(httpResponse: Response)
        fun onRequestError(httpError: String)
        fun onCancelledCall()
    }
}
