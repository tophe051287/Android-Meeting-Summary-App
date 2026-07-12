package com.ramonapps.meetingscribe.ui.settings

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.ramonapps.meetingscribe.prefs.SecurePrefs

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = SecurePrefs(application)

    var openAiKey by mutableStateOf(prefs.openAiApiKey)
        private set
    var anthropicKey by mutableStateOf(prefs.anthropicApiKey)
        private set
    var anthropicModel by mutableStateOf(prefs.anthropicModel)
        private set

    fun updateOpenAiKey(value: String) {
        openAiKey = value
        prefs.openAiApiKey = value
    }

    fun updateAnthropicKey(value: String) {
        anthropicKey = value
        prefs.anthropicApiKey = value
    }

    fun updateAnthropicModel(value: String) {
        anthropicModel = value
        prefs.anthropicModel = value
    }
}
