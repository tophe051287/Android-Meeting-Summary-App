package com.ramonapps.meetingscribe.recording

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.MediaRecorder
import android.os.Build
import android.os.CountDownTimer
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.ramonapps.meetingscribe.MainActivity
import com.ramonapps.meetingscribe.R
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Foreground service that owns the MediaRecorder instance so recording keeps
 * running even if the user backgrounds the app or the screen turns off.
 */
class RecordingService : Service() {

    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var tickTimer: CountDownTimer? = null
    private var secondsElapsed = 0

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startRecording()
            ACTION_STOP -> stopRecording()
        }
        return START_NOT_STICKY
    }

    private fun startRecording() {
        val dir = File(filesDir, "recordings").apply { mkdirs() }
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())
        val file = File(dir, "meeting_$timestamp.m4a")
        outputFile = file

        startForeground(NOTIFICATION_ID, buildNotification(0))

        recorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(128000)
            setAudioSamplingRate(44100)
            setOutputFile(file.absolutePath)
            prepare()
            start()
        }

        secondsElapsed = 0
        RecordingState.onStarted()
        startTicker()
    }

    private fun startTicker() {
        tickTimer?.cancel()
        tickTimer = object : CountDownTimer(Long.MAX_VALUE, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                secondsElapsed += 1
                RecordingState.onTick(secondsElapsed)
                val nm = getSystemService(NotificationManager::class.java)
                nm?.notify(NOTIFICATION_ID, buildNotification(secondsElapsed))
            }
            override fun onFinish() { /* never reached */ }
        }.start()
    }

    private fun stopRecording() {
        tickTimer?.cancel()
        tickTimer = null

        val finishedPath = try {
            recorder?.apply {
                stop()
                release()
            }
            outputFile?.absolutePath
        } catch (t: Throwable) {
            // Recorder can throw if stopped almost immediately after start with no data.
            null
        } finally {
            recorder = null
        }

        RecordingState.onStopped(finishedPath)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun buildNotification(seconds: Int): Notification {
        ensureChannel()
        val minutes = seconds / 60
        val secs = seconds % 60
        val timeText = String.format(Locale.US, "%02d:%02d", minutes, secs)

        val openAppIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.recording_in_progress))
            .setContentText(timeText)
            .setSmallIcon(android.R.drawable.presence_audio_online)
            .setOngoing(true)
            .setContentIntent(openAppIntent)
            .build()
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                nm.createNotificationChannel(
                    NotificationChannel(
                        CHANNEL_ID,
                        getString(R.string.notification_channel_name),
                        NotificationManager.IMPORTANCE_LOW
                    )
                )
            }
        }
    }

    override fun onDestroy() {
        tickTimer?.cancel()
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "com.ramonapps.meetingscribe.action.START"
        const val ACTION_STOP = "com.ramonapps.meetingscribe.action.STOP"
        private const val CHANNEL_ID = "recording_channel"
        private const val NOTIFICATION_ID = 42
    }
}
