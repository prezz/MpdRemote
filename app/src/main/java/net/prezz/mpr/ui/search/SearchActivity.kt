package net.prezz.mpr.ui.search

import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.EditText
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.view.MenuProvider
import androidx.preference.PreferenceManager
import net.prezz.mpr.R
import net.prezz.mpr.Utils
import net.prezz.mpr.databinding.ActivitySearchBinding
import net.prezz.mpr.model.LibraryEntity
import net.prezz.mpr.model.LibraryEntity.Tag
import net.prezz.mpr.model.MusicPlayerControl
import net.prezz.mpr.model.ResponseReceiver
import net.prezz.mpr.model.SearchResult
import net.prezz.mpr.model.TaskHandle
import net.prezz.mpr.model.UriEntity
import net.prezz.mpr.model.UriEntity.UriType
import net.prezz.mpr.model.command.AddToPlaylistCommand
import net.prezz.mpr.model.command.AddUriToPlaylistCommand
import net.prezz.mpr.model.command.ClearPlaylistCommand
import net.prezz.mpr.model.command.Command
import net.prezz.mpr.model.command.PlayCommand
import net.prezz.mpr.model.command.PrioritizeCommand
import net.prezz.mpr.model.command.PrioritizeUriCommand
import net.prezz.mpr.model.command.UpdatePrioritiesCommand
import net.prezz.mpr.mpd.database.MpdDatabaseBuilder
import net.prezz.mpr.mpd.database.MpdDatabaseBuilder.UpdateDatabaseResult
import net.prezz.mpr.ui.adapter.AdapterEntity
import net.prezz.mpr.ui.adapter.AlbumAdapterEntity
import net.prezz.mpr.ui.adapter.ArtistAdapterEntity
import net.prezz.mpr.ui.adapter.LibraryAdapterEntity
import net.prezz.mpr.ui.adapter.LibraryArrayAdapter
import net.prezz.mpr.ui.adapter.SearchTitleAdapterEntity
import net.prezz.mpr.ui.adapter.SectionAdapterEntity
import net.prezz.mpr.ui.adapter.SectionSortedAdapterIndexStrategy
import net.prezz.mpr.ui.adapter.UriAdapterEntity
import net.prezz.mpr.ui.helpers.Boast
import net.prezz.mpr.ui.helpers.MiniControlHelper
import net.prezz.mpr.ui.helpers.UriFilterHelper
import net.prezz.mpr.ui.helpers.VolumeButtonsHelper
import net.prezz.mpr.ui.helpers.setupToolbar
import net.prezz.mpr.ui.library.filtered.FilteredActivity
import net.prezz.mpr.ui.library.filtered.FilteredAlbumAndTitleActivity
import net.prezz.mpr.ui.library.filtered.FilteredTrackAndTitleActivity
import net.prezz.mpr.ui.library.filtered.FilteredUriActivity
import net.prezz.mpr.ui.state.DataState

class SearchActivity : AppCompatActivity(), OnItemClickListener, SearchView.OnQueryTextListener, UriFilterHelper.UriFilterChangedListener {

    private lateinit var binding: ActivitySearchBinding

    private var activityTitle: String? = null
    private var setSearchFocus = true
    private var adapterEntities: Array<AdapterEntity>? = null
    private var searchLibraryHandle: TaskHandle = TaskHandle.NULL_HANDLE
    private lateinit var controlHelper: MiniControlHelper
    private lateinit var uriFilterHelper: UriFilterHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupToolbar(showUpButton = true)

        searchLibraryHandle = TaskHandle.NULL_HANDLE

        val dataState = DataState.get(this)
        // restore entities if loaded into memory again (or after rotation)
        activityTitle = dataState.getData(ACTIVITY_TITLE_SAVED_INSTANCE_STATE, null) as String?
        setSearchFocus = dataState.getData(FOCUS_SEARCH_SAVED_INSTANCE_STATE, false) as Boolean

        @Suppress("UNCHECKED_CAST")
        (dataState.getData(ENTITIES_SAVED_INSTANCE_STATE, null) as? Array<AdapterEntity>)?.let {
            adapterEntities = it
        }

        activityTitle?.let { setTitle(it) }

        adapterEntities?.let { createEntityAdapter(it) }

        controlHelper = MiniControlHelper(this)
        uriFilterHelper = UriFilterHelper(this, this)

