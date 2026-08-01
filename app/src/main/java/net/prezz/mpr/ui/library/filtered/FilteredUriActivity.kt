package net.prezz.mpr.ui.library.filtered

import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.ListAdapter
import android.widget.ListView
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import net.prezz.mpr.R
import net.prezz.mpr.model.MusicPlayerControl
import net.prezz.mpr.model.ResponseReceiver
import net.prezz.mpr.model.StoredPlaylistEntity
import net.prezz.mpr.model.TaskHandle
import net.prezz.mpr.model.UriEntity
import net.prezz.mpr.model.UriEntity.FileType
import net.prezz.mpr.model.UriEntity.UriType
import net.prezz.mpr.model.command.AddUriToPlaylistCommand
import net.prezz.mpr.model.command.ClearPlaylistCommand
import net.prezz.mpr.model.command.Command
import net.prezz.mpr.model.command.LoadStoredPlaylistCommand
import net.prezz.mpr.model.command.PlayCommand
import net.prezz.mpr.model.command.PrioritizeUriCommand
import net.prezz.mpr.model.command.UpdatePrioritiesCommand
import net.prezz.mpr.ui.adapter.AdapterEntity
import net.prezz.mpr.ui.adapter.LibraryArrayAdapter
import net.prezz.mpr.ui.adapter.SectionAdapterEntity
import net.prezz.mpr.ui.adapter.SectionSortedAdapterIndexStrategy
import net.prezz.mpr.ui.adapter.UriAdapterEntity
import net.prezz.mpr.ui.helpers.AddToStoredPlaylistHelper
import net.prezz.mpr.ui.helpers.Boast
import net.prezz.mpr.ui.helpers.MiniControlHelper
import net.prezz.mpr.ui.helpers.VolumeButtonsHelper
import net.prezz.mpr.ui.helpers.setupToolbar
import net.prezz.mpr.ui.state.DataState

class FilteredUriActivity : AppCompatActivity(), OnItemClickListener {

