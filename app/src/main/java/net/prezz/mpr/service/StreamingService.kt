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
import android.graphics.BitmapFactory
import android.graphics.drawable.Icon
import android.media.AudioAttributes
import android.media.MediaMetadata
import android.media.MediaPlayer
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.net.wifi.WifiManager
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.content.ContextCompat
import net.prezz.mpr.R
import net.prezz.mpr.model.PlayerState
import net.prezz.mpr.model.PlayerStatus
import net.prezz.mpr.model.ResponseReceiver
import net.prezz.mpr.model.ResponseResult
import net.prezz.mpr.model.StatusListener
import net.prezz.mpr.model.command.Command
import net.prezz.mpr.model.command.NextCommand
import net.prezz.mpr.model.command.PauseCommand
import net.prezz.mpr.model.command.PreviousCommand
import net.prezz.mpr.mpd.MpdPlayer
import net.prezz.mpr.ui.ApplicationActivator
import net.prezz.mpr.ui.helpers.Boast
import net.prezz.mpr.ui.mpd.MpdPlayerSettings
import net.prezz.mpr.ui.player.PlayerActivity

/**
 * Foreground service that plays the MPD server's audio output as an HTTP stream on the device (see
 * https://developer.android.com/guide/topics/media/mediaplayer.html). It mirrors MPD's play/pause
 * state, exposes a media notification with transport controls, and auto-reconnects on drop-outs.
 * Faithful 1:1 port of V1's behaviour.
 *
 * PORTING NOTES / small idiomatic improvement (documented per request):
 * - The internal notification-action broadcasts (CMD_STOP / CMD_PAUSE) are now registered with
 *   `RECEIVER_NOT_EXPORTED` (via `ContextCompat.registerReceiver`) and their Intents are scoped to this
 *   app's package. V1 used `RECEIVER_EXPORTED`, which let any other app trigger stop/pause on our
 *   receiver — an unnecessary, minor security hole. These actions are app-private, so nothing external
 *   should ever deliver them.
 * - Otherwise kept faithful, including the framework `android.media.session.MediaSession` +
 *   `MediaPlayer` wiring. FUTURE MODERNIZATION (not done here to avoid a risky rewrite): migrate to
 *   `androidx.media3` (`MediaSession` + `ExoPlayer` + `MediaSessionService`), which handles the
 *   notification, transport actions and audio-focus/reconnect concerns idiomatically.
 */
class StreamingService : Service() {

    private var mpdPlayer: MpdPlayer? = null
    private var mpdState: PlayerState? = null
    private var mediaSession: MediaSession? = null
    private var mediaPlayer: MediaPlayer? = null
    private var wifiLock: WifiManager.WifiLock? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var url: String? = null
    private var mediaPlayerListener: MediaPlayerListener? = null
    private var broadcastReceiver: StreamBroadcastReceiver? = null
    private var preparing = true

    // Decoded once and reused: the notification is refreshed frequently (buffering/pause updates)
    // and re-decoding the launcher bitmap each time is wasteful.
    private val largeIcon: Bitmap by lazy { BitmapFactory.decodeResource(resources, R.drawable.ic_launcher) }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        started = true // if the service gets killed and restarted we need to set it to true

        url = intent?.getStringExtra(URL_ARGUMENT_KEY)

        // Post the foreground notification immediately so the mandatory startForeground() happens
        // well within the 5s window required by startForegroundService(), even if the media setup
        // below fails (e.g. a bad stream URL). It is refreshed once buffering/playback begins.
        updateNotification(getString(R.string.notification_streaming_service_buffering))

        val context = applicationContext

        wifiLock?.release()

