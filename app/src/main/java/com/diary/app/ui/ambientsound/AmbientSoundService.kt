package com.diary.app.ui.ambientsound

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.diary.app.MainActivity

class AmbientSoundService : Service() {
    private val player = AmbientSoundPlayer.getInstance()
    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        mediaSession = MediaSession(this, "AmbientSound").apply {
            setCallback(object : MediaSession.Callback() {
                override fun onPlay() { player.resume(); updateNotification(); updatePlaybackState() }
                override fun onPause() { player.pause(); updateNotification(); updatePlaybackState() }
                override fun onStop() {
                    player.stop()
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            })
            isActive = true
        }
        updatePlaybackState()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            if (player.hasSession) {
                try { startForeground(NOTIFICATION_ID, buildNotification()) } catch (e: Exception) { return START_NOT_STICKY }
                updatePlaybackState()
                return START_STICKY
            }
            return START_NOT_STICKY
        }
        when (intent.action) {
            ACTION_STOP_ALL -> {
                player.stop()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_PAUSE -> {
                player.pause()
                updateNotification()
                updatePlaybackState()
                return START_NOT_STICKY
            }
            ACTION_RESUME -> {
                player.resume()
                updateNotification()
                updatePlaybackState()
                return START_NOT_STICKY
            }
        }
        if (!player.hasSession) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        try {
            startForeground(NOTIFICATION_ID, buildNotification())
        } catch (e: Exception) { Log.w("AmbientSoundSvc", "startForeground failed", e); return START_NOT_STICKY }
        updatePlaybackState()
        return START_STICKY
    }

    override fun onDestroy() {
        mediaSession?.isActive = false
        mediaSession?.release()
        mediaSession = null
        super.onDestroy()
    }

    private fun buildNotification(): Notification {
        val openIntent = PendingIntent.getActivity(this, 0,
            Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val trackName = player.currentTrack?.name ?: ""
        val paused = player.isPausedState

        val playPauseAction = if (paused)
            NotificationCompat.Action(android.R.drawable.ic_media_play, "\u64AD\u653E",
                PendingIntent.getService(this, 2,
                    Intent(this, AmbientSoundService::class.java).setAction(ACTION_RESUME),
                    PendingIntent.FLAG_IMMUTABLE))
        else
            NotificationCompat.Action(android.R.drawable.ic_media_pause, "\u6682\u505C",
                PendingIntent.getService(this, 2,
                    Intent(this, AmbientSoundService::class.java).setAction(ACTION_PAUSE),
                    PendingIntent.FLAG_IMMUTABLE))

        val stopAction = NotificationCompat.Action(android.R.drawable.ic_menu_close_clear_cancel, "\u505C\u6B62",
            PendingIntent.getService(this, 1,
                Intent(this, AmbientSoundService::class.java).setAction(ACTION_STOP_ALL),
                PendingIntent.FLAG_IMMUTABLE))

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("\u573A\u666F\u73AF\u5883\u97F3")
            .setContentText(if (paused) "\u5DF2\u6682\u505C\uFF1A$trackName" else "\u6B63\u5728\u64AD\u653E\uFF1A$trackName")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(openIntent)
            .addAction(playPauseAction)
            .addAction(stopAction)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun updateNotification() {
        try {
            val nm = getSystemService(android.app.NotificationManager::class.java)
            nm.notify(NOTIFICATION_ID, buildNotification())
        } catch (e: Exception) { Log.w("AmbientSoundSvc", "updateNotification failed", e) }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "\u573A\u666F\u73AF\u5883\u97F3", NotificationManager.IMPORTANCE_LOW).apply {
                description = "\u73AF\u5883\u97F3\u64AD\u653E\u63A7\u5236"
                setShowBadge(false)
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    private fun updatePlaybackState() {
        val state = when {
            player.isPlaying -> PlaybackState.STATE_PLAYING
            player.isPausedState -> PlaybackState.STATE_PAUSED
            else -> PlaybackState.STATE_NONE
        }
        try { mediaSession?.setPlaybackState(PlaybackState.Builder().setState(state, 0, 1f).build()) } catch (e: Exception) { Log.w("AmbientSoundSvc", "updatePlaybackState failed", e) }
    }

    companion object {
        const val CHANNEL_ID = "ambient_sound"
        const val NOTIFICATION_ID = 1001
        const val ACTION_STOP_ALL = "com.diary.app.ambientsound.STOP_ALL"
        const val ACTION_PAUSE = "com.diary.app.ambientsound.PAUSE"
        const val ACTION_RESUME = "com.diary.app.ambientsound.RESUME"

        fun start(ctx: Context) { ctx.startForegroundService(Intent(ctx, AmbientSoundService::class.java)) }
        fun stop(ctx: Context) { ctx.startService(Intent(ctx, AmbientSoundService::class.java).setAction(ACTION_STOP_ALL)) }
    }
}
