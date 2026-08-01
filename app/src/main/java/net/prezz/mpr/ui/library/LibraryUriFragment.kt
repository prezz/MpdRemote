package net.prezz.mpr.ui.library

import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.content.Intent
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
import net.prezz.mpr.databinding.FragmentLibraryBinding
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
import net.prezz.mpr.ui.library.filtered.FilteredUriActivity
import net.prezz.mpr.ui.state.DataState
import java.util.SortedSet
import java.util.TreeSet

class LibraryUriFragment : Fragment(), LibraryCommonsFragment, OnItemClickListener {

    private var _binding: FragmentLibraryBinding? = null

    private var adapterEntities: Array<AdapterEntity>? = null
    private var updating = false
    private var getFromLibraryHandle: TaskHandle = TaskHandle.NULL_HANDLE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        getFromLibraryHandle = TaskHandle.NULL_HANDLE

        val dataState = DataState.get(requireActivity())
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

        val uriFilterChanged = (requireActivity() as LibraryActivity).attachFragment(this, FRAGMENT_POSITION)
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

        (requireActivity() as LibraryActivity).detachFragment(FRAGMENT_POSITION)
        _binding = null
        super.onDestroyView()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        val dataState = DataState.get(requireActivity())
        dataState.setData(ENTITIES_SAVED_INSTANCE_STATE, adapterEntities)

        super.onSaveInstanceState(outState)
    }

    private fun showContextMenu(position: Int) {
        val entity = adapterEntities?.get(position) ?: return
        if (entity is UriAdapterEntity) {
            val uriEntity = entity.getEntity()
            val displayText = getString(R.string.library_added_to_playlist_toast, entity.getText())
            val menuItems = resources.getStringArray(if (uriEntity.fileType == FileType.PLAYLIST) R.array.library_playlist_selected_menu else R.array.library_selected_menu)
            MaterialAlertDialogBuilder(requireActivity())
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
                        4 -> AddToStoredPlaylistHelper.addUriToStoredPlaylist(requireActivity(), displayText, uriEntity)
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
                val intent = Intent(activity, FilteredUriActivity::class.java)
                val args = Bundle()
                args.putString(FilteredUriActivity.TITLE_ARGUMENT_KEY, entity.getFullUriPath(false))
                args.putSerializable(FilteredUriActivity.ENTITY_ARGUMENT_KEY, entity)
                intent.putExtras(args)
                startActivity(intent)
            }
        }
    }

    override fun onChoiceMenuClick(view: View) {
        val items = resources.getStringArray(R.array.library_uri_root_menu)
        val title = requireActivity().title

        MaterialAlertDialogBuilder(requireActivity()).apply {
            setTitle(title)
            setItems(items) { _, item ->
                if (adapterEntities != null) {
                    var commandList: MutableList<Command> = ArrayList()
                    val displayText = getString(R.string.library_added_to_playlist_toast, title)
                    when (item) {
                        0 -> {
                            commandList.add(ClearPlaylistCommand())
                            commandList = addAll(commandList)
                            sendControlCommands(displayText, commandList)
                        }
                        1 -> {
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

    private fun addAll(commandList: MutableList<Command>): MutableList<Command> {
        val source = adapterEntities ?: return commandList
        val entities = ArrayList<UriEntity>(source.size)
        for (adapterEntity in source) {
            if (adapterEntity is UriAdapterEntity) {
                entities.add(adapterEntity.getEntity())
            }
        }
        commandList.add(AddUriToPlaylistCommand(entities.toTypedArray()))
        return commandList
    }

    private fun sendControlCommands(displayText: CharSequence, commands: List<Command>) {
        MusicPlayerControl.sendControlCommands(commands)
        Boast.makeText(requireActivity(), displayText).show()
    }

    private fun updateEntities() {
        val existing = adapterEntities
        if (existing != null) {
            createEntityAdapter(existing)
        } else if (!updating) {
            showUpdatingIndicator()
            val libraryActivity = requireActivity() as LibraryActivity
            val uriEntityFilter = libraryActivity.getUriEntityFilter()
            val hiddenUriFolders: SortedSet<String> = if (uriEntityFilter == null) libraryActivity.getUriFilter() else TreeSet()
            getFromLibraryHandle.cancelTask()
            getFromLibraryHandle = MusicPlayerControl.getUriFromLibrary(uriEntityFilter, hiddenUriFolders, object : ResponseReceiver<Array<UriEntity>>() {
                override fun receiveResponse(response: Array<UriEntity>) {
                    val entities = createAdapterEntities(response)
                    adapterEntities = entities
                    createEntityAdapter(entities)
                    hideUpdatingIndicator()
                }
            })
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

    private fun createAdapter(adapterEntities: Array<AdapterEntity>): LibraryArrayAdapter {
        return LibraryArrayAdapter(requireActivity(), android.R.layout.simple_list_item_1, adapterEntities, SectionSortedAdapterIndexStrategy, false)
    }

    private fun findListView(): ListView? {
        // if swiping fast left and right the view might actually be destroyed when the response returns
        return _binding?.libraryListViewBrowse
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

    companion object {
        private const val FRAGMENT_POSITION = 3
        private const val ENTITIES_SAVED_INSTANCE_STATE = "UriEntities"
    }
}
