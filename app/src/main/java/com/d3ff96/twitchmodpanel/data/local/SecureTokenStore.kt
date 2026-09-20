package com.d3ff96.twitchmodpanel.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Stores Twitch OAuth tokens in EncryptedSharedPreferences when available.
 * Falls back to a clearly named private SharedPreferences with a TODO if crypto init fails.
 */
class SecureTokenStore(context: Context) {
    private val prefs: SharedPreferences = createPrefs(context.applicationContext)

    var accessToken: String?
        get() = prefs.getString(KEY_ACCESS, null)
        set(value) = prefs.edit().putString(KEY_ACCESS, value).apply()

    var refreshToken: String?
        get() = prefs.getString(KEY_REFRESH, null)
        set(value) = prefs.edit().putString(KEY_REFRESH, value).apply()

    fun clear() {
        prefs.edit().clear().apply()
    }

    fun saveTokens(access: String, refresh: String?) {
        prefs.edit()
            .putString(KEY_ACCESS, access)
            .putString(KEY_REFRESH, refresh)
            .apply()
    }

    companion object {
        private const val ENCRYPTED_FILE = "twitch_oauth_tokens_encrypted"
        private const val FALLBACK_FILE = "twitch_oauth_tokens_plaintext_TODO_secure"
        private const val KEY_ACCESS = "access_token"
        private const val KEY_REFRESH = "refresh_token"

        private fun createPrefs(context: Context): SharedPreferences {
            return try {
                val masterKey = MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()
                EncryptedSharedPreferences.create(
                    context,
                    ENCRYPTED_FILE,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
                )
            } catch (_: Exception) {
                // TODO: migrate to EncryptedSharedPreferences once MasterKey works on this device/API.
                context.getSharedPreferences(FALLBACK_FILE, Context.MODE_PRIVATE)
            }
        }
    }
}
