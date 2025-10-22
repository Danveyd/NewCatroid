package org.catrobat.catroid.content

import android.app.Activity
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
import android.widget.FrameLayout


//УСТАРЕВШИЙ И НЕ ИСПОЛЬЗУЕТСЯ
//УСТАРЕВШИЙ И НЕ ИСПОЛЬЗУЕТСЯ
class WebViewController {
    companion object {
        private val webViews = mutableMapOf<String, WebView>()
        var rootLayout: FrameLayout = FrameLayout(CatroidApplication.getAppContext())

        fun createWebView(
            activity: Activity,
            name: String,
            url: String,
            x: Int,
            y: Int,
            width: Int,
            height: Int
        ) {
            activity.runOnUiThread {
                if (webViews.containsKey(name)) {
                    println("WebView с именем $name уже существует")
                    return@runOnUiThread
                }

                val webView = WebView(activity).apply {
                    settings.javaScriptEnabled = true
                    webViewClient = WebViewClient()
                    loadUrl(url)
                }

                val params = FrameLayout.LayoutParams(width, height).apply {
                    leftMargin = x
                    topMargin = y
                }
                rootLayout.addView(webView, params)
                webViews[name] = webView
            }
        }

        fun loadHtmlIntoWebView(
            activity: Activity,
            name: String,
            html: String,
            x: Int,
            y: Int,
            width: Int,
            height: Int
        ) {
            activity.runOnUiThread {
                if (webViews.containsKey(name)) {
                    println("WebView с именем $name уже существует")
                    return@runOnUiThread
                }

                val webView = WebView(activity).apply {
                    settings.javaScriptEnabled = true
                    webViewClient = WebViewClient()
                    loadData(html, "text/html", "UTF-8")
                }

                val params = FrameLayout.LayoutParams(width, height).apply {
                    leftMargin = x
                    topMargin = y
                }
                rootLayout.addView(webView, params)
                webViews[name] = webView
            }
        }


        fun removeWebView(name: String) {
            val webView = webViews[name]
            if (webView != null) {
                rootLayout.removeView(webView)
                webView.destroy()
                webViews.remove(name)
                println("WebView с именем $name удалён")
            } else {
                println("WebView с именем $name не найден")
            }
        }
    }
}