package com.ramonapps.meetingscribe.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class MeetingStatus {
    RECORDING,
    TRANSCRIBING,
    SUMMARIZING,
    DONE,
    ERROR
}

/**
 * A single action item extracted from a meeting summary.
 */
data class ActionItem(
    val task: String,
    val owner: String
)

@Entity(tableName = "meetings")
data class Meeting(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val createdAtEpochMs: Long,
    val durationSeconds: Int = 0,
    val audioFilePath: String,
    val status: MeetingStatus = MeetingStatus.RECORDING,
    val transcript: String? = null,
    val summary: String? = null,
    val keyPoints: List<String> = emptyList(),
    val actionItems: List<ActionItem> = emptyList(),
    val decisions: List<String> = emptyList(),
    val errorMessage: String? = null
)
