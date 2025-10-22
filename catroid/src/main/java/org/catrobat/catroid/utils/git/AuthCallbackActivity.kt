package org.catrobat.catroid.utils.git

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class AuthCallbackActivity : AppCompatActivity() {

    private val GITHUB_CLIENT_ID = "Ov23liKoq3h0cTgAbVYA"
    private val GITHUB_CLIENT_SECRET = "936da4332f8a31ebed1bc97aa5d2f89a989a56d2"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val uri = intent.data
        if (uri != null && uri.toString().startsWith("newcatroid://github-callback")) {
            val code = uri.getQueryParameter("code")
            if (code != null) {
                lifecycleScope.launch {
                    val accessToken = exchangeCodeForToken(code)
                    if (accessToken != null) {
                        TokenManager.saveToken(this@AuthCallbackActivity, accessToken)
                    } else {
                        Log.e("AuthCallback", "Failed to get access token")
                    }
                    finish()
                }
            } else {
                finish()
            }
        } else {
            finish()
        }
    }

    private suspend fun exchangeCodeForToken(code: String): String? = withContext(Dispatchers.IO) {
        val client = OkHttpClient()
        val requestBody = FormBody.Builder()
            .add("client_id", GITHUB_CLIENT_ID)
            .add("client_secret", GITHUB_CLIENT_SECRET)
            .add("code", code)
            .build()

        val request = Request.Builder()
            .url("https://github.com/login/oauth/access_token")
            .header("Accept", "application/json")
            .post(requestBody)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val jsonResponse = response.body?.string()
                return@withContext jsonResponse?.let { JSONObject(it).optString("access_token", null) }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }
}