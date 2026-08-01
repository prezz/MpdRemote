package net.prezz.mpr.ui.library.filtered

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.Surface
import android.view.View
import android.widget.AdapterView
import android.widget.ImageView
import android.widget.ListAdapter
import android.widget.TextView
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.MenuProvider
import net.prezz.mpr.R
import net.prezz.mpr.model.LibraryEntity
import net.prezz.mpr.model.MusicPlayerControl
import net.prezz.mpr.model.ResponseReceiver
import net.prezz.mpr.model.TaskHandle
import net.prezz.mpr.model.external.CoverReceiver
import net.prezz.mpr.model.external.ExternalInformationService
import net.prezz.mpr.model.external.UrlReceiver
import net.prezz.mpr.ui.CoverActivity
import net.prezz.mpr.ui.adapter.AdapterEntity
import net.prezz.mpr.ui.adapter.FilteredTrackTitleAdapterEntity
import net.prezz.mpr.ui.adapter.LibraryAdapterEntity
import net.prezz.mpr.ui.adapter.LibraryArrayAdapter
import net.prezz.mpr.ui.adapter.SortedAdapterIndexStrategy
import net.prezz.mpr.ui.helpers.Boast
import java.util.TreeSet

class FilteredTrackAndTitleActivity : FilteredActivity(), ActivityResultCallback<ActivityResult> {

    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent>

