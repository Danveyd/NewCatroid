package org.catrobat.catroid.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.catrobat.catroid.R
import org.catrobat.catroid.common.FlavoredConstants.DEFAULT_ROOT_DIRECTORY
import org.catrobat.catroid.databinding.ActivityCommunityWebviewBinding
import org.catrobat.catroid.io.ZipArchiver
import org.catrobat.catroid.io.asynctask.ProjectLoader
import org.catrobat.catroid.utils.FileMetaDataExtractor
import org.catrobat.catroid.utils.community.CommunityTokenManager
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayInputStream
import java.io.File
import java.util.Collections

class CommunityWebViewActivity : AppCompatActivity(), ProjectLoader.ProjectLoadListener {

    private lateinit var binding: ActivityCommunityWebviewBinding
    private lateinit var webView: WebView
    private val client = OkHttpClient()

    private val activeDownloads = Collections.synchronizedSet(HashSet<String>())

    private var hideWarningRunnable: Runnable? = null
    private val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityCommunityWebviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        webView = binding.webView

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true)
        }

        setupWebView()
        setupNotificationListeners()

        webView.loadUrl("https://newcatroid.mindpy.dev")
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            useWideViewPort = true
            loadWithOverviewMode = true
            javaScriptCanOpenWindowsAutomatically = true
            setSupportMultipleWindows(false)
        }

        webView.webChromeClient = WebChromeClient()

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                syncSessionWithWeb(view)
            }

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url?.toString() ?: return false
                if (isDownloadUrl(url)) {
                    startNativeDownloadAndImport(url)
                    return true
                }
                return false
            }

            override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                val url = request?.url?.toString() ?: return null
                val method = request.method ?: "GET"

                if (isDownloadUrl(url)) {
                    if (method.equals("GET", ignoreCase = true)) {
                        runOnUiThread {
                            startNativeDownloadAndImport(url)
                        }
                    }

                    val response = WebResourceResponse("text/plain", "UTF-8", ByteArrayInputStream(ByteArray(0)))
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        val headers = HashMap<String, String>().apply {
                            put("Access-Control-Allow-Origin", "*")
                            put("Access-Control-Allow-Headers", "*")
                            put("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
                        }
                        response.responseHeaders = headers
                    }
                    return response
                }
                return super.shouldInterceptRequest(view, request)
            }
        }

        webView.setDownloadListener { url, _, _, _, _ ->
            if (url.startsWith("blob:", ignoreCase = true)) return@setDownloadListener
            if (isDownloadUrl(url)) {
                startNativeDownloadAndImport(url)
            }
        }
    }

    private fun setupNotificationListeners() {
        binding.btnDismissNotification.setOnClickListener {
            hideNotificationCard()
        }
    }

    private fun isDownloadUrl(url: String): Boolean {
        val lowerUrl = url.lowercase()
        return (lowerUrl.contains("/file") ||
                lowerUrl.contains(".newtrobat") ||
                lowerUrl.contains(".catrobat") ||
                lowerUrl.contains("/files/")) &&
                lowerUrl.contains("backend.mindpy.dev")
    }

    private fun syncSessionWithWeb(view: WebView?) {
        val token = CommunityTokenManager.getToken(this)
        val userJson = CommunityTokenManager.getUserJson(this)

        if (!token.isNullOrEmpty() && !userJson.isNullOrEmpty()) {
            val userJsonBase64 = Base64.encodeToString(userJson.toByteArray(), Base64.NO_WRAP)
            val script = """
                if (localStorage.getItem('meander_token') !== '$token') {
                    localStorage.setItem('meander_token', '$token');
                    localStorage.setItem('meander_user', atob('$userJsonBase64'));
                    window.location.reload();
                }
            """.trimIndent()
            view?.evaluateJavascript(script, null)
        } else {
            val script = """
                if (localStorage.getItem('meander_token')) {
                    localStorage.removeItem('meander_token');
                    localStorage.removeItem('meander_user');
                    window.location.reload();
                }
            """.trimIndent()
            view?.evaluateJavascript(script, null)
        }
    }

    private fun showWarningNotificationCard(message: String) {
        runOnUiThread {
            binding.tvWarningText.text = message

            hideWarningRunnable?.let { mainHandler.removeCallbacks(it) }

            if (binding.warningNotificationCard.visibility != View.VISIBLE) {
                binding.warningNotificationCard.translationY = -300f
                binding.warningNotificationCard.alpha = 0f
                binding.warningNotificationCard.visibility = View.VISIBLE

                binding.warningNotificationCard.animate()
                    .translationY(0f)
                    .alpha(1f)
                    .setDuration(300)
                    .setInterpolator(android.view.animation.DecelerateInterpolator())
                    .start()
            }

            val runnable = Runnable { hideWarningNotificationCard() }
            hideWarningRunnable = runnable
            mainHandler.postDelayed(runnable, 3000)
        }
    }

    private fun hideWarningNotificationCard() {
        runOnUiThread {
            if (binding.warningNotificationCard.visibility == View.VISIBLE) {
                binding.warningNotificationCard.animate()
                    .translationY(-300f)
                    .alpha(0f)
                    .setDuration(250)
                    .setInterpolator(android.view.animation.AccelerateInterpolator())
                    .withEndAction {
                        binding.warningNotificationCard.visibility = View.GONE
                    }
                    .start()
            }
        }
    }

    private fun showNotificationCard(title: String, status: String, isFinished: Boolean) {
        runOnUiThread {
            binding.tvNotificationTitle.text = title
            binding.tvNotificationStatus.text = status

            if (isFinished) {
                binding.notificationProgressBar.visibility = View.GONE
                binding.btnOpenProject.visibility = View.VISIBLE
            } else {
                binding.notificationProgressBar.visibility = View.VISIBLE
                binding.btnOpenProject.visibility = View.GONE
            }

            if (binding.downloadNotificationCard.visibility != View.VISIBLE) {
                binding.downloadNotificationCard.translationY = 300f
                binding.downloadNotificationCard.alpha = 0f
                binding.downloadNotificationCard.visibility = View.VISIBLE

                binding.downloadNotificationCard.animate()
                    .translationY(0f)
                    .alpha(1f)
                    .setDuration(400)
                    .setInterpolator(android.view.animation.OvershootInterpolator(1.1f))
                    .start()
            }
        }
    }

    private fun hideNotificationCard(onAnimationEnd: (() -> Unit)? = null) {
        runOnUiThread {
            if (binding.downloadNotificationCard.visibility == View.VISIBLE) {
                binding.downloadNotificationCard.animate()
                    .translationY(300f)
                    .alpha(0f)
                    .setDuration(300)
                    .setInterpolator(android.view.animation.AccelerateInterpolator())
                    .withEndAction {
                        binding.downloadNotificationCard.visibility = View.GONE
                        onAnimationEnd?.invoke()
                    }
                    .start()
            } else {
                onAnimationEnd?.invoke()
            }
        }
    }

    fun startNativeDownloadAndImport(url: String) {
        if (!url.startsWith("http://", ignoreCase = true) && !url.startsWith("https://", ignoreCase = true)) {
            return
        }

        val gameId = when {
            url.contains("/file") -> url.substringBefore("/file").substringAfterLast("/")
            url.contains(".newtrobat") -> url.substringBefore(".newtrobat").substringAfterLast("_")
            url.contains(".catrobat") -> url.substringBefore(".catrobat").substringAfterLast("_")
            else -> url.substringAfterLast("/")
        }

        if (activeDownloads.isNotEmpty()) {
            showWarningNotificationCard(getString(R.string.community_download_already_active))
            return
        }

        activeDownloads.add(gameId)

        showNotificationCard(
            getString(R.string.community_download_title),
            getString(R.string.community_saving_to_cache),
            isFinished = false
        )

        lifecycleScope.launch {
            val tempFile = withContext(Dispatchers.IO) {
                downloadProjectFile(url, gameId)
            }

            if (tempFile != null && tempFile.exists()) {
                runOnUiThread {
                    binding.tvNotificationStatus.text = getString(R.string.community_importing_files)
                }

                val projectName = withContext(Dispatchers.IO) {
                    unzipAndImport(tempFile, gameId)
                }

                if (projectName != null) {
                    showNotificationCard(
                        getString(R.string.community_import_completed),
                        getString(R.string.community_project_ready),
                        isFinished = true
                    )

                    runOnUiThread {
                        binding.btnOpenProject.setOnClickListener(null)

                        binding.btnOpenProject.setOnClickListener {
                            Log.d("CommunityWebViewDebug", "Кнопка 'Открыть' нажата! Запускаем проект: $projectName")
                            binding.downloadNotificationCard.visibility = View.GONE
                            loadImportedProject(projectName)
                        }
                    }
                } else {
                    hideNotificationCard()
                }
            } else {
                hideNotificationCard()
            }
            activeDownloads.remove(gameId)
        }
    }

    private fun downloadProjectFile(url: String, gameId: String): File? {
        val token = CommunityTokenManager.getToken(this)
        val requestBuilder = Request.Builder().url(url)
        if (!token.isNullOrEmpty()) {
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }

        return try {
            client.newCall(requestBuilder.build()).execute().use { response ->
                if (response.isSuccessful && response.body != null) {
                    val file = File(cacheDir, "temp_project_${gameId}_${System.currentTimeMillis()}.zip")
                    response.body!!.byteStream().use { inputStream ->
                        file.outputStream().use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                    file
                } else {
                    val errorMsg = "HTTP ${response.code}: ${response.message}"
                    runOnUiThread {
                        val localizedError = getString(R.string.community_error_server_rejected, errorMsg)
                        Toast.makeText(this@CommunityWebViewActivity, localizedError, Toast.LENGTH_LONG).show()
                    }
                    null
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            runOnUiThread {
                val localizedError = getString(R.string.community_error_network, e.message ?: "")
                Toast.makeText(this@CommunityWebViewActivity, localizedError, Toast.LENGTH_LONG).show()
            }
            null
        }
    }

    private fun isValidZipFile(file: File): Boolean {
        if (!file.exists() || file.length() < 4) return false
        val bytes = ByteArray(4)
        return try {
            file.inputStream().use { stream ->
                stream.read(bytes)
            }
            bytes[0] == 0x50.toByte() &&
                    bytes[1] == 0x4B.toByte() &&
                    bytes[2] == 0x03.toByte() &&
                    bytes[3] == 0x04.toByte()
        } catch (e: Exception) {
            false
        }
    }

    private fun getProjectNameFromXml(unzipDir: File): String? {
        val codeXml = File(unzipDir, "code.xml")
        if (!codeXml.exists()) return null
        return try {
            val content = codeXml.readText()
            var name = Regex("<programName>(.*?)</programName>").find(content)?.groupValues?.get(1)
            if (name.isNullOrEmpty()) {
                name = Regex("<projectName>(.*?)</projectName>").find(content)?.groupValues?.get(1)
            }
            if (name.isNullOrEmpty()) {
                name = Regex("<name>(.*?)</name>").find(content)?.groupValues?.get(1)
            }
            name?.trim()?.let { decodeXmlEntities(it) }
        } catch (e: Exception) {
            null
        }
    }

    private fun decodeXmlEntities(input: String): String {
        return input.replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
    }

    private fun unzipAndImport(zipFile: File, gameId: String): String? {
        if (!isValidZipFile(zipFile)) {
            runOnUiThread {
                Toast.makeText(this@CommunityWebViewActivity, R.string.community_error_zip_damaged, Toast.LENGTH_LONG).show()
            }
            zipFile.delete()
            return null
        }

        val tempUnzipDir = File(cacheDir, "temp_unzip_${gameId}_${System.currentTimeMillis()}")

        try {
            if (tempUnzipDir.exists()) {
                tempUnzipDir.deleteRecursively()
            }
            tempUnzipDir.mkdirs()

            ZipArchiver().unzip(zipFile.inputStream(), tempUnzipDir)
            zipFile.delete()

            var projectName = getProjectNameFromXml(tempUnzipDir)
            if (projectName.isNullOrEmpty()) {
                projectName = "Imported_$gameId"
            }

            val safeFolderName = FileMetaDataExtractor.encodeSpecialCharsForFileSystem(projectName)
            val destDir = File(DEFAULT_ROOT_DIRECTORY, safeFolderName)

            if (destDir.exists()) {
                destDir.deleteRecursively()
            }

            if (tempUnzipDir.renameTo(destDir)) {
                Log.d("CommunityWebViewDebug", "Перемещено в: ${destDir.absolutePath}")
            } else {
                destDir.mkdirs()
                tempUnzipDir.copyRecursively(destDir, overwrite = true)
                tempUnzipDir.deleteRecursively()
            }

            return projectName
        } catch (e: Exception) {
            e.printStackTrace()
            if (tempUnzipDir.exists()) {
                tempUnzipDir.deleteRecursively()
            }
            runOnUiThread {
                val localizedError = getString(R.string.community_error_unzip, e.message ?: "")
                Toast.makeText(this@CommunityWebViewActivity, localizedError, Toast.LENGTH_LONG).show()
            }
            return null
        }
    }

    private fun loadImportedProject(name: String) {
        val projectDir = File(
            DEFAULT_ROOT_DIRECTORY,
            FileMetaDataExtractor.encodeSpecialCharsForFileSystem(name)
        )
        Log.d("CommunityWebViewDebug", "Загрузка импортированного проекта: ${projectDir.absolutePath}")

        ProjectLoader(projectDir, this)
            .setListener(this)
            .loadProjectAsync()
    }

    override fun onLoadFinished(success: Boolean) {
        Log.d("CommunityWebViewDebug", "Импорт завершен. Результат загрузки проекта: $success")
        if (success) {
            val intent = Intent(this@CommunityWebViewActivity, ProjectActivity::class.java).apply {
                putExtra(ProjectActivity.EXTRA_FRAGMENT_POSITION, ProjectActivity.FRAGMENT_SCENES)
            }
            startActivity(intent)
            finish()
        } else {
            Toast.makeText(this@CommunityWebViewActivity, R.string.community_load_project_failed, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}
