package net.prezz.mpr.ui.adapter

import android.annotation.SuppressLint

import net.prezz.mpr.model.LibraryEntity

class AlbumAdapterEntity(entity: LibraryEntity, private val sortByArtist: Boolean) : LibraryAdapterEntity(entity) {

    override fun getSectionIndexText(): String {
        return if (sortByArtist) getSubText() else getText()
    }

    override fun getText(): String {
        val album = if (sortByArtist) getEntity().getAlbum() else getEntity().getMetaAlbum()
        return album ?: ""
    }

    override fun getSubText(): String {
        val artist = if (sortByArtist) getEntity().getMetaArtist() else getEntity().getLookupArtist()
        return artist ?: ""
    }

    @SuppressLint("DefaultLocale")
    override fun getTime(): String {
        val metaLength = getEntity().getMetaLength()
        return if (metaLength != null) String.format("%d:%02d", metaLength.toInt() / 60, metaLength.toInt() % 60) else ""
    }

    override fun getData(): String {
        val playedDaysAgo = getEntity().getPlayedDaysAgo()
        return if (playedDaysAgo != null) String.format("%d", playedDaysAgo) else "-"
    }

    companion object {
        private const val serialVersionUID = -8801955200935156385L
    }
}
