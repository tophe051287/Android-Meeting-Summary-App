package com.ramonapps.meetingscribe.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ramonapps.meetingscribe.data.Meeting
import com.ramonapps.meetingscribe.data.MeetingStatus
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onStartRecording: () -> Unit,
    onOpenMeeting: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val meetings by viewModel.meetings.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MeetingScribe") },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onStartRecording,
                icon = { Icon(Icons.Filled.Mic, contentDescription = null) },
                text = { Text("Record") },
                containerColor = MaterialTheme.colorScheme.primary,
                elevation = FloatingActionButtonDefaults.elevation()
            )
        }
    ) { padding ->
        if (meetings.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.Mic,
                        contentDescription = null,
                        modifier = Modifier.padding(bottom = 12.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text("No meetings yet", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Tap Record to capture and summarize your first meeting.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(meetings, key = { it.id }) { meeting ->
                    MeetingRow(
                        meeting = meeting,
                        onClick = { onOpenMeeting(meeting.id) },
                        onDelete = { viewModel.deleteMeeting(meeting) }
                    )
                }
            }
        }
    }
}

@Composable
private fun MeetingRow(meeting: Meeting, onClick: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(meeting.title, fontWeight = FontWeight.SemiBold)
                Text(
                    text = "${formatDate(meeting.createdAtEpochMs)} · ${formatDuration(meeting.durationSeconds)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                StatusLabel(meeting.status, meeting.errorMessage)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete")
            }
        }
    }
}

@Composable
private fun StatusLabel(status: MeetingStatus, errorMessage: String?) {
    val (text, color) = when (status) {
        MeetingStatus.RECORDING -> "Recording…" to MaterialTheme.colorScheme.primary
        MeetingStatus.TRANSCRIBING -> "Transcribing…" to MaterialTheme.colorScheme.primary
        MeetingStatus.SUMMARIZING -> "Summarizing…" to MaterialTheme.colorScheme.primary
        MeetingStatus.DONE -> "Ready" to MaterialTheme.colorScheme.tertiary
        MeetingStatus.ERROR -> (errorMessage ?: "Failed") to MaterialTheme.colorScheme.error
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (status == MeetingStatus.TRANSCRIBING || status == MeetingStatus.SUMMARIZING) {
            CircularProgressIndicator(modifier = Modifier.padding(end = 6.dp).size(12.dp), strokeWidth = 2.dp)
        }
        Text(text, style = MaterialTheme.typography.labelMedium, color = color)
    }
}

private fun formatDate(epochMs: Long): String =
    SimpleDateFormat("MMM d, h:mm a", Locale.US).format(epochMs)

private fun formatDuration(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return String.format(Locale.US, "%d:%02d", m, s)
}
