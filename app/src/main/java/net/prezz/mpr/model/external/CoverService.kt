package net.prezz.mpr.model.external

interface CoverService {

    fun getCoverUrls(artist: String?, album: String?): List<String>
}
