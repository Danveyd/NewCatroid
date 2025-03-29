package org.catrobat.catroid.content.actions

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

    override fun update(percent: Float) {
        val activity = StageActivity.activeStageActivity.get()
        activity?.runOnUiThread {
            val value = runScript?.interpretObject(scope) as? String ?: ""
            evaluateJS(value, userVariable)
        }
    }

    fun evaluateJS(script: String, variable: UserVariable?) {
        val webView = WebView(CatroidApplication.getAppContext())
        webView.settings.javaScriptEnabled = true
        webView.webViewClient = WebViewClient()

        webView.addJavascriptInterface(JsInterface(), "Android")

        webView.loadData("", "text/html", null)

        // Проверяем, что переменная не null перед использованием
        if (variable != null) {
            webView.evaluateJavascript(script) { result ->
                println("JS out: $result")
                variable.value = result // Присваиваем результат переменной
            }
        }
    }
}
