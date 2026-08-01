package net.prezz.mpr.ui.library.filtered

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ListAdapter
import androidx.core.view.MenuProvider
import androidx.preference.PreferenceManager
import net.prezz.mpr.R
import net.prezz.mpr.model.LibraryEntity
import net.prezz.mpr.model.LibraryEntity.Tag
import net.prezz.mpr.model.MusicPlayerControl
import net.prezz.mpr.model.ResponseReceiver
import net.prezz.mpr.model.TaskHandle
import net.prezz.mpr.model.external.ExternalInformationService
import net.prezz.mpr.model.external.UrlReceiver
import net.prezz.mpr.ui.adapter.AdapterEntity
import net.prezz.mpr.ui.adapter.FilteredAlbumAdapterEntity
import net.prezz.mpr.ui.adapter.FilteredTitleAdapterEntity
import net.prezz.mpr.ui.adapter.LibraryArrayAdapter
import net.prezz.mpr.ui.adapter.SectionAdapterEntity
import net.prezz.mpr.ui.adapter.SectionSortedAdapterIndexStrategy
import net.prezz.mpr.ui.helpers.Boast

class FilteredAlbumAndTitleActivity : FilteredActivity() {

    private var lastFmHandle: TaskHandle = TaskHandle.NULL_HANDLE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.artist, menu)
            }

            override fun onMenuItemSelected(item: MenuItem): Boolean {
                return when (item.itemId) {
                    R.id.filtered_lastfm_artist -> {
                        goToLastFm()
                        true
                    }
                    else -> false
                }
            }
        }, this)
    }

    override fun onStop() {
        super.onStop()

        lastFmHandle.cancelTask()
    }

    override fun getLayout(): Int {
        return R.layout.activity_filtered
    }

    override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        val adapterEntity = getAdapterEntity(position)
        if (adapterEntity is FilteredAlbumAdapterEntity) {
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
        return MusicPlayerControl.getFilteredAlbumsAndTitlesFromLibrary(entity, responseReceiver)
    }

    override fun createAdapterEntities(entities: Array<LibraryEntity>): Array<AdapterEntity> {
        val addAlbums = hasAlbum(entities)
        var addAlbumSection = addAlbums
        var addTitleSection = true
        val result = ArrayList<AdapterEntity>(entities.size + 2)

        for (i in entities.indices) {
            if (addAlbums && entities[i].getTag() == Tag.ALBUM) {
                if (addAlbumSection) {
                    result.add(SectionAdapterEntity(getString(R.string.library_albums_section)))
                    addAlbumSection = false
                }
                result.add(FilteredAlbumAdapterEntity(entities[i]))
            }
            if (entities[i].getTag() == Tag.TITLE) {
                if (addTitleSection) {
                    result.add(SectionAdapterEntity(getString(R.string.library_titles_section)))
                    addTitleSection = false
                }
                result.add(FilteredTitleAdapterEntity(entities[i]))
            }
        }

        return result.toTypedArray()
    }

    override fun createAdapter(adapterEntities: Array<AdapterEntity>): ListAdapter {
        return LibraryArrayAdapter(this, android.R.layout.simple_list_item_1, adapterEntities, SectionSortedAdapterIndexStrategy, showCovers())
    }

    private fun goToLastFm() {
        val artist = getEntityArgument()!!.getArtist()

        if (artist.isNullOrEmpty()) {
            Boast.makeText(this, R.string.library_action_not_possible).show()
            return
        }

        lastFmHandle.cancelTask()
        lastFmHandle = ExternalInformationService.getArtistInfoUrls(artist, UrlReceiver { urls ->
            if (urls.isNotEmpty()) {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(urls[0])))
            } else {
                Boast.makeText(this@FilteredAlbumAndTitleActivity, R.string.library_action_not_possible).show()
            }
        })
    }

    private fun hasAlbum(entities: Array<LibraryEntity>): Boolean {
        for (i in entities.indices) {
            if (entities[i].getTag() == Tag.ALBUM && !entities[i].getAlbum().isNullOrEmpty()) {
                return true
            }
        }

        return false
    }

    private fun showCovers(): Boolean {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val resources = resources
        return sharedPreferences.getBoolean(resources.getString(R.string.settings_library_show_covers_for_all_albums_key), true)
    }
}
