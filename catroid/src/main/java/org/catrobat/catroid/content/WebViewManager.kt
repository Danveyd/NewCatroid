import android.content.Context
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.LinearLayout

class WebViewManager(private val context: Context) {  // Вы принимаете контекст
    private val webViews = mutableMapOf<String, WebView>()  // Хранение WebView по имени

    fun createWebView(name: String, url: String, posX: Int, posY: Int, width: Int, height: Int, container: LinearLayout) {
        val webView = WebView(context).apply {
            webViewClient = WebViewClient()
            settings.javaScriptEnabled = true
            loadUrl(url)
            layoutParams = LinearLayout.LayoutParams(width, height).also {
                it.setMargins(posX, posY, 0, 0)
            }
        }

        webViews[name] = webView  // Сохраняем WebView по имени
        container.addView(webView)
    }

    fun removeWebView(name: String, container: LinearLayout) {
        webViews[name]?.let {
            container.removeView(it)  // Удаляем WebView из контейнера
            webViews.remove(name)  // Удаляем из мапы
        }
    }
}
