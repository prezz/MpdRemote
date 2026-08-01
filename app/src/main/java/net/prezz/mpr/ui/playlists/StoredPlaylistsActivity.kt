package net.prezz.mpr.ui.playlists

import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import net.prezz.mpr.R
import net.prezz.mpr.Utils
import net.prezz.mpr.databinding.ActivityStoredPlaylistsBinding
import net.prezz.mpr.model.MusicPlayerControl
import net.prezz.mpr.model.ResponseReceiver
import net.prezz.mpr.model.ResponseResult
import net.prezz.mpr.model.StoredPlaylistEntity
import net.prezz.mpr.model.TaskHandle
import net.prezz.mpr.model.command.ClearPlaylistCommand
import net.prezz.mpr.model.command.Command
import net.prezz.mpr.model.command.DeleteStoredPlaylistCommand
import net.prezz.mpr.model.command.LoadStoredPlaylistCommand
import net.prezz.mpr.model.command.PlayCommand
import net.prezz.mpr.model.command.SaveCurrentPlaylistCommand
import net.prezz.mpr.model.command.UpdatePrioritiesCommand
import net.prezz.mpr.ui.adapter.StoredPlaylistAdapterEntity
import net.prezz.mpr.ui.helpers.Boast
import net.prezz.mpr.ui.helpers.MiniControlHelper
import net.prezz.mpr.ui.helpers.VolumeButtonsHelper
import net.prezz.mpr.ui.helpers.setupToolbar
import net.prezz.mpr.ui.state.DataState

class StoredPlaylistsActivity : AppCompatActivity(), OnItemClickListener {

    private lateinit var binding: ActivityStoredPlaylistsBinding

    private val refreshResponseReceiver = RefreshEntitiesResponseReceiver()

    private var adapterEntities: Array<StoredPlaylistAdapterEntity>? = null
    private var updating = false
    private var updatingPlaylistsHandle: TaskHandle = TaskHandle.NULL_HANDLE
    private lateinit var controlHelper: MiniControlHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityStoredPlaylistsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupToolbar(showUpButton = true)

        val dataState = DataState.get(this)
        // restore entities if loaded into memory again (or after rotation)
        @Suppress("UNCHECKED_CAST")
        (dataState.getData(ENTITIES_SAVED_INSTANCE_STATE, null) as? Array<StoredPlaylistAdapterEntity>)?.let {
            adapterEntities = it
        }

        controlHelper = MiniControlHelper(this)

        binding.storedPlaylistsButtonChoiceMenu.setOnClickListener { onChoiceMenuClick(it) }
        binding.storedPlaylistsButtonControlMenu.setOnClickListener { onControlMenuClick(it) }

