package net.prezz.mpr.ui.adapter

import net.prezz.mpr.model.LibraryEntity
import android.annotation.SuppressLint

class FilteredTrackTitleAdapterEntity(entity: LibraryEntity) : LibraryAdapterEntity(entity) {

    override fun getSectionIndexText(): String {
        val sb = StringBuilder()

        val metaDisc = getEntity().getMetaDisc()
        if (metaDisc != null) {
            sb.append(metaDisc)
        }

        val metaTrack = getEntity().getMetaTrack()
        if (metaTrack != null) {
            sb.append(metaTrack)
        }

        val artist = getEntity().getMetaArtist()
        if (artist != null) {
            sb.append(artist)
        }

        val title = getEntity().getTitle()
        if (title != null) {
            sb.append(title)
        }

        return sb.toString()
    }

    @SuppressLint("DefaultLocale")
    override fun getText(): String {
        val sb = StringBuilder()

//        Integer metaDisc = getEntity().getMetaDisc();
//        if (metaDisc != null) {
//            sb.append(String.format("(%d) ", metaDisc));
//        }

        val metaTrack = getEntity().getMetaTrack()
        if (metaTrack != null) {
            sb.append(String.format("%02d - ", metaTrack))
        }

        sb.append(getEntity().getTitle())
        return sb.toString()
    }

    @SuppressLint("DefaultLocale")
    override fun getSubText(): String {
        val artist = getEntity().getMetaArtist()
        return artist ?: ""
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
        private const val serialVersionUID = 7762682442042782036L
    }
}
