package net.prezz.mpr.ui.player

import androidx.core.content.edit

import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import net.prezz.mpr.R
import net.prezz.mpr.databinding.FragmentPlayerControlBinding
import net.prezz.mpr.model.AudioOutput
import net.prezz.mpr.model.LibraryEntity
import net.prezz.mpr.model.MusicPlayerControl
import net.prezz.mpr.model.PlayerState
import net.prezz.mpr.model.PlayerStatus
import net.prezz.mpr.model.PlaylistEntity
import net.prezz.mpr.model.TaskHandle
import net.prezz.mpr.model.command.ConsumeCommand
import net.prezz.mpr.model.command.NextCommand
import net.prezz.mpr.model.command.PauseCommand
import net.prezz.mpr.model.command.PlayCommand
import net.prezz.mpr.model.command.PreviousCommand
import net.prezz.mpr.model.command.RandomCommand
import net.prezz.mpr.model.command.RepeatCommand
import net.prezz.mpr.model.command.SeekCommand
import net.prezz.mpr.model.command.StopCommand
import net.prezz.mpr.model.command.VolumeDownCommand
import net.prezz.mpr.model.command.VolumeUpCommand
import net.prezz.mpr.model.external.CoverReceiver
import net.prezz.mpr.model.external.ExternalInformationService
import net.prezz.mpr.model.external.UrlReceiver
import net.prezz.mpr.mpd.MpdPartitionProvider
import net.prezz.mpr.ui.helpers.Boast
import net.prezz.mpr.ui.helpers.LyngdorfHelper
import net.prezz.mpr.ui.helpers.ToggleButtonHelper
import net.prezz.mpr.ui.helpers.UpdatePlayDataHelper
import net.prezz.mpr.ui.helpers.UriFilterHelper
import net.prezz.mpr.ui.helpers.VolumeButtonsHelper
import net.prezz.mpr.ui.library.filtered.FilteredActivity
import net.prezz.mpr.ui.library.filtered.FilteredAlbumAndTitleActivity
import net.prezz.mpr.ui.library.filtered.FilteredTrackAndTitleActivity
import java.lang.ref.WeakReference

class PlayerControlFragment : Fragment(), PlayerFragment, View.OnClickListener {

    private var _binding: FragmentPlayerControlBinding? = null
    private val binding get() = _binding!!

    private lateinit var updateTimeHandler: UpdateTimeHandler
    private var playerStatus = PlayerStatus(false)
    private var playlistEntities: Array<PlaylistEntity>? = null
    private var seeking = false
    private var outputVisible = false

    private lateinit var uriFilterHelper: UriFilterHelper

    private var getCoverHandle: TaskHandle = TaskHandle.NULL_HANDLE
    private var lastFmHandle: TaskHandle = TaskHandle.NULL_HANDLE

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPlayerControlBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val seekBar = binding.playerSeekBarTime
        setSeekBarEnablement(seekBar)
        seekBar.setOnSeekBarChangeListener(TimeBarChangedListener())

        updateTimeHandler = UpdateTimeHandler(this)

        uriFilterHelper = UriFilterHelper(requireActivity(), UriFilterHelper.UriFilterChangedListener { })

        (requireActivity() as PlayerActivity).attachFragment(this, FRAGMENT_POSITION)

