package net.prezz.mpr.model

import java.io.Serializable

data class StoredPlaylistEntity(val playlistName: String) : Serializable {

    companion object {
        private const val serialVersionUID = -3601721304916558267L
    }
}
