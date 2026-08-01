package net.prezz.mpr.model.external

import java.net.URL
import java.util.ArrayList
import java.util.LinkedHashSet
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException

import net.prezz.mpr.model.FutureTaskHandleImpl
import net.prezz.mpr.model.TaskHandle
import net.prezz.mpr.model.external.cache.CoverCache
import net.prezz.mpr.model.external.lastfm.LastFmCoverAndInfoService
import net.prezz.mpr.model.external.local.MpdCoverService
import net.prezz.mpr.mpd.connection.MpdConnection
import net.prezz.mpr.ui.ApplicationActivator
import net.prezz.mpr.R
import net.prezz.mpr.ui.mpd.MpdPlayerSettings

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import android.util.Log

object ExternalInformationService {

    const val NULL_URL = ""
    private const val MAX_HEIGHT = 1024
    private val lock = Any()

    private const val ASCII =
        ("AaEeIiOoUu"    // grave
                + "AaEeIiOoUuYy"  // acute
                + "AaEeIiOoUuYy"  // circumflex
                + "AaOoNn"        // tilde
                + "AaEeIiOoUuYy"  // umlaut
                + "Aa"            // ring
                + "Cc"            // cedilla
                + "OoUu")          // double acute

    private const val UNICODE =
        ("ÀàÈèÌìÒòÙù"
                + "ÁáÉéÍíÓóÚúÝý"
                + "ÂâÊêÎîÔôÛûŶŷ"
                + "ÃãÕõÑñ"
                + "ÄäËëÏïÖöÜüŸÿ"
                + "Åå"
                + "Çç"
                + "ŐőŰű")

    private val coverCache = CoverCache()
    private val mpdCoverService = MpdCoverService()
    private val lastFmCoverAndInfoService = LastFmCoverAndInfoService()
    private val executor = Executors.newSingleThreadExecutor()
    private val handler = Handler(Looper.getMainLooper())

    fun getCover(artist: String?, album: String?, maxHeight: Int?, coverReceiver: CoverReceiver): TaskHandle {

        val task = Runnable {
            synchronized(lock) {
                try {
                    val artistParam = artist ?: ""
                    val albumParam = album ?: ""

                    var coverData: ByteArray? = null

                    var coverUrl = coverCache.getCoverUrl(artistParam, albumParam)
                    if (coverUrl == null) {
                        coverUrl = getCoverUrlInternal(artistParam, albumParam)

                        if (NULL_URL != coverUrl) {
                            coverData = downloadCoverImage(coverUrl)
                            if (coverData != null) {
                                coverCache.insertCoverImage(coverUrl, coverData)
                            } else {
                                coverUrl = NULL_URL
                            }
                        }

                        coverCache.insertCoverUrl(artistParam, albumParam, coverUrl)
                    }

                    if (NULL_URL == coverUrl) {
                        val cover = getNoCoverImage(maxHeight)
                        postResult(cover, coverReceiver)
                        return@Runnable
                    }

                    if (coverData == null) {
                        coverData = coverCache.getCoverImage(coverUrl)
                    }

                    if (coverData != null) {
                        try {
                            val bitmap = BitmapFactory.decodeByteArray(coverData, 0, coverData.size)
                            val cover = scaleImage(maxHeight, bitmap)
                            postResult(cover, coverReceiver)
                            return@Runnable
                        } catch (ex: Exception) {
                            Log.e(ExternalInformationService::class.java.name, "Error decoding byte array to bitmap", ex)
                            coverData = null
                        }
                    }

                    coverCache.deleteCoverImageIfLastUsage(coverUrl)
                    coverCache.deleteCoverUrl(artistParam, albumParam)

                    coverUrl = getCoverUrlInternal(artistParam, albumParam)

                    if (NULL_URL != coverUrl) {
                        coverData = downloadCoverImage(coverUrl)
                        if (coverData != null) {
                            coverCache.insertCoverImage(coverUrl, coverData)
                        } else {
                            coverUrl = NULL_URL
                        }
                    }

                    coverCache.insertCoverUrl(artistParam, albumParam, coverUrl)

                    if (NULL_URL == coverUrl) {
                        val cover = getNoCoverImage(maxHeight)
                        postResult(cover, coverReceiver)
                        return@Runnable
                    }

                    if (coverData != null) {
                        val bitmap = BitmapFactory.decodeByteArray(coverData, 0, coverData.size)
                        val cover = scaleImage(maxHeight, bitmap)
                        postResult(cover, coverReceiver)
                        return@Runnable
                    }

                    val cover = getNoCoverImage(maxHeight)
                    postResult(cover, coverReceiver)
                    return@Runnable
                } catch (ex: Exception) {
                    Log.e(ExternalInformationService::class.java.name, "Error fetching cover", ex)
                }
            }
        }

        try {
            return FutureTaskHandleImpl(executor.submit(task))
        } catch (ex: RejectedExecutionException) {
            Log.e(ExternalInformationService::class.java.name, "Unable to load cover. Exection rejected", ex)
            return TaskHandle.NULL_HANDLE
        }
    }

