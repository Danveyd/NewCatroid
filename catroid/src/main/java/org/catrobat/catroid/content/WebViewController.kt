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

class WebViewController {
    companion object {
        private val webViews = mutableMapOf<String, WebView>() // Хранение WebView по имени
        var rootLayout: FrameLayout = FrameLayout(CatroidApplication.getAppContext())

        fun createWebView(
            activity: Activity, // Передаем активность
            name: String,
            url: String,
            x: Int,
            y: Int,
            width: Int,
            height: Int
        ) {
            // Запускаем создание WebView в главном потоке
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
            activity: Activity, // Передаем активность
            name: String,
            html: String,
            x: Int,
            y: Int,
            width: Int,
            height: Int
        ) {
            // Запускаем загрузку HTML в главном потоке
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


        /**
         * Удаление WebView по имени
         */
        fun removeWebView(name: String) {
            val webView = webViews[name]
            if (webView != null) {
                rootLayout.removeView(webView) // Убираем из макета
                webView.destroy() // Уничтожаем WebView
                webViews.remove(name) // Удаляем из Map
                println("WebView с именем $name удалён")
            } else {
                println("WebView с именем $name не найден")
            }
        }
    }
}