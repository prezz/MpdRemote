package net.prezz.mpr.ui.adapter

import net.prezz.mpr.model.PlaylistEntity

class PlaylistAdapterEntity(private val entity: PlaylistEntity, private val showPriority: Boolean) : AdapterEntity {

    fun getEntity(): PlaylistEntity {
        return entity
    }

    override fun getSectionIndexText(): String {
        return getText()
    }

    override fun getText(): String {
        val sb = StringBuilder()

        val track = entity.getTrack()
        if (track != null) {
            sb.append(String.format("%02d - ", track))
        }

        val title = entity.getTitle()
        if (title != null) {
            sb.append(title)
        } else {
            sb.append(entity.getUriEntity().getUriFilname())
        }

        return sb.toString()
    }

    fun getSubText(): String {
        val sb = StringBuilder()

        val artist = entity.getArtist()
        if (artist != null) {
            sb.append(artist)
        }

        val album = entity.getAlbum()
        if (album != null) {
            sb.append(" - ")
            sb.append(album)
        }

        if (sb.isEmpty()) {
            val name = entity.getName()
            if (name != null) {
                sb.append(name)
            } else {
                sb.append(entity.getUriEntity().getUriFilname())
            }
        }

        return sb.toString()
    }

    fun getTime(): String {
        val time = entity.getTime()
        return if (time != null) String.format("%d:%02d", time / 60, time % 60) else ""
    }

    fun getPriority(): String? {
        val priority = entity.getPriority()
        return if (showPriority && priority != null) priority.toString() else null
    }

    fun prioritized(): Boolean {
        val priority = entity.getPriority()
        return priority != null && priority > 0
    }

    companion object {
        private const val serialVersionUID = -1787348995732040871L
    }
}
