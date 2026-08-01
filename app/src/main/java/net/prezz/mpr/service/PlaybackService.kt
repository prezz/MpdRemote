package net.prezz.mpr.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.drawable.Icon
import android.os.IBinder
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import net.prezz.mpr.R
import net.prezz.mpr.model.PlayerState
import net.prezz.mpr.model.PlayerStatus
import net.prezz.mpr.model.PlaylistEntity
import net.prezz.mpr.model.ResponseReceiver
import net.prezz.mpr.model.ResponseResult
import net.prezz.mpr.model.StatusListener
import net.prezz.mpr.model.TaskHandle
import net.prezz.mpr.model.command.Command
import net.prezz.mpr.model.command.NextCommand
import net.prezz.mpr.model.command.PlayPauseCommand
import net.prezz.mpr.model.command.PreviousCommand
import net.prezz.mpr.model.command.VolumeDownCommand
import net.prezz.mpr.model.command.VolumeUpCommand
import net.prezz.mpr.model.external.CoverReceiver
import net.prezz.mpr.model.external.ExternalInformationService
import net.prezz.mpr.mpd.MpdPlayer
import net.prezz.mpr.ui.ApplicationActivator
import net.prezz.mpr.ui.helpers.VolumeButtonsHelper
import net.prezz.mpr.ui.mpd.MpdPlayerSettings
import net.prezz.mpr.ui.player.PlayerActivity

/**
 * Foreground media-playback notification tied to the MPD server's playback state: it mirrors the
 * currently-playing track (title/artist/cover/volume) and exposes transport controls that are sent
 * back to MPD. Faithful 1:1 port of V1's behaviour.
 *
 * PORTING NOTES / small idiomatic improvements (documented per request):
 * - The internal notification-action broadcasts (CMD_*) are now registered with
 *   `RECEIVER_NOT_EXPORTED` (via `ContextCompat.registerReceiver`) and the command Intents are scoped
 *   to this app's package. V1 used `RECEIVER_EXPORTED`, which let any other app trigger volume/play/
 *   next on our receiver — an unnecessary, minor security hole. `SCREEN_ON`/`SCREEN_OFF` are still
 *   delivered because they are system broadcasts, which reach non-exported context receivers.
 * - Kept faithful: `startService` (not `startForegroundService`) is intentional — this service only
 *   calls `startForeground` while actually playing, so it must NOT be bound by the
 *   "must call startForeground within 5s" contract of `startForegroundService`.
 * - FUTURE MODERNIZATION (not done here to avoid a risky rewrite): this uses the framework
 *   `Notification.MediaStyle` without a `MediaSession` token. The idiomatic target on modern Android
 *   is a `androidx.media3` `MediaSession` + `MediaStyleNotificationHelper`, which also drives the
 *   notification-action buttons instead of the custom broadcast receiver below.
 */
class PlaybackService : Service() {

    private var isForegroundService = false
    private var player: MpdPlayer? = null
    private var playerStatus: PlayerStatus? = null
    private var playlistEntity: PlaylistEntity? = null
    private var cover: Bitmap? = null
    private var playerInfoRefreshListener: PlayerInfoRefreshListener? = null
    private var updatePlaylistHandle: TaskHandle = TaskHandle.NULL_HANDLE
    private var updateCoverHandle: TaskHandle = TaskHandle.NULL_HANDLE
    private var broadcastReceiver: ControlBroadcastReceiver? = null
    private var coverSize = 0

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()

        updatePlaylistHandle = TaskHandle.NULL_HANDLE
        updateCoverHandle = TaskHandle.NULL_HANDLE

        coverSize = (resources.displayMetrics.density * 64).toInt()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        started = true // if the service gets killed and restarted we need to set it to true

        isForegroundService = false
        playlistEntity = null

        val settings = MpdPlayerSettings.create(this)

        broadcastReceiver?.let { unregisterReceiver(it) }

        broadcastReceiver = ControlBroadcastReceiver()
        val filter = IntentFilter()
        filter.addAction(CMD_VOL_DOWN)
        filter.addAction(CMD_VOL_UP)
        filter.addAction(CMD_PREV)
        filter.addAction(CMD_PLAY_PAUSE)
        filter.addAction(CMD_NEXT)
        filter.addAction(Intent.ACTION_SCREEN_ON)
        filter.addAction(Intent.ACTION_SCREEN_OFF)
        // NOT_EXPORTED: only this app (and the system, for SCREEN_ON/OFF) may deliver these.
        ContextCompat.registerReceiver(this, broadcastReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)

        player?.let {
            updateCoverHandle.cancelTask()
            updatePlaylistHandle.cancelTask()
            it.dispose()
        }

        updateNotification(isForegroundService)

        val newPlayer = MpdPlayer(settings)
        player = newPlayer
        playerStatus = PlayerStatus(false)

        playerInfoRefreshListener = PlayerInfoRefreshListener()
        newPlayer.setStatusListener(playerInfoRefreshListener)

