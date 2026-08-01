package net.prezz.mpr.ui.library.filtered

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ListAdapter
import androidx.preference.PreferenceManager
import net.prezz.mpr.R
import net.prezz.mpr.model.LibraryEntity
import net.prezz.mpr.model.MusicPlayerControl
import net.prezz.mpr.model.ResponseReceiver
import net.prezz.mpr.model.TaskHandle
import net.prezz.mpr.ui.adapter.AdapterEntity
import net.prezz.mpr.ui.adapter.AdapterIndexStrategy
import net.prezz.mpr.ui.adapter.AlbumAdapterEntity
import net.prezz.mpr.ui.adapter.LibraryArrayAdapter
import net.prezz.mpr.ui.adapter.SectionAdapterEntity
import net.prezz.mpr.ui.adapter.SectionSortedAdapterIndexStrategy
import net.prezz.mpr.ui.adapter.SortedAdapterIndexStrategy

class FilteredAlbumsActivity : FilteredActivity() {

    private var sortByArtist = false

    override fun onCreate(savedInstanceState: Bundle?) {
        sortByArtist = sortByArtist()
        super.onCreate(savedInstanceState)
    }

    override fun getLayout(): Int {
        return R.layout.activity_filtered
    }

    override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        val adapterEntity = getAdapterEntity(position)
        if (adapterEntity is AlbumAdapterEntity) {
            val entity = adapterEntity.getEntity()

            val intent = Intent(this, FilteredTrackAndTitleActivity::class.java)
            val args = Bundle()
            args.putString(TITLE_ARGUMENT_KEY, entity.getAlbum())
            args.putSerializable(ENTITY_ARGUMENT_KEY, entity)
            intent.putExtras(args)
            startActivity(intent)
        }
    }

    override fun getEntities(entity: LibraryEntity, responseReceiver: ResponseReceiver<Array<LibraryEntity>>): TaskHandle {
        return MusicPlayerControl.getAlbumsFromLibrary(sortByArtist, entity, responseReceiver)
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

    override fun createAdapter(adapterEntities: Array<AdapterEntity>): ListAdapter {
        val indexStrategy: AdapterIndexStrategy = if (adapterEntities.isNotEmpty() && adapterEntities[0] is SectionAdapterEntity) {
            SectionSortedAdapterIndexStrategy
        } else {
            SortedAdapterIndexStrategy
        }
        return LibraryArrayAdapter(this, android.R.layout.simple_list_item_1, adapterEntities, indexStrategy, showCovers())
    }

    private fun sortByArtist(): Boolean {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val resources = resources
        return sharedPreferences.getBoolean(resources.getString(R.string.settings_library_sort_album_by_artist_key), true)
    }

    private fun showCovers(): Boolean {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val resources = resources
        return sharedPreferences.getBoolean(resources.getString(R.string.settings_library_show_covers_for_all_albums_key), true)
    }
}
