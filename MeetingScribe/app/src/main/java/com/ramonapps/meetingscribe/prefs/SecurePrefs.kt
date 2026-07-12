package com.ramonapps.meetingscribe.prefs

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Encrypted, on-device storage for the user's own API keys. Keys never leave
 * the device except as Authorization headers on direct calls to OpenAI/Anthropic.
 */
class SecurePrefs(context: Context) {

    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            "meetingscribe_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    var openAiApiKey: String
        get() = prefs.getString(KEY_OPENAI, "") ?: ""
        set(value) = prefs.edit().putString(KEY_OPENAI, value).apply()

    var anthropicApiKey: String
        get() = prefs.getString(KEY_ANTHROPIC, "") ?: ""
        set(value) = prefs.edit().putString(KEY_ANTHROPIC, value).apply()

    var anthropicModel: String
        get() = prefs.getString(KEY_MODEL, DEFAULT_MODEL) ?: DEFAULT_MODEL
        set(value) = prefs.edit().putString(KEY_MODEL, value).apply()

    val hasRequiredKeys: Boolean
        get() = openAiApiKey.isNotBlank() && anthropicApiKey.isNotBlank()

    companion object {
        private const val KEY_OPENAI = "openai_api_key"
        private const val KEY_ANTHROPIC = "anthropic_api_key"
        private const val KEY_MODEL = "anthropic_model"
        const val DEFAULT_MODEL = "claude-3-5-sonnet-20241022"
    }
}