        val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
        wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL_LOW_LATENCY, "mpd_stream:wifi_lock").also {
            it.acquire()
        }

        wakeLock?.release()

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "mpd_stream:wake_lock").also {
            it.acquire()
        }

        broadcastReceiver?.let { unregisterReceiver(it) }

        broadcastReceiver = StreamBroadcastReceiver()
        val filter = IntentFilter()
        filter.addAction(CMD_STOP)
        filter.addAction(CMD_PAUSE)
        // NOT_EXPORTED: these actions are app-private, only this app may deliver them.
        ContextCompat.registerReceiver(this, broadcastReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)

        startMediaSession()
        startMediaPlayer(context)

        return START_REDELIVER_INTENT
    }

    override fun onDestroy() {
        stopMediaSession()

        wakeLock?.release()
        wifiLock?.release()

        mediaPlayer?.let {
            it.stop()
            it.release()
        }

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)

        broadcastReceiver?.let { unregisterReceiver(it) }

        started = false

        super.onDestroy()
    }

    private fun updateNotification(text: String) {
        updateMediaMetadata(text)
        updateMediaNotification(text)
    }

    private fun updateMediaMetadata(text: String) {
        val mediaMetadata = MediaMetadata.Builder()
            .putString(MediaMetadata.METADATA_KEY_TITLE, getString(R.string.notification_streaming_service_title))
            .putString(MediaMetadata.METADATA_KEY_ARTIST, text)
            .build()

        mediaSession?.setMetadata(mediaMetadata)
    }

    private fun updateMediaNotification(text: String) {
        // Internal command intents are scoped to this app's package (see NOT_EXPORTED note above).
        val stopIntent = Intent(CMD_STOP).setPackage(packageName)
        val pauseIntent = Intent(CMD_PAUSE).setPackage(packageName)
        val launchIntent = Intent(this, PlayerActivity::class.java)

        val icPlay = if (mediaPlayer?.isPlaying == true) R.drawable.ic_pause_w else R.drawable.ic_paused_w

        val channelId = getString(R.string.notification_streaming_channel_id)

        val notification = Notification.Builder(this, channelId)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setSmallIcon(R.drawable.ic_stream)
            .setShowWhen(false)
            // Alert (sound/heads-up) only on the first post, not on every buffering/pause update; without
            // this an IMPORTANCE_DEFAULT channel would re-alert on each notification refresh.
            .setOnlyAlertOnce(true)
            .addAction(Notification.Action.Builder(Icon.createWithResource(this, icPlay), "", PendingIntent.getBroadcast(this, 0, pauseIntent, PendingIntent.FLAG_IMMUTABLE)).build()) // #0
            .addAction(Notification.Action.Builder(Icon.createWithResource(this, R.drawable.ic_stop_w), "", PendingIntent.getBroadcast(this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE)).build()) // #1
            .setStyle(Notification.MediaStyle().setMediaSession(mediaSession?.sessionToken).setShowActionsInCompactView(1))
            .setContentTitle(getString(R.string.notification_streaming_service_title))
            .setContentText(text)
            .setLargeIcon(largeIcon)
            .setContentIntent(PendingIntent.getActivity(this, 0, launchIntent, PendingIntent.FLAG_IMMUTABLE))
            .setAutoCancel(false)
            .setOngoing(true)
            .build()

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        var channel = notificationManager.getNotificationChannel(channelId)
        if (channel == null) {
            // IMPORTANCE_DEFAULT (V1 used IMPORTANCE_LOW): a LOW/"silent" channel is not shown as an icon
            // in the status bar on Android 12+, but a media notification is expected to have one. DEFAULT
            // keeps the status-bar icon; setSound(null) preserves V1's silence (no ding) and DEFAULT (not
            // HIGH) means no intrusive heads-up. setOnlyAlertOnce(true) above further guards against
            // re-alerting on the frequent notification refreshes.
            channel = NotificationChannel(channelId, getString(R.string.notification_streaming_channel_name), NotificationManager.IMPORTANCE_DEFAULT)
            channel.setSound(null, null)
            notificationManager.createNotificationChannel(channel)
        }

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun startMediaSession() {
        stopMediaSession()

        mediaSession = MediaSession(this, "MPD Remote").also {
            it.setCallback(MediaSessionCallback())
            it.isActive = true
        }

        mpdState = null

        val settings = MpdPlayerSettings.create(this)
        mpdPlayer = MpdPlayer(settings).also {
            it.setStatusListener(PlayerInfoRefreshListener())
        }
    }

    private fun stopMediaSession() {
        mpdPlayer?.dispose()

        mediaSession?.let {
            it.isActive = false
            it.release()
        }
    }

    private fun setMediaSessionState(playerState: PlayerState?) {
        val commonAction = PlaybackState.ACTION_PLAY_PAUSE or PlaybackState.ACTION_SKIP_TO_NEXT or PlaybackState.ACTION_SKIP_TO_PREVIOUS

        val playbackStateBuilder = PlaybackState.Builder()

        when (playerState) {
            PlayerState.PLAY -> {
                playbackStateBuilder.setActions(commonAction or PlaybackState.ACTION_PAUSE)
                playbackStateBuilder.setState(PlaybackState.STATE_PLAYING, PlaybackState.PLAYBACK_POSITION_UNKNOWN, 0f)
            }
            PlayerState.PAUSE -> {
                playbackStateBuilder.setActions(commonAction or PlaybackState.ACTION_PLAY)
                playbackStateBuilder.setState(PlaybackState.STATE_PAUSED, PlaybackState.PLAYBACK_POSITION_UNKNOWN, 0f)
            }
            else -> {
                playbackStateBuilder.setState(PlaybackState.STATE_NONE, PlaybackState.PLAYBACK_POSITION_UNKNOWN, 0f)
            }
        }

        mediaSession?.setPlaybackState(playbackStateBuilder.build())
    }

    private fun startMediaPlayer(context: Context) {
        preparing = true

        mediaPlayer?.let {
            it.stop()
            it.release()
        }

        val player = MediaPlayer()
        mediaPlayer = player
        player.setAudioAttributes(AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())

        mediaPlayerListener = MediaPlayerListener().also {
            player.setOnPreparedListener(it)
            player.setOnBufferingUpdateListener(it)
            player.setOnCompletionListener(it)
            player.setOnInfoListener(it)
            player.setOnErrorListener(it)
        }

        try {
            player.setDataSource(url!!)
            player.setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK)
            player.prepareAsync()
        } catch (ex: Exception) {
            stop()
        }

        updateNotification(getString(R.string.notification_streaming_service_buffering))
    }

    private inner class MediaPlayerListener : MediaPlayer.OnPreparedListener, MediaPlayer.OnBufferingUpdateListener,
        MediaPlayer.OnCompletionListener, MediaPlayer.OnInfoListener, MediaPlayer.OnErrorListener {

        override fun onPrepared(mediaPlayer: MediaPlayer) {
            Log.i(StreamingService::class.java.name, "Stream prepared")

            mediaPlayer.start()
            updateNotification(url ?: "")
            preparing = false
        }

        override fun onBufferingUpdate(mp: MediaPlayer, percent: Int) {
            Log.i(StreamingService::class.java.name, "Stream buffering $percent%")
        }

        override fun onCompletion(mediaPlayer: MediaPlayer) {
            Log.i(StreamingService::class.java.name, "Stream completed")

            if (mpdState == PlayerState.PLAY) {
                startMediaPlayer(applicationContext)
            } else {
                updateNotification(getString(R.string.notification_streaming_service_idle))
            }
        }

        override fun onInfo(mediaPlayer: MediaPlayer, what: Int, extra: Int): Boolean {
            Log.i(StreamingService::class.java.name, "Stream info code $what, $extra")

            if (what == MediaPlayer.MEDIA_INFO_BUFFERING_START) {
                // Likely a dropped connection. Restart the media player.
                startMediaPlayer(applicationContext)
                return true
            }

            return false
        }

        override fun onError(mediaPlayer: MediaPlayer, what: Int, extra: Int): Boolean {
            Log.i(StreamingService::class.java.name, "Stream error code $what, $extra")

            if (mpdState == PlayerState.PLAY) {
                startMediaPlayer(applicationContext)
            } else {
                updateNotification(getString(R.string.notification_streaming_service_idle))
            }

            return true
        }
    }

    private inner class StreamBroadcastReceiver : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                CMD_STOP -> stop()
                CMD_PAUSE -> {
                    if (!preparing) {
                        mediaPlayer?.let {
                            if (it.isPlaying) {
                                it.pause()
                            } else {
                                it.start()
                            }
                        }
                        updateNotification(url ?: "")
                    }
                }
            }
        }
    }

    private inner class PlayerInfoRefreshListener : StatusListener {

        override fun statusUpdated(status: PlayerStatus) {
            if (mpdState != status.state) {
                if (mpdState != null && status.state == PlayerState.PLAY) {
                    startMediaPlayer(applicationContext)
                }

                mpdState = status.state
                setMediaSessionState(mpdState)
            }
        }
    }

    private inner class MediaSessionCallback : MediaSession.Callback() {

        override fun onPlay() {
            sendControlCommand(PauseCommand(true))
        }

        override fun onPause() {
            sendControlCommand(PauseCommand(false))
        }

        override fun onSkipToNext() {
            sendControlCommand(NextCommand())
        }

        override fun onSkipToPrevious() {
            sendControlCommand(PreviousCommand())
        }

        private fun sendControlCommand(command: Command) {
            mpdPlayer?.sendControlCommands(listOf(command), object : ResponseReceiver<ResponseResult>() {
                override fun receiveResponse(response: ResponseResult) {
                }
            })
        }
    }

    companion object {
        private const val CMD_STOP = "net.prezz.mpr.service.StreamingService.CMD_STOP"
        private const val CMD_PAUSE = "net.prezz.mpr.service.StreamingService.CMD_PAUSE"

        private const val NOTIFICATION_ID = 864532
        private const val URL_ARGUMENT_KEY = "url"

        private val lock = Any()
        private var started = false

        @JvmStatic
        fun isStarted(): Boolean {
            return started
        }

        @JvmStatic
        fun start(url: String) {
            synchronized(lock) {
                val context = ApplicationActivator.context

                if (!started) {
                    started = true
                    context.startForegroundService(Intent(context, StreamingService::class.java).putExtra(URL_ARGUMENT_KEY, url))
                } else {
                    Boast.makeText(context, "Stream already running,")
                }
            }
        }

        @JvmStatic
        fun stop() {
            synchronized(lock) {
                val context = ApplicationActivator.context
                context.stopService(Intent(context, StreamingService::class.java))
                started = false
            }
        }
    }
}
