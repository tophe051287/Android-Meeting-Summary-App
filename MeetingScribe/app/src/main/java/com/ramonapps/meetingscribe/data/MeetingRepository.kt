package com.ramonapps.meetingscribe.data

import android.content.Context
import com.ramonapps.meetingscribe.network.ClaudeSummaryClient
import com.ramonapps.meetingscribe.network.OpenAiTranscriptionClient
import com.ramonapps.meetingscribe.prefs.SecurePrefs
import kotlinx.coroutines.flow.Flow
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Coordinates: save a finished recording -> transcribe (OpenAI Whisper) ->
 * summarize (Anthropic Claude) -> persist the final structured recap.
 */
class MeetingRepository(context: Context) {

    private val dao = AppDatabase.getInstance(context).meetingDao()
    private val securePrefs = SecurePrefs(context)

    fun observeAll(): Flow<List<Meeting>> = dao.observeAll()

    fun observeById(id: Long): Flow<Meeting?> = dao.observeById(id)

    suspend fun deleteMeeting(meeting: Meeting) {
        runCatching { File(meeting.audioFilePath).delete() }
        dao.delete(meeting)
    }

    /**
     * Call this right after RecordingState reports a finished audio file.
     * Creates the DB row and kicks off transcription + summarization.
     * Safe to call from a coroutine scope that outlives the recording screen
     * (e.g. a ViewModel scope or an application-level scope), since the
     * network calls can take a minute or two.
     */
    suspend fun processFinishedRecording(audioFilePath: String, durationSeconds: Int) {
        val title = defaultTitleFor(audioFilePath)
        val meetingId = dao.insert(
            Meeting(
                title = title,
                createdAtEpochMs = System.currentTimeMillis(),
                durationSeconds = durationSeconds,
                audioFilePath = audioFilePath,
                status = MeetingStatus.TRANSCRIBING
            )
        )

        if (!securePrefs.hasRequiredKeys) {
            dao.update(
                dao.getById(meetingId)!!.copy(
                    status = MeetingStatus.ERROR,
                    errorMessage = "Add your OpenAI and Anthropic API keys in Settings first."
                )
            )
            return
        }

        val transcriptionClient = OpenAiTranscriptionClient(securePrefs.openAiApiKey)
        val transcriptResult = transcriptionClient.transcribe(File(audioFilePath))

        val transcript = transcriptResult.getOrElse { error ->
            dao.update(
                dao.getById(meetingId)!!.copy(
                    status = MeetingStatus.ERROR,
                    errorMessage = "Transcription failed: ${error.message}"
                )
            )
            return
        }

        dao.update(
            dao.getById(meetingId)!!.copy(
                status = MeetingStatus.SUMMARIZING,
                transcript = transcript
            )
        )

        if (transcript.isBlank()) {
            dao.update(
                dao.getById(meetingId)!!.copy(
                    status = MeetingStatus.ERROR,
                    errorMessage = "No speech was detected in the recording."
                )
            )
            return
        }

        val summaryClient = ClaudeSummaryClient(securePrefs.anthropicApiKey, securePrefs.anthropicModel)
        val summaryResult = summaryClient.summarize(transcript)

        val summary = summaryResult.getOrElse { error ->
            dao.update(
                dao.getById(meetingId)!!.copy(
                    status = MeetingStatus.ERROR,
                    errorMessage = "Summarization failed: ${error.message}"
                )
            )
            return
        }

        dao.update(
            dao.getById(meetingId)!!.copy(
                status = MeetingStatus.DONE,
                summary = summary.summary,
                keyPoints = summary.keyPoints,
                actionItems = summary.actionItems,
                decisions = summary.decisions
            )
        )
    }

    private fun defaultTitleFor(path: String): String {
        val fmt = SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.US)
        return "Meeting on ${fmt.format(System.currentTimeMillis())}"
    }
}
