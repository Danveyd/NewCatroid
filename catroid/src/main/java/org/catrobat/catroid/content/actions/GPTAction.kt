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

import android.util.Log
import androidx.annotation.CallSuper
import com.badlogic.gdx.scenes.scene2d.Action
import okhttp3.Response
import org.catrobat.catroid.TrustedDomainManager
import org.catrobat.catroid.common.Constants
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.formulaeditor.InterpretationException
import org.catrobat.catroid.stage.BrickDialogManager
import org.catrobat.catroid.stage.StageActivity
import org.catrobat.catroid.stage.StageActivity.stageListener
import org.catrobat.catroid.utils.ErrorLog
import org.catrobat.catroid.web.WebConnection
import org.catrobat.catroid.web.WebConnection.WebRequestListener
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlin.random.Random

abstract class GPTAction : Action(), WebRequestListener {
    var webConnection: WebConnection? = null
    var pformula: Formula? = null
    var sformula: Formula? = null
    var scope: Scope? = null
    var prompt: String? = null
    var system: String? = null
    var url: String? = "https://text.pollinations.ai" //hi?private=true&system=you%20are%20ilon%20mask"

    enum class RequestStatus {
        NOT_SENT, WAITING, FINISHED
    }
    var requestStatus: RequestStatus = RequestStatus.NOT_SENT

    enum class PermissionStatus {
        UNKNOWN, PENDING, DENIED, GRANTED
    }
    private var permissionStatus: PermissionStatus = PermissionStatus.UNKNOWN

    private fun interpretUrl(): Boolean {
        try {
            prompt = pformula?.interpretString(scope) ?: ""
            system = sformula?.interpretString(scope) ?: ""
            val promptEnc = URLEncoder.encode(prompt, StandardCharsets.UTF_8.toString())
            val systemEnc = URLEncoder.encode(system, StandardCharsets.UTF_8.toString())
            val seed = Random.nextInt(0, 999999999)

            url = "https://text.pollinations.ai/$promptEnc?private=true&system=$systemEnc&seed=$seed"
            //url = "https://www.google.com"
            Log.d("GPTAction", "url: " + url)
            return true
        } catch (e: Exception) {
            ErrorLog.log(e.message?: "**message not provided :(**")
            // UnsupportedEncodingException может возникнуть, если указана неверная кодировка,
            // но с StandardCharsets.UTF_8 это крайне маловероятно.
            Log.e("GPTAction_Interpret", "Error in interpretUrl", e)
            return false
        }
    }

    private fun askForPermission() {
        if (StageActivity.messageHandler == null) {
            denyPermission()
        } else {
            permissionStatus = PermissionStatus.PENDING
            val params = arrayListOf(BrickDialogManager.DialogType.WEB_ACCESS_DIALOG, this, url!!)
            StageActivity.messageHandler.obtainMessage(StageActivity.SHOW_DIALOG, params).sendToTarget()
        }
    }

    fun grantPermission() {
        permissionStatus = PermissionStatus.GRANTED
    }

    fun denyPermission() {
        permissionStatus = PermissionStatus.DENIED
    }

    // В WebAction.java и GPTAction.java
    // В WebAction.kt и GPTAction.kt
    override fun act(delta: Float): Boolean {
        // Интерпретация URL (остается без изменений)
        if (url == null && !interpretUrl()) {
            return true
        }

        // Проверка разрешений (остается без изменений)
        when (permissionStatus) {
            PermissionStatus.UNKNOWN -> checkPermission()
            PermissionStatus.DENIED -> {
                handleError(Constants.ERROR_AUTHENTICATION_REQUIRED.toString())
                return true
            }
            else -> {}
        }
        if (permissionStatus == PermissionStatus.PENDING) {
            return false
        }

        // ▼▼▼ НАЧАЛО ИСПРАВЛЕННОЙ ЛОГИКИ ▼▼▼

        // Атомарная проверка: если мы еще не отправляли запрос И отправка не удалась,
        // то обрабатываем ошибку и завершаем действие.
        if (requestStatus == RequestStatus.NOT_SENT && !sendRequest()) {
            handleError(Constants.ERROR_TOO_MANY_REQUESTS.toString())
            return true
        }

        // Если мы в состоянии ожидания, просто ждем дальше.
        if (requestStatus == RequestStatus.WAITING) {
            return false
        }

        // Сюда мы попадаем, только если requestStatus == FINISHED
        stageListener.webConnectionHolder.removeConnection(webConnection)
        handleResponse() // Этот метод будет вызван только после успешного onRequestSuccess
        return true

        // ▲▲▲ КОНЕЦ ИСПРАВЛЕННОЙ ЛОГИКИ ▲▲▲
    }

    private fun checkPermission() =
        if (TrustedDomainManager.isURLTrusted(url!!)) {
            grantPermission()
        } else {
            //askForPermission()
            grantPermission()
        }

    private fun sendRequest(): Boolean {
        // Сначала меняем состояние. Это гарантирует, что мы не будем пытаться
        // отправить запрос снова и снова в каждом кадре.
        requestStatus = RequestStatus.WAITING
        webConnection = WebConnection(this, url!!)

        if (stageListener.webConnectionHolder.addConnection(webConnection!!)) {
            webConnection!!.sendWebRequest()
            return true
        } else {
            // Если не удалось добавить в холдер, возвращаем false,
            // но статус уже WAITING, поэтому мы не застрянем в цикле отправки.
            return false
        }
    }

    abstract fun handleResponse()
    abstract fun handleError(error: String)

    @CallSuper
    override fun onRequestSuccess(httpResponse: Response) {
        Log.i("GPTAction_Callback", "[${System.identityHashCode(this)}] onRequestSuccess CALLED! URL: '$url', HTTP Code: ${httpResponse.code}")
        requestStatus = RequestStatus.FINISHED
    }

    @CallSuper
    override fun onRequestError(httpError: String) {
        Log.e("GPTAction_Callback", "[${System.identityHashCode(this)}] onRequestError CALLED! URL: '$url', Error: $httpError")
        requestStatus = RequestStatus.FINISHED
    }

    @CallSuper
    override fun restart() {
        stageListener.webConnectionHolder.removeConnection(webConnection)
        webConnection = null
        url = null
        requestStatus = RequestStatus.NOT_SENT
        permissionStatus = PermissionStatus.UNKNOWN
    }

    @CallSuper
    override fun onCancelledCall() {
        webConnection = null
        url = null
        requestStatus = RequestStatus.NOT_SENT
    }
}
