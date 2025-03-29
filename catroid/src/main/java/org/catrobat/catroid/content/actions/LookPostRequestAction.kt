/*package org.catrobat.catroid.content.actions

import android.util.Log
import okhttp3.*
import org.catrobat.catroid.CatroidApplication
import org.catrobat.catroid.R
import org.catrobat.catroid.common.LookData
import org.catrobat.catroid.io.StorageOperations
import org.catrobat.catroid.stage.StageActivity
import org.catrobat.catroid.stage.StageActivity.stageListener
import org.catrobat.catroid.utils.Utils
import org.catrobat.catroid.web.WebConnection
import java.io.File
import java.io.IOException
import java.io.InputStream

class LookPostRequestAction : WebAction() {
    private var response: Response? = null
    private var errorCode: String? = null
    private var requestBodyJson: String? = null
    private var header: String? = null
    private var lookName: String? = null
    private var fileExtension: String? = null

    fun setRequestBody(json: String) {
        requestBodyJson = json
    }

    fun setUrl(newUrl: String) {
        url = newUrl
    }

    fun setHeader(value: String) {
        header = value
    }

    override fun act(delta: Float): Boolean {
        if (url == null) {
            return true
        }

        if (requestBodyJson == null) {
            handleError("Request body cannot be null.")
            return true
        }

        if (requestStatus == RequestStatus.NOT_SENT && !sendRequest()) {
            handleError("423")
            return true
        }

        if (requestStatus == RequestStatus.WAITING) {
            return false
        }

        stageListener.webConnectionHolder.removeConnection(webConnection)
        handleResponse()
        return true
    }

    private fun sendRequest(): Boolean {
        requestStatus = RequestStatus.WAITING
        webConnection = WebConnection(this, url!!)

        val client = OkHttpClient()
        val requestBody = RequestBody.create(MediaType.parse("${header}; charset=utf-8"), requestBodyJson!!)

        // Создание запроса с заголовками
        val requestBuilder = Request.Builder()
            .url(url!!)
            .post(requestBody)

        val request = requestBuilder.build()

        // Отправка запроса и обработка результата
        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                this@LookPostRequestAction.response = response
                handleResponse()
            }

            override fun onFailure(call: Call, e: IOException) {
                errorCode = e.message
                handleError(errorCode ?: "Unknown error")
            }
        })

        return true // запрос отправлен
    }

    fun getLookFromResponse(response: InputStream): LookData? {
        when {
            errorCode != null -> handleError(errorCode!!)
            response == null -> showToastMessage("Invalid format: response empty")
            else -> try {
                val lookFile = File.createTempFile(lookName, fileExtension)
                StorageOperations.copyStreamToFile(response, lookFile)
                LookData(lookName, lookFile).apply {
                    collisionInformation.calculate()
                    return this
                }
            } catch (exception: IOException) {
                Log.e(javaClass.simpleName, "Couldn't interpret InputStream as image", exception)
                showToastMessage("Invalid format: interperet error")
            }
        }
        return null
    }

    override fun handleResponse() {
        response?.let {
            if (it.isSuccessful) {
                val responseBody = it.body()?.string()
                Log.d(javaClass.simpleName, "Response: $responseBody")
                // Здесь можно обработать ответ, если это необходимо
            } else {
                handleError(it.code().toString())
            }
        } ?: handleError("Response is null")
    }

    override fun handleError(error: String) {
        errorCode = error
        CatroidApplication.getAppContext()?.let {
            showToastMessage(it.getString(R.string.look_request_http_error_message, url, errorCode))
        }
    }

    private fun showToastMessage(message: String) {
        val params = arrayListOf<Any>(message)
        StageActivity.messageHandler.obtainMessage(StageActivity.SHOW_TOAST, params).sendToTarget()
    }

    override fun restart() {
        response = null
        errorCode = null
        requestBodyJson = null
        header = null // очищаем заголовки
        super.restart()
    }

    override fun onCancelledCall() {
        response = null
        errorCode = null
        requestBodyJson = null
        header = null // очищаем заголовки
        super.onCancelledCall()
    }
}
*/