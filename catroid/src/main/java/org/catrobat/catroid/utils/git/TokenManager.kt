package org.catrobat.catroid.utils.git

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

object TokenManager {
    private const val PREF_FILE_NAME = "github_secure_prefs"
    private const val KEY_AUTH_TOKEN = "auth_token"

    private fun getEncryptedPrefs(context: Context) = EncryptedSharedPreferences.create(
        PREF_FILE_NAME,
        MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveToken(context: Context, token: String) {
        getEncryptedPrefs(context).edit().putString(KEY_AUTH_TOKEN, token).apply()
    }

    fun getToken(context: Context): String? {
        return getEncryptedPrefs(context).getString(KEY_AUTH_TOKEN, null)
    }

    fun clearToken(context: Context) {
        getEncryptedPrefs(context).edit().remove(KEY_AUTH_TOKEN).apply()
    }
}