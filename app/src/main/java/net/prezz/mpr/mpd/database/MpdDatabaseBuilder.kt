package net.prezz.mpr.mpd.database

import kotlin.math.roundToInt

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Resources

import androidx.preference.PreferenceManager

import java.io.IOException

import net.prezz.mpr.R
import net.prezz.mpr.Utils
import net.prezz.mpr.mpd.command.MpdCommandHelper
import net.prezz.mpr.mpd.connection.MpdConnection
import net.prezz.mpr.ui.ApplicationActivator

object MpdDatabaseBuilder {

    enum class UpdateDatabaseResult { NO_ERROR, UPDATE_RUNNING_ERROR, TRACK_COUNT_MISMATCH_ERROR }

    private var lastDatabaseResult = UpdateDatabaseResult.NO_ERROR

    fun getLastDatabaseResult(): UpdateDatabaseResult {
        return lastDatabaseResult
    }

    @Throws(IOException::class)
    fun buildDatabase(connection: MpdConnection, libraryDatabaseHelper: MpdLibraryDatabaseHelper) {
        lastDatabaseResult = UpdateDatabaseResult.NO_ERROR

        var expectedTrackCount = 0
        var updatingJobId = 0

        val lines = connection.writeResponseCommandList(arrayOf("status\n", "stats\n"))
        for (line in lines[0]!!) {
            if (line.startsWith("updating_db: ")) {
                updatingJobId = Integer.decode(line.substring(13))
            }
        }

        if (updatingJobId != 0) {
            lastDatabaseResult = UpdateDatabaseResult.UPDATE_RUNNING_ERROR
            return
        }

        for (line in lines[1]!!) {
            if (line.startsWith("songs: ")) {
                expectedTrackCount = Integer.decode(line.substring(7))
            }
        }

        val properSorting = properSorting()

        var actualTrackCount = 0
        libraryDatabaseHelper.beginTransaction()
        try {
            libraryDatabaseHelper.cleanDatabase()

            connection.writeCommand("listallinfo\n")
            val record = MusicLibraryRecord()
            var add = false
            while (true) {
                val line = connection.readLine() ?: break
                if (line.startsWith(MpdConnection.OK)) {
                    break
                }
                if (line.startsWith(MpdConnection.ACK)) {
                    throw IOException("Error reading MPD response: " + line)
                }

                if (line.startsWith("file: ")) {
                    if (add) {
                        libraryDatabaseHelper.addMusicEntity(record)
                        actualTrackCount++
                    }
                    record.clear()
                    add = true
                    record.setUri(line.substring(6))
                }
                if (line.startsWith("Artist: ")) {
                    val artist = line.substring(8)
                    record.setArtist(artist)

                    val metaArtist = if (properSorting) Utils.moveInsignificantWordsLast(artist) else artist
                    record.setMetaArtist(metaArtist)
                }
                if (line.startsWith("AlbumArtist: ")) {
                    val albumArtist = line.substring(13)
                    record.setAlbumArtist(albumArtist)

                    val metaAlbumArtist = if (properSorting) Utils.moveInsignificantWordsLast(albumArtist) else albumArtist
                    record.setMetaAlbumArtist(metaAlbumArtist)
                }
                if (line.startsWith("Composer: ")) {
                    record.setComposer(line.substring(10))
                }
                if (line.startsWith("Artist: ")) {
                    record.setArtist(line.substring(8))
                }
                if (line.startsWith("Album: ")) {
                    val album = line.substring(7)
                    record.setAlbum(line.substring(7))

                    val metaAlbum = if (properSorting) Utils.moveInsignificantWordsLast(album) else album
                    record.setMetaAlbum(metaAlbum)
                }
                if (line.startsWith("Title: ")) {
                    record.setTitle(line.substring(7))
                }
                if (line.startsWith("Disc: ")) {
                    record.setDisc(MpdCommandHelper.getDecimalNumber(line.substring(6)))
                }
                if (line.startsWith("Track: ")) {
                    record.setTrack(MpdCommandHelper.getDecimalNumber(line.substring(7)))
                }
                if (line.startsWith("Genre: ")) {
                    record.setGenre(line.substring(7))
                }
                if (line.startsWith("Date: ")) {
                    record.setYear(parseInteger(line.substring(6)))
                }
                if (line.startsWith("duration: ") && connection.isMinimumVersion(0, 22, 0)) {
                    record.setLength(parseFloatToInteger(line.substring(10)))
                }
                if (line.startsWith("Time: ") && !connection.isMinimumVersion(0, 22, 0)) { // deprecated
                    record.setLength(parseInteger(line.substring(6)))
                }
                if (line.startsWith("playlist: ")) {
                    val uri = line.substring(10)
                    libraryDatabaseHelper.addPlaylistEntity(uri)
                }
            }

            if (add) {
                libraryDatabaseHelper.addMusicEntity(record)
                actualTrackCount++
            }

            libraryDatabaseHelper.setTransactionSuccessful()
        } finally {
            libraryDatabaseHelper.endTransaction()
        }

        if (expectedTrackCount != actualTrackCount) {
            lastDatabaseResult = UpdateDatabaseResult.TRACK_COUNT_MISMATCH_ERROR
        }
    }

    private fun parseFloatToInteger(floating: String): Int? {
        try {
            return floating.toFloat().roundToInt()
        } catch (ex: Exception) {
        }
        return null
    }

    private fun parseInteger(integer: String): Int? {
        try {
            return Integer.decode(integer)
        } catch (ex: Exception) {
        }
        return null
    }

    private fun properSorting(): Boolean {
        val context: Context = ApplicationActivator.context
        val sharedPreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val resources: Resources = context.resources
        return sharedPreferences.getBoolean(resources.getString(R.string.settings_library_proper_sort_key), false)
    }
}
