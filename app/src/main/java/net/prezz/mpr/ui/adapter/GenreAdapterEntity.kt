package net.prezz.mpr.ui.adapter

import net.prezz.mpr.R
import net.prezz.mpr.model.LibraryEntity
import net.prezz.mpr.ui.ApplicationActivator

class GenreAdapterEntity(entity: LibraryEntity) : LibraryAdapterEntity(entity) {

    override fun getText(): String {
        return getEntity().getGenre()!!
    }

    override fun getSubText(): String {
        val sb = StringBuilder()

        val metaCount = getEntity().getMetaCount()
        if (metaCount != null) {
            sb.append(metaCount)
            sb.append(" ")
            if (metaCount.toInt() == 1) {
                sb.append(ApplicationActivator.context.getString(R.string.library_album_count))
            } else {
                sb.append(ApplicationActivator.context.getString(R.string.library_albums_count))
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

    companion object {
        private const val serialVersionUID = 2475536371192907039L
    }
}
