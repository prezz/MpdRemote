package net.prezz.mpr.ui.adapter

import net.prezz.mpr.R
import net.prezz.mpr.model.LibraryEntity
import net.prezz.mpr.ui.ApplicationActivator

class ArtistAdapterEntity(entity: LibraryEntity) : LibraryAdapterEntity(entity) {

    override fun getText(): String {
        val entity = getEntity()
        return when (entity.getTag()) {
            LibraryEntity.Tag.ARTIST -> entity.getMetaArtist() ?: ""
            LibraryEntity.Tag.ALBUM_ARTIST -> entity.getMetaAlbumArtist() ?: ""
            LibraryEntity.Tag.COMPOSER -> entity.getComposer() ?: ""
            else -> ""
        }
    }

    override fun getSubText(): String {
        val sb = StringBuilder()

        val metaCount = getEntity().getMetaCount()
        if (metaCount != null) {
            sb.append(metaCount)
            sb.append(" ")
            if (metaCount.toInt() == 1) {
                sb.append(ApplicationActivator.context.getString(R.string.library_track_count))
            } else {
                sb.append(ApplicationActivator.context.getString(R.string.library_tracks_count))
            }
        }

        return sb.toString()
    }

    override fun getTime(): String {
        return ""
    }

    override fun getData(): String {
        return ""
    }

    fun getEntityText(): String {
        val entity = getEntity()
        return when (entity.getTag()) {
            LibraryEntity.Tag.ARTIST -> entity.getArtist() ?: ""
            LibraryEntity.Tag.ALBUM_ARTIST -> entity.getAlbumArtist() ?: ""
            LibraryEntity.Tag.COMPOSER -> entity.getComposer() ?: ""
            else -> ""
        }
    }

    companion object {
        private const val serialVersionUID = 8835914650261723361L
    }
}
