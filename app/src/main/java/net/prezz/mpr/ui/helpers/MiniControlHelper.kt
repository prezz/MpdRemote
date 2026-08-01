package net.prezz.mpr.ui.helpers

import android.app.Activity
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import net.prezz.mpr.R
import net.prezz.mpr.model.MusicPlayerControl
import net.prezz.mpr.model.PlayerState
import net.prezz.mpr.model.PlayerStatus
import net.prezz.mpr.model.PlaylistEntity
import net.prezz.mpr.model.ResponseReceiver
import net.prezz.mpr.model.StatusListener
import net.prezz.mpr.model.TaskHandle
import net.prezz.mpr.model.command.NextCommand
import net.prezz.mpr.model.command.PauseCommand
import net.prezz.mpr.model.command.PlayCommand
import net.prezz.mpr.model.command.PreviousCommand
import net.prezz.mpr.model.command.VolumeDownCommand
import net.prezz.mpr.model.command.VolumeUpCommand

class MiniControlHelper(private val activity: Activity) : StatusListener {

    private var taskHandler: TaskHandle = TaskHandle.NULL_HANDLE
    private var playerStatus = PlayerStatus(false)

    init {
        attachButtonListeners()
    }

    fun toggleVisibility() {
        val view = activity.findViewById<View>(R.id.control_layout_mini_control)
        if (view != null) {
            when (view.visibility) {
                View.GONE -> {
                    view.visibility = View.VISIBLE
                    MusicPlayerControl.setStatusListener(this)
                }
                View.VISIBLE -> doHideVisability(view)
            }
        }
    }

    fun hideVisibility() {
        val view = activity.findViewById<View>(R.id.control_layout_mini_control)
        doHideVisability(view)
    }

    override fun statusUpdated(status: PlayerStatus) {
        if (playerStatus.volume != status.volume) {
            setVolumeText(status.volume)
        }

        if (playerStatus.state != status.state) {
            setPlayButtonState(status.state)
        }

        val refreshPlaying = playerStatus.playlistVersion != status.playlistVersion || (playerStatus.currentSong != status.currentSong)
        playerStatus = status
        if (refreshPlaying) {
            refreshPlayingInfo()
        }
    }

    private fun doHideVisability(view: View?) {
        if (view != null) {
            taskHandler.cancelTask()
            MusicPlayerControl.setStatusListener(null)
            view.visibility = View.GONE
        }
    }

    private fun setVolumeText(volume: Int) {
        val textView = activity.findViewById<TextView>(R.id.control_text_volume)
        if (volume != -1) {
            textView.text = activity.getString(R.string.control_volume_text_format, volume)
        } else {
            textView.text = activity.getString(R.string.control_volume_text_no_mixer)
        }
    }

    private fun refreshPlayingInfo() {
        val songIndex = maxOf(0, playerStatus.currentSong)
        taskHandler.cancelTask()
        taskHandler = MusicPlayerControl.getPlaylistEntity(songIndex, object : ResponseReceiver<PlaylistEntity?>() {
            override fun receiveResponse(response: PlaylistEntity?) {
                val artistTextView = activity.findViewById<TextView>(R.id.control_text_playing_artist)
                val titleTextView = activity.findViewById<TextView>(R.id.control_text_playing_title)
                if (response != null) {
                    val artist = response.getArtist()
                    if (artist != null) {
                        artistTextView.text = artist
                    } else {
                        val name = response.getName()
                        if (name != null) {
                            artistTextView.text = name
                        } else {
                            artistTextView.text = ""
                        }
                    }

                    val track = response.getTrack()
                    val title = response.getTitle()
                    if (track != null && title != null) {
                        titleTextView.text = "$track - $title"
                    } else if (title != null) {
                        titleTextView.text = title
                    } else {
                        titleTextView.text = response.getUriEntity().getUriFilname()
                    }
                } else {
                    artistTextView.text = ""
                    titleTextView.setText(R.string.control_playing_info_none)
                }
            }
        })
    }

    private fun setPlayButtonState(state: PlayerState) {
        val button = activity.findViewById<ImageButton>(R.id.control_button_play)
        when (state) {
            PlayerState.STOP -> {
                toggleButton(button, false)
                button.setImageResource(R.drawable.ic_play)
            }
            PlayerState.PLAY -> {
                toggleButton(button, false)
                button.setImageResource(R.drawable.ic_pause)
            }
            PlayerState.PAUSE -> {
                toggleButton(button, true)
                button.setImageResource(R.drawable.ic_pause)
            }
        }
    }

    private fun toggleButton(button: ImageButton, toggled: Boolean) {
        ToggleButtonHelper.toggleButton(activity, button, toggled)
    }

    private fun attachButtonListeners() {
        activity.findViewById<ImageButton>(R.id.control_button_volume_down).setOnClickListener {
            if (!LyngdorfHelper.volumeDown(activity)) {
                MusicPlayerControl.sendControlCommand(VolumeDownCommand(VolumeButtonsHelper.getVolumeAmount(activity)))
            }
        }

        activity.findViewById<ImageButton>(R.id.control_button_volume_up).setOnClickListener {
            if (!LyngdorfHelper.volumeUp(activity)) {
                MusicPlayerControl.sendControlCommand(VolumeUpCommand(VolumeButtonsHelper.getVolumeAmount(activity)))
            }
        }

        activity.findViewById<ImageButton>(R.id.control_button_previous).setOnClickListener {
            MusicPlayerControl.sendControlCommand(PreviousCommand())
        }

        activity.findViewById<ImageButton>(R.id.control_button_play).setOnClickListener {
            MusicPlayerControl.sendControlCommand(if (playerStatus.state == PlayerState.STOP) PlayCommand() else PauseCommand(playerStatus.state == PlayerState.PAUSE))
        }

        activity.findViewById<ImageButton>(R.id.control_button_next).setOnClickListener {
            MusicPlayerControl.sendControlCommand(NextCommand())
        }
    }
}