    private fun getCoverUrlInternal(artistParam: String, albumParam: String): String {

        var coverUrlList = mpdCoverService.getCoverUrls(artistParam, albumParam)
        if (coverUrlList.isNotEmpty()) {
            return coverUrlList[0]
        }

        val artists = createQueryStrings(artistParam, false)
        val albums = createQueryStrings(albumParam, true)
        artists.add(null)

        for (artist in artists) {
            for (album in albums) {
                coverUrlList = lastFmCoverAndInfoService.getCoverUrls(artist, album)
                if (coverUrlList.isNotEmpty()) {
                    return coverUrlList[0]
                }
            }
        }

        return NULL_URL
    }

    fun getCover(url: String?, coverReceiver: CoverReceiver): TaskHandle {
        val task = Runnable {
            synchronized(lock) {
                try {
                    if (url.isNullOrEmpty()) {
                        val cover = getNoCoverImage(null)
                        postResult(cover, coverReceiver)
                        return@Runnable
                    }

                    val coverData = downloadCoverImage(url)
                    if (coverData == null) {
                        val cover = getNoCoverImage(null)
                        postResult(cover, coverReceiver)
                        return@Runnable
                    }

                    val bitmap = BitmapFactory.decodeByteArray(coverData, 0, coverData.size)
                    val cover = scaleImage(null, bitmap)
                    postResult(cover, coverReceiver)
                    return@Runnable
                } catch (ex: Exception) {
                    Log.e(ExternalInformationService::class.java.name, "Error fetching cover", ex)
                }
            }
        }

        try {
            return FutureTaskHandleImpl(executor.submit(task))
        } catch (ex: RejectedExecutionException) {
            Log.e(ExternalInformationService::class.java.name, "Unable to load cover. Exection rejected", ex)
            return TaskHandle.NULL_HANDLE
        }
    }

    fun getCoverUrls(artist: String?, album: String?, urlReceiver: UrlReceiver): TaskHandle {
        val task = Runnable {
            synchronized(lock) {
                val result: MutableSet<String> = LinkedHashSet<String>()

                try {
                    val artistParam = artist ?: ""
                    val albumParam = album ?: ""

                    val mpdUrlList = mpdCoverService.getCoverUrls(artistParam, albumParam)
                    result.addAll(mpdUrlList)

                    val artists = createQueryStrings(artistParam, false)
                    val albums = createQueryStrings(albumParam, true)
                    artists.add(null)

                    for (artist2 in artists) {
                        for (album2 in albums) {
                            val lastfmUrlList = lastFmCoverAndInfoService.getCoverUrls(artist2, album2)
                            result.addAll(lastfmUrlList)
                        }
                    }
                } catch (ex: Exception) {
                    Log.e(ExternalInformationService::class.java.name, "Error fetching cover", ex)
                }

                val urls = result.toTypedArray()
                postResult(urls, urlReceiver)
                return@Runnable
            }
        }

        try {
            return FutureTaskHandleImpl(executor.submit(task))
        } catch (ex: RejectedExecutionException) {
            Log.e(ExternalInformationService::class.java.name, "Unable to load cover. Exection rejected", ex)
            return TaskHandle.NULL_HANDLE
        }
    }

