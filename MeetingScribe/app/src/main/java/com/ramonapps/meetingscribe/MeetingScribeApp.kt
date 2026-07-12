package com.ramonapps.meetingscribe

import android.app.Application
import com.ramonapps.meetingscribe.data.MeetingRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MeetingScribeApp : Application() {

    // Outlives any single screen so transcription/summarization keeps running
    // even if the user navigates away from the record screen.
    val appScope = CoroutineScope(SupervisorJob())

    lateinit var repository: MeetingRepository
        private set

    override fun onCreate() {
        super.onCreate()
        repository = MeetingRepository(this)
    }

    fun processRecording(audioFilePath: String, durationSeconds: Int) {
        appScope.launch {
            repository.processFinishedRecording(audioFilePath, durationSeconds)
        }
    }
}
