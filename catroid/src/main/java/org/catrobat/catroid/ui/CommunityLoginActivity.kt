package org.catrobat.catroid.ui

import android.os.Bundle
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.hcaptcha.sdk.HCaptcha
import com.hcaptcha.sdk.HCaptchaConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.catrobat.catroid.R
import org.catrobat.catroid.databinding.ActivityCommunityLoginBinding
import org.catrobat.catroid.utils.community.CommunityTokenManager
import org.json.JSONObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class CommunityLoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCommunityLoginBinding
    private var isRegisterMode = false
    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCommunityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.tvSwitchMode.setOnClickListener {
            val transition = AutoTransition().apply {
                duration = 250
            }
            TransitionManager.beginDelayedTransition(binding.root, transition)

            isRegisterMode = !isRegisterMode
            if (isRegisterMode) {
                binding.tilEmail.visibility = View.VISIBLE
                binding.tvTitle.text = getString(R.string.community_register_title)
                binding.btnAction.text = getString(R.string.community_register_btn)
                binding.tvSwitchMode.text = getString(R.string.community_have_account_hint)
            } else {
                binding.tilEmail.visibility = View.GONE
                binding.tvTitle.text = getString(R.string.community_login_title)
                binding.btnAction.text = getString(R.string.community_login_btn)
                binding.tvSwitchMode.text = getString(R.string.community_no_account_hint)
            }
        }

        binding.btnAction.setOnClickListener {
            startAuthFlow()
        }
    }

    private fun startAuthFlow() {
        val login = binding.etLogin.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()

        if (login.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, R.string.community_error_empty_fields, Toast.LENGTH_SHORT).show()
            return
        }

        if (isRegisterMode && email.isEmpty()) {
            Toast.makeText(this, R.string.community_error_empty_email, Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnAction.isEnabled = false

        lifecycleScope.launch {
            try {
                val siteKey = withContext(Dispatchers.IO) { fetchCaptchaSiteKey() }

                if (siteKey == null) {
                    showToastOnMain(getString(R.string.community_error_config_failed))
                    binding.btnAction.isEnabled = true
                    return@launch
                }

                verifyHCaptcha(siteKey) { captchaToken ->
                    if (isFinishing || isDestroyed) return@verifyHCaptcha

                    if (captchaToken != null) {
                        executeFinalAuthRequest(login, email, password, captchaToken)
                    } else {
                        showToastOnMain(getString(R.string.community_captcha_cancelled))
                        binding.btnAction.isEnabled = true
                    }
                }
            } catch (e: Exception) {
                showToastOnMain(getString(R.string.community_error_template, e.message ?: ""))
                binding.btnAction.isEnabled = true
            }
        }
    }

    private fun fetchCaptchaSiteKey(): String? {
        val request = Request.Builder().url("https://backend.mindpy.dev/config").get().build()
        return try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: return null
                    JSONObject(body).getString("hcaptcha_site_key")
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun verifyHCaptcha(siteKey: String, onResult: (String?) -> Unit) {
        val config = HCaptchaConfig.builder()
            .siteKey(siteKey)
            .build()

        HCaptcha.getClient(this).verifyWithHCaptcha(config)
            .addOnSuccessListener { response ->
                onResult(response.tokenResult)
            }
            .addOnFailureListener {
                onResult(null)
            }
    }

    private fun executeFinalAuthRequest(login: String, email: String, pass: String, captchaToken: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val url = if (isRegisterMode) "https://backend.mindpy.dev/auth/register"
            else "https://backend.mindpy.dev/auth/login"

            val jsonPayload = JSONObject().apply {
                if (isRegisterMode) {
                    put("username", login)
                    put("email", email)
                } else {
                    put("login", login)
                }
                put("password", pass)
                put("h-captcha-response", captchaToken)
                put("website", "")
            }

            val body = jsonPayload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder().url(url).post(body).build()

            try {
                client.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string()

                    if (response.isSuccessful && responseBody != null) {
                        val jsonResponse = JSONObject(responseBody)
                        val token = jsonResponse.getString("token")
                        val userObj = jsonResponse.getJSONObject("user")
                        val usernameResult = userObj.getString("username")

                        val userJson = userObj.toString()

                        CommunityTokenManager.saveSession(
                            this@CommunityLoginActivity,
                            token,
                            usernameResult,
                            userJson
                        )

                        val msg = if (isRegisterMode) getString(R.string.community_register_success)
                        else getString(R.string.community_login_success)
                        showToastOnMain(msg)

                        withContext(Dispatchers.Main) { finish() }
                    } else {
                        val errorMessage = parseError(responseBody)
                        showToastOnMain(errorMessage)
                        withContext(Dispatchers.Main) { binding.btnAction.isEnabled = true }
                    }
                }
            } catch (e: Exception) {
                showToastOnMain(getString(R.string.community_error_connection))
                withContext(Dispatchers.Main) { binding.btnAction.isEnabled = true }
            }
        }
    }

    private fun parseError(errorJson: String?): String {
        return try {
            if (errorJson.isNullOrEmpty()) getString(R.string.community_error_unknown)
            else JSONObject(errorJson).getString("detail")
        } catch (e: Exception) {
            getString(R.string.community_error_server)
        }
    }

    private fun showToastOnMain(message: String) {
        if (isFinishing || isDestroyed) return
        runOnUiThread {
            Toast.makeText(this@CommunityLoginActivity, message, Toast.LENGTH_LONG).show()
        }
    }
}