    fun setCoverUrl(artist: String?, album: String?, url: String?, maxHeight: Int?, coverReceiver: CoverReceiver?): TaskHandle {
        val task = Runnable {
            synchronized(lock) {
                try {
                    val artistParam = artist ?: ""
                    val albumParam = album ?: ""
                    val urlParam = if (url != null) url else NULL_URL

                    val existingUrl = coverCache.getCoverUrl(artistParam, albumParam)
                    if (existingUrl != null) {
                        coverCache.deleteCoverImageIfLastUsage(existingUrl)
                        coverCache.deleteCoverUrl(artistParam, albumParam)
                    }
                    coverCache.insertCoverUrl(artistParam, albumParam, urlParam)

                    if (coverReceiver != null) {
                        if (NULL_URL == urlParam) {
                            val cover = getNoCoverImage(maxHeight)
                            postResult(cover, coverReceiver)
                            return@Runnable
                        }

                        val coverData = downloadCoverImage(urlParam)
                        if (coverData == null) {
                            val cover = getNoCoverImage(maxHeight)
                            postResult(cover, coverReceiver)
                            return@Runnable
                        }
                        coverCache.insertCoverImage(urlParam, coverData)

                        val bitmap = BitmapFactory.decodeByteArray(coverData, 0, coverData.size)
                        val cover = scaleImage(maxHeight, bitmap)
                        postResult(cover, coverReceiver)
                        return@Runnable
                    }
                } catch (ex: Exception) {
                    Log.e(ExternalInformationService::class.java.name, "Error fetching cover", ex)
                }
            }
        }

        try {
            return FutureTaskHandleImpl(executor.submit(task))
        } catch (ex: RejectedExecutionException) {
            Log.e(ExternalInformationService::class.java.name, "Unable to load cover. Exection rejected", ex)
            return TaskHandle.NULL_HANDLE
        }
    }

    fun getArtistInfoUrls(artist: String?, urlReceiver: UrlReceiver): TaskHandle {
        val task = Runnable {
            synchronized(lock) {
                val result: MutableList<String> = ArrayList<String>()
                try {
                    val artists = createQueryStrings(artist, false)

                    for (artist2 in artists) {
                        val infoUrlList = lastFmCoverAndInfoService.getArtistInfoUrls(artist2)
                        result.addAll(infoUrlList!!)
                    }
                } catch (ex: Exception) {
                    Log.e(ExternalInformationService::class.java.name, "Error getting artist info", ex)
                }

                val urls = result.toTypedArray()
                postResult(urls, urlReceiver)
                return@Runnable
            }
        }

        try {
            return FutureTaskHandleImpl(executor.submit(task))
        } catch (ex: RejectedExecutionException) {
            Log.e(ExternalInformationService::class.java.name, "Unable to artist info. Exection rejected", ex)
            return TaskHandle.NULL_HANDLE
        }
    }

    fun getAlbumInfoUrls(artist: String?, album: String?, urlReceiver: UrlReceiver): TaskHandle {
        val task = Runnable {
            synchronized(lock) {
                val result: MutableList<String> = ArrayList<String>()
                try {
                    val artists = createQueryStrings(artist, false)
                    val albums = createQueryStrings(album, true)

                    for (artist2 in artists) {
                        for (album2 in albums) {
                            val infoUrlList = lastFmCoverAndInfoService.getAlbumInfoUrls(artist2, album2)
                            result.addAll(infoUrlList!!)
                        }
                    }
                } catch (ex: Exception) {
                    Log.e(ExternalInformationService::class.java.name, "Error getting album info", ex)
                }

                val urls = result.toTypedArray()
                postResult(urls, urlReceiver)
                return@Runnable
            }
        }

        try {
            return FutureTaskHandleImpl(executor.submit(task))
        } catch (ex: RejectedExecutionException) {
            Log.e(ExternalInformationService::class.java.name, "Unable to album info. Exection rejected", ex)
            return TaskHandle.NULL_HANDLE
        }
    }

    private fun postResult(bitmap: Bitmap?, coverReceiver: CoverReceiver) {
        handler.post {
            coverReceiver.receiveCover(bitmap)
        }
    }

    private fun postResult(urls: Array<String>, urlReceiver: UrlReceiver) {
        handler.post {
            urlReceiver.receiveUrls(urls)
        }
    }

    private fun getNoCoverImage(maxHeight: Int?): Bitmap? {
        val bitmap = BitmapFactory.decodeResource(ApplicationActivator.context.resources, R.drawable.no_cover)
        return scaleImage(maxHeight, bitmap)
    }

    private fun scaleImage(scaledHeight: Int?, coverImage: Bitmap): Bitmap {
        try {
            val height = if (scaledHeight != null) minOf(scaledHeight.toInt(), MAX_HEIGHT) else MAX_HEIGHT
            val width = ((height.toFloat() / coverImage.height.toFloat()) * coverImage.width).toInt()
            return Bitmap.createScaledBitmap(coverImage, width, height, true)
        } catch (ex: Exception) {
            Log.e(ExternalInformationService::class.java.name, "Error scaling cover", ex)
        }

        return coverImage
    }

