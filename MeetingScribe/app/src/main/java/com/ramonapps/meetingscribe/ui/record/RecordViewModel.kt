package com.ramonapps.meetingscribe.ui.record

import android.app.Application
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ramonapps.meetingscribe.MeetingScribeApp
import com.ramonapps.meetingscribe.recording.RecordingService
import com.ramonapps.meetingscribe.recording.RecordingState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RecordViewModel(application: Application) : AndroidViewModel(application) {

    val isRecording: StateFlow<Boolean> = RecordingState.isRecording
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val elapsedSeconds: StateFlow<Int> = RecordingState.elapsedSeconds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun startRecording() {
        val intent = Intent(getApplication(), RecordingService::class.java).apply {
            action = RecordingService.ACTION_START
        }
        ContextCompat.startForegroundService(getApplication(), intent)
    }

    /** Stops the recording and kicks off transcription + summarization in the background. */
    fun stopRecording(onQueued: () -> Unit) {
        val app = getApplication<Application>()
        val startedElapsed = elapsedSeconds.value

        val intent = Intent(app, RecordingService::class.java).apply {
            action = RecordingService.ACTION_STOP
        }
        app.startService(intent)

        // The service updates RecordingState.lastFinishedFilePath synchronously
        // in stopRecording(), but that call is dispatched on the service's own
        // process turn, so poll briefly on the main thread via a callback flow
        // consumer instead of assuming it's already set here.
        (app as MeetingScribeApp).appScope.launch {
            var path = RecordingState.lastFinishedFilePath.value
            var attempts = 0
            while (path == null && attempts < 20) {
                delay(100)
                path = RecordingState.lastFinishedFilePath.value
                attempts++
            }
            if (path != null) {
                app.processRecording(path, startedElapsed)
            }
            onQueued()
        }
    }
}
