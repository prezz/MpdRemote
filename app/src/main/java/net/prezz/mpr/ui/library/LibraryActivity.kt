package net.prezz.mpr.ui.library

import androidx.core.content.edit

import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.content.SharedPreferences
import android.os.Bundle
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.view.MenuProvider
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import androidx.viewpager2.widget.ViewPager2
import net.prezz.mpr.R
import net.prezz.mpr.databinding.ActivityLibraryBinding
import net.prezz.mpr.model.LibraryEntity
import net.prezz.mpr.model.MusicPlayerControl
import net.prezz.mpr.model.ResponseReceiver
import net.prezz.mpr.model.TaskHandle
import net.prezz.mpr.model.UriEntity
import net.prezz.mpr.mpd.database.MpdDatabaseBuilder
import net.prezz.mpr.mpd.database.MpdDatabaseBuilder.UpdateDatabaseResult
import net.prezz.mpr.ui.helpers.MiniControlHelper
import net.prezz.mpr.ui.helpers.setupToolbar
import net.prezz.mpr.ui.helpers.UriFilterHelper
import net.prezz.mpr.ui.helpers.VolumeButtonsHelper
import net.prezz.mpr.ui.state.DataState
import java.util.SortedSet

class LibraryActivity : AppCompatActivity(), UriFilterHelper.UriFilterChangedListener {

    private lateinit var binding: ActivityLibraryBinding

    private val attachedFragments = arrayOfNulls<LibraryCommonsFragment>(LibraryPagerAdapter.FRAGMENT_COUNT)
    private var entitiesChanged = BooleanArray(attachedFragments.size)
    private var uriEntityFilter: UriEntity? = null
    private var fragmentPosition = 0
    private lateinit var controlHelper: MiniControlHelper
    private lateinit var uriFilterHelper: UriFilterHelper

    private lateinit var pageChangeCallback: ViewPager2.OnPageChangeCallback

    private var buildDatabaseErrorDialog: AlertDialog? = null
    private var swipeHintDialog: AlertDialog? = null

    private var getUrientitiesFilterHandle: TaskHandle = TaskHandle.NULL_HANDLE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLibraryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupToolbar(showUpButton = true)

        addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.library, menu)
            }

            override fun onMenuItemSelected(item: MenuItem): Boolean {
                return when (item.itemId) {
                    R.id.library_action_visible_folders -> {
                        uriFilterHelper.setUriFilter()
                        true
                    }
                    else -> false
                }
            }
        }, this)

        binding.libraryButtonFilterMenu.setOnClickListener { onFilterMenuClick(it) }
        binding.libraryButtonChoiceMenu.setOnClickListener { onChoiceMenuClick(it) }
        binding.libraryButtonControlMenu.setOnClickListener { onControlMenuClick(it) }

        buildDatabaseErrorDialog = null
        swipeHintDialog = null

        // setup swipe between fragments
        val pageAdapter = LibraryPagerAdapter(this)
        val viewPager = binding.libraryViewPagerSwipe
        viewPager.adapter = pageAdapter

        fragmentPosition = minOf(readFragmentPosition(), pageAdapter.itemCount)
        if (fragmentPosition > 0) {
            viewPager.currentItem = fragmentPosition
        }

        setTitle(pageAdapter.getTitle(fragmentPosition))

        pageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                fragmentPosition = position
                setTitle(pageAdapter.getTitle(position))
            }
        }

        viewPager.registerOnPageChangeCallback(pageChangeCallback)

        controlHelper = MiniControlHelper(this)
        uriFilterHelper = UriFilterHelper(this, this)

        val dataState = DataState.get(this)
        entitiesChanged = dataState.getData(ENTITIES_CHANGED, entitiesChanged) as BooleanArray
        uriEntityFilter = dataState.getData(URI_ENTITY_FILTER, uriEntityFilter) as UriEntity?
    }

    fun verifyBuildDatabase() {
        if (buildDatabaseErrorDialog?.isShowing == true) {
            return
        }

        val lastDatabaseResult = MpdDatabaseBuilder.getLastDatabaseResult()

        if (lastDatabaseResult != UpdateDatabaseResult.NO_ERROR) {
            buildDatabaseErrorDialog = MaterialAlertDialogBuilder(this).apply {
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
                setNeutralButton(android.R.string.ok) { _, _ ->
                    buildDatabaseErrorDialog = null
                }
            }.create().also { it.show() }
        } else {
            showSwipeHint()
        }
    }

    override fun onPause() {
        super.onPause()

        controlHelper.hideVisibility()
    }

    override fun onStop() {
        super.onStop()

        storeFragmentPosition(fragmentPosition)
    }

    override fun onDestroy() {
        binding.libraryViewPagerSwipe.unregisterOnPageChangeCallback(pageChangeCallback)

        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        val dataState = DataState.get(this)
        dataState.setData(ENTITIES_CHANGED, entitiesChanged)
        dataState.setData(URI_ENTITY_FILTER, uriEntityFilter)

        super.onSaveInstanceState(outState)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (VolumeButtonsHelper.handleKeyDown(this, keyCode, event)) {
            return true
        }

        return super.onKeyDown(keyCode, event)
    }

    override fun entityFilterChanged() {
        for (i in attachedFragments.indices) {
            entitiesChanged[i] = true
            val fragment = attachedFragments[i]
            if (fragment != null) {
                fragment.entitiesChanged()
                entitiesChanged[i] = false
            }
        }
    }

    fun getLibraryEntityFilter(): LibraryEntity {
        return LibraryEntity.createBuilder()
            .setUriEntity(getUriEntityFilter())
            .setUriFilter(getUriFilter())
            .build()
    }

    fun getUriEntityFilter(): UriEntity? {
        return uriEntityFilter
    }

    fun getUriFilter(): SortedSet<String> {
        return uriFilterHelper.getUriFilter()
    }

    fun attachFragment(fragment: LibraryCommonsFragment, pos: Int): Boolean {
        attachedFragments[pos] = fragment

        val changed = entitiesChanged[pos]
        entitiesChanged[pos] = false
        return changed
    }

    fun detachFragment(pos: Int) {
        attachedFragments[pos] = null
    }

    private fun onFilterMenuClick(view: View) {
        getUrientitiesFilterHandle.cancelTask()
        getUrientitiesFilterHandle = MusicPlayerControl.getUriFromLibrary(null, getUriFilter(), object : ResponseReceiver<Array<UriEntity>>() {
            override fun receiveResponse(entities: Array<UriEntity>) {
                val selections = arrayOfNulls<UriEntity>(entities.size + 1)
                System.arraycopy(entities, 0, selections, 1, entities.size)

                val items = arrayOfNulls<String>(selections.size)
                for (i in selections.indices) {
                    items[i] = if (selections[i] != null) selections[i]!!.getFullUriPath(true) else this@LibraryActivity.resources.getString(R.string.library_selected_folder_none)
                }

                MaterialAlertDialogBuilder(this@LibraryActivity).apply {
                    setTitle(R.string.library_selected_folder_header)
                    setItems(items) { _, which ->
                        uriEntityFilter = selections[which]
                        entityFilterChanged()
                    }
                }.create().show()
            }
        })
    }

    private fun onChoiceMenuClick(view: View) {
        attachedFragments[fragmentPosition]?.onChoiceMenuClick(view)
    }

    private fun onControlMenuClick(view: View) {
        controlHelper.toggleVisibility()
    }

    private fun readFragmentPosition(): Int {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        return sharedPreferences.getInt(PREFERENCE_FRAGMENT_POSITION_KEY, 0)
    }

    private fun storeFragmentPosition(position: Int) {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        sharedPreferences.edit {
            putInt(PREFERENCE_FRAGMENT_POSITION_KEY, position)
        }
    }

    private fun showSwipeHint() {
        if (swipeHintDialog?.isShowing == true) {
            return
        }

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val show = sharedPreferences.getBoolean(PREFERENCE_SHOW_SWIPE_HINT_KEY, true)

        if (show) {
            swipeHintDialog = MaterialAlertDialogBuilder(this).apply {
                setCancelable(false)
                setTitle(R.string.library_swipe_hint_header)
                setMessage(R.string.library_swipe_hint_message)
                setPositiveButton(R.string.library_swipe_hint_button) { _, _ ->
                    sharedPreferences.edit {
                        putBoolean(PREFERENCE_SHOW_SWIPE_HINT_KEY, false)
                    }
                }
            }.create().also { it.show() }
        }
    }

    companion object {
        private const val ENTITIES_CHANGED = "entities_changed"
        private const val URI_ENTITY_FILTER = "uri_entity_filter"

        private const val PREFERENCE_FRAGMENT_POSITION_KEY = "fragment_position_key"
        private const val PREFERENCE_SHOW_SWIPE_HINT_KEY = "library_show_swipe_hint"
    }
}
