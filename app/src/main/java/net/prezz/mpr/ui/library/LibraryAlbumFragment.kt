package net.prezz.mpr.ui.library

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import androidx.preference.PreferenceManager
import net.prezz.mpr.R
import net.prezz.mpr.model.LibraryEntity
import net.prezz.mpr.model.LibraryEntity.Tag
import net.prezz.mpr.model.MusicPlayerControl
import net.prezz.mpr.model.ResponseReceiver
import net.prezz.mpr.model.TaskHandle
import net.prezz.mpr.ui.adapter.AdapterEntity
import net.prezz.mpr.ui.adapter.AdapterIndexStrategy
import net.prezz.mpr.ui.adapter.AlbumAdapterEntity
import net.prezz.mpr.ui.adapter.LibraryAdapterEntity
import net.prezz.mpr.ui.adapter.LibraryArrayAdapter
import net.prezz.mpr.ui.adapter.SectionAdapterEntity
import net.prezz.mpr.ui.adapter.SectionSortedAdapterIndexStrategy
import net.prezz.mpr.ui.adapter.SortedAdapterIndexStrategy
import net.prezz.mpr.ui.library.filtered.FilteredActivity
import net.prezz.mpr.ui.library.filtered.FilteredAlbumAndTitleActivity
import net.prezz.mpr.ui.library.filtered.FilteredTrackAndTitleActivity

class LibraryAlbumFragment : LibraryFragment() {

    private var sortByArtist = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sortByArtist = sortByArtist()
    }

    override fun onItemClick(adapterView: AdapterView<*>?, view: View?, position: Int, id: Long) {
        val adapterEntity = getAdapterEntity(position)
        if (adapterEntity is AlbumAdapterEntity) {
            val entity = adapterEntity.getEntity()

            val intent = Intent(activity, FilteredTrackAndTitleActivity::class.java)
            val args = Bundle()
            args.putString(FilteredActivity.TITLE_ARGUMENT_KEY, entity.getAlbum())
            args.putSerializable(FilteredActivity.ENTITY_ARGUMENT_KEY, entity)
            intent.putExtras(args)
            startActivity(intent)
        }
    }

    override fun onContextMenuItemSelected(which: Int, entity: LibraryAdapterEntity) {
        if (which == 5) {
            val libraryEntity = entity.getEntity()
            val artistEntity = LibraryEntity.createBuilder().setTag(Tag.ARTIST).setArtist(libraryEntity.getLookupArtist()).setUriEntity(libraryEntity.getUriEntity()).setUriFilter(libraryEntity.getUriFilter()).build()

            val intent = Intent(activity, FilteredAlbumAndTitleActivity::class.java)
            val args = Bundle()
            args.putString(FilteredActivity.TITLE_ARGUMENT_KEY, artistEntity.getArtist())
            args.putSerializable(FilteredActivity.ENTITY_ARGUMENT_KEY, artistEntity)
            intent.putExtras(args)
            startActivity(intent)
        } else {
            super.onContextMenuItemSelected(which, entity)
        }
    }

    override fun getContextMenuItems(entity: LibraryEntity): Array<String> {
        return if (entity.getMetaCompilation() == false) {
            resources.getStringArray(R.array.library_album_selected_menu)
        } else {
            resources.getStringArray(R.array.library_selected_menu)
        }
    }

    override fun getFragmentPosition(): Int {
        return FRAGMENT_POSITION
    }

    override fun getEntities(responseReceiver: ResponseReceiver<Array<LibraryEntity>>): TaskHandle {
        val libraryActivity = requireActivity() as LibraryActivity
        return MusicPlayerControl.getAlbumsFromLibrary(sortByArtist, libraryActivity.getLibraryEntityFilter(), responseReceiver)
    }

    override fun createAdapterEntities(entities: Array<LibraryEntity>): Array<AdapterEntity> {
        val sections = entities.isNotEmpty() && entities[0].getAlbum().isNullOrEmpty()
        var tracksSection = sections
        var albumsSection = sections

        val result = ArrayList<AdapterEntity>(entities.size + 2)
        for (i in entities.indices) {
            if (tracksSection) {
                result.add(SectionAdapterEntity(getString(R.string.library_titles_section)))
                tracksSection = false
            } else if (albumsSection && !entities[i].getAlbum().isNullOrEmpty()) {
                result.add(SectionAdapterEntity(getString(R.string.library_albums_section)))
                albumsSection = false
            }

            result.add(AlbumAdapterEntity(entities[i], sortByArtist))
        }

        return result.toTypedArray()
    }

    override fun createAdapter(adapterEntities: Array<AdapterEntity>): LibraryArrayAdapter {
        val indexStrategy: AdapterIndexStrategy = if (adapterEntities.isNotEmpty() && adapterEntities[0] is SectionAdapterEntity) {
            SectionSortedAdapterIndexStrategy
        } else {
            SortedAdapterIndexStrategy
        }
        return LibraryArrayAdapter(requireActivity(), android.R.layout.simple_list_item_1, adapterEntities, indexStrategy, showCovers())
    }

    private fun sortByArtist(): Boolean {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireActivity())
        val resources = requireActivity().resources
        return sharedPreferences.getBoolean(resources.getString(R.string.settings_library_sort_album_by_artist_key), true)
    }

    private fun showCovers(): Boolean {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireActivity())
        val resources = requireActivity().resources
        return sharedPreferences.getBoolean(resources.getString(R.string.settings_library_show_covers_for_all_albums_key), true)
    }

    companion object {
        private const val FRAGMENT_POSITION = 0
    }
}
