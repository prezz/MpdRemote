package net.prezz.mpr.model.external.local

import android.content.Context
import android.util.Log

import net.prezz.mpr.model.UriEntity
import net.prezz.mpr.model.external.CoverService
import net.prezz.mpr.mpd.connection.MpdConnection
import net.prezz.mpr.mpd.database.MpdLibraryDatabaseHelper
import net.prezz.mpr.ui.ApplicationActivator
import net.prezz.mpr.ui.mpd.MpdPlayerSettings

import java.util.ArrayList
import java.util.HashMap
import java.util.HashSet

class MpdCoverService : CoverService {

    override fun getCoverUrls(artist: String?, album: String?): List<String> {
        try {
            val context = ApplicationActivator.context

            if (album != null) {
                val mpdSettings = MpdPlayerSettings.create(context)
                val databaseName = mpdSettings.getName()

                val files = getFileFromEachDirectories(context, databaseName, artist, album)
                val result = getValidUrls(mpdSettings, files)
                return result
            }
        } catch (ex: Exception) {
            Log.e(MpdCoverService::class.java.name, "Error getting covers from Local cover service", ex)
        }

        return emptyList()
    }

    companion object {

        private fun getFileFromEachDirectories(context: Context, databaseName: String?, artist: String?, album: String?): Set<String> {
            val result: MutableMap<String, String> = HashMap()

            val databaseHelper = MpdLibraryDatabaseHelper(context, databaseName!!)
            try {
                val c = databaseHelper.findUris(artist, album)
                try {
                    if (c.moveToFirst()) {
                        do {
                            val uri = c.getString(0)
                            val dir = uri.substring(0, uri.lastIndexOf(UriEntity.DIR_SEPERATOR))
                            result[dir] = uri
                        } while (c.moveToNext())
                    }
                } finally {
                    c.close()
                }
            } finally {
                databaseHelper.close()
            }

            return HashSet(result.values)
        }

        private fun getValidUrls(mpdSettings: MpdPlayerSettings, uris: Collection<String>): List<String> {
            val result = ArrayList<String>()

            for (uri in uris) {
                if (uriExists(mpdSettings, uri)) {
                    result.add("mpd://" + uri)
                }
            }

            return result
        }

        private fun uriExists(mpdSettings: MpdPlayerSettings, uri: String): Boolean {
            var result = false

            val connection = MpdConnection(mpdSettings)

            try {
                connection.connect()
                if (connection.isMinimumVersion(0, 21, 0)) {

                    connection.writeCommand("albumart \"" + MpdConnection.escape(uri) + "\" 0\n")

                    var exists = false
                    while (true) {
                        val line = connection.readLine() ?: break
                        if (line.startsWith(MpdConnection.OK)) {
                            result = exists
                            break
                        }
                        if (line.startsWith(MpdConnection.ACK)) {
                            break
                        }
                        if (line.startsWith("size: ")) {
                            val size = line.substring(6).toInt()
                            exists = (size <= 2097152) // 2MB
                        }
                        if (line.startsWith("binary: ")) {
                            val length = line.substring(8).toInt()
                            val buffer = ByteArray(length)
                            connection.readBinary(buffer, 0, length)
                        }
                    }
                }
            } catch (ex: Exception) {
                Log.e(MpdCoverService::class.java.name, "Error checking if mpd cover exists", ex)
            } finally {
                connection.disconnect()
            }

            return result
        }
    }
}
