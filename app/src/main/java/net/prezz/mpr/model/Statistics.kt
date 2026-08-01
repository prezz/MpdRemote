package net.prezz.mpr.model

class Statistics {

    private var artists: Int? = null
    private var albums: Int? = null
    private var songs: Int? = null
    private var updatingJobId: Int? = null

    fun getArtists(): Int? {
        return artists
    }

    fun setArtists(artists: Int?) {
        this.artists = artists
    }

    fun getAlbums(): Int? {
        return albums
    }

    fun setAlbums(albums: Int?) {
        this.albums = albums
    }

    fun getSongs(): Int? {
        return songs
    }

    fun setSongs(songs: Int?) {
        this.songs = songs
    }

    fun setUpdatingJob(updatingJobId: Int?) {
        this.updatingJobId = updatingJobId
    }

    fun getUpdatingJob(): Int? {
        return updatingJobId
    }
}
