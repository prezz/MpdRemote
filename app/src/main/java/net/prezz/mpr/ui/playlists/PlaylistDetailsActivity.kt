package net.prezz.mpr.ui.playlists

import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import net.prezz.mpr.R
import net.prezz.mpr.Utils
import net.prezz.mpr.databinding.ActivityPlaylistDetailsBinding
import net.prezz.mpr.model.MusicPlayerControl
import net.prezz.mpr.model.PlayerState
import net.prezz.mpr.model.PlaylistEntity
import net.prezz.mpr.model.ResponseReceiver
import net.prezz.mpr.model.ResponseResult
import net.prezz.mpr.model.StoredPlaylistEntity
import net.prezz.mpr.model.TaskHandle
import net.prezz.mpr.model.UriEntity
import net.prezz.mpr.model.UriEntity.FileType
import net.prezz.mpr.model.UriEntity.UriType
import net.prezz.mpr.model.command.AddUriToPlaylistCommand
import net.prezz.mpr.model.command.AddUriToStoredPlaylistCommand
import net.prezz.mpr.model.command.ClearPlaylistCommand
import net.prezz.mpr.model.command.Command
import net.prezz.mpr.model.command.DeleteFromStoredPlaylistCommand
import net.prezz.mpr.model.command.LoadStoredPlaylistCommand
import net.prezz.mpr.model.command.MoveInStoredPlaylistCommand
import net.prezz.mpr.model.command.PlayCommand
import net.prezz.mpr.model.command.PrioritizeUriCommand
import net.prezz.mpr.model.command.UpdatePrioritiesCommand
import net.prezz.mpr.ui.adapter.PlaylistAdapterEntity
import net.prezz.mpr.ui.adapter.PlaylistArrayAdapter
import net.prezz.mpr.ui.helpers.Boast
import net.prezz.mpr.ui.helpers.MiniControlHelper
import net.prezz.mpr.ui.helpers.VolumeButtonsHelper
import net.prezz.mpr.ui.helpers.setupToolbar
import net.prezz.mpr.ui.state.DataState
import net.prezz.mpr.ui.view.DragListView

class PlaylistDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlaylistDetailsBinding

    private var adapterEntities: Array<PlaylistAdapterEntity>? = null
    private var updating = false
    private var updatingPlaylistsHandle: TaskHandle = TaskHandle.NULL_HANDLE
    private var addUrlHandle: TaskHandle = TaskHandle.NULL_HANDLE
    private lateinit var controlHelper: MiniControlHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityPlaylistDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupToolbar(showUpButton = true)

        updatingPlaylistsHandle = TaskHandle.NULL_HANDLE

        val title = getPlaylistArgument().playlistName
        setTitle(title)

        val dataState = DataState.get(this)
        // restore entities if loaded into memory again (or after rotation)
        @Suppress("UNCHECKED_CAST")
        (dataState.getData(ENTITIES_SAVED_INSTANCE_STATE, null) as? Array<PlaylistAdapterEntity>)?.let {
            adapterEntities = it
        }

        controlHelper = MiniControlHelper(this)

        binding.playlistDetailsButtonChoiceMenu.setOnClickListener { onChoiceMenuClick(it) }
        binding.playlistDetailsButtonControlMenu.setOnClickListener { onControlMenuClick(it) }

        updateEntities()
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)

        val listView = findListView()
        listView.setOnItemLongClickListener { _, _, position, _ ->
            showContextMenu(position)
            true
        }

        listView.setDropListener(EntityDropListener())
        listView.setRemoveListener(EntityRemoveListener())
    }

    override fun onPause() {
        super.onPause()

        controlHelper.hideVisibility()
    }

    override fun onStop() {
        super.onStop()

        updatingPlaylistsHandle.cancelTask()
        addUrlHandle.cancelTask()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        val dataFragment = DataState.get(this)
        dataFragment.setData(ENTITIES_SAVED_INSTANCE_STATE, adapterEntities)

        super.onSaveInstanceState(outState)
    }

    private fun showContextMenu(position: Int) {
        val entities = adapterEntities ?: return
        val adapterEntity = entities[position]
        val playlistEntity = adapterEntity.getEntity()
        val menuItems = resources.getStringArray(R.array.playlist_details_selected_menu)
        MaterialAlertDialogBuilder(this)
            .setTitle(adapterEntity.getText())
            .setItems(menuItems) { _, which ->
                val commandList = ArrayList<Command>()
                when (which) {
                    0 -> {
                        val displayText = getString(R.string.playlist_details_added_to_playlist_toast, adapterEntity.getText())
                        commandList.add(AddUriToPlaylistCommand(playlistEntity.getUriEntity()))
                        commandList.add(UpdatePrioritiesCommand())
                        sendControlCommands(displayText, commandList)
                    }
                    1 -> {
                        val displayText = getString(R.string.playlist_details_added_to_playlist_toast, adapterEntity.getText())
                        commandList.add(PrioritizeUriCommand(playlistEntity.getUriEntity()))
                        sendControlCommands(displayText, commandList)
                    }
                    2 -> {
                        val displayText = getString(R.string.playlist_details_added_to_playlist_toast, adapterEntity.getText())
                        commandList.add(ClearPlaylistCommand())
                        commandList.add(AddUriToPlaylistCommand(playlistEntity.getUriEntity()))
                        sendControlCommands(displayText, commandList)
                    }
                    3 -> {
                        val album = adapterEntity.getEntity().getAlbum()
                        val displayText = getString(R.string.playlist_details_added_to_playlist_toast, album ?: "")
                        commandList.add(ClearPlaylistCommand())
                        for (i in entities.indices) {
                            val entity = entities[i].getEntity()
                            if (album == entity.getAlbum()) {
                                commandList.add(AddUriToPlaylistCommand(entity.getUriEntity()))
                            }
                        }
                        sendControlCommands(displayText, commandList)
                    }
                    4 -> removeTrack(position)
                    5 -> removeAlbum(position)
                }
            }
            .show()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (VolumeButtonsHelper.handleKeyDown(this, keyCode, event)) {
            return true
        }

        return super.onKeyDown(keyCode, event)
    }

    private fun onChoiceMenuClick(view: View) {
        if (adapterEntities != null) {
            val items = resources.getStringArray(R.array.playlist_details_choice_menu)

            MaterialAlertDialogBuilder(this).apply {
                setTitle(title)
                setItems(items) { _, item ->
                    val playlistEntity = getPlaylistArgument()
                    val displayText = getString(R.string.playlist_details_loaded_playlist_toast, title)

                    val commandList = ArrayList<Command>()
                    when (item) {
                        0 -> {
                            commandList.add(LoadStoredPlaylistCommand(playlistEntity))
                            commandList.add(UpdatePrioritiesCommand())
                            sendControlCommands(displayText, commandList)
                        }
                        1 -> {
                            commandList.add(ClearPlaylistCommand())
                            commandList.add(LoadStoredPlaylistCommand(playlistEntity))
                            sendControlCommands(displayText, commandList)
                        }
                        2 -> {
                            commandList.add(ClearPlaylistCommand())
                            commandList.add(LoadStoredPlaylistCommand(playlistEntity))
                            commandList.add(PlayCommand())
                            sendControlCommands(displayText, commandList)
                        }
                        3 -> removeDuplicateTracks()
                        4 -> addUrlToPlaylist()
                    }
                }
            }.create().show()
        }
    }

    private fun onControlMenuClick(view: View) {
        controlHelper.toggleVisibility()
    }

    private fun updateEntities() {
        val existing = adapterEntities
        if (existing != null) {
            createEntityAdapter(existing)
            setActivityTitle()
        } else if (!updating) {
            showUpdatingIndicator()
            updatingPlaylistsHandle.cancelTask()
            updatingPlaylistsHandle = MusicPlayerControl.getPlaylistDetails(getPlaylistArgument(), object : ResponseReceiver<Array<PlaylistEntity>>() {
                override fun receiveResponse(response: Array<PlaylistEntity>) {
                    val entities = createAdapterEntities(response)
                    adapterEntities = entities
                    createEntityAdapter(entities)
                    hideUpdatingIndicator()
                    setActivityTitle()
                }
            })
        }
    }

    private fun createAdapterEntities(entities: Array<PlaylistEntity>): Array<PlaylistAdapterEntity> {
        return Array(entities.size) { PlaylistAdapterEntity(entities[it], false) }
    }

    private fun createEntityAdapter(adapterEntities: Array<PlaylistAdapterEntity>) {
        val listView = findListView()
        val adapter = createAdapter(adapterEntities)
        listView.adapter = adapter
    }

    private fun createAdapter(adapterEntities: Array<PlaylistAdapterEntity>): PlaylistArrayAdapter {
        return PlaylistArrayAdapter(this, android.R.layout.simple_list_item_2, ArrayList(adapterEntities.asList()))
    }

    private fun sendControlCommands(displayText: CharSequence, commands: List<Command>) {
        MusicPlayerControl.sendControlCommands(commands)
        Boast.makeText(this, displayText).show()
    }

    private fun findListView(): DragListView {
        return binding.playlistDetailsListViewBrowse
    }

    private fun showUpdatingIndicator() {
        updating = true
        binding.playlistDetailsProgressBarLoad.visibility = View.VISIBLE
    }

    private fun hideUpdatingIndicator() {
        updating = false
        binding.playlistDetailsProgressBarLoad.visibility = View.GONE
    }

    private fun getPlaylistArgument(): StoredPlaylistEntity {
        return this.intent.extras!!.getSerializable(PLAYLIST_ARGUMENT_KEY, StoredPlaylistEntity::class.java)!!
    }

    private fun refreshEntities(refreshLocalEntities: Boolean) {
        val listView = findListView()
        if (refreshLocalEntities) {
            // first update the list view with what we assume is the result of the move
            val adapter = listView.adapter as PlaylistArrayAdapter
            adapter.setData(adapterEntities, -1, PlayerState.STOP)
        }

        // then re-query the playlist from server just in case
        updatingPlaylistsHandle.cancelTask()
        updatingPlaylistsHandle = MusicPlayerControl.getPlaylistDetails(getPlaylistArgument(), object : ResponseReceiver<Array<PlaylistEntity>>() {
            override fun receiveResponse(response: Array<PlaylistEntity>) {
                val entities = createAdapterEntities(response)
                adapterEntities = entities
                val innerListView = findListView()
                val adapter = innerListView.adapter as PlaylistArrayAdapter
                adapter.setData(entities, -1, PlayerState.STOP)
                setActivityTitle()
            }
        })
    }

    private fun removeDuplicateTracks() {
        val entities = adapterEntities ?: return
        val toDelete = ArrayList<Int>()

        val uriSet = HashSet<String>()
        for (i in entities.indices) {
            val entity = entities[i].getEntity()
            val uri = entity.getUriEntity().getFullUriPath(false)
            if (uriSet.contains(uri)) {
                toDelete.add(i)
            } else {
                uriSet.add(uri)
            }
        }

        if (toDelete.isNotEmpty()) {
            val commands = ArrayList<Command>(toDelete.size)

            val playlist = getPlaylistArgument()
            for (i in toDelete.indices.reversed()) {
                val pos = toDelete[i]
                commands.add(DeleteFromStoredPlaylistCommand(playlist, pos))
            }

            MusicPlayerControl.sendControlCommands(commands)
            refreshEntities(false)
        }

        Boast.makeText(this, getString(R.string.playlist_details_duplicates_removed_toast, toDelete.size)).show()
    }

    private fun addUrlToPlaylist() {
        val editTextView = EditText(this)

        val dialog = MaterialAlertDialogBuilder(this).apply {
            setTitle(R.string.playlist_details_add_url_title)
            setView(editTextView)
            setPositiveButton(android.R.string.ok) { _, _ ->
                val uri = editTextView.text.toString()
                if (!uri.isNullOrEmpty()) {
                    val command = AddUriToStoredPlaylistCommand(getPlaylistArgument(), UriEntity(UriType.FILE, FileType.MUSIC, "", uri))
                    addUrlHandle.cancelTask()
                    addUrlHandle = MusicPlayerControl.sendControlCommand(command, object : ResponseReceiver<ResponseResult>() {
                        override fun receiveResponse(response: ResponseResult) {
                            refreshEntities(false)
                            if (!response.isSuccess) {
                                Boast.makeText(this@PlaylistDetailsActivity, R.string.playlist_details_added_error_url_toast).show()
                            }
                        }
                    })
                    Boast.makeText(this@PlaylistDetailsActivity, getString(R.string.playlist_details_added_url_toast, uri)).show()
                }
            }
            setNegativeButton(android.R.string.cancel) { _, _ -> }
        }.create()
        editTextView.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
            }
        }
        dialog.show()
    }

    private fun setActivityTitle() {
        var title = getPlaylistArgument().playlistName
        title += getPlaylistTime()
        setTitle(title)
    }

    private fun getPlaylistTime(): String {
        var totalTime = 0

        adapterEntities?.let { entities ->
            for (i in entities.indices) {
                val time = entities[i].getEntity().getTime()
                if (time != null) {
                    totalTime += time
                }
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

    private fun removeTrack(which: Int) {
        val entities = adapterEntities ?: return
        val newEntities = arrayOfNulls<PlaylistAdapterEntity>(entities.size - 1)
        System.arraycopy(entities, 0, newEntities, 0, which)
        System.arraycopy(entities, which + 1, newEntities, which, newEntities.size - which)
        MusicPlayerControl.sendControlCommand(DeleteFromStoredPlaylistCommand(getPlaylistArgument(), which))
        @Suppress("UNCHECKED_CAST")
        adapterEntities = newEntities as Array<PlaylistAdapterEntity>
        refreshEntities(true)
    }

    private fun removeAlbum(which: Int) {
        val entities = adapterEntities ?: return
        val toDelete = ArrayList<Int>()

        val album = entities[which].getEntity().getAlbum()

        for (i in entities.indices) {
            val entity = entities[i].getEntity()
            if (album == entity.getAlbum()) {
                toDelete.add(i)
            }
        }

        if (toDelete.isNotEmpty()) {
            val commands = ArrayList<Command>(toDelete.size)

            val playlist = getPlaylistArgument()
            for (i in toDelete.indices.reversed()) {
                val pos = toDelete[i]
                commands.add(DeleteFromStoredPlaylistCommand(playlist, pos))
            }

            MusicPlayerControl.sendControlCommands(commands)
            refreshEntities(false)
        }
    }

    private inner class EntityDropListener : DragListView.DropListener {

        override fun drop(from: Int, to: Int) {
            val entities = adapterEntities ?: return
            val movingEntity = entities[from]
            if (from > to) { // moving up in list
                System.arraycopy(entities, to, entities, to + 1, from - to)
                entities[to] = movingEntity
                MusicPlayerControl.sendControlCommand(MoveInStoredPlaylistCommand(getPlaylistArgument(), from, to))
                refreshEntities(true)
            } else if (from < to) { // moving down in list
                System.arraycopy(entities, from + 1, entities, from, to - from)
                entities[to] = movingEntity
                MusicPlayerControl.sendControlCommand(MoveInStoredPlaylistCommand(getPlaylistArgument(), from, to))
                refreshEntities(true)
            }
        }
    }

    private inner class EntityRemoveListener : DragListView.RemoveListener {

        override fun remove(which: Int) {
            removeTrack(which)
        }
    }

    companion object {
        const val PLAYLIST_ARGUMENT_KEY = "playlistArgument"

        private const val ENTITIES_SAVED_INSTANCE_STATE = "storedPlaylistEntities"
    }
}
