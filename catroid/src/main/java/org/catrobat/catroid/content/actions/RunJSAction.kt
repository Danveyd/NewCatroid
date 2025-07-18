package org.catrobat.catroid.content.actions

import android.os.Handler
import android.os.Looper
import android.webkit.WebView
import android.webkit.WebViewClient
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.CatroidApplication
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.formulaeditor.UserVariable
import org.catrobat.catroid.stage.StageActivity
import org.catrobat.catroid.stage.StageActivity.IntentListener
import android.util.Log
import androidx.core.app.ActivityCompat
import app.cash.quickjs.QuickJs
import org.catrobat.catroid.R

class JsInterface {
    @android.webkit.JavascriptInterface
    fun showResult(result: String) {
        println("Result In JS: $result")
    }
}

class RunJSAction : TemporalAction() {
    var scope: Scope? = null
    var runScript: Formula? = null
    var userVariable: UserVariable? = null

    init {
        // Это действие должно быть мгновенным
        duration = 0f
    }

    override fun update(percent: Float) {
        if (percent < 1.0f) {
            return
        }

        val scriptToRun = runScript?.interpretObject(scope) as? String ?: ""


        // Выполняем JS, передавая скрипт и переменную для результата
        Companion.evaluateJS(scriptToRun, userVariable)
    }

    // В классе RunJSAction
    companion object {

        private var isWebViewCreated = false
        private val uiHandler = Handler(Looper.getMainLooper())

        private val sharedWebView: WebView by lazy {
            // В блоке lazy мы теперь делаем только самую базовую настройку.
            // loadData здесь больше не нужен.
            val webView = WebView(CatroidApplication.getAppContext())
            webView.settings.javaScriptEnabled = true
            webView.addJavascriptInterface(JsInterface(), "Android")
            webView.webViewClient = object : WebViewClient() {}

            isWebViewCreated = true
            webView
        }

        fun evaluateJS(script: String, variable: UserVariable?) {
            uiHandler.post {
                // !!! ГЛАВНОЕ ИСПРАВЛЕНИЕ !!!
                // Перед каждым выполнением скрипта мы перезагружаем пустую страницу.
                // Это сбрасывает JavaScript-контекст и гарантирует чистое окружение.
                // Эта операция очень быстрая.
                sharedWebView.loadData("", "text/html", null)

                // Теперь выполняем скрипт
                sharedWebView.evaluateJavascript(script) { result ->
                    val cleanResult = result ?: "" //?.removeSurrounding("\"") ?: ""
                    variable?.value = cleanResult
                }
            }
        }

        fun destroyWebView() {
            if (isWebViewCreated) {
                uiHandler.post {
                    sharedWebView.stopLoading()
                    //sharedWebView.destroy()
                }
            }
        }
    }
}