        binding.playerTextOutput.setOnClickListener(this)
        binding.playerButtonVolumeDown.setOnClickListener(this)
        binding.playerButtonVolumeUp.setOnClickListener(this)
        binding.playerButtonRepeat.setOnClickListener(this)
        binding.playerButtonConsume.setOnClickListener(this)
        binding.playerButtonRandom.setOnClickListener(this)
        binding.playerButtonPrevious.setOnClickListener(this)
        binding.playerButtonStop.setOnClickListener(this)
        binding.playerButtonPlay.setOnClickListener(this)
        binding.playerButtonNext.setOnClickListener(this)
    }

    override fun onResume() {
        super.onResume()
        updateTimeHandler.running(playerStatus.state == PlayerState.PLAY)
    }

    override fun onPause() {
        updateTimeHandler.running(false)
        super.onPause()
    }

    override fun onDestroyView() {
        updateTimeHandler.running(false)
        getCoverHandle.cancelTask()
        lastFmHandle.cancelTask()
        (requireActivity() as PlayerActivity).detachFragment(FRAGMENT_POSITION)
        _binding = null
        super.onDestroyView()
    }

    override fun statusUpdated(status: PlayerStatus) {
        refreshTime(status.elapsedTime.toLong(), status.totalTime.toLong())
        updateTimeHandler.running(status.state == PlayerState.PLAY)

        if (playerStatus.volume != status.volume) {
            setVolumeText(status.volume)
        }

        if (playerStatus.repeat != status.repeat) {
            toggleButton(binding.playerButtonRepeat, status.repeat)
        }

        if (playerStatus.consume != status.consume) {
            toggleButton(binding.playerButtonConsume, status.consume)
        }

        if (playerStatus.random != status.random) {
            toggleButton(binding.playerButtonRandom, status.random)
        }

        if (playerStatus.state != status.state) {
            setPlayButtonState(status.state)
        }

        val showOutput = showOutput()
        if (showOutput != outputVisible || playerStatus.partition != status.partition || !playerStatus.audioOutputs.contentEquals(status.audioOutputs)) {
            setOutputText(showOutput, status.partition, status.audioOutputs)
            outputVisible = showOutput
        }

        val refreshPlaying = playerStatus.playlistVersion == status.playlistVersion && (playerStatus.currentSong != status.currentSong)
        playerStatus = status
        if (refreshPlaying) {
            refreshPlayingInfo()
        }
    }

    override fun playlistUpdated(playlistEntities: Array<PlaylistEntity>) {
        this.playlistEntities = playlistEntities
        refreshPlayingInfo()
    }

    override fun onChoiceMenuClick(view: View) {
        val items = resources.getStringArray(R.array.player_control_choice_menu)
        val seekBar = binding.playerSeekBarTime
        items[0] = String.format(items[0], getString(if (seekBar.isEnabled) R.string.player_seek_bar_disable else R.string.player_seek_bar_enable))

        MaterialAlertDialogBuilder(requireActivity()).apply {
            setTitle(getString(R.string.player_remote_control))
            setItems(items) { _, item ->
                when (item) {
                    0 -> toggleSeekBarEnablement()
                    1 -> showNext()
                    2 -> goTo()
                    3 -> goToLastFm()
                    4 -> UpdatePlayDataHelper.updatePlayData(requireActivity(), playlistEntities)
                }
            }
        }.create().show()
    }

    override fun forceRefresh() {
        // do nothing
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.player_text_output -> (requireActivity() as PlayerActivity).onSelectOutput()
            R.id.player_button_volume_down -> volumeDown()
            R.id.player_button_volume_up -> volumeUp()
            R.id.player_button_repeat -> MusicPlayerControl.sendControlCommand(RepeatCommand(!playerStatus.repeat))
            R.id.player_button_consume -> {
                MusicPlayerControl.sendControlCommand(ConsumeCommand(!playerStatus.consume))
                if (showPlayerOptionToast()) {
                    Boast.makeText(requireActivity(), if (playerStatus.consume) R.string.player_consume_off_toast else R.string.player_consume_on_toast).show()
                }
            }
            R.id.player_button_random -> MusicPlayerControl.sendControlCommand(RandomCommand(!playerStatus.random))
            R.id.player_button_previous -> MusicPlayerControl.sendControlCommand(PreviousCommand())
            R.id.player_button_stop -> MusicPlayerControl.sendControlCommand(StopCommand())
            R.id.player_button_play -> MusicPlayerControl.sendControlCommand(if (playerStatus.state == PlayerState.STOP) PlayCommand() else PauseCommand(playerStatus.state == PlayerState.PAUSE))
            R.id.player_button_next -> MusicPlayerControl.sendControlCommand(NextCommand())
        }
    }

    private fun volumeDown() {
        if (!LyngdorfHelper.volumeDown(requireContext())) {
            MusicPlayerControl.sendControlCommand(VolumeDownCommand(VolumeButtonsHelper.getVolumeAmount(requireContext())))
        }
    }

    private fun volumeUp() {
        if (!LyngdorfHelper.volumeUp(requireContext())) {
            MusicPlayerControl.sendControlCommand(VolumeUpCommand(VolumeButtonsHelper.getVolumeAmount(requireContext())))
        }
    }

    private fun refreshPlayingInfo() {
        val artistTextView = binding.playerTextInfoArtist
        val albumTextView = binding.playerTextInfoAlbum
        val titleTextView = binding.playerTextInfoTitle

        val playingEntity = getPlayingEntity()
        if (playingEntity != null) {
            val oldArtist = artistTextView.text
            val oldAlbum = albumTextView.text

            val artist = playingEntity.getArtist()
            artistTextView.text = artist ?: ""

            val album = playingEntity.getAlbum()
            if (album != null) {
                albumTextView.text = album
            } else if (artist.isNullOrEmpty()) {
                val name = playingEntity.getName()
                if (name != null) {
                    albumTextView.text = name
                } else {
                    val uri = playingEntity.getUriEntity()
                    albumTextView.text = uri?.getUriFilname() ?: ""
                }
            } else {
                albumTextView.text = ""
            }

            val track = playingEntity.getTrack()
            val title = playingEntity.getTitle()
            if (track != null && title != null) {
                titleTextView.text = "$track - $title"
            } else if (title != null) {
                titleTextView.text = title
            } else {
                val uri = playingEntity.getUriEntity()
                titleTextView.text = uri?.getUriFilname() ?: ""
            }

            if (oldArtist != artist || oldAlbum != album) {
                findCoverView()?.setImageBitmap(null)

                getCoverHandle.cancelTask()
                getCoverHandle = ExternalInformationService.getCover(artist, album, null, CoverReceiver { bitmap ->
                    if (bitmap != null) {
                        findCoverView()?.setImageBitmap(bitmap)
                    }
                })
            }
        } else {
            artistTextView.text = ""
            albumTextView.text = ""
            titleTextView.setText(R.string.player_playing_info_none)

            findCoverView()?.setImageBitmap(null)
        }

        hideEmptyTextView(artistTextView)
        hideEmptyTextView(albumTextView)
    }

    private fun findCoverView() = _binding?.playerCoverImage

    private fun getPlayingEntity(): PlaylistEntity? {
        val songIndex = maxOf(0, playerStatus.currentSong)
        val entities = playlistEntities
        if (entities != null && songIndex < entities.size) {
            return entities[songIndex]
        }
        return null
    }

    private fun refreshTime(elapsed: Long, total: Long) {
        binding.playerTextSeekTotal.text = String.format("%d:%02d", total / 60, total % 60)

        val seekBar = binding.playerSeekBarTime
        seekBar.max = total.toInt()
        if (!seeking) {
            updateElapsedTimeText(elapsed)
            if (elapsed in 0..total) {
                seekBar.progress = elapsed.toInt()
            } else {
                seekBar.progress = 0
            }
        }
    }

    private fun hideEmptyTextView(textView: TextView) {
        val text = textView.text
        textView.visibility = if (text == null || text.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun setPlayButtonState(state: PlayerState) {
        val button = binding.playerButtonPlay
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

    private fun setSeekBarEnablement(seekBar: SeekBar) {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireActivity())
        seekBar.isEnabled = sharedPreferences.getBoolean(PREFERENCE_SEEK_BAR_ENABLED_KEY, true)
    }

    private fun toggleSeekBarEnablement() {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireActivity())
        val enabled = !sharedPreferences.getBoolean(PREFERENCE_SEEK_BAR_ENABLED_KEY, true)

        sharedPreferences.edit(commit = true) {
            putBoolean(PREFERENCE_SEEK_BAR_ENABLED_KEY, enabled)
        }

        binding.playerSeekBarTime.isEnabled = enabled
    }

    private fun showNext() {
        val toastText: String
        val songIndex = maxOf(0, playerStatus.nextSong)
        val entities = playlistEntities
        toastText = if (playerStatus.state != PlayerState.STOP && entities != null && songIndex < entities.size) {
            val nextEntity = entities[songIndex]
            nextEntity.getArtist() + " - " + nextEntity.getTitle()
        } else {
            "?"
        }
        Boast.makeText(requireActivity(), toastText).show()
    }

    private fun goTo() {
        val items = resources.getStringArray(R.array.player_context_goto)

        MaterialAlertDialogBuilder(requireActivity()).apply {
            setTitle(R.string.player_goto_header)
            setItems(items) { _, which ->
                when (which) {
                    0 -> {
                        val playingEntity = getPlayingEntity()
                        val artist = playingEntity?.getArtist()
                        if (playingEntity != null && !artist.isNullOrEmpty()) {
                            val intent = Intent(requireActivity(), FilteredAlbumAndTitleActivity::class.java)
                            val args = Bundle()
                            args.putString(FilteredActivity.TITLE_ARGUMENT_KEY, artist)
                            args.putSerializable(FilteredActivity.ENTITY_ARGUMENT_KEY, LibraryEntity.createBuilder().setArtist(artist).setUriFilter(uriFilterHelper.getUriFilter()).build())
                            intent.putExtras(args)
                            startActivity(intent)
                        } else {
                            Boast.makeText(requireActivity(), R.string.player_not_possible).show()
                        }
                    }
                    1 -> {
                        val playingEntity = getPlayingEntity()
                        val album = playingEntity?.getAlbum()
                        if (playingEntity != null && !album.isNullOrEmpty()) {
                            val intent = Intent(requireActivity(), FilteredTrackAndTitleActivity::class.java)
                            val args = Bundle()
                            args.putString(FilteredActivity.TITLE_ARGUMENT_KEY, album)
                            args.putSerializable(FilteredActivity.ENTITY_ARGUMENT_KEY, LibraryEntity.createBuilder().setAlbum(album).setUriFilter(uriFilterHelper.getUriFilter()).build())
                            intent.putExtras(args)
                            startActivity(intent)
                        } else {
                            Boast.makeText(requireActivity(), R.string.player_not_possible).show()
                        }
                    }
                }
            }
        }.create().show()
    }

    private fun goToLastFm() {
        val items = resources.getStringArray(R.array.player_lastfm_goto_items)

        MaterialAlertDialogBuilder(requireActivity()).apply {
            setTitle(R.string.player_lastfm_goto_header)
            setItems(items) { _, which ->
                when (which) {
                    0 -> {
                        val playingEntity = getPlayingEntity()
                        val artist = playingEntity?.getArtist()
                        if (playingEntity != null && !artist.isNullOrEmpty()) {
                            lastFmHandle.cancelTask()
                            lastFmHandle = ExternalInformationService.getArtistInfoUrls(artist, UrlReceiver { urls ->
                                if (urls.isNotEmpty()) {
                                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(urls[0])))
                                } else {
                                    Boast.makeText(requireActivity(), R.string.player_not_possible).show()
                                }
                            })
                        } else {
                            Boast.makeText(requireActivity(), R.string.player_not_possible).show()
                        }
                    }
                    1 -> {
                        val playingEntity = getPlayingEntity()
                        val artist = playingEntity?.getArtist()
                        val album = playingEntity?.getAlbum()
                        if (playingEntity != null && !artist.isNullOrEmpty() && !album.isNullOrEmpty()) {
                            lastFmHandle.cancelTask()
                            lastFmHandle = ExternalInformationService.getAlbumInfoUrls(artist, album, UrlReceiver { urls ->
                                if (urls.isNotEmpty()) {
                                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(urls[0])))
                                } else {
                                    Boast.makeText(requireActivity(), R.string.player_not_possible).show()
                                }
                            })
                        } else {
                            Boast.makeText(requireActivity(), R.string.player_not_possible).show()
                        }
                    }
                }
            }
        }.create().show()
    }

    private fun setVolumeText(volume: Int) {
        val textView = binding.playerTextVolume
        if (volume != -1) {
            textView.text = getString(R.string.player_volume_text_format, volume)
        } else {
            textView.text = getString(R.string.player_volume_text_no_mixer)
        }
    }

    fun setOutputText(visible: Boolean, partition: String?, audioOutputs: Array<AudioOutput>) {

        val textView = binding.playerTextOutput

        if (visible) {
            var partitionPrefix = ""
            if (partition != null && partition.isNotEmpty() && MpdPartitionProvider.DEFAULT_PARTITION != partition) {
                partitionPrefix = "$partition - "
            }

            val outputName = StringBuilder()
            for (output in audioOutputs) {
                if (output.enabled) {
                    if (outputName.isNotEmpty()) {
                        outputName.append(", ")
                    }
                    outputName.append(output.outputName)
                }
            }

            val text = partitionPrefix + outputName
            textView.text = text.ifEmpty { "-" }
            textView.visibility = View.VISIBLE
        } else {
            textView.visibility = View.GONE
            textView.text = ""
        }
    }

    private fun updateElapsedTimeText(elapsed: Long) {
        val totalText = binding.playerTextSeekTotal.text

        val leadingZeroes = maxOf(1, totalText.length - ":00".length)
        binding.playerTextSeekElapsed.text = String.format("%0${leadingZeroes}d:%02d", elapsed / 60, elapsed % 60)
    }

    private fun toggleButton(button: ImageButton, toggled: Boolean) {
        ToggleButtonHelper.toggleButton(requireActivity(), button, toggled)
    }

    private fun showOutput(): Boolean {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireActivity())
        val resources = requireActivity().resources
        return sharedPreferences.getBoolean(resources.getString(R.string.settings_control_show_output_key), false)
    }

    private fun showPlayerOptionToast(): Boolean {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireActivity())
        val resources = requireActivity().resources
        return sharedPreferences.getBoolean(resources.getString(R.string.settings_control_options_show_toast_key), true)
    }

    private inner class TimeBarChangedListener : OnSeekBarChangeListener {

        override fun onStartTrackingTouch(seekBar: SeekBar) {
            seeking = true
            updateTimeHandler.running(false)
        }

        override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
            if (fromUser) {
                updateElapsedTimeText(progress.toLong())
            }
        }

        override fun onStopTrackingTouch(seekBar: SeekBar) {
            seeking = false
            updateTimeHandler.running(false)
            if (playerStatus.state == PlayerState.PLAY || playerStatus.state == PlayerState.PAUSE) {
                val playingEntity = getPlayingEntity()
                if (playingEntity != null) {
                    MusicPlayerControl.sendControlCommand(SeekCommand(playingEntity.getId()!!, seekBar.progress))
                }
            }
        }
    }

    private class UpdateTimeHandler(fragment: PlayerControlFragment) : Handler(Looper.getMainLooper()) {

        // keep a weak reference such that the fragment can be garbage collected even though
        // there might be a pending message for this handler.
        private val fragmentRef = WeakReference(fragment)
        private var running = false

        fun running(run: Boolean) {
            if (!running && run) {
                dispatchNext()
            }
            running = run

            if (!running) {
                removeMessages(UPDATE_TIME_EVENT)
            }
        }

        override fun handleMessage(msg: Message) {
            when (msg.what) {
                UPDATE_TIME_EVENT -> {
                    if (running) {
                        val fragment = fragmentRef.get()
                        if (fragment != null) {
                            val delta = System.currentTimeMillis() - fragment.playerStatus.timestamp
                            fragment.refreshTime(fragment.playerStatus.elapsedTime + (delta / 1000), fragment.playerStatus.totalTime.toLong())
                            dispatchNext()
                        }
                    }
                }
            }
        }

        private fun dispatchNext() {
            val message = obtainMessage(UPDATE_TIME_EVENT)
            sendMessageDelayed(message, INTERVAL.toLong())
        }

        companion object {
            const val UPDATE_TIME_EVENT = 20011
            const val INTERVAL = 475
        }
    }

    companion object {
        const val FRAGMENT_POSITION = 1

        private const val PREFERENCE_SEEK_BAR_ENABLED_KEY = "seek_bar_enabled_key"
    }
}
