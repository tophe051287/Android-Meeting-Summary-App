package com.ramonapps.meetingscribe.recording

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Process-wide observable state so Compose screens can react to what the
 * foreground RecordingService is doing without a bound-service round trip.
 */
object RecordingState {
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _elapsedSeconds = MutableStateFlow(0)
    val elapsedSeconds: StateFlow<Int> = _elapsedSeconds.asStateFlow()

    private val _lastFinishedFilePath = MutableStateFlow<String?>(null)
    val lastFinishedFilePath: StateFlow<String?> = _lastFinishedFilePath.asStateFlow()

    fun onStarted() {
        _isRecording.value = true
        _elapsedSeconds.value = 0
        _lastFinishedFilePath.value = null
    }

    fun onTick(seconds: Int) {
        _elapsedSeconds.value = seconds
    }

    fun onStopped(filePath: String?) {
        _isRecording.value = false
        _lastFinishedFilePath.value = filePath
    }
}
