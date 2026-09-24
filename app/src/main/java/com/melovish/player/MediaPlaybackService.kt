package com.melovish.player

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.content.ContextCompat

class MediaPlaybackService : Service() {

    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): MediaPlaybackService = this@MediaPlaybackService
    }

    companion object {
        private const val NOTIFICATION_ID = 1001
        var instance: MediaPlaybackService? = null
            private set

        fun start(context: Context) {
            val intent = Intent(context, MediaPlaybackService::class.java)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    ContextCompat.startForegroundService(context, intent)
                } else {
                    context.startService(intent)
                }
            } catch (_: Exception) {
                try {
                    context.startService(intent)
                } catch (_: Exception) {}
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, MediaPlaybackService::class.java)
            try {
                context.stopService(intent)
            } catch (_: Exception) {}
        }

        fun attachNotification(notification: Notification) {
            val service = instance ?: return
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    service.startForeground(
                        NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                    )
                } else {
                    service.startForeground(NOTIFICATION_ID, notification)
                }
            } catch (_: Exception) {}
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        instance = this
        return START_STICKY
    }

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
    }
}
