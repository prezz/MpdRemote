package net.prezz.mpr.ui.player

import androidx.core.content.edit

import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuProvider
import androidx.preference.PreferenceManager
import androidx.viewpager2.widget.ViewPager2
import net.prezz.mpr.R
import net.prezz.mpr.databinding.ActivityPlayerBinding
import net.prezz.mpr.model.AudioOutput
import net.prezz.mpr.model.MusicPlayerControl
import net.prezz.mpr.model.PlayerState
import net.prezz.mpr.model.PlayerStatus
import net.prezz.mpr.model.PlaylistEntity
import net.prezz.mpr.model.ResponseReceiver
import net.prezz.mpr.model.StatusListener
import net.prezz.mpr.model.TaskHandle
import net.prezz.mpr.model.command.Command
import net.prezz.mpr.model.command.PauseCommand
import net.prezz.mpr.model.command.PlayCommand
import net.prezz.mpr.model.command.ToggleOutputCommand
import net.prezz.mpr.model.servers.ServerConfiguration
import net.prezz.mpr.model.servers.ServerConfigurationService
import net.prezz.mpr.mpd.MpdPlayer
import net.prezz.mpr.service.PlaybackService
import net.prezz.mpr.service.StreamingService
import net.prezz.mpr.ui.DatabaseActivity
import net.prezz.mpr.ui.PlayDataActivity
import net.prezz.mpr.ui.helpers.setupToolbar
import net.prezz.mpr.ui.helpers.VolumeButtonsHelper
import net.prezz.mpr.ui.library.LibraryActivity
import net.prezz.mpr.ui.mpd.MpdPlayerSettings
import net.prezz.mpr.ui.partitions.PartitionsActivity
import net.prezz.mpr.ui.playlists.StoredPlaylistsActivity
import net.prezz.mpr.ui.search.SearchActivity
import net.prezz.mpr.ui.settings.SettingsActivity
import net.prezz.mpr.ui.state.DataState

class PlayerActivity : AppCompatActivity(), ActivityResultCallback<ActivityResult> {

    private lateinit var binding: ActivityPlayerBinding

    private lateinit var currentMpdSettings: MpdPlayerSettings

    private val musicPlayerRefreshListener = MusicPlayerRefreshListener()
    private val pageChangeCallback: ViewPager2.OnPageChangeCallback = PageChangeCallback()
    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent>
    private val attachedFragments = arrayOfNulls<PlayerFragment>(2)
    private var fragmentPosition = 0
    private var playerStatus = PlayerStatus(false)
    private var playlistEntities: Array<PlaylistEntity> = emptyArray()

    private var updatePlaylistHandle: TaskHandle = TaskHandle.NULL_HANDLE
    private var selectOutputsHandle: TaskHandle = TaskHandle.NULL_HANDLE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupToolbar()

        val pageAdapter = PlayerPagerAdapter(this)
        val viewPager = binding.playerViewPagerSwipe
        viewPager.adapter = pageAdapter

        fragmentPosition = getDefaultFragment()
        if (fragmentPosition > 0) {
            viewPager.currentItem = fragmentPosition
        }

