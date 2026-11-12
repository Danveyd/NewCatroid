package org.catrobat.catroid.common

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Proxy
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

object ChEngineHttpManager {

    private val baseClient = OkHttpClient()

    private val cookieStore = HashMap<String, List<Cookie>>()
    private val cookieJar = object : CookieJar {
        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            cookieStore[url.host] = cookies
        }
        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            return cookieStore[url.host] ?: emptyList()
        }
    }

    class RequestState(val id: String, val method: String, var url: String) {
        val headers = mutableMapOf<String, String>()
        val queryParams = mutableMapOf<String, String>()
        var bodyText: String? = null
        var bodyContentType: String = "application/json"

        var uploadFile: File? = null
        var uploadFilePartName: String = "file"
        var uploadFileMimeType: String = "application/octet-stream"

        var connectTimeoutMs: Long = 10_000
        var readTimeoutMs: Long = 10_000
        var writeTimeoutMs: Long = 10_000
        var followRedirects: Boolean = true
        var trustAllCerts: Boolean = false
        var useCookies: Boolean = true

        var proxyHost: String? = null
        var proxyPort: Int? = null
    }

    class ResponseState(
        val code: Int,
        val headers: Map<String, String>,
        val bodyBytes: ByteArray?
    ) {
        val bodyString: String by lazy {
            bodyBytes?.decodeToString() ?: ""
        }
    }

    private val activeRequests = ConcurrentHashMap<String, RequestState>()
    private val completedResponses = ConcurrentHashMap<String, ResponseState>()

    fun createRequest(id: String, method: String, url: String) {
        activeRequests[id] = RequestState(id, method.uppercase(), url)
        completedResponses.remove(id)
    }

    fun setConfig(id: String, type: String, key: String, value: String) {
        val state = activeRequests[id] ?: return
        when (type.lowercase()) {
            "header" -> state.headers[key] = value
            "query" -> state.queryParams[key] = value
            "timeout_connect" -> state.connectTimeoutMs = value.toLongOrNull() ?: 10_000
            "timeout_read" -> state.readTimeoutMs = value.toLongOrNull() ?: 10_000
            "timeout_write" -> state.writeTimeoutMs = value.toLongOrNull() ?: 10_000
            "follow_redirects" -> state.followRedirects = value.toBoolean()
            "trust_all_certs" -> state.trustAllCerts = value.toBoolean()
            "use_cookies" -> state.useCookies = value.toBoolean()
            "proxy_host" -> state.proxyHost = value
            "proxy_port" -> state.proxyPort = value.toIntOrNull()
        }
    }

    private fun sanitizeUrl(urlStr: String): String {
        val withEscapedSpaces = urlStr.replace(" ", "%20")
        val sb = StringBuilder()
        for (char in withEscapedSpaces) {
            if (char.code > 127) {
                sb.append(java.net.URLEncoder.encode(char.toString(), "UTF-8"))
            } else {
                sb.append(char)
            }
        }
        return sb.toString()
    }


    fun setBodyText(id: String, body: String, contentType: String) {
        val state = activeRequests[id] ?: return
        state.bodyText = body
        state.bodyContentType = contentType
    }

    fun attachFile(id: String, file: File, partName: String, mimeType: String) {
        val state = activeRequests[id] ?: return
        state.uploadFile = file
        state.uploadFilePartName = partName
        state.uploadFileMimeType = if (mimeType.trim().isEmpty()) "application/octet-stream" else mimeType
    }

    fun executeRequest(id: String, onComplete: () -> Unit) {
        val state = activeRequests[id]
        if (state == null) {
            completedResponses[id] = ResponseState(
                code = -1,
                headers = emptyMap(),
                bodyBytes = "Request failed: Request ID '$id' was not created. Check for typos!".toByteArray()
            )
            onComplete()
            return
        }

        completedResponses.remove(id)

        if (!state.headers.containsKey("User-Agent")) {
            state.headers["User-Agent"] = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        }

        val clientBuilder = baseClient.newBuilder()
            .connectTimeout(state.connectTimeoutMs, TimeUnit.MILLISECONDS)
            .readTimeout(state.readTimeoutMs, TimeUnit.MILLISECONDS)
            .writeTimeout(state.writeTimeoutMs, TimeUnit.MILLISECONDS)
            .followRedirects(state.followRedirects)
            .followSslRedirects(state.followRedirects)

        if (state.useCookies) {
            clientBuilder.cookieJar(cookieJar)
        }

        if (state.trustAllCerts) {
            try {
                val trustAllCerts = arrayOf<X509TrustManager>(object : X509TrustManager {
                    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                    override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
                })
                val sslContext = SSLContext.getInstance("SSL")
                sslContext.init(null, trustAllCerts, SecureRandom())
                clientBuilder.sslSocketFactory(sslContext.socketFactory, trustAllCerts[0])
                clientBuilder.hostnameVerifier { _, _ -> true }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (!state.proxyHost.isNullOrEmpty() && state.proxyPort != null) {
            val proxyAddr = InetSocketAddress(state.proxyHost, state.proxyPort!!)
            clientBuilder.proxy(Proxy(Proxy.Type.HTTP, proxyAddr))
        }

        val client = clientBuilder.build()

        val sanitizedUrl = sanitizeUrl(state.url)
        val urlBuilder = sanitizedUrl.toHttpUrlOrNull()?.newBuilder()

        if (urlBuilder == null) {
            completedResponses[id] = ResponseState(
                code = -1,
                headers = emptyMap(),
                bodyBytes = "Request failed: Invalid URL '$sanitizedUrl'".toByteArray()
            )
            onComplete()
            return
        }

        state.queryParams.forEach { (k, v) -> urlBuilder.addQueryParameter(k, v) }
        val finalUrl = urlBuilder.build()

        var requestBody: RequestBody? = null
        if (state.uploadFile != null && state.uploadFile!!.exists()) {
            val mediaType = state.uploadFileMimeType.toMediaTypeOrNull()
            val fileBody = state.uploadFile!!.asRequestBody(mediaType)
            val multipartBuilder = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(state.uploadFilePartName, state.uploadFile!!.name, fileBody)

            state.bodyText?.let { multipartBuilder.addFormDataPart("payload", it) }
            requestBody = multipartBuilder.build()
        } else if (state.bodyText != null) {
            requestBody = state.bodyText!!.toRequestBody(state.bodyContentType.toMediaTypeOrNull())
        } else if (state.method != "GET" && state.method != "HEAD") {
            requestBody = "".toRequestBody(null)
        }

        val request = Request.Builder()
            .url(finalUrl)
            .method(state.method, requestBody)
            .apply {
                state.headers.forEach { (k, v) -> addHeader(k, v) }
            }
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val headersMap = mutableMapOf<String, String>()
                for (name in response.headers.names()) {
                    headersMap[name] = response.header(name) ?: ""
                }

                val bodyBytes = response.body?.bytes()

                completedResponses[id] = ResponseState(
                    code = response.code,
                    headers = headersMap,
                    bodyBytes = bodyBytes
                )
            }
        } catch (e: Throwable) {
            e.printStackTrace()
            completedResponses[id] = ResponseState(
                code = -1,
                headers = emptyMap(),
                bodyBytes = "Request failed: ${e.localizedMessage}".toByteArray()
            )
        } finally {
            onComplete()
        }
    }

    fun saveResponseToFile(id: String, destinationFile: File): Boolean {
        val response = completedResponses[id] ?: return false
        val bytes = response.bodyBytes ?: return false
        return try {
            FileOutputStream(destinationFile).use { fos ->
                fos.write(bytes)
            }
            true
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }
    }

    fun getResponseText(id: String): String {
        return completedResponses[id]?.bodyString ?: ""
    }

    fun getResponseCode(id: String): Int {
        return completedResponses[id]?.code ?: -1
    }

    fun getResponseHeader(id: String, headerName: String): String {
        return completedResponses[id]?.headers?.get(headerName) ?: ""
    }
}