        updateEntities()
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)

        val listView = findListView()
        listView.onItemClickListener = this
        listView.setOnItemLongClickListener { _, _, position, _ ->
            showContextMenu(position)
            true
        }
    }

    override fun onPause() {
        super.onPause()

        controlHelper.hideVisibility()
    }

    override fun onStop() {
        super.onStop()

        updatingPlaylistsHandle.cancelTask()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        val dataState = DataState.get(this)
        dataState.setData(ENTITIES_SAVED_INSTANCE_STATE, adapterEntities)

        super.onSaveInstanceState(outState)
    }

    private fun showContextMenu(position: Int) {
        val adapterEntity = adapterEntities?.get(position) ?: return
        val playlistEntity = adapterEntity.getEntity()
        val displayText = getString(R.string.stored_playlists_loaded_playlist_toast, adapterEntity.toString())
        val menuItems = resources.getStringArray(R.array.stored_playlists_selected_menu)
        MaterialAlertDialogBuilder(this)
            .setTitle(adapterEntity.toString())
            .setItems(menuItems) { _, which ->
                val commandList = ArrayList<Command>()
                when (which) {
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
                }
            }
            .show()
    }

    override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        val storedPlaylist = adapterEntities?.get(position) ?: return
        val intent = Intent(this, PlaylistDetailsActivity::class.java)
        val args = Bundle()
        args.putSerializable(PlaylistDetailsActivity.PLAYLIST_ARGUMENT_KEY, storedPlaylist.getEntity())
        intent.putExtras(args)
        startActivity(intent)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (VolumeButtonsHelper.handleKeyDown(this, keyCode, event)) {
            return true
        }

        return super.onKeyDown(keyCode, event)
    }

    private fun onChoiceMenuClick(view: View) {
        if (adapterEntities != null) {
            val items = resources.getStringArray(R.array.stored_playlists_choice_menu)

            MaterialAlertDialogBuilder(this).apply {
                setTitle(title)
                setItems(items) { _, item ->
                    when (item) {
                        0 -> savePlaylist(items[0])
                        1 -> deletePlaylists(items[1])
                    }
                }
            }.create().show()
        }
    }

    private fun onControlMenuClick(view: View) {
        controlHelper.toggleVisibility()
    }

    private fun sendControlCommands(displayText: CharSequence, commands: List<Command>) {
        MusicPlayerControl.sendControlCommands(commands)
        Boast.makeText(this, displayText).show()
    }

    private fun updateEntities() {
        val existing = adapterEntities
        if (existing != null) {
            createEntityAdapter(existing)
        } else if (!updating) {
            showUpdatingIndicator()
            updatingPlaylistsHandle.cancelTask()
            updatingPlaylistsHandle = MusicPlayerControl.getStoredPlaylists(object : ResponseReceiver<Array<StoredPlaylistEntity>>() {
                override fun receiveResponse(response: Array<StoredPlaylistEntity>) {
                    val entities = createAdapterEntities(response)
                    adapterEntities = entities
                    createEntityAdapter(entities)
                    hideUpdatingIndicator()
                }
            })
        }
    }

    private fun createAdapterEntities(entities: Array<StoredPlaylistEntity>): Array<StoredPlaylistAdapterEntity> {
        return Array(entities.size) { StoredPlaylistAdapterEntity(entities[it]) }
    }

    private fun createEntityAdapter(adapterEntities: Array<StoredPlaylistAdapterEntity>) {
        val listView = findListView()
        val adapter = createAdapter(adapterEntities)
        listView.adapter = adapter
    }

    private fun createAdapter(adapterEntities: Array<StoredPlaylistAdapterEntity>): ArrayAdapter<StoredPlaylistAdapterEntity> {
        return ArrayAdapter(this, R.layout.view_list_item_single_line, ArrayList(adapterEntities.asList()))
    }

    private fun findListView(): ListView {
        return binding.storedPlaylistsListViewBrowse
    }

    private fun showUpdatingIndicator() {
        updating = true
        binding.storedPlaylistsProgressBarLoad.visibility = View.VISIBLE
    }

    private fun hideUpdatingIndicator() {
        updating = false
        binding.storedPlaylistsProgressBarLoad.visibility = View.GONE
    }

    private fun savePlaylist(header: String) {
        val editTextView = EditText(this)
        editTextView.setSingleLine()

        val dialog = MaterialAlertDialogBuilder(this).apply {
            setTitle(header)
            setView(editTextView)
            setPositiveButton(android.R.string.ok) { _, _ ->
                val saveName = editTextView.text.toString()
                if (saveName.isNotEmpty()) {
                    val existing = getExistingPlaylistEntity(saveName)
                    if (existing != null) {
                        MaterialAlertDialogBuilder(this@StoredPlaylistsActivity).apply {
                            setTitle(R.string.stored_playlist_file_exist_title)
                            setMessage(getString(R.string.stored_playlist_file_exist_message, saveName))
                            setPositiveButton(android.R.string.ok) { _, _ ->
                                updatingPlaylistsHandle.cancelTask()
                                val commandList = ArrayList<Command>(listOf(DeleteStoredPlaylistCommand(existing), SaveCurrentPlaylistCommand(saveName)))
                                updatingPlaylistsHandle = MusicPlayerControl.sendControlCommands(commandList, refreshResponseReceiver)
                            }
                            setNegativeButton(android.R.string.cancel) { _, _ -> }
                        }.show()
                    } else {
                        updatingPlaylistsHandle.cancelTask()
                        updatingPlaylistsHandle = MusicPlayerControl.sendControlCommand(SaveCurrentPlaylistCommand(saveName), refreshResponseReceiver)
                    }
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

    private fun deletePlaylists(header: String) {
        val entities = adapterEntities ?: return
        if (entities.isEmpty()) {
            Boast.makeText(this, R.string.stored_playlist_nothing_to_delete_toast).show()
        } else {
            val items = arrayOfNulls<String>(entities.size)
            val checkedItems = BooleanArray(entities.size)
            for (i in entities.indices) {
                items[i] = entities[i].getEntity().playlistName
                checkedItems[i] = false
            }

            MaterialAlertDialogBuilder(this).apply {
                setTitle(header)
                setMultiChoiceItems(items, checkedItems) { _, which, isChecked ->
                    checkedItems[which] = isChecked
                }
                setPositiveButton(android.R.string.ok) { _, _ ->
                    val commands = ArrayList<Command>()
                    for (i in entities.indices) {
                        if (checkedItems[i]) {
                            val entity = entities[i].getEntity()
                            commands.add(DeleteStoredPlaylistCommand(entity))
                        }
                    }
                    if (commands.isNotEmpty()) {
                        updatingPlaylistsHandle.cancelTask()
                        updatingPlaylistsHandle = MusicPlayerControl.sendControlCommands(commands, refreshResponseReceiver)
                    }
                }
            }.create().show()
        }
    }

    private fun getExistingPlaylistEntity(name: String): StoredPlaylistEntity? {
        val entities = adapterEntities ?: return null
        for (i in entities.indices) {
            val entity = entities[i].getEntity()
            if (name == entity.playlistName) {
                return entity
            }
        }
        return null
    }

    private inner class RefreshEntitiesResponseReceiver : ResponseReceiver<ResponseResult>() {

        override fun receiveResponse(response: ResponseResult) {
            if (!response.isSuccess) {
                Boast.makeText(this@StoredPlaylistsActivity, R.string.stored_playlist_server_error_toast).show()
            }

            updatingPlaylistsHandle.cancelTask()
            updatingPlaylistsHandle = MusicPlayerControl.getStoredPlaylists(object : ResponseReceiver<Array<StoredPlaylistEntity>>() {
                override fun receiveResponse(response: Array<StoredPlaylistEntity>) {
                    val entities = createAdapterEntities(response)
                    adapterEntities = entities
                    val listView = findListView()
                    @Suppress("UNCHECKED_CAST")
                    val arrayAdapter = listView.adapter as ArrayAdapter<StoredPlaylistAdapterEntity>
                    arrayAdapter.setNotifyOnChange(false)
                    arrayAdapter.clear()
                    arrayAdapter.addAll(*entities)
                    arrayAdapter.notifyDataSetChanged()
                }
            })
        }
    }

    companion object {
        private const val ENTITIES_SAVED_INSTANCE_STATE = "storedPlaylists"
    }
}
