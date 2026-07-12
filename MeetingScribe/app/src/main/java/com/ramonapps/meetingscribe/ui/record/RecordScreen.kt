package com.ramonapps.meetingscribe.ui.record

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordScreen(
    onDone: () -> Unit,
    viewModel: RecordViewModel = viewModel()
) {
    val isRecording by viewModel.isRecording.collectAsState()
    val elapsed by viewModel.elapsedSeconds.collectAsState()
    var isQueuing by remember { mutableStateOf(false) }
    var permissionsGranted by remember { mutableStateOf(false) }

    val permissions = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.POST_NOTIFICATIONS)
        } else {
            arrayOf(Manifest.permission.RECORD_AUDIO)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        permissionsGranted = results.values.all { it }
        if (permissionsGranted) viewModel.startRecording()
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Record Meeting") }) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Text(
                    text = formatElapsed(elapsed),
                    style = MaterialTheme.typography.displayMedium
                )
                Text(
                    text = when {
                        isQueuing -> "Saving and starting analysis…"
                        isRecording -> "Recording in progress"
                        else -> "Tap to start recording"
                    },
                    style = MaterialTheme.typography.bodyLarge
                )
                FloatingActionButton(
                    onClick = {
                        if (isQueuing) return@FloatingActionButton
                        if (isRecording) {
                            isQueuing = true
                            viewModel.stopRecording {
                                isQueuing = false
                                onDone()
                            }
                        } else {
                            permissionLauncher.launch(permissions)
                        }
                    },
                    modifier = Modifier.size(96.dp),
                    shape = CircleShape,
                    containerColor = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                ) {
                    Icon(
                        imageVector = if (isRecording) Icons.Filled.Stop else Icons.Filled.Mic,
                        contentDescription = if (isRecording) "Stop" else "Start",
                        modifier = Modifier.size(40.dp)
                    )
                }
                if (!isRecording && !isQueuing) {
                    Button(onClick = onDone) { Text("Back") }
                }
            }
        }
    }
}

private fun formatElapsed(seconds: Int): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, s)
    else String.format(Locale.US, "%02d:%02d", m, s)
}
