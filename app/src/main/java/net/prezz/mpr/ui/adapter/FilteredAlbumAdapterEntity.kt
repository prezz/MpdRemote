package net.prezz.mpr.ui.adapter

import android.annotation.SuppressLint
import net.prezz.mpr.R
import net.prezz.mpr.model.LibraryEntity
import net.prezz.mpr.ui.ApplicationActivator

class FilteredAlbumAdapterEntity(entity: LibraryEntity) : LibraryAdapterEntity(entity) {

    override fun getText(): String {
        return getEntity().getMetaAlbum() ?: ""
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
        private const val serialVersionUID = 3344502606040445346L
    }
}
