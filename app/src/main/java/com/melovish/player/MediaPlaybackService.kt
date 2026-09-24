package com.melovish.player

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.content.ContextCompat

class MediaPlaybackService : Service() {

    companion object {
        private var instance: MediaPlaybackService? = null

        fun start(context: Context) {
            val intent = Intent(context, MediaPlaybackService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }

        fun attachNotification(notification: Notification) {
            instance?.startForeground(1001, notification)
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_NOT_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        // Stops playback and releases resources when swiped away from Recents
        MusicManager.activeInstance?.let { manager ->
            try {
                if (manager.player.isPlaying) {
                    manager.player.stop()
                }
                manager.isPlaying = false
                manager.currentSong = null
            } catch (_: Exception) {}
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
