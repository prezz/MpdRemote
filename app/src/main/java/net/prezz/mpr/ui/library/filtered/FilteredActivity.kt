package net.prezz.mpr.ui.library.filtered

import com.google.android.material.dialog.MaterialAlertDialogBuilder
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
import net.prezz.mpr.model.LibraryEntity
import net.prezz.mpr.model.MusicPlayerControl
import net.prezz.mpr.model.ResponseReceiver
import net.prezz.mpr.model.TaskHandle
import net.prezz.mpr.model.command.AddToPlaylistCommand
import net.prezz.mpr.model.command.ClearPlaylistCommand
import net.prezz.mpr.model.command.Command
import net.prezz.mpr.model.command.PlayCommand
import net.prezz.mpr.model.command.PrioritizeCommand
import net.prezz.mpr.model.command.UpdatePrioritiesCommand
import net.prezz.mpr.ui.adapter.AdapterEntity
import net.prezz.mpr.ui.adapter.LibraryAdapterEntity
import net.prezz.mpr.ui.helpers.AddToStoredPlaylistHelper
import net.prezz.mpr.ui.helpers.Boast
import net.prezz.mpr.ui.helpers.MiniControlHelper
import net.prezz.mpr.ui.helpers.VolumeButtonsHelper
import net.prezz.mpr.ui.helpers.setupToolbar
import net.prezz.mpr.ui.state.DataState

abstract class FilteredActivity : AppCompatActivity(), OnItemClickListener {

    protected var adapterEntities: Array<AdapterEntity>? = null
    private var updating = false
    private var getFromLibraryHandle: TaskHandle = TaskHandle.NULL_HANDLE
    private lateinit var controlHelper: MiniControlHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(getLayout())
        setupToolbar(showUpButton = true)

        getFromLibraryHandle = TaskHandle.NULL_HANDLE

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

    override fun onPause() {
        super.onPause()

        controlHelper.hideVisibility()
    }

    override fun onStop() {
        super.onStop()

        getFromLibraryHandle.cancelTask()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        val dataState = DataState.get(this)
        dataState.setData(ENTITIES_SAVED_INSTANCE_STATE, adapterEntities)

        super.onSaveInstanceState(outState)
    }

    private fun showContextMenu(position: Int) {
        val entity = adapterEntities?.get(position) ?: return
        if (entity is LibraryAdapterEntity) {
            val libraryEntity = entity.getEntity()
            val displayText = getString(R.string.library_added_to_playlist_toast, entity.getText())
            val menuItems = getContextMenuItems(libraryEntity)
            MaterialAlertDialogBuilder(this)
                .setTitle(entity.getText())
                .setItems(menuItems) { _, which ->
                    val commandList = ArrayList<Command>()
                    when (which) {
                        0 -> {
                            commandList.add(AddToPlaylistCommand(libraryEntity))
                            commandList.add(UpdatePrioritiesCommand())
                            sendControlCommands(displayText, commandList)
                        }
                        1 -> {
                            commandList.add(PrioritizeCommand(libraryEntity))
                            sendControlCommands(displayText, commandList)
                        }
                        2 -> {
                            commandList.add(ClearPlaylistCommand())
                            commandList.add(AddToPlaylistCommand(libraryEntity))
                            sendControlCommands(displayText, commandList)
                        }
                        3 -> {
                            commandList.add(ClearPlaylistCommand())
                            commandList.add(AddToPlaylistCommand(libraryEntity))
                            commandList.add(PlayCommand())
                            sendControlCommands(displayText, commandList)
                        }
                        4 -> AddToStoredPlaylistHelper.addToStoredPlaylist(this, displayText, libraryEntity)
                    }
                }
                .show()
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
                            commandList.add(AddToPlaylistCommand(entity))
                            commandList.add(UpdatePrioritiesCommand())
                            sendControlCommands(displayText, commandList)
                        }
                        1 -> {
                            commandList.add(PrioritizeCommand(entity))
                            sendControlCommands(displayText, commandList)
                        }
                        2 -> {
                            commandList.add(ClearPlaylistCommand())
                            commandList.add(AddToPlaylistCommand(entity))
                            sendControlCommands(displayText, commandList)
                        }
                        3 -> {
                            commandList.add(ClearPlaylistCommand())
                            commandList.add(AddToPlaylistCommand(entity))
                            commandList.add(PlayCommand())
                            sendControlCommands(displayText, commandList)
                        }
                        4 -> {
                            AddToStoredPlaylistHelper.addToStoredPlaylist(this@FilteredActivity, displayText, entity)
                        }
                    }
                }
            }
        }.create().show()
    }

    private fun onControlMenuClick(view: View) {
        controlHelper.toggleVisibility()
    }

    protected open fun getContextMenuItems(entity: LibraryEntity): Array<String> {
        return resources.getStringArray(R.array.library_selected_menu)
    }

    protected fun getAdapterEntity(pos: Int): AdapterEntity {
        return adapterEntities!![pos]
    }

    protected fun getEntityArgument(): LibraryEntity? {
        return this.intent.extras?.getSerializable(ENTITY_ARGUMENT_KEY, LibraryEntity::class.java)
    }

    protected abstract fun getLayout(): Int

    protected abstract fun getEntities(entity: LibraryEntity, responseReceiver: ResponseReceiver<Array<LibraryEntity>>): TaskHandle

    protected abstract fun createAdapterEntities(entities: Array<LibraryEntity>): Array<AdapterEntity>

    protected abstract fun createAdapter(adapterEntities: Array<AdapterEntity>): ListAdapter

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
                getFromLibraryHandle = getEntities(entity, object : ResponseReceiver<Array<LibraryEntity>>() {
                    override fun receiveResponse(response: Array<LibraryEntity>) {
                        val entities = createAdapterEntities(response)
                        adapterEntities = entities
                        createEntityAdapter(entities)
                        hideUpdatingIndicator()
                    }
                })
            }
        }
    }

    private fun createEntityAdapter(adapterEntities: Array<AdapterEntity>) {
        val listView = findListView()
        val adapter = createAdapter(adapterEntities)
        listView.adapter = adapter
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