        return START_STICKY
    }

    override fun onDestroy() {
        updateCoverHandle.cancelTask()
        updatePlaylistHandle.cancelTask()
        player?.dispose()

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)

        broadcastReceiver?.let { unregisterReceiver(it) }

        started = false

        super.onDestroy()
    }

    private fun updateNotification(sticky: Boolean) {
        val artistText: String
        val titleText: String
        val entity = playlistEntity
        if (entity != null) {
            val artist = entity.getArtist()
            artistText = artist ?: (entity.getName() ?: "")

            val track = entity.getTrack()
            val title = entity.getTitle()
            titleText = if (track != null && title != null) {
                "$track - $title"
            } else if (title != null) {
                title
            } else {
                entity.getUriEntity().getUriFilname()
            }
        } else {
            artistText = ""
            titleText = getString(R.string.player_playing_info_none)
        }

        val volumeText: String
        val volume = playerStatus?.volume ?: -1
        volumeText = if (volume != -1) {
            getString(R.string.general_volume_text_format, volume)
        } else {
            getString(R.string.general_volume_text_no_mixer)
        }

        val playerState = playerStatus?.state ?: PlayerState.STOP

        updateMediaNotification(artistText, titleText, volumeText, playerState, sticky)
    }

    private fun updateMediaNotification(artist: String, title: String, volume: String, playerState: PlayerState, sticky: Boolean) {
        // Internal command intents are scoped to this app's package (see NOT_EXPORTED note above).
        val volDownIntent = Intent(CMD_VOL_DOWN).setPackage(packageName)
        val volUpIntent = Intent(CMD_VOL_UP).setPackage(packageName)
        val prevIntent = Intent(CMD_PREV).setPackage(packageName)
        val playPauseIntent = Intent(CMD_PLAY_PAUSE).setPackage(packageName)
        val nextIntent = Intent(CMD_NEXT).setPackage(packageName)
        val launchIntent = Intent(this, PlayerActivity::class.java)

        val icPlay = when (playerState) {
            PlayerState.PLAY -> R.drawable.ic_pause_w
            PlayerState.PAUSE -> R.drawable.ic_paused_w
            else -> R.drawable.ic_play_w
        }

        val channelId = getString(R.string.notification_media_player_channel_id)

        val notification = Notification.Builder(this, channelId)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setSmallIcon(R.drawable.ic_notification)
            .setShowWhen(false)
            // Alert (sound/heads-up) only on the first post, not on every playlist/volume update; without
            // this an IMPORTANCE_DEFAULT channel would re-alert on each frequent notification refresh.
            .setOnlyAlertOnce(true)
            .addAction(Notification.Action.Builder(Icon.createWithResource(this, R.drawable.ic_volume_down_w), "", PendingIntent.getBroadcast(this, 0, volDownIntent, PendingIntent.FLAG_IMMUTABLE)).build()) // #0
            .addAction(Notification.Action.Builder(Icon.createWithResource(this, R.drawable.ic_volume_up_w), "", PendingIntent.getBroadcast(this, 0, volUpIntent, PendingIntent.FLAG_IMMUTABLE)).build()) // #1
            .addAction(Notification.Action.Builder(Icon.createWithResource(this, R.drawable.ic_previous_w), "", PendingIntent.getBroadcast(this, 0, prevIntent, PendingIntent.FLAG_IMMUTABLE)).build()) // #2
            .addAction(Notification.Action.Builder(Icon.createWithResource(this, icPlay), "", PendingIntent.getBroadcast(this, 0, playPauseIntent, PendingIntent.FLAG_IMMUTABLE)).build()) // #3
            .addAction(Notification.Action.Builder(Icon.createWithResource(this, R.drawable.ic_next_w), "", PendingIntent.getBroadcast(this, 0, nextIntent, PendingIntent.FLAG_IMMUTABLE)).build()) // #4
            .setStyle(Notification.MediaStyle().setShowActionsInCompactView(3)) // #3 play toggle button
            .setContentTitle(title)
            .setContentText(artist)
            .setSubText(volume)
            .setLargeIcon(cover)
            .setContentIntent(PendingIntent.getActivity(this, 0, launchIntent, PendingIntent.FLAG_IMMUTABLE))
            .setAutoCancel(false)
            .setOngoing(sticky)
            .build()

        createMediaNotificationChannel(this)

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (sticky) {
            startForeground(NOTIFICATION_ID, notification)
            isForegroundService = true
        } else {
            if (isForegroundService) {
                stopForeground(STOP_FOREGROUND_DETACH)
            }

            notificationManager.notify(NOTIFICATION_ID, notification)
            isForegroundService = false
        }
    }

    private inner class PlayerInfoRefreshListener : ResponseReceiver<PlaylistEntity?>(), StatusListener {

        override fun statusUpdated(status: PlayerStatus) {
            if (!status.connected || status.state == PlayerState.STOP) {
                stop()
            } else {
                val previous = playerStatus
                val updateState = previous == null || previous.state != status.state || previous.volume != status.volume
                val updatePlaying = playlistEntity == null || previous == null || previous.playlistVersion != status.playlistVersion || previous.currentSong != status.currentSong
                playerStatus = status

                if (updateState) {
                    // Keep the notification foreground/ongoing while paused too (not just while
                    // playing), so it isn't freely swipeable by accident. A STOP/disconnect is
                    // handled above and removes the notification via stop().
                    updateNotification(status.state != PlayerState.STOP)
                }

                if (updatePlaying) {
                    val position = maxOf(0, status.currentSong)
                    updatePlaylistHandle.cancelTask()
                    updatePlaylistHandle = player!!.getPlaylistEntity(position, this)
                }
            }
        }

        override fun receiveResponse(response: PlaylistEntity?) {
            val updateCover = if (response != null && playlistEntity == null) {
                true
            } else if (response == null && playlistEntity != null) {
                true
            } else if (response != null && playlistEntity != null) {
                response.getArtist() != playlistEntity!!.getArtist() || response.getAlbum() != playlistEntity!!.getAlbum()
            } else {
                false
            }

            playlistEntity = response

            if (updateCover) {
                cover = null
            }

            updateNotification(isForegroundService)

            if (updateCover && response != null) {
                updateCoverHandle.cancelTask()
                updateCoverHandle = ExternalInformationService.getCover(response.getArtist(), response.getAlbum(), coverSize, CoverReceiver { bitmap ->
                    cover = bitmap
                    updateNotification(isForegroundService)
                })
            }
        }
    }

    private inner class ControlBroadcastReceiver : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                CMD_VOL_DOWN -> sendControlCommand(VolumeDownCommand(VolumeButtonsHelper.getVolumeAmount(context)))
                CMD_VOL_UP -> sendControlCommand(VolumeUpCommand(VolumeButtonsHelper.getVolumeAmount(context)))
                CMD_PREV -> sendControlCommand(PreviousCommand())
                CMD_PLAY_PAUSE -> sendControlCommand(PlayPauseCommand())
                CMD_NEXT -> sendControlCommand(NextCommand())
                Intent.ACTION_SCREEN_ON -> player?.setStatusListener(playerInfoRefreshListener)
                Intent.ACTION_SCREEN_OFF -> player?.setStatusListener(null)
            }
        }

        private fun sendControlCommand(command: Command) {
            player?.sendControlCommands(listOf(command), object : ResponseReceiver<ResponseResult>() {
                override fun receiveResponse(response: ResponseResult) {
                }
            })
        }
    }

    companion object {
        private const val CMD_VOL_DOWN = "net.prezz.mpr.service.PlaybackService.CMD_VOL_DOWN"
        private const val CMD_VOL_UP = "net.prezz.mpr.service.PlaybackService.CMD_VOL_UP"
        private const val CMD_PREV = "net.prezz.mpr.service.PlaybackService.CMD_PREV"
        private const val CMD_PLAY_PAUSE = "net.prezz.mpr.service.PlaybackService.CMD_PLAY_PAUSE"
        private const val CMD_NEXT = "net.prezz.mpr.service.PlaybackService.CMD_NEXT"

        private const val NOTIFICATION_ID = 64545

        private val lock = Any()
        private var started = false

        @JvmStatic
        fun start() {
            synchronized(lock) {
                val context = ApplicationActivator.context

                val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
                val resources = context.resources
                val enabled = sharedPreferences.getBoolean(resources.getString(R.string.settings_behavior_show_notification_key), false)

                if (enabled && !started) {
                    try {
                        context.startService(Intent(context, PlaybackService::class.java))
                        started = true
                    } catch (ex: IllegalStateException) {
                        // On Android 12+ startService throws if the app is in the background and not
                        // otherwise allowed to start a service. The playback notification is a
                        // nice-to-have, so skip it and leave started=false; a later status update
                        // while the app is in the foreground will start it.
                        Log.w(PlaybackService::class.java.name, "unable to start playback notification service", ex)
                    }
                }
            }
        }

        @JvmStatic
        fun stop() {
            synchronized(lock) {
                val context = ApplicationActivator.context
                context.stopService(Intent(context, PlaybackService::class.java))
                started = false
            }
        }

        /**
         * Single source of truth for the media notification channel. Created lazily with
         * IMPORTANCE_DEFAULT and no sound: DEFAULT keeps the status-bar icon on Android 12+ (a
         * LOW/"silent" channel is not shown), while setSound(null) + setOnlyAlertOnce on the
         * notification itself keeps it silent and non-intrusive across the frequent refreshes.
         * Both the service and the settings toggle route through here so the channel is never
         * created with a conflicting importance (importance is immutable after first creation).
         */
        @JvmStatic
        fun createMediaNotificationChannel(context: Context) {
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            val channelId = context.getString(R.string.notification_media_player_channel_id)
            if (notificationManager.getNotificationChannel(channelId) == null) {
                val channel = NotificationChannel(channelId, context.getString(R.string.notification_media_player_channel_name), NotificationManager.IMPORTANCE_DEFAULT)
                channel.setSound(null, null)
                notificationManager.createNotificationChannel(channel)
            }
        }
    }
}