    private fun createQueryStrings(input: String?, stripParentheseSuffix: Boolean): MutableList<String?> {
        val result: MutableList<String?> = ArrayList<String?>()

        if (!input.isNullOrEmpty()) {
            val ascii = convertNonAscii(input)
            if (ascii != null) {
                if (stripParentheseSuffix) {
                    val strippedAsciiParenthese = stripParentheseSuffix(ascii)
                    if (strippedAsciiParenthese != null) {
                        result.add(strippedAsciiParenthese)
                    }
                    val strippedAsciiBracket = stripBracketSuffix(ascii)
                    if (strippedAsciiBracket != null) {
                        result.add(strippedAsciiBracket)
                    }
                }
                result.add(ascii)
            }

            if (stripParentheseSuffix) {
                val strippedParenthese = stripParentheseSuffix(input!!)
                if (strippedParenthese != null) {
                    result.add(strippedParenthese)
                }
                val strippedBracket = stripBracketSuffix(input!!)
                if (strippedBracket != null) {
                    result.add(strippedBracket)
                }
            }
            result.add(input)
        }

        return result
    }

    private fun convertNonAscii(s: String?): String? {
        var converted = false

        val sb = StringBuilder()
        val l = s!!.length
        for (i in 0 until l) {
            val c = s.get(i)
            val pos = UNICODE.indexOf(c)
            if (pos > -1) {
                converted = true
                sb.append(ASCII.get(pos))
            } else {
                sb.append(c)
            }
        }

        return if (converted) sb.toString() else null
    }

    private fun stripParentheseSuffix(input: String): String? {
        if (input.endsWith(")")) {
            val end = input.lastIndexOf("(")
            if (end > 0) {
                val stripped = input.substring(0, end)
                return stripped.trim()
            }
        }

        return null
    }

    private fun stripBracketSuffix(input: String): String? {
        if (input.endsWith("]")) {
            val end = input.lastIndexOf("[")
            if (end > 0) {
                val stripped = input.substring(0, end)
                return stripped.trim()
            }
        }

        return null
    }

    private fun downloadCoverImage(coverUrl: String?): ByteArray? {
        if (coverUrl == null) {
            return null
        }

        if (coverUrl.startsWith("mpd://")) {
            return downloadCoverImageFromMpd(coverUrl.substring(6))
        }

        if (coverUrl.startsWith("http://") || coverUrl.startsWith("https://")) {
            return downloadCoverImageFromHttp(coverUrl)
        }

        return null
    }

    private fun downloadCoverImageFromMpd(coverUrl: String): ByteArray? {
        val context = ApplicationActivator.context
        val mpdSettings = MpdPlayerSettings.create(context)
        val connection = MpdConnection(mpdSettings)

        try {
            connection.connect()
            if (connection.isMinimumVersion(0, 21, 0)) {
                var buffer: ByteArray? = null
                var size = 0
                var offset = 0

                do {
                    connection.writeCommand("albumart \"" + MpdConnection.escape(coverUrl) + "\" " + offset + "\n")

                    while (true) {
                        val line = connection.readLine() ?: break
                        if (line.startsWith(MpdConnection.OK)) {
                            break
                        }
                        if (line.startsWith(MpdConnection.ACK)) {
                            break
                        }
                        if (line.startsWith("size: ") && buffer == null) {
                            size = line.substring(6).toInt()
                            buffer = ByteArray(size)
                        }
                        if (line.startsWith("binary: ") && buffer != null) {
                            val length = line.substring(8).toInt()
                            val read = connection.readBinary(buffer, offset, length)
                            offset += read

                            if (read == 0) {
                                // if we fail to read anything set offset to size to break the loop
                                offset = size
                            }
                        }
                    }
                } while (offset < size)

                return buffer
            }
        } catch (ex: Exception) {
            Log.e(MpdCoverService::class.java.name, "Error checking if mpd cover exists", ex)
        } finally {
            connection.disconnect()
        }

        return null
    }

    private fun downloadCoverImageFromHttp(coverUrl: String): ByteArray? {
        try {
            val url = URL(coverUrl)
            val connection = url.openConnection()
            connection.connectTimeout = 5000
            connection.readTimeout = 10000

            // Read from the timeout-configured connection itself (a second url.openStream() would be
            // untimed and could hang), and use readBytes() so a missing/unknown Content-Length no
            // longer produces a negative-sized buffer.
            connection.getInputStream().use { stream ->
                return stream.readBytes()
            }
        } catch (ex: Exception) {
            Log.e(ExternalInformationService::class.java.name, "error downloading cover", ex)
        }

        return null
    }
}
