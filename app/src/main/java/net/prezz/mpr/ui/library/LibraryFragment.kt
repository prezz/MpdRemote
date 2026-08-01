package net.prezz.mpr.ui.library

import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.ListView
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
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
import net.prezz.mpr.databinding.FragmentLibraryBinding
import net.prezz.mpr.ui.adapter.AdapterEntity
import net.prezz.mpr.ui.adapter.LibraryAdapterEntity
import net.prezz.mpr.ui.adapter.LibraryArrayAdapter
import net.prezz.mpr.ui.helpers.AddToStoredPlaylistHelper
import net.prezz.mpr.ui.helpers.Boast
import net.prezz.mpr.ui.state.DataState
import java.util.Random

abstract class LibraryFragment : Fragment(), LibraryCommonsFragment, OnItemClickListener {

    private var _binding: FragmentLibraryBinding? = null

    private var adapterEntities: Array<AdapterEntity>? = null
    private var updating = false
    private var getFromLibraryHandle: TaskHandle = TaskHandle.NULL_HANDLE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        getFromLibraryHandle = TaskHandle.NULL_HANDLE

        val dataState = DataState.get(this)
        @Suppress("UNCHECKED_CAST")
        (dataState.getData(ENTITIES_SAVED_INSTANCE_STATE, null) as? Array<AdapterEntity>)?.let {
            adapterEntities = it
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLibraryBinding.inflate(inflater, container, false)
        return _binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val uriFilterChanged = (requireActivity() as LibraryActivity).attachFragment(this, getFragmentPosition())
        if (uriFilterChanged) {
            adapterEntities = null
        }

        val listView = findListView()
        listView!!.onItemClickListener = this
        listView.setOnItemLongClickListener { _, _, position, _ ->
            showContextMenu(position)
            true
        }

        // ensure to call before onViewStateRestored (i think) as the scroll position then will be restored.
        // if calling it after, the scroll position will be reset as the list is re-populated after the scroll is restored
        updateEntities()
    }

    override fun onStart() {
        super.onStart()

        if (updating) {
            showUpdatingIndicator()
        }
    }

    override fun onDestroyView() {
        getFromLibraryHandle.cancelTask()

        (requireActivity() as LibraryActivity).detachFragment(getFragmentPosition())
        _binding = null
        super.onDestroyView()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        val dataState = DataState.get(this)
        dataState.setData(ENTITIES_SAVED_INSTANCE_STATE, adapterEntities)

        super.onSaveInstanceState(outState)
    }

    private fun showContextMenu(position: Int) {
        val entity = adapterEntities?.get(position) ?: return
        if (entity is LibraryAdapterEntity) {
            val menuItems = getContextMenuItems(entity.getEntity())
            MaterialAlertDialogBuilder(requireActivity())
                .setTitle(entity.getText())
                .setItems(menuItems) { _, which ->
                    onContextMenuItemSelected(which, entity)
                }
                .show()
        }
    }