    private var getCoverHandle: TaskHandle = TaskHandle.NULL_HANDLE
    private var lastFmHandle: TaskHandle = TaskHandle.NULL_HANDLE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        activityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult(), this)

        addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.album, menu)
            }

            override fun onMenuItemSelected(item: MenuItem): Boolean {
                return when (item.itemId) {
                    R.id.filtered_change_cover -> {
                        changeCover()
                        true
                    }
                    R.id.filtered_clear_cover -> {
                        setCover(ExternalInformationService.NULL_URL)
                        true
                    }
                    R.id.filtered_lastfm_album -> {
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

        getCoverHandle.cancelTask()
        lastFmHandle.cancelTask()
    }

    override fun getLayout(): Int {
        return R.layout.activity_filtered_track_and_title
    }

    override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
    }

    override fun getEntities(entity: LibraryEntity, responseReceiver: ResponseReceiver<Array<LibraryEntity>>): TaskHandle {
        return MusicPlayerControl.getFilteredTracksAndTitlesFromLibrary(entity, responseReceiver)
    }

    override fun createAdapterEntities(entities: Array<LibraryEntity>): Array<AdapterEntity> {
        return Array(entities.size) { FilteredTrackTitleAdapterEntity(entities[it]) }
    }

    override fun createAdapter(adapterEntities: Array<AdapterEntity>): ListAdapter {
        updateMainInfo(adapterEntities)
        return LibraryArrayAdapter(this, android.R.layout.simple_list_item_1, adapterEntities, SortedAdapterIndexStrategy, false)
    }

    override fun onActivityResult(result: ActivityResult) {
        if (result.resultCode == RESULT_OK) {
            val url = result.data?.getStringExtra(CoverActivity.URL_RESULT_KEY)
            setCover(url)
        }
    }

    private fun setCover(url: String?) {
        val artist = getArtist(adapterEntities)
        val album = getAlbum(adapterEntities)
        if (album.isNullOrEmpty()) {
            Boast.makeText(this, R.string.library_action_not_possible).show()
            return
        }

        val maxHeight: Int? = if (isLandscape()) null else resources.getDimensionPixelSize(R.dimen.library_album_cover_height)

        getCoverHandle.cancelTask()
        getCoverHandle = ExternalInformationService.setCoverUrl(artist, album, url, maxHeight, CoverReceiver { bitmap ->
            val imageView = findViewById<ImageView>(R.id.filtered_track_title_cover_image)
            imageView?.setImageBitmap(bitmap)
        })

        // in case of compilation, set cover for all artists
        val allArtists = getAllArtists(adapterEntities)
        allArtists.remove(artist)
        for (a in allArtists) {
            ExternalInformationService.setCoverUrl(a, album, url, maxHeight, null)
        }
    }

    private fun changeCover() {
        val artist = getArtist(adapterEntities)
        val album = getAlbum(adapterEntities)
        if (album.isNullOrEmpty()) {
            Boast.makeText(this, R.string.library_action_not_possible).show()
            return
        }

        val intent = Intent(this, CoverActivity::class.java)
        val args = Bundle()
        args.putString(CoverActivity.ARTIST_ARGUMENT_KEY, artist)
        args.putString(CoverActivity.ALBUM_ARGUMENT_KEY, album)
        intent.putExtras(args)
        activityResultLauncher.launch(intent)
    }

    private fun goToLastFm() {
        val artist = getArtist(adapterEntities)
        val album = getAlbum(adapterEntities)
        if (artist.isNullOrEmpty() || album.isNullOrEmpty()) {
            Boast.makeText(this, R.string.library_action_not_possible).show()
            return
        }

        lastFmHandle.cancelTask()
        lastFmHandle = ExternalInformationService.getAlbumInfoUrls(artist, album, UrlReceiver { urls ->
            if (urls.isNotEmpty()) {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(urls[0])))
            } else {
                Boast.makeText(this@FilteredTrackAndTitleActivity, R.string.library_action_not_possible).show()
            }
        })
    }

    private fun updateMainInfo(entities: Array<AdapterEntity>) {
        findViewById<TextView>(R.id.filtered_track_title_album_track_count)?.text = getCount(entities)
        findViewById<TextView>(R.id.filtered_track_title_album_length)?.text = getString(R.string.library_length, getLength(entities))
        findViewById<TextView>(R.id.filtered_track_title_album_year)?.text = getString(R.string.library_year, getYear(entities))
        findViewById<TextView>(R.id.filtered_track_title_album_genre)?.text = getString(R.string.library_genre, getGenre(entities))

        val artist = getArtist(entities)
        val album = getAlbum(entities)
        val maxHeight: Int? = if (isLandscape()) null else resources.getDimensionPixelSize(R.dimen.library_album_cover_height)
        getCoverHandle.cancelTask()
        getCoverHandle = ExternalInformationService.getCover(artist, album, maxHeight, CoverReceiver { bitmap ->
            if (bitmap != null) {
                val imageView = findViewById<ImageView>(R.id.filtered_track_title_cover_image)
                imageView?.setImageBitmap(bitmap)
            }
        })
    }

    private fun getCount(entities: Array<AdapterEntity>): String {
        val sb = StringBuilder()

        val trackCount = entities.size
        sb.append(trackCount)
        sb.append(" ")
        if (trackCount == 1) {
            sb.append(getString(R.string.library_track_count))
        } else {
            sb.append(getString(R.string.library_tracks_count))
        }

        return sb.toString()
    }

    private fun getLength(entities: Array<AdapterEntity>): String {
        val sb = StringBuilder()

        var length = 0
        for (e in entities) {
            length += (e as LibraryAdapterEntity).getEntity().getMetaLength() ?: 0
        }
        val hours = length / 3600
        val remaining = length % 3600
        val minutes = remaining / 60
        val seconds = remaining % 60
        if (hours > 0) {
            sb.append(String.format("%d:%02d:%02d", hours, minutes, seconds))
        } else {
            sb.append(String.format("%d:%02d", minutes, seconds))
        }

        return sb.toString()
    }

    private fun getYear(entities: Array<AdapterEntity>): String {
        val years = TreeSet<Int>()
        for (e in entities) {
            val year = (e as LibraryAdapterEntity).getEntity().getMetaYear()
            if (year != null) {
                years.add(year)
            }
        }

        val sb = StringBuilder()
        for (year in years) {
            if (sb.isNotEmpty()) {
                sb.append(", ")
            }
            sb.append(year)
        }

        return sb.toString()
    }

    private fun getGenre(entities: Array<AdapterEntity>): String {
        val genres = TreeSet<String>()
        for (e in entities) {
            val genre = (e as LibraryAdapterEntity).getEntity().getMetaGenre()
            if (genre != null) {
                genres.add(genre)
            }
        }

        val sb = StringBuilder()
        for (genre in genres) {
            if (sb.isNotEmpty()) {
                sb.append(", ")
            }
            sb.append(genre)
        }

        return sb.toString()
    }

    private fun getAllArtists(entities: Array<AdapterEntity>?): MutableSet<String?> {
        val artists = HashSet<String?>()
        if (entities != null) {
            for (entity in entities) {
                artists.add((entity as LibraryAdapterEntity).getEntity().getArtist())
            }
        }
        return artists
    }

    private fun getArtist(entities: Array<AdapterEntity>?): String? {
        val artists = getAllArtists(entities)
        return if (artists.size == 1) artists.iterator().next() else null
    }

    private fun getAlbum(entities: Array<AdapterEntity>?): String? {
        val albums = HashSet<String?>()
        if (entities != null) {
            for (entity in entities) {
                albums.add((entity as LibraryAdapterEntity).getEntity().getAlbum())
            }
        }

        return if (albums.size == 1) albums.iterator().next() else null
    }

    private fun isLandscape(): Boolean {
        val rotation = this.display?.rotation ?: Surface.ROTATION_0
        return rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270
    }
}
