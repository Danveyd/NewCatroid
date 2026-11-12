package org.catrobat.catroid.utils.community

import android.content.Context
import android.content.SharedPreferences

object CommunityTokenManager {
    private const val PREFS_NAME = "chengine_community_prefs"
    private const val KEY_TOKEN = "jwt_token"
    private const val KEY_USERNAME = "username"
    private const val KEY_USER_JSON = "user_json"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    @JvmStatic
    fun saveSession(context: Context, token: String, username: String, userJson: String) {
        getPrefs(context).edit()
            .putString(KEY_TOKEN, token)
            .putString(KEY_USERNAME, username)
            .putString(KEY_USER_JSON, userJson)
            .apply()
    }

    @JvmStatic
    fun getToken(context: Context): String? {
        return getPrefs(context).getString(KEY_TOKEN, null)
    }

    @JvmStatic
    fun getUsername(context: Context): String? {
        return getPrefs(context).getString(KEY_USERNAME, null)
    }

    @JvmStatic
    fun getUserJson(context: Context): String? {
        return getPrefs(context).getString(KEY_USER_JSON, null)
    }

    @JvmStatic
    fun clearSession(context: Context) {
        getPrefs(context).edit().clear().apply()
    }

    @JvmStatic
    fun isLoggedIn(context: Context): Boolean {
        return !getToken(context).isNullOrEmpty()
    }
}