    // Handles the shared items 0-4; subclasses override to add their own (calling super for the rest).
    protected open fun onContextMenuItemSelected(which: Int, entity: LibraryAdapterEntity) {
        val libraryEntity = entity.getEntity()
        val displayText = getString(R.string.library_added_to_playlist_toast, entity.getText())
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
            4 -> AddToStoredPlaylistHelper.addToStoredPlaylist(requireActivity(), displayText, libraryEntity)
        }
    }

    override fun onChoiceMenuClick(view: View) {
        val items = resources.getStringArray(R.array.library_root_menu)
        val title = requireActivity().title

        MaterialAlertDialogBuilder(requireActivity()).apply {
            setTitle(title)
            setItems(items) { _, item ->
                if (adapterEntities != null) {
                    when (item) {
                        0 -> addRandom(false)
                        1 -> addRandom(true)
                        2 -> {
                            var commandList: MutableList<Command> = ArrayList()
                            val displayText = getString(R.string.library_added_to_playlist_toast, title)
                            commandList.add(ClearPlaylistCommand())
                            commandList = addAll(commandList)
                            sendControlCommands(displayText, commandList)
                        }
                        3 -> {
                            var commandList: MutableList<Command> = ArrayList()
                            val displayText = getString(R.string.library_added_to_playlist_toast, title)
                            commandList.add(ClearPlaylistCommand())
                            commandList = addAll(commandList)
                            commandList.add(PlayCommand())
                            sendControlCommands(displayText, commandList)
                        }
                    }
                }
            }
        }.create().show()
    }

    override fun entitiesChanged() {
        adapterEntities = null
        updateEntities()
    }

    protected fun getAdapterEntity(pos: Int): AdapterEntity {
        return adapterEntities!![pos]
    }

    protected fun addAll(commandList: MutableList<Command>): MutableList<Command> {
        val source = adapterEntities ?: return commandList
        val entities = ArrayList<LibraryEntity>(source.size)
        for (adapterEntity in source) {
            if (adapterEntity is LibraryAdapterEntity) {
                entities.add(adapterEntity.getEntity())
            }
        }
        commandList.add(AddToPlaylistCommand(entities.toTypedArray()))
        return commandList
    }

    protected fun sendControlCommands(displayText: CharSequence, commands: List<Command>) {
        MusicPlayerControl.sendControlCommands(commands)
        Boast.makeText(requireActivity(), displayText).show()
    }

    protected open fun getContextMenuItems(entity: LibraryEntity): Array<String> {
        return resources.getStringArray(R.array.library_selected_menu)
    }

    protected abstract fun getFragmentPosition(): Int

    protected abstract fun getEntities(responseReceiver: ResponseReceiver<Array<LibraryEntity>>): TaskHandle

    protected abstract fun createAdapterEntities(entities: Array<LibraryEntity>): Array<AdapterEntity>

    protected abstract fun createAdapter(adapterEntities: Array<AdapterEntity>): LibraryArrayAdapter

    private fun findListView(): ListView? {
        // if swiping fast left and right the view might actually be destroyed when the response returns
        return _binding?.libraryListViewBrowse
    }

    private fun updateEntities() {
        val existing = adapterEntities
        if (existing != null) {
            createEntityAdapter(existing)
        } else if (!updating) {
            showUpdatingIndicator()
            getFromLibraryHandle.cancelTask()
            getFromLibraryHandle = getEntities(object : ResponseReceiver<Array<LibraryEntity>>() {
                override fun buildingDatabase() {
                    Boast.makeText(requireActivity(), R.string.database_build_building_database_toast).show()
                }

                override fun receiveResponse(response: Array<LibraryEntity>) {
                    val entities = createAdapterEntities(response)
                    adapterEntities = entities
                    createEntityAdapter(entities)
                    hideUpdatingIndicator()

                    (requireActivity() as LibraryActivity).verifyBuildDatabase()
                }
            })
        }
    }

    private fun createEntityAdapter(adapterEntities: Array<AdapterEntity>) {
        val listView = findListView()
        // if swiping fast left and right the view might actually be destroyed when the response returns
        if (listView != null) {
            val notify = listView.adapter != null
            val adapter = createAdapter(adapterEntities)
            listView.adapter = adapter
            if (notify) {
                adapter.notifyDataSetChanged()
            }
        }
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
        return _binding?.libraryProgressBarLoad
    }

    private fun addRandom(clear: Boolean) {
        val commandList = ArrayList<Command>()

        val entities = adapterEntities ?: return
        val libraryAdapterEntities = ArrayList<LibraryAdapterEntity>(entities.size)
        for (adapterEntity in entities) {
            if (adapterEntity is LibraryAdapterEntity) {
                libraryAdapterEntities.add(adapterEntity)
            }
        }

        if (libraryAdapterEntities.size > 0) {
            val rand = Random()
            val idx = rand.nextInt(libraryAdapterEntities.size)
            val libraryEntity = libraryAdapterEntities[idx]
            val displayText = getString(R.string.library_added_to_playlist_toast, libraryEntity.getText())
            if (clear) {
                commandList.add(ClearPlaylistCommand())
            }
            commandList.add(AddToPlaylistCommand(libraryEntity.getEntity()))
            sendControlCommands(displayText, commandList)
        }
    }

    companion object {
        private const val ENTITIES_SAVED_INSTANCE_STATE = "libraryEntities"
    }
}
