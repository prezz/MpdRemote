package net.prezz.mpr.model.external

interface InfoService {

    fun getArtistInfoUrls(artist: String?): List<String>?

    fun getAlbumInfoUrls(artist: String?, album: String?): List<String>?
}
