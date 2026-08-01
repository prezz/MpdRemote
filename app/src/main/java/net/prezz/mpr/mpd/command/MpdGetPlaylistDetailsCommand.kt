package net.prezz.mpr.mpd.command

import kotlin.math.roundToInt

import net.prezz.mpr.model.PlaylistEntity
import net.prezz.mpr.model.StoredPlaylistEntity
import net.prezz.mpr.mpd.MpdPartitionProvider
import net.prezz.mpr.mpd.connection.MpdConnection
import java.io.IOException
import java.util.LinkedList

class MpdGetPlaylistDetailsCommand(storedPlaylist: StoredPlaylistEntity, partitionProvider: MpdPartitionProvider) :
    MpdConnectionCommand<StoredPlaylistEntity, Array<PlaylistEntity>>(storedPlaylist, partitionProvider) {

    @Throws(Exception::class)
    override fun doExecute(connection: MpdConnection, storedPlaylist: StoredPlaylistEntity): Array<PlaylistEntity> {
        val builder = PlaylistEntity.createBuilder()

        val result: MutableList<PlaylistEntity> = LinkedList()

        connection.writeCommand("listplaylistinfo \"${MpdCommandHelper.escape(storedPlaylist.playlistName)}\"\n")
        var position = -1
        var sort = false
        var add = false
        var artist: String? = null
        while (true) {
            val current = connection.readLine() ?: break
            if (current.startsWith(MpdConnection.OK)) {
                break
            }
            if (current.startsWith(MpdConnection.ACK)) {
                throw IOException("Error reading MPD response: $current")
            }

            if (current.startsWith("file: ")) {
                if (add) {
                    builder.setPosition(position)
                    result.add(builder.build())
                }
                position++
                add = true
                artist = null
                builder.clear()
                builder.setUri(current.substring(6))
            }
            if (current.startsWith("Artist: ")) {
                artist = current.substring(8)
                if (!artist.isNullOrEmpty()) {
                    builder.setArtist(artist)
                }
            }
            if (current.startsWith("AlbumArtist: ")) {
                if (artist.isNullOrEmpty()) {
                    builder.setArtist(current.substring(13))
                }
            }
            if (current.startsWith("Album: ")) {
                builder.setAlbum(current.substring(7))
            }
            if (current.startsWith("Disc: ")) {
                builder.setDisc(MpdCommandHelper.getDecimalNumber(current.substring(6)))
            }
            if (current.startsWith("Track: ")) {
                builder.setTrack(MpdCommandHelper.getDecimalNumber(current.substring(7)))
            }
            if (current.startsWith("Title: ")) {
                builder.setTitle(current.substring(7))
            }
            if (current.startsWith("duration: ") && connection.isMinimumVersion(0, 22, 0)) {
                builder.setTime(current.substring(10).toFloat().roundToInt())
            }
            if (current.startsWith("Time: ") && !connection.isMinimumVersion(0, 22, 0)) { // deprecated
                builder.setTime(Integer.decode(current.substring(6)))
            }
            if (current.startsWith("Name: ")) {
                builder.setName(current.substring(6))
            }
        }
        if (add) {
            result.add(builder.build())
        }

        if (sort) {
            result.sortWith(compareBy { it.getPosition()!! })
        }

        return result.toTypedArray()
    }

    override fun onError(): Array<PlaylistEntity> = arrayOf()
}