        // since search is only possible from within this activity the handling of the intent is actually not necessary
        // we just keep it anyway if we later want to search from other activities.
        handleSearch(intent)

        addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.search, menu)

                val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager
                val searchView = menu.findItem(R.id.search_action_search).actionView as SearchView
                searchView.setSearchableInfo(searchManager.getSearchableInfo(componentName))
                searchView.isFocusable = true
                searchView.setOnQueryTextListener(this@SearchActivity)

                // The query field otherwise inherits a dark text color, rendering dark-on-dark on the dark
                // toolbar; tint it to the toolbar's on-primary color instead.
                searchView.findViewById<EditText>(androidx.appcompat.R.id.search_src_text)?.let { searchText ->
                    val onPrimary = TypedValue()
                    theme.resolveAttribute(com.google.android.material.R.attr.colorOnPrimary, onPrimary, true)
                    searchText.setTextColor(onPrimary.data)
                    searchText.setHintTextColor(onPrimary.data and 0x00FFFFFF or (0x99 shl 24))
                }
                if (setSearchFocus) {
                    searchView.isIconified = false
                    searchView.requestFocus()
                }
            }

            override fun onMenuItemSelected(item: MenuItem): Boolean {
                return when (item.itemId) {
                    R.id.search_action_search -> {
                        onSearchRequested()
                        true
                    }
                    R.id.search_action_hide_folders -> {
                        uriFilterHelper.setUriFilter()
                        true
                    }
                    else -> false
                }
            }
        }, this)

        binding.searchButtonChoiceMenu.setOnClickListener { onChoiceMenuClick(it) }
        binding.searchButtonControlMenu.setOnClickListener { onControlMenuClick(it) }
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

        searchLibraryHandle.cancelTask()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        val dataState = DataState.get(this)
        dataState.setData(ACTIVITY_TITLE_SAVED_INSTANCE_STATE, title)

        val searchView = findViewById<SearchView>(R.id.search_action_search)
        if (searchView != null) {
            dataState.setData(FOCUS_SEARCH_SAVED_INSTANCE_STATE, searchView.hasFocus())
        }

        dataState.setData(ENTITIES_SAVED_INSTANCE_STATE, adapterEntities)

        super.onSaveInstanceState(outState)
    }

    private fun showContextMenu(position: Int) {
        val entity = adapterEntities?.get(position) ?: return
        if (entity !is LibraryAdapterEntity && entity !is UriAdapterEntity) {
            return
        }
        val menuItems = resources.getStringArray(R.array.search_selected_menu)
        MaterialAlertDialogBuilder(this)
            .setTitle(entity.getText())
            .setItems(menuItems) { _, which ->
                val commandList = ArrayList<Command>()
                if (entity is LibraryAdapterEntity) {
                    val libraryEntity = entity.getEntity()
                    val displayText = getString(R.string.search_added_to_playlist_toast, entity.getText())
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
                    }
                } else if (entity is UriAdapterEntity) {
                    val uriEntity = entity.getEntity()
                    val displayText = getString(R.string.search_added_to_playlist_toast, entity.getText())
                    when (which) {
                        0 -> {
                            commandList.add(AddUriToPlaylistCommand(uriEntity))
                            commandList.add(UpdatePrioritiesCommand())
                            sendControlCommands(displayText, commandList)
                        }
                        1 -> {
                            commandList.add(PrioritizeUriCommand(uriEntity))
                            sendControlCommands(displayText, commandList)
                        }
                        2 -> {
                            commandList.add(ClearPlaylistCommand())
                            commandList.add(AddUriToPlaylistCommand(uriEntity))
                            sendControlCommands(displayText, commandList)
                        }
                        3 -> {
                            commandList.add(ClearPlaylistCommand())
                            commandList.add(AddUriToPlaylistCommand(uriEntity))
                            commandList.add(PlayCommand())
                            sendControlCommands(displayText, commandList)
                        }
                    }
                }
            }
            .show()
    }

    override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        val adapterEntity = adapterEntities?.get(position) ?: return
        if (adapterEntity is ArtistAdapterEntity) {
            val entity = adapterEntity.getEntity()
            val intent = Intent(this, FilteredAlbumAndTitleActivity::class.java)
            val args = Bundle()
            args.putString(FilteredActivity.TITLE_ARGUMENT_KEY, adapterEntity.getText())
            args.putSerializable(FilteredActivity.ENTITY_ARGUMENT_KEY, entity)
            intent.putExtras(args)
            startActivity(intent)
        } else if (adapterEntity is AlbumAdapterEntity) {
            val entity = adapterEntity.getEntity()
            val intent = Intent(this, FilteredTrackAndTitleActivity::class.java)
            val args = Bundle()
            args.putString(FilteredActivity.TITLE_ARGUMENT_KEY, adapterEntity.getText())
            args.putSerializable(FilteredActivity.ENTITY_ARGUMENT_KEY, entity)
            intent.putExtras(args)
            startActivity(intent)
        } else if (adapterEntity is UriAdapterEntity) {
            val uriEntity = adapterEntity.getEntity()
            if (uriEntity.uriType == UriType.DIRECTORY) {
                val intent = Intent(this, FilteredUriActivity::class.java)
                val args = Bundle()
                args.putString(FilteredUriActivity.TITLE_ARGUMENT_KEY, uriEntity.getFullUriPath(false))
                args.putSerializable(FilteredUriActivity.ENTITY_ARGUMENT_KEY, uriEntity)
                intent.putExtras(args)
                startActivity(intent)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleSearch(intent)
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        return false
    }

    override fun onQueryTextChange(text: String?): Boolean {
        val searchView = findViewById<SearchView>(R.id.search_action_search)
        if (searchView != null && searchView.hasFocus()) {
            if (text.isNullOrEmpty()) {
                adapterEntities = arrayOf()
                adapterEntities?.let { createEntityAdapter(it) }
            } else {
                search(text!!)
            }
            return true
        }

        return false
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (VolumeButtonsHelper.handleKeyDown(this, keyCode, event)) {
            return true
        }

        return super.onKeyDown(keyCode, event)
    }

    override fun entityFilterChanged() {
        val searchView = findViewById<SearchView>(R.id.search_action_search)
        if (searchView != null) {
            val text = searchView.query.toString()
            if (text.isNullOrEmpty()) {
                adapterEntities = arrayOf()
                adapterEntities?.let { createEntityAdapter(it) }
            } else {
                search(text)
            }
        }
    }

    private fun onChoiceMenuClick(view: View) {
        val searchView = findViewById<SearchView>(R.id.search_action_search)
        searchView.isIconified = false
        searchView.requestFocus()
    }

    private fun onControlMenuClick(view: View) {
        controlHelper.toggleVisibility()
    }

    private fun handleSearch(intent: Intent) {
        if (Intent.ACTION_SEARCH == intent.action) {
            val query = intent.getStringExtra(SearchManager.QUERY)
            search(query!!)

            val searchView = findViewById<SearchView>(R.id.search_action_search)
            if (searchView != null) {
                searchView.clearFocus()
                searchView.setQuery("", false)
                searchView.isIconified = true
            }
        }
    }

    private fun search(query: String) {
        setTitle(query)
        showSearchIndicator()
        searchLibraryHandle.cancelTask()
        searchLibraryHandle = MusicPlayerControl.searchLibrary(query, searchUri(), uriFilterHelper.getUriFilter(), object : ResponseReceiver<SearchResult>() {
            override fun buildingDatabase() {
                Boast.makeText(this@SearchActivity, R.string.database_build_building_database_toast).show()
            }

            override fun receiveResponse(response: SearchResult) {
                val entities = createAdapterEntities(response.getLibraryEntities(), response.getUriEntities())
                adapterEntities = entities
                createEntityAdapter(entities)
                hideSearchIndicator()
                verifyBuildDatabase()
            }
        })
    }

    private fun verifyBuildDatabase() {
        val lastDatabaseResult = MpdDatabaseBuilder.getLastDatabaseResult()

        if (lastDatabaseResult != UpdateDatabaseResult.NO_ERROR) {
            MaterialAlertDialogBuilder(this).apply {
                setCancelable(false)
                when (lastDatabaseResult) {
                    UpdateDatabaseResult.UPDATE_RUNNING_ERROR -> {
                        setTitle(R.string.database_build_error_updating_title)
                        setMessage(R.string.database_build_error_updating_message)
                    }
                    UpdateDatabaseResult.TRACK_COUNT_MISMATCH_ERROR -> {
                        setTitle(R.string.database_build_error_buffer_title)
                        setMessage(R.string.database_build_error_buffer_message)
                    }
                    UpdateDatabaseResult.NO_ERROR -> {}
                }
                setNeutralButton(android.R.string.ok) { _, _ -> }
            }.create().show()
        }
    }

    private fun sendControlCommands(displayText: CharSequence, commands: List<Command>) {
        MusicPlayerControl.sendControlCommands(commands)
        Boast.makeText(this, displayText).show()
    }

    private fun createAdapterEntities(libraryEntities: Array<LibraryEntity>, uriEntities: Array<UriEntity>): Array<AdapterEntity> {
        val result = ArrayList<AdapterEntity>(libraryEntities.size + uriEntities.size + 5)

        var addArtistSection = true
        var addAlbumArtistSection = true
        var addComposerSection = true
        var addAlbumSection = true
        var addTitleSection = true
        for (i in libraryEntities.indices) {
            if (libraryEntities[i].getTag() == Tag.ARTIST) {
                if (addArtistSection) {
                    result.add(SectionAdapterEntity(getString(R.string.search_artist_section)))
                    addArtistSection = false
                }
                result.add(ArtistAdapterEntity(libraryEntities[i]))
            }
            if (libraryEntities[i].getTag() == Tag.ALBUM_ARTIST) {
                if (addAlbumArtistSection) {
                    result.add(SectionAdapterEntity(getString(R.string.search_album_artist_section)))
                    addAlbumArtistSection = false
                }
                result.add(ArtistAdapterEntity(libraryEntities[i]))
            }
            if (libraryEntities[i].getTag() == Tag.COMPOSER) {
                if (addComposerSection) {
                    result.add(SectionAdapterEntity(getString(R.string.search_composer_section)))
                    addComposerSection = false
                }
                result.add(ArtistAdapterEntity(libraryEntities[i]))
            }
            if (libraryEntities[i].getTag() == Tag.ALBUM) {
                if (addAlbumSection) {
                    result.add(SectionAdapterEntity(getString(R.string.search_albums_section)))
                    addAlbumSection = false
                }
                result.add(AlbumAdapterEntity(libraryEntities[i], false))
            }
            if (libraryEntities[i].getTag() == Tag.TITLE) {
                if (addTitleSection) {
                    result.add(SectionAdapterEntity(getString(R.string.search_titles_section)))
                    addTitleSection = false
                }
                result.add(SearchTitleAdapterEntity(libraryEntities[i]))
            }
        }

        var addDirSection = true
        var addFileSection = true
        for (i in uriEntities.indices) {
            if (uriEntities[i].uriType == UriType.DIRECTORY) {
                if (addDirSection) {
                    result.add(SectionAdapterEntity(getString(R.string.library_directories_section)))
                    addDirSection = false
                }
                result.add(UriAdapterEntity(uriEntities[i]))
            }
            if (uriEntities[i].uriType == UriType.FILE) {
                if (addFileSection) {
                    result.add(SectionAdapterEntity(getString(R.string.library_files_section)))
                    addFileSection = false
                }
                result.add(UriAdapterEntity(uriEntities[i]))
            }
        }

        return result.toTypedArray()
    }

    private fun createEntityAdapter(adapterEntities: Array<AdapterEntity>) {
        val listView = findListView()
        listView.isFastScrollEnabled = false // To ensure the section indexes are recalculated
        val adapter = createAdapter(adapterEntities)
        listView.adapter = adapter
        adapter.notifyDataSetChanged() // also to ensure the section indexes are recalculated
        listView.isFastScrollEnabled = true
    }

    private fun findListView(): ListView {
        return binding.searchListViewBrowse
    }

    private fun createAdapter(adapterEntities: Array<AdapterEntity>): LibraryArrayAdapter {
        return LibraryArrayAdapter(this, android.R.layout.simple_list_item_1, adapterEntities, SectionSortedAdapterIndexStrategy, false)
    }

    private fun showSearchIndicator() {
        binding.searchProgressBarSearch.visibility = View.VISIBLE
    }

    private fun hideSearchIndicator() {
        binding.searchProgressBarSearch.visibility = View.GONE
    }

    private fun searchUri(): Boolean {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val resources = resources
        return sharedPreferences.getBoolean(resources.getString(R.string.settings_library_search_uri_key), false)
    }

    companion object {
        private const val ACTIVITY_TITLE_SAVED_INSTANCE_STATE = "activityTitle"
        private const val FOCUS_SEARCH_SAVED_INSTANCE_STATE = "focusSearch"
        private const val ENTITIES_SAVED_INSTANCE_STATE = "adapterEntities"
    }
}