    private var adapterEntities: Array<AdapterEntity>? = null
    private var updating = false
    private var getFromLibraryHandle: TaskHandle = TaskHandle.NULL_HANDLE
    private lateinit var controlHelper: MiniControlHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_filtered)
        setupToolbar(showUpButton = true)

        val title = this.intent.extras?.getString(TITLE_ARGUMENT_KEY)
        setTitle(title)

        val dataState = DataState.get(this)
        // restore entities if loaded into memory again (or after rotation)
        @Suppress("UNCHECKED_CAST")
        (dataState.getData(ENTITIES_SAVED_INSTANCE_STATE, null) as? Array<AdapterEntity>)?.let {
            adapterEntities = it
        }

        controlHelper = MiniControlHelper(this)

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

        findViewById<View>(R.id.filtered_button_choice_menu).setOnClickListener { onChoiceMenuClick(it) }
        findViewById<View>(R.id.library_button_control_menu).setOnClickListener { onControlMenuClick(it) }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        val dataFragment = DataState.get(this)
        dataFragment.setData(ENTITIES_SAVED_INSTANCE_STATE, adapterEntities)

        super.onSaveInstanceState(outState)
    }

    private fun showContextMenu(position: Int) {
        val entity = adapterEntities?.get(position) ?: return
        if (entity is UriAdapterEntity) {
            val uriEntity = entity.getEntity()
            val displayText = getString(R.string.library_added_to_playlist_toast, entity.getText())
            val menuItems = resources.getStringArray(if (uriEntity.fileType == FileType.PLAYLIST) R.array.library_playlist_selected_menu else R.array.library_selected_menu)
            MaterialAlertDialogBuilder(this)
                .setTitle(entity.getText())
                .setItems(menuItems) { _, which ->
                    val commandList = ArrayList<Command>()
                    // The playlist menu omits "prioritize", so shift indices past 0 to the full action set.
                    var itemId = which
                    if (uriEntity.fileType == FileType.PLAYLIST && which > 0) {
                        itemId += 1
                    }
                    when (itemId) {
                        0 -> {
                            if (uriEntity.fileType == UriEntity.FileType.PLAYLIST) {
                                commandList.add(LoadStoredPlaylistCommand(StoredPlaylistEntity(uriEntity.getFullUriPath(false))))
                            } else {
                                commandList.add(AddUriToPlaylistCommand(uriEntity))
                            }
                            commandList.add(UpdatePrioritiesCommand())
                            sendControlCommands(displayText, commandList)
                        }
                        1 -> {
                            commandList.add(PrioritizeUriCommand(uriEntity))
                            sendControlCommands(displayText, commandList)
                        }
                        2 -> {
                            commandList.add(ClearPlaylistCommand())
                            if (uriEntity.fileType == UriEntity.FileType.PLAYLIST) {
                                commandList.add(LoadStoredPlaylistCommand(StoredPlaylistEntity(uriEntity.getFullUriPath(false))))
                            } else {
                                commandList.add(AddUriToPlaylistCommand(uriEntity))
                            }
                            sendControlCommands(displayText, commandList)
                        }
                        3 -> {
                            commandList.add(ClearPlaylistCommand())
                            if (uriEntity.fileType == UriEntity.FileType.PLAYLIST) {
                                commandList.add(LoadStoredPlaylistCommand(StoredPlaylistEntity(uriEntity.getFullUriPath(false))))
                            } else {
                                commandList.add(AddUriToPlaylistCommand(uriEntity))
                            }
                            commandList.add(PlayCommand())
                            sendControlCommands(displayText, commandList)
                        }
                        4 -> AddToStoredPlaylistHelper.addUriToStoredPlaylist(this, displayText, uriEntity)
                    }
                }
                .show()
        }
    }

    override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        val adapterEntity = adapterEntities?.get(position) ?: return

        if (adapterEntity is UriAdapterEntity) {
            val entity = adapterEntity.getEntity()
            if (entity.uriType == UriType.DIRECTORY) {
                val intent = Intent(this, FilteredUriActivity::class.java)
                val args = Bundle()
                args.putString(TITLE_ARGUMENT_KEY, entity.getFullUriPath(false))
                args.putSerializable(ENTITY_ARGUMENT_KEY, entity)
                intent.putExtras(args)
                startActivity(intent)
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (VolumeButtonsHelper.handleKeyDown(this, keyCode, event)) {
            return true
        }

        return super.onKeyDown(keyCode, event)
    }

    private fun onChoiceMenuClick(view: View) {
        val title = getTitle()
        val items = resources.getStringArray(R.array.library_selected_menu)
        for (i in items.indices) {
            items[i] = String.format(items[i], title)
        }

        MaterialAlertDialogBuilder(this).apply {
            setTitle(title)
            setItems(items) { _, item ->
                if (adapterEntities != null) {
                    val commandList = ArrayList<Command>()
                    val displayText = getString(R.string.library_added_to_playlist_toast, title)
                    val entity = getEntityArgument()!!
                    when (item) {
                        0 -> {
                            commandList.add(AddUriToPlaylistCommand(entity))
                            commandList.add(UpdatePrioritiesCommand())
                            sendControlCommands(displayText, commandList)
                        }
                        1 -> {
                            commandList.add(PrioritizeUriCommand(entity))
                            sendControlCommands(displayText, commandList)
                        }
                        2 -> {
                            commandList.add(ClearPlaylistCommand())
                            commandList.add(AddUriToPlaylistCommand(entity))
                            sendControlCommands(displayText, commandList)
                        }
                        3 -> {
                            commandList.add(ClearPlaylistCommand())
                            commandList.add(AddUriToPlaylistCommand(entity))
                            commandList.add(PlayCommand())
                            sendControlCommands(displayText, commandList)
                        }
                        4 -> {
                            AddToStoredPlaylistHelper.addUriToStoredPlaylist(this@FilteredUriActivity, displayText, entity)
                        }
                    }
                }
            }
        }.create().show()
    }

    private fun onControlMenuClick(view: View) {
        controlHelper.toggleVisibility()
    }

    override fun onPause() {
        super.onPause()

        controlHelper.hideVisibility()
    }

    override fun onStop() {
        super.onStop()

        // Cancel the in-flight library load so its callback doesn't update a stopped activity.
        getFromLibraryHandle.cancelTask()
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
            val entity = getEntityArgument()
            if (entity != null) {
                showUpdatingIndicator()
                getFromLibraryHandle.cancelTask()
                getFromLibraryHandle = MusicPlayerControl.getUriFromLibrary(entity, null, object : ResponseReceiver<Array<UriEntity>>() {
                    override fun receiveResponse(response: Array<UriEntity>) {
                        val entities = createAdapterEntities(response)
                        adapterEntities = entities
                        createEntityAdapter(entities)
                        hideUpdatingIndicator()
                    }
                })
            }
        }
    }

    private fun createAdapterEntities(entities: Array<UriEntity>): Array<AdapterEntity> {
        var addDirSection = true
        var addFileSection = true
        val result = ArrayList<AdapterEntity>(entities.size + 2)

        for (i in entities.indices) {
            if (entities[i].uriType == UriType.DIRECTORY) {
                if (addDirSection) {
                    result.add(SectionAdapterEntity(getString(R.string.library_directories_section)))
                }
                addDirSection = false
                result.add(UriAdapterEntity(entities[i]))
            }
            if (entities[i].uriType == UriType.FILE) {
                if (addFileSection) {
                    result.add(SectionAdapterEntity(getString(R.string.library_files_section)))
                }
                addFileSection = false
                result.add(UriAdapterEntity(entities[i]))
            }
        }

        return result.toTypedArray()
    }

    private fun createEntityAdapter(adapterEntities: Array<AdapterEntity>) {
        val listView = findListView()
        val adapter = createAdapter(adapterEntities)
        listView.adapter = adapter
    }

    private fun createAdapter(adapterEntities: Array<AdapterEntity>): ListAdapter {
        return LibraryArrayAdapter(this, android.R.layout.simple_list_item_1, adapterEntities, SectionSortedAdapterIndexStrategy, false)
    }

    private fun getEntityArgument(): UriEntity? {
        return this.intent.extras?.getSerializable(ENTITY_ARGUMENT_KEY, UriEntity::class.java)
    }

    private fun findListView(): ListView {
        return this.findViewById(R.id.filtered_list_view_browse)
    }

    private fun showUpdatingIndicator() {
        updating = true
        val progressBar = findProgressBar()
        progressBar?.visibility = View.VISIBLE
    }

    private fun hideUpdatingIndicator() {
        updating = false
        val progressBar = findProgressBar()
        progressBar?.visibility = View.GONE
    }

    private fun findProgressBar(): ProgressBar? {
        return this.findViewById(R.id.filtered_progress_bar_load)
    }

    companion object {
        const val TITLE_ARGUMENT_KEY = "title"
        const val ENTITY_ARGUMENT_KEY = "entity"

        private const val ENTITIES_SAVED_INSTANCE_STATE = "adapterEntities"
    }
}
