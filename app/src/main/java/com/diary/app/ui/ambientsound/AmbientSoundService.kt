package com.diary.app.ui.ambientsound

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.diary.app.MainActivity

class AmbientSoundService : Service() {
    private val player = AmbientSoundPlayer.getInstance()
    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        mediaSession = MediaSession(this, "AmbientSound").apply {
            setCallback(object : MediaSession.Callback() {
                override fun onPlay() { player.resumeAll(); updateNotification(); updatePlaybackState() }
                override fun onPause() { player.pauseAll(); updateNotification(); updatePlaybackState() }
                override fun onStop() {
                    player.stopAll()
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
        when (intent?.action) {
            ACTION_STOP_ALL -> {
                player.stopAll()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_PAUSE -> { player.pauseAll(); updateNotification(); updatePlaybackState(); return START_NOT_STICKY }
            ACTION_RESUME -> { player.resumeAll(); updateNotification(); updatePlaybackState(); return START_NOT_STICKY }
        }
        if (!player.isAnyPlaying) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        try {
            startForeground(NOTIFICATION_ID, buildNotification())
        } catch (_: Exception) { return START_NOT_STICKY }
        updatePlaybackState()
        return START_NOT_STICKY
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

        val activeText = player.getActiveTypes().joinToString("、") { it.displayName }
        val paused = player.isPaused

        val playPauseAction = if (paused)
            NotificationCompat.Action(android.R.drawable.ic_media_play, "播放",
                PendingIntent.getService(this, 2,
                    Intent(this, AmbientSoundService::class.java).setAction(ACTION_RESUME),
                    PendingIntent.FLAG_IMMUTABLE))
        else
            NotificationCompat.Action(android.R.drawable.ic_media_pause, "暂停",
                PendingIntent.getService(this, 2,
                    Intent(this, AmbientSoundService::class.java).setAction(ACTION_PAUSE),
                    PendingIntent.FLAG_IMMUTABLE))

        val stopAction = NotificationCompat.Action(android.R.drawable.ic_media_pause, "停止",
            PendingIntent.getService(this, 1,
                Intent(this, AmbientSoundService::class.java).setAction(ACTION_STOP_ALL),
                PendingIntent.FLAG_IMMUTABLE))

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("场景环境音")
            .setContentText(if (paused) "已暂停：$activeText" else "正在播放：$activeText")
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
        } catch (_: Exception) {}
    }

    private fun updatePlaybackState() {
        val state = when {
            player.isAnyPlaying && !player.isPaused -> PlaybackState.STATE_PLAYING
            player.isPaused -> PlaybackState.STATE_PAUSED
            else -> PlaybackState.STATE_NONE
        }
        try { mediaSession?.setPlaybackState(PlaybackState.Builder().setState(state, 0, 1f).build()) } catch (_: Exception) {}
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
