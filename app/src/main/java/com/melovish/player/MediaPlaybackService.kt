package com.melovish.player

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

@UnstableApi
class MediaPlaybackService : MediaSessionService() {

    companion object {
        const val NOTIF_CHANNEL_ID = "melovish_playback_channel"
        const val NOTIF_ID = 1001

        /**
         * Safe startup hook handling API-level branching and Android 12+ foreground start rules.
         */
        fun start(context: Context) {
            val intent = Intent(context, MediaPlaybackService::class.java)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                // Intercepts ForegroundServiceStartNotAllowedException on Android 12+
                e.printStackTrace()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startInitialForeground()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIF_CHANNEL_ID,
                "Melovish Playback Controls",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Active playback controls and media notification pipeline"
                setShowBadge(false)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }

    /**
     * Phase 3 Invariant: Immediate Foreground Promotion.
     * Prevents Android 14+ ForegroundServiceDidNotStartInTimeException ANRs/crashes
     * by calling startForeground within the OS-mandated window during service initialization.
     */
    private fun startInitialForeground() {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, NOTIF_CHANNEL_ID)
            .setContentTitle("Melovish")
            .setContentText("Audio service ready")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(pendingIntent)
            .setOngoing(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIF_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                )
            } else {
                startForeground(NOTIF_ID, notification)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Bridges Android media routing, Lockscreen UI, Wear OS, and Bluetooth accessories
     * directly to the active MusicManager's ExoPlayer session.
     */
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return MusicManager.activeInstance?.mediaSession
    }

    /**
     * Phase 3 Invariant: Task-Kill Resilience.
     * When the user swipes Melovish from Recent Apps, verify playback state.
     * If playing or preparing, retain foreground execution to prevent playback interruption.
     */
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        val player = MusicManager.activeInstance?.player
        if (player == null || 
            !player.playWhenReady || 
            player.playbackState == Player.STATE_ENDED || 
            player.playbackState == Player.STATE_IDLE
        ) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
