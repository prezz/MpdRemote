package net.prezz.mpr.ui.adapter

import net.prezz.mpr.model.LibraryEntity

class SearchTitleAdapterEntity(entity: LibraryEntity) : LibraryAdapterEntity(entity) {

    override fun getSectionIndexText(): String {
        val sb = StringBuilder()

        val title = getEntity().getTitle()
        if (title != null) {
            sb.append(title)
        }

        val artist = getEntity().getArtist()
        if (artist != null) {
            sb.append(artist)
        }

        return sb.toString()
    }

    override fun getText(): String {
        return getEntity().getTitle()!!
    }

    override fun getSubText(): String {
        val artist = getEntity().getArtist()
        return artist ?: ""
    }

    override fun getTime(): String {
        return ""
    }

    override fun getData(): String {
        return ""
    }

    companion object {
        private const val serialVersionUID = 5519388736500542177L
    }
}
