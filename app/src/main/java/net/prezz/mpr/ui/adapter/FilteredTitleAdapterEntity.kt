package net.prezz.mpr.ui.adapter

import net.prezz.mpr.model.LibraryEntity
import android.annotation.SuppressLint

class FilteredTitleAdapterEntity(entity: LibraryEntity) : LibraryAdapterEntity(entity) {

    @SuppressLint("DefaultLocale")
    override fun getText(): String {
        return getEntity().getTitle() ?: ""
    }

    @SuppressLint("DefaultLocale")
    override fun getSubText(): String {
        val sb = StringBuilder()

        val album = getEntity().getAlbum()
        if (album != null && !album.isEmpty()) {
            sb.append(album)
        }

        val year = getEntity().getMetaYear()
        if (year != null) {
            if (sb.length > 0) {
                sb.append(", ")
            }
            sb.append(year)
        }

        val genre = getEntity().getMetaGenre()
        if (genre != null) {
            if (sb.length > 0) {
                sb.append(", ")
            }
            sb.append(genre)
        }

        return sb.toString()
    }

    @SuppressLint("DefaultLocale")
    override fun getTime(): String {
        val metaLength = getEntity().getMetaLength()
        return if (metaLength != null) String.format("%d:%02d", metaLength.toInt() / 60, metaLength.toInt() % 60) else ""
    }

    override fun getData(): String {
        return ""
    }

    companion object {
        private const val serialVersionUID = -8867175839235949379L
    }
}
