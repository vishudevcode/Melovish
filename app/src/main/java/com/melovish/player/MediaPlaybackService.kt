package com.melovish.player

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.MediaStyleNotificationHelper

@UnstableApi
class MediaPlaybackService : MediaSessionService() {

    companion object {
        const val NOTIF_CHANNEL_ID = "melovish_playback_channel_v2"
        const val NOTIF_ID = 1001
        const val ACTION_PLAY = "com.melovish.player.ACTION_PLAY"
        const val ACTION_PAUSE = "com.melovish.player.ACTION_PAUSE"
        const val ACTION_NEXT = "com.melovish.player.ACTION_NEXT"
        const val ACTION_PREV = "com.melovish.player.ACTION_PREV"

        fun start(context: Context) {
            try {
                val intent = Intent(context, MediaPlaybackService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private var musicManager: MusicManager? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        if (musicManager == null) {
            musicManager = MusicManager.activeInstance ?: MusicManager(applicationContext)
        }
        return musicManager?.mediaSession
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val manager = musicManager ?: MusicManager.activeInstance ?: MusicManager(applicationContext).also {
            musicManager = it
        }

        when (intent?.action) {
            ACTION_PLAY -> manager.togglePlayPause()
            ACTION_PAUSE -> manager.togglePlayPause()
            ACTION_NEXT -> manager.playNext()
            ACTION_PREV -> manager.playPrevious()
        }

        startForegroundNotification(manager)
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val manager = musicManager ?: MusicManager.activeInstance
        if (manager != null) {
            if (manager.isAlwaysPlay) {
                // Feature "Always play": retain background playback and foreground notification even when swiped from recents
                val restartIntent = Intent(applicationContext, MediaPlaybackService::class.java)
                val restartPendingIntent = PendingIntent.getService(
                    applicationContext, 11, restartIntent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_ONE_SHOT
                )
                val alarmManager = getSystemService(Context.ALARM_SERVICE) as? android.app.AlarmManager
                alarmManager?.set(
                    android.app.AlarmManager.RTC_WAKEUP,
                    System.currentTimeMillis() + 1000,
                    restartPendingIntent
                )
                startForegroundNotification(manager)
                return
            } else {
                // Normal Behavior: Stop music, dismantle notification and cease playback
                manager.player.pause()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        super.onTaskRemoved(rootIntent)
    }

    fun startForegroundNotification(manager: MusicManager) {
        val song = manager.currentSong ?: return
        val session = manager.mediaSession ?: return
        val art = manager.getCachedAlbumArt(song.id)

        val launchIntent = Intent(this, MainActivity::class.java).apply {
            this.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this, 0, launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val prevIntent = PendingIntent.getService(
            this, 1, Intent(this, MediaPlaybackService::class.java).apply { action = ACTION_PREV },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val playPauseIntent = PendingIntent.getService(
            this, 2, Intent(this, MediaPlaybackService::class.java).apply {
                action = if (manager.isPlaying) ACTION_PAUSE else ACTION_PLAY
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val nextIntent = PendingIntent.getService(
            this, 3, Intent(this, MediaPlaybackService::class.java).apply { action = ACTION_NEXT },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val playPauseIcon = if (manager.isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
        val playPauseTitle = if (manager.isPlaying) "Pause" else "Play"

        val mediaStyle = MediaStyleNotificationHelper.MediaStyle(session)
            .setShowActionsInCompactView(0, 1, 2)

        val builder = NotificationCompat.Builder(this, NOTIF_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(song.title)
            .setContentText(if (song.artist.isNotBlank()) song.artist else "Unknown Artist")
            .setSubText(song.album)
            .setContentIntent(contentPendingIntent)
            .setStyle(mediaStyle)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(manager.isPlaying)
            .addAction(android.R.drawable.ic_media_previous, "Previous", prevIntent)
            .addAction(playPauseIcon, playPauseTitle, playPauseIntent)
            .addAction(android.R.drawable.ic_media_next, "Next", nextIntent)

        if (art != null) {
            builder.setLargeIcon(art)
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIF_ID, builder.build(), ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
            } else {
                startForeground(NOTIF_ID, builder.build())
            }
        } catch (e: Exception) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(NOTIF_ID, builder.build())
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIF_CHANNEL_ID,
                "Playback Controls",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Music playback controls and lockscreen carousel"
                setShowBadge(false)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopForeground(STOP_FOREGROUND_REMOVE)
    }
}
