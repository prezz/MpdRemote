package net.prezz.mpr.ui

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import android.widget.TextView.OnEditorActionListener
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import net.prezz.mpr.R
import net.prezz.mpr.databinding.ActivityCoverBinding
import net.prezz.mpr.model.TaskHandle
import net.prezz.mpr.model.external.CoverReceiver
import net.prezz.mpr.model.external.ExternalInformationService
import net.prezz.mpr.model.external.UrlReceiver
import net.prezz.mpr.ui.helpers.VolumeButtonsHelper
import net.prezz.mpr.ui.helpers.setupToolbar
import net.prezz.mpr.ui.state.DataState

class CoverActivity : AppCompatActivity(), OnEditorActionListener {

    private lateinit var binding: ActivityCoverBinding

    private var coverIndex = 0
    private var coverUrls: Array<String>? = null
    private var getCoverHandle: TaskHandle = TaskHandle.NULL_HANDLE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityCoverBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupToolbar()

        val artist = getArgument(ARTIST_ARGUMENT_KEY)
        if (artist != null) {
            binding.coverArtistText.setText(artist)
        }

        val album = getArgument(ALBUM_ARGUMENT_KEY)
        if (album != null) {
            binding.coverAlbumText.setText(album)
        }
        binding.coverAlbumText.setOnEditorActionListener(this)

        binding.coverButtonPrevious.setOnClickListener { onPreviousClick(it) }
        binding.coverButtonSelect.setOnClickListener { onSelectClick(it) }
        binding.coverButtonNext.setOnClickListener { onNextClick(it) }

        getCoverHandle = TaskHandle.NULL_HANDLE

        val dataState = DataState.get(this)
        coverIndex = dataState.getData(INDEX_SAVED_INSTANCE_STATE, 0) as Int

        // restore entities if loaded into memory again (or after rotation)
        @Suppress("UNCHECKED_CAST")
        (dataState.getData(URLS_SAVED_INSTANCE_STATE, null) as? Array<String>)?.let {
            coverUrls = it
        }

        if (coverUrls != null) {
            toggleButtonEnablement(false)
            getCover(coverIndex)
        } else {
            getCoverUrls(artist, album)
        }
    }

    override fun onStop() {
        super.onStop()

        getCoverHandle.cancelTask()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        val dataState = DataState.get(this)
        dataState.setData(INDEX_SAVED_INSTANCE_STATE, coverIndex)
        dataState.setData(URLS_SAVED_INSTANCE_STATE, coverUrls)

        super.onSaveInstanceState(outState)
    }

    override fun onEditorAction(v: TextView?, actionId: Int, event: KeyEvent?): Boolean {
        if (actionId == EditorInfo.IME_ACTION_SEARCH) {
            getCoverUrls(binding.coverArtistText.text.toString(), binding.coverAlbumText.text.toString())

            WindowCompat.getInsetsController(window, binding.coverAlbumText).hide(WindowInsetsCompat.Type.ime())
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

    private fun onPreviousClick(view: View) {
        if (--coverIndex < 0) {
            coverIndex = coverUrls!!.size - 1
        }

        toggleButtonEnablement(false)
        getCover(coverIndex)
    }

    private fun onSelectClick(view: View) {
        if (coverIndex >= 0 && coverIndex < coverUrls!!.size) {
            val returnValue = Intent()
            returnValue.putExtra(URL_RESULT_KEY, coverUrls!![coverIndex])
            setResult(RESULT_OK, returnValue)
            finish()
        }
    }

    private fun onNextClick(view: View) {
        if (++coverIndex >= coverUrls!!.size) {
            coverIndex = 0
        }

        toggleButtonEnablement(false)
        getCover(coverIndex)
    }

    private fun getCoverUrls(artist: String?, album: String?) {
        if (!album.isNullOrEmpty()) {
            toggleButtonEnablement(false)

            binding.coverAlbumImage.setImageBitmap(null)
            binding.coverIndexText.setText(R.string.cover_searching_covers)

            getCoverHandle.cancelTask()
            getCoverHandle = ExternalInformationService.getCoverUrls(artist, album, UrlReceiver { urls ->
                coverIndex = 0
                coverUrls = urls
                getCover(coverIndex)
            })
        }
    }

    private fun getCover(index: Int) {
        setCurrentCoverText(index)
        if (coverIndex >= 0 && coverIndex < coverUrls!!.size) {
            getCoverHandle.cancelTask()
            getCoverHandle = ExternalInformationService.getCover(coverUrls!![index], CoverReceiver { bitmap ->
                toggleButtonEnablement(true)
                binding.coverAlbumImage.setImageBitmap(bitmap)
            })
        }
    }

    private fun setCurrentCoverText(index: Int) {
        val urls = coverUrls
        if (urls != null && urls.isNotEmpty()) {
            val sb = StringBuilder()
            sb.append(index + 1)
            sb.append("/")
            sb.append(urls.size)
            binding.coverIndexText.text = sb.toString()
        } else {
            binding.coverIndexText.setText(R.string.cover_no_covers)
        }
    }

    private fun getArgument(argumentKey: String): String? {
        return this.intent.extras?.getString(argumentKey)
    }

    private fun toggleButtonEnablement(enabled: Boolean) {
        binding.coverButtonPrevious.isEnabled = enabled
        binding.coverButtonSelect.isEnabled = enabled
        binding.coverButtonNext.isEnabled = enabled
    }

    companion object {
        const val ARTIST_ARGUMENT_KEY = "artist"
        const val ALBUM_ARGUMENT_KEY = "album"

        const val URL_RESULT_KEY = "url"

        const val INDEX_SAVED_INSTANCE_STATE = "saved_index"
        const val URLS_SAVED_INSTANCE_STATE = "saved_urls"
    }
}
