package com.ramonapps.meetingscribe.ui.detail

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ramonapps.meetingscribe.data.Meeting
import com.ramonapps.meetingscribe.data.MeetingStatus
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeetingDetailScreen(
    meetingId: Long,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val application = context.applicationContext as android.app.Application
    val viewModel: MeetingDetailViewModel = viewModel(
        factory = MeetingDetailViewModel.Factory(application, meetingId)
    )
    val meeting by viewModel.meeting.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(meeting?.title ?: "Meeting") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (meeting?.status == MeetingStatus.DONE) {
                        IconButton(onClick = { meeting?.let { shareAsText(context, it) } }) {
                            Icon(Icons.Filled.Share, contentDescription = "Share")
                        }
                        IconButton(onClick = { meeting?.let { exportAsFile(context, it) } }) {
                            Icon(Icons.Filled.FileDownload, contentDescription = "Export")
                        }
                    }
                }
            )
        }
    ) { padding ->
        val current = meeting
        if (current == null) {
            Text("Loading…", modifier = Modifier.padding(padding).padding(16.dp))
            return@Scaffold
        }

        when (current.status) {
            MeetingStatus.RECORDING, MeetingStatus.TRANSCRIBING, MeetingStatus.SUMMARIZING -> {
                Column(modifier = Modifier.padding(padding).padding(16.dp)) {
                    Text(
                        text = when (current.status) {
                            MeetingStatus.TRANSCRIBING -> "Transcribing your recording…"
                            MeetingStatus.SUMMARIZING -> "Analyzing the transcript…"
                            else -> "Recording…"
                        },
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
            MeetingStatus.ERROR -> {
                Column(modifier = Modifier.padding(padding).padding(16.dp)) {
                    Text("Something went wrong", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
                    Text(current.errorMessage ?: "Unknown error", modifier = Modifier.padding(top = 8.dp))
                }
            }
            MeetingStatus.DONE -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item { SectionCard(title = "Summary") { Text(current.summary ?: "") } }

                    if (current.keyPoints.isNotEmpty()) {
                        item {
                            SectionCard(title = "Key Points") {
                                current.keyPoints.forEach { point ->
                                    Text("•  $point", modifier = Modifier.padding(vertical = 2.dp))
                                }
                            }
                        }
                    }

                    if (current.actionItems.isNotEmpty()) {
                        item {
                            SectionCard(title = "Action Items") {
                                current.actionItems.forEach { item ->
                                    Row(modifier = Modifier.padding(vertical = 4.dp)) {
                                        Text("•  ", fontWeight = FontWeight.Bold)
                                        Column {
                                            Text(item.task)
                                            Text(
                                                "Owner: ${item.owner}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (current.decisions.isNotEmpty()) {
                        item {
                            SectionCard(title = "Decisions") {
                                current.decisions.forEach { d ->
                                    Text("•  $d", modifier = Modifier.padding(vertical = 2.dp))
                                }
                            }
                        }
                    }

                    item {
                        SectionCard(title = "Full Transcript") {
                            Text(current.transcript ?: "", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            content()
        }
    }
}

private fun buildShareText(meeting: Meeting): String {
    val fmt = SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.US)
    return buildString {
        appendLine(meeting.title)
        appendLine(fmt.format(meeting.createdAtEpochMs))
        appendLine()
        appendLine("SUMMARY")
        appendLine(meeting.summary ?: "")
        if (meeting.keyPoints.isNotEmpty()) {
            appendLine()
            appendLine("KEY POINTS")
            meeting.keyPoints.forEach { appendLine("- $it") }
        }
        if (meeting.actionItems.isNotEmpty()) {
            appendLine()
            appendLine("ACTION ITEMS")
            meeting.actionItems.forEach { appendLine("- ${it.task} (Owner: ${it.owner})") }
        }
        if (meeting.decisions.isNotEmpty()) {
            appendLine()
            appendLine("DECISIONS")
            meeting.decisions.forEach { appendLine("- $it") }
        }
        appendLine()
        appendLine("FULL TRANSCRIPT")
        appendLine(meeting.transcript ?: "")
    }
}

private fun shareAsText(context: Context, meeting: Meeting) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, meeting.title)
        putExtra(Intent.EXTRA_TEXT, buildShareText(meeting))
    }
    context.startActivity(Intent.createChooser(intent, "Share meeting recap"))
}

private fun exportAsFile(context: Context, meeting: Meeting) {
    val dir = File(context.cacheDir, "exports").apply { mkdirs() }
    val safeName = meeting.title.replace(Regex("[^A-Za-z0-9]+"), "_").take(40)
    val file = File(dir, "${safeName}_recap.txt")
    file.writeText(buildShareText(meeting))

    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Export meeting recap"))
}
