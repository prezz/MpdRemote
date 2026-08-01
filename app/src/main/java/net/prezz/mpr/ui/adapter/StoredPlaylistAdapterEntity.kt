package net.prezz.mpr.ui.adapter

import net.prezz.mpr.model.StoredPlaylistEntity
import java.io.Serializable

class StoredPlaylistAdapterEntity(private val entity: StoredPlaylistEntity) : Serializable {

    fun getEntity(): StoredPlaylistEntity {
        return entity
    }

    override fun toString(): String {
        return entity.playlistName
    }

    companion object {
        private const val serialVersionUID = -2314273911954285555L
    }
}
