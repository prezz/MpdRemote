package net.prezz.mpr.model.external.lastfm

import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.ArrayList
import java.util.LinkedList

import net.prezz.mpr.model.external.CoverService
import net.prezz.mpr.model.external.InfoService

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException

import android.net.Uri
import android.util.Log
import android.util.Xml

class LastFmCoverAndInfoService : CoverService, InfoService {

    override fun getCoverUrls(artist: String?, album: String?): List<String> {
        try {
            val request = createCoverRequest(API_KEY, artist, album)
                ?: return emptyList()

            val connection = createHttpConnection(request)
            try {
                val inputStream = connection.inputStream
                try {
                    return parseCoverResponse(inputStream)
                } finally {
                    inputStream.close()
                }
            } finally {
                connection.disconnect()
            }
        } catch (ex: Exception) {
            Log.e(LastFmCoverAndInfoService::class.java.name, "Error getting covers from Last.FM", ex)
        }

        return emptyList()
    }

    override fun getArtistInfoUrls(artist: String?): List<String>? {
        try {
            val request = createArtistInfoRequest(API_KEY, artist)
                ?: return null

            val connection = createHttpConnection(request)
            try {
                val inputStream = connection.inputStream
                try {
                    return parseArtistAndAlbumInfoResponse(artist, null, inputStream)
                } finally {
                    inputStream.close()
                }
            } finally {
                connection.disconnect()
            }
        } catch (ex: Exception) {
            Log.e(LastFmCoverAndInfoService::class.java.name, "Error searching Last.fm for artist", ex)
        }

        return emptyList()
    }

    override fun getAlbumInfoUrls(artist: String?, album: String?): List<String>? {
        try {
            val request = createAlbumInfoRequest(API_KEY, artist, album)
                ?: return null

            val connection = createHttpConnection(request)
            try {
                val inputStream = connection.inputStream
                try {
                    return parseArtistAndAlbumInfoResponse(album, artist, inputStream)
                } finally {
                    inputStream.close()
                }
            } finally {
                connection.disconnect()
            }
        } catch (ex: Exception) {
            Log.e(LastFmCoverAndInfoService::class.java.name, "Error searching Last.fm for album", ex)
        }

        return emptyList()
    }

    private fun createCoverRequest(apiKey: String, artist: String?, album: String?): String? {
        if (album != null) {
            val builder = Uri.parse(URL).buildUpon()
            builder.appendQueryParameter("method", "album.getinfo")
            builder.appendQueryParameter("api_key", apiKey)
            builder.appendQueryParameter("artist", if (artist.isNullOrEmpty()) VARIOUS_ARTISTS else artist)
            builder.appendQueryParameter("album", album)
            return builder.build().toString()
        }

        return null
    }

    private fun createArtistInfoRequest(apiKey: String, artist: String?): String? {
        if (artist != null) {
            val builder = Uri.parse(URL).buildUpon()
            builder.appendQueryParameter("method", "artist.search")
            builder.appendQueryParameter("api_key", apiKey)
            builder.appendQueryParameter("artist", artist)
            return builder.build().toString()
        }

        return null
    }

    private fun createAlbumInfoRequest(apiKey: String, artist: String?, album: String?): String? {
        if (artist != null && album != null) {
            val builder = Uri.parse(URL).buildUpon()
            builder.appendQueryParameter("method", "album.search")
            builder.appendQueryParameter("api_key", apiKey)
            builder.appendQueryParameter("artist", artist)
            builder.appendQueryParameter("album", album)
            return builder.build().toString()
        }

        return null
    }

    @Throws(IOException::class)
    private fun createHttpConnection(request: String): HttpURLConnection {
        val url = URL(request)
        val connection = url.openConnection() as HttpURLConnection
        connection.connectTimeout = 5000
        connection.readTimeout = 10000
        return connection
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun parseCoverResponse(inputStream: InputStream): List<String> {
        val result = ArrayList<String>()

        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(inputStream, "UTF-8")

        var tagName: String? = null
        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    tagName = parser.name
                    if ("image" == tagName) {
                        val size = parser.getAttributeValue(null, "size")
                        if ("extralarge" == size) {
                            val coverUrl = parser.nextText()
                            if (!coverUrl.isNullOrEmpty() && !result.contains(coverUrl)) {
                                result.add(coverUrl)
                            }
                        }
                    }
                }
            }
            eventType = parser.next()
        }

        return result
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun parseArtistAndAlbumInfoResponse(queryName: String?, queryArtist: String?, inputStream: InputStream): List<String> {
        val result = LinkedList<String>()

        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(inputStream, "UTF-8")

        var tagName: String? = null
        var nameMatch = false
        var artistMatch = (queryArtist == null)
        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    tagName = parser.name

                    if ("name" == tagName) {
                        val text = parser.nextText()
                        nameMatch = text.equals(queryName, ignoreCase = true)
                    }

                    if (queryArtist != null && "artist" == tagName) {
                        val text = parser.nextText()
                        artistMatch = text.equals(queryArtist, ignoreCase = true)
                    }

                    if ("url" == tagName) {
                        val text = parser.nextText()
                        if (!text.isNullOrEmpty() && !result.contains(text)) {
                            if (nameMatch && artistMatch) {
                                result.addFirst(text)
                            } else {
                                result.add(text)
                            }
                            nameMatch = false
                            artistMatch = (queryArtist == null)
                        }
                    }
                }
            }
            eventType = parser.next()
        }

        return result
    }

    companion object {
        private const val VARIOUS_ARTISTS = "Various Artists"
        private const val URL = "https://ws.audioscrobbler.com/2.0/"
        private const val API_KEY = "debdc3f32ba3f7b5b4b915da3ce04852"
    }
}