        viewPager.registerOnPageChangeCallback(pageChangeCallback)
        activityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult(), this)

        addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.player, menu)
            }

            override fun onPrepareMenu(menu: Menu) {
                val streamingItem = menu.findItem(R.id.player_action_toggle_streaming)
                streamingItem?.isVisible = !currentMpdSettings.getMpdStreamingUrl().isNullOrEmpty()
            }

            override fun onMenuItemSelected(item: MenuItem): Boolean {
                return when (item.itemId) {
                    R.id.player_action_settings -> {
                        val intent = Intent(this@PlayerActivity, SettingsActivity::class.java)
                        activityResultLauncher.launch(intent)
                        true
                    }
                    R.id.player_action_server -> {
                        selectServer()
                        true
                    }
                    R.id.player_action_database -> {
                        startActivity(Intent(this@PlayerActivity, DatabaseActivity::class.java))
                        true
                    }
                    R.id.player_action_play_data -> {
                        startActivity(Intent(this@PlayerActivity, PlayDataActivity::class.java))
                        true
                    }
                    R.id.player_action_partitions -> {
                        startActivity(Intent(this@PlayerActivity, PartitionsActivity::class.java))
                        true
                    }
                    R.id.player_action_outputs -> {
                        selectOutputs()
                        true
                    }
                    R.id.player_action_toggle_streaming -> {
                        if (StreamingService.isStarted()) {
                            stopStreaming()
                        } else {
                            val streamingUrl = currentMpdSettings.getMpdStreamingUrl()
                            if (!streamingUrl.isNullOrEmpty()) {
                                startStreaming(streamingUrl!!)
                            }
                        }
                        true
                    }
                    else -> false
                }
            }
        }, this)

        binding.playerButtonStoredPlaylists.setOnClickListener { onStoredPlaylistsClick(it) }
        binding.playerButtonLibrary.setOnClickListener { onLibraryClick(it) }
        binding.playerButtonChoiceMenu.setOnClickListener { onChoiceMenuClick(it) }
        binding.playerButtonSearchLibrary.setOnClickListener { onSearchClick(it) }

        val dataState = DataState.get(this)
        (dataState.getData(PLAYER_STATUS_INSTANCE_STATE, null) as PlayerStatus?)?.let { playerStatus = it }
        @Suppress("UNCHECKED_CAST")
        (dataState.getData(PLAYLIST_ENTITIES_INSTANCE_STATE, null) as? Array<PlaylistEntity>)?.let { playlistEntities = it }

        setActivityTitle()

        if (connectMusicPlayer()) {
            showSwipeHint()
        }
    }

    override fun onResume() {
        super.onResume()
        MusicPlayerControl.setStatusListener(musicPlayerRefreshListener)
    }

    override fun onPause() {
        super.onPause()
        MusicPlayerControl.setStatusListener(null)
        updatePlaylistHandle.cancelTask()
        selectOutputsHandle.cancelTask()
    }

    override fun onDestroy() {
        binding.playerViewPagerSwipe.unregisterOnPageChangeCallback(pageChangeCallback)
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        val dataState = DataState.get(this)
        dataState.setData(PLAYER_STATUS_INSTANCE_STATE, playerStatus)
        dataState.setData(PLAYLIST_ENTITIES_INSTANCE_STATE, playlistEntities)
        super.onSaveInstanceState(outState)
    }

    override fun onActivityResult(result: ActivityResult) {
        if (reconnectMusicPlayerOnSettingsChanged(false)) {
            showSwipeHint()
        } else {
            attachedFragments[PlayerPlaylistFragment.FRAGMENT_POSITION]?.playlistUpdated(playlistEntities)
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (VolumeButtonsHelper.handleKeyDown(this, keyCode, event)) {
            return true
        }

        return super.onKeyDown(keyCode, event)
    }

    fun attachFragment(fragment: PlayerFragment, pos: Int) {
        attachedFragments[pos] = fragment

        fragment.statusUpdated(playerStatus)
        fragment.playlistUpdated(playlistEntities)
    }

    fun detachFragment(pos: Int) {
        attachedFragments[pos] = null
    }

    private fun onStoredPlaylistsClick(v: View) {
        startActivity(Intent(this, StoredPlaylistsActivity::class.java))
    }

    private fun onLibraryClick(v: View) {
        startActivity(Intent(this, LibraryActivity::class.java))
    }

    private fun onChoiceMenuClick(view: View) {
        attachedFragments[fragmentPosition]?.onChoiceMenuClick(view)
    }

    private fun onSearchClick(v: View) {
        startActivity(Intent(this, SearchActivity::class.java))
    }

    fun onSelectOutput() {
        selectOutputs()
    }

    private fun selectServer() {
        val serverConfigurations = ServerConfigurationService.getServerConfigurations()
        val selectedConfiguration = ServerConfigurationService.getSelectedServerConfiguration()

        val selectedItem = intArrayOf(-1)
        val items = Array(serverConfigurations.size) { i ->
            if (selectedConfiguration == serverConfigurations[i]) {
                selectedItem[0] = i
            }
            serverConfigurations[i].toString()
        }

        MaterialAlertDialogBuilder(this).apply {
            setTitle(R.string.player_action_server)
            setSingleChoiceItems(items, selectedItem[0]) { _, which ->
                selectedItem[0] = which
            }
            setPositiveButton(android.R.string.ok) { _, _ ->
                if (selectedItem[0] != -1) {
                    val selected = serverConfigurations[selectedItem[0]]
                    if (selectedConfiguration != selected) {
                        ServerConfigurationService.setSelectedServerConfiguration(selected)
                        reconnectMusicPlayerOnSettingsChanged(true)
                    }
                }
            }
        }.create().show()
    }

    private fun selectOutputs() {
        selectOutputsHandle.cancelTask()
        selectOutputsHandle = MusicPlayerControl.getOutputs(false, object : ResponseReceiver<Array<AudioOutput>>() {
            override fun receiveResponse(response: Array<AudioOutput>) {
                val items = Array(response.size) { "" }
                val preChecked = BooleanArray(response.size)
                val postChecked = BooleanArray(response.size)
                for (i in response.indices) {
                    val pluginSuffix = if (response[i].plugin.isNotEmpty()) " (" + response[i].plugin + ")" else ""
                    items[i] = response[i].outputName + pluginSuffix
                    preChecked[i] = response[i].enabled
                    postChecked[i] = response[i].enabled
                }

                MaterialAlertDialogBuilder(this@PlayerActivity).apply {
                    setTitle(R.string.player_action_outputs)
                    setMultiChoiceItems(items, postChecked) { _, which, isChecked ->
                        postChecked[which] = isChecked
                    }
                    setPositiveButton(android.R.string.ok) { _, _ ->
                        val commands = ArrayList<Command>()

                        for (i in response.indices) { // first add all commands that enables
                            if (preChecked[i] != postChecked[i] && postChecked[i]) {
                                commands.add(ToggleOutputCommand(response[i].outputId, postChecked[i]))
                            }
                        }
                        for (i in response.indices) { // then the commands that disables
                            if (preChecked[i] != postChecked[i] && !postChecked[i]) {
                                commands.add(ToggleOutputCommand(response[i].outputId, postChecked[i]))
                            }
                        }

                        if (commands.isNotEmpty()) {
                            MusicPlayerControl.sendControlCommands(commands)
                        }
                    }
                }.create().show()
            }
        })
    }

    private fun connectMusicPlayer(): Boolean {
        currentMpdSettings = MpdPlayerSettings.create(applicationContext)
        if (currentMpdSettings.getMpdHost().isEmpty()) {
            MaterialAlertDialogBuilder(this).apply {
                setCancelable(false)
                setTitle(R.string.player_no_server_configured_header)
                setMessage(R.string.player_no_server_configured_message)
                setNegativeButton(android.R.string.cancel) { _, _ -> }
                setPositiveButton(R.string.player_action_settings) { _, _ ->
                    val intent = Intent(this@PlayerActivity, SettingsActivity::class.java)
                    activityResultLauncher.launch(intent)
                }
            }.create().show()
            return false
        } else {
            MusicPlayerControl.setMusicPlayer(MpdPlayer(currentMpdSettings))
            return true
        }
    }

    private fun reconnectMusicPlayerOnSettingsChanged(connectStatusListener: Boolean): Boolean {
        val mpdSettings = MpdPlayerSettings.create(applicationContext)
        if (mpdSettings != currentMpdSettings) {
            PlaybackService.stop()
            currentMpdSettings = mpdSettings
            invalidateOptionsMenu()
            if (currentMpdSettings.getMpdHost().isNotEmpty()) {
                MusicPlayerControl.setMusicPlayer(MpdPlayer(currentMpdSettings))
                if (connectStatusListener) {
                    MusicPlayerControl.setStatusListener(musicPlayerRefreshListener)
                }
                return true
            } else {
                MusicPlayerControl.setMusicPlayer(null)
                playlistEntities = emptyArray()
                playerStatus = PlayerStatus(false)
                return false
            }
        }

        return false
    }

    private fun startStreaming(url: String) {
        if (playerStatus.state == PlayerState.STOP) {
            MusicPlayerControl.sendControlCommand(PlayCommand())
        }

        StreamingService.start(url)
    }

    private fun stopStreaming() {
        if (playerStatus.state == PlayerState.PLAY) {
            MusicPlayerControl.sendControlCommand(PauseCommand(false))
        }

        StreamingService.stop()
    }

    private fun getDefaultFragment(): Int {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val resources = this.resources
        return sharedPreferences.getString(resources.getString(R.string.settings_default_player_fragment_key), "0")!!.toInt()
    }

    private fun setActivityTitle() {
        val pageAdapter = binding.playerViewPagerSwipe.adapter as? PlayerPagerAdapter ?: return
        var title = pageAdapter.getTitle(fragmentPosition)
        if (fragmentPosition == 0) {
            title += getPlaylistTime()
        }
        setTitle(title)
    }

    private fun getPlaylistTime(): String {
        var totalTime = 0

        for (entity in playlistEntities) {
            val time = entity.getTime()
            if (time != null) {
                totalTime += time
            }
        }

        val hours = totalTime / 3600
        val remaining = totalTime % 3600
        val minutes = remaining / 60
        val seconds = remaining % 60

        return if (hours > 0) {
            String.format(" (%d:%02d:%02d)", hours, minutes, seconds)
        } else {
            String.format(" (%d:%02d)", minutes, seconds)
        }
    }

    private fun showSwipeHint() {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val show = sharedPreferences.getBoolean(PREFERENCE_SHOW_SWIPE_HINT_KEY, true)

        if (show) {
            MaterialAlertDialogBuilder(this).apply {
                setCancelable(false)
                setTitle(R.string.player_swipe_hint_header)
                setMessage(R.string.player_swipe_hint_message)
                setPositiveButton(R.string.player_swipe_hint_button) { _, _ ->
                    sharedPreferences.edit(commit = true) {
                        putBoolean(PREFERENCE_SHOW_SWIPE_HINT_KEY, false)
                    }
                }
            }.create().show()
        }
    }

    private inner class MusicPlayerRefreshListener : ResponseReceiver<Array<PlaylistEntity>>(), StatusListener {

        private var pendingPlayerStatus: PlayerStatus? = null

        override fun statusUpdated(status: PlayerStatus) {
            if (playerStatus.playlistVersion != status.playlistVersion) {
                pendingPlayerStatus = status
                updatePlaylistHandle.cancelTask()
                updatePlaylistHandle = MusicPlayerControl.getPlaylist(this)
            } else {
                playerStatus = status
                for (fragment in attachedFragments) {
                    fragment?.statusUpdated(playerStatus)
                }
            }
        }

        override fun receiveResponse(response: Array<PlaylistEntity>) {
            playerStatus = pendingPlayerStatus!!
            for (fragment in attachedFragments) {
                fragment?.statusUpdated(playerStatus)
            }

            playlistEntities = response
            for (fragment in attachedFragments) {
                fragment?.playlistUpdated(playlistEntities)
            }

            setActivityTitle()
        }
    }

    private inner class PageChangeCallback : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            fragmentPosition = position
            attachedFragments[fragmentPosition]?.forceRefresh()
            setActivityTitle()
        }
    }

    companion object {
        private const val PREFERENCE_SHOW_SWIPE_HINT_KEY = "player_show_swipe_hint"

        private const val PLAYER_STATUS_INSTANCE_STATE = "playerStatus"
        private const val PLAYLIST_ENTITIES_INSTANCE_STATE = "playlistEntities"
    }
}
