package net.prezz.mpr.ui.library

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import net.prezz.mpr.R
import net.prezz.mpr.model.LibraryEntity
import net.prezz.mpr.model.LibraryEntity.Tag
import net.prezz.mpr.model.MusicPlayerControl
import net.prezz.mpr.model.ResponseReceiver
import net.prezz.mpr.model.TaskHandle
import net.prezz.mpr.ui.adapter.AdapterEntity
import net.prezz.mpr.ui.adapter.ArtistAdapterEntity
import net.prezz.mpr.ui.adapter.LibraryArrayAdapter
import net.prezz.mpr.ui.adapter.SectionAdapterEntity
import net.prezz.mpr.ui.adapter.SectionSortedAdapterIndexStrategy
import net.prezz.mpr.ui.library.filtered.FilteredActivity
import net.prezz.mpr.ui.library.filtered.FilteredAlbumAndTitleActivity

class LibraryArtistFragment : LibraryFragment() {

    override fun onItemClick(adapterView: AdapterView<*>?, view: View?, position: Int, id: Long) {
        val adapterEntity = getAdapterEntity(position)
        if (adapterEntity is ArtistAdapterEntity) {
            val entity = adapterEntity.getEntity()

            val intent = Intent(activity, FilteredAlbumAndTitleActivity::class.java)
            val args = Bundle()
            args.putString(FilteredActivity.TITLE_ARGUMENT_KEY, adapterEntity.getEntityText())
            args.putSerializable(FilteredActivity.ENTITY_ARGUMENT_KEY, entity)
            intent.putExtras(args)
            startActivity(intent)
        }
    }

    override fun getFragmentPosition(): Int {
        return FRAGMENT_POSITION
    }

    override fun getEntities(responseReceiver: ResponseReceiver<Array<LibraryEntity>>): TaskHandle {
        val libraryActivity = requireActivity() as LibraryActivity
        return MusicPlayerControl.getArtistsFromLibrary(libraryActivity.getLibraryEntityFilter(), responseReceiver)
    }

    override fun createAdapterEntities(entities: Array<LibraryEntity>): Array<AdapterEntity> {
        val result = ArrayList<AdapterEntity>(entities.size + 3)

        var addArtistSection = true
        var addAlbumArtistSection = true
        var addComposerSection = true
        for (i in entities.indices) {
            if (entities[i].getTag() == Tag.ARTIST) {
                if (addArtistSection) {
                    result.add(SectionAdapterEntity(getString(R.string.library_artists)))
                    addArtistSection = false
                }
                result.add(ArtistAdapterEntity(entities[i]))
            }
            if (entities[i].getTag() == Tag.ALBUM_ARTIST) {
                if (addAlbumArtistSection) {
                    result.add(SectionAdapterEntity(getString(R.string.library_album_artists)))
                    addAlbumArtistSection = false
                }
                result.add(ArtistAdapterEntity(entities[i]))
            }
            if (entities[i].getTag() == Tag.COMPOSER) {
                if (addComposerSection) {
                    result.add(SectionAdapterEntity(getString(R.string.library_composers)))
                    addComposerSection = false
                }
                result.add(ArtistAdapterEntity(entities[i]))
            }
        }

        return result.toTypedArray()
    }

    override fun createAdapter(adapterEntities: Array<AdapterEntity>): LibraryArrayAdapter {
        return LibraryArrayAdapter(requireActivity(), android.R.layout.simple_list_item_1, adapterEntities, SectionSortedAdapterIndexStrategy, false)
    }

    companion object {
        private const val FRAGMENT_POSITION = 2
    }
}
