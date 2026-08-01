package net.prezz.mpr.mpd.command

import kotlin.math.roundToInt

import net.prezz.mpr.model.PlaylistEntity
import net.prezz.mpr.mpd.MpdPartitionProvider
import net.prezz.mpr.mpd.connection.MpdConnection

class MpdGetPlaylistEntityCommand(position: Int, partitionProvider: MpdPartitionProvider) :
    MpdConnectionCommand<Int, PlaylistEntity?>(position, partitionProvider) {

    @Throws(Exception::class)
    override fun doExecute(connection: MpdConnection, position: Int): PlaylistEntity? {
        val builder = PlaylistEntity.createBuilder()

        var exist = false
        val lines = connection.writeResponseCommand("playlistinfo $position\n")
        for (line in lines) {
            if (line.startsWith("file: ")) {
                builder.setUri(line.substring(6))
                exist = true
            }
            if (line.startsWith("Id: ")) {
                builder.setId(Integer.decode(line.substring(4)))
            }
            if (line.startsWith("Pos: ")) {
                val pos = Integer.decode(line.substring(5))
                builder.setPosition(pos)
            }
            if (line.startsWith("Prio: ")) {
                builder.setPriority(Integer.decode(line.substring(6)))
            }
            if (line.startsWith("Artist: ")) {
                builder.setArtist(line.substring(8))
            }
            if (line.startsWith("Album: ")) {
                builder.setAlbum(line.substring(7))
            }
            if (line.startsWith("Disc: ")) {
                builder.setDisc(MpdCommandHelper.getDecimalNumber(line.substring(6)))
            }
            if (line.startsWith("Track: ")) {
                builder.setTrack(MpdCommandHelper.getDecimalNumber(line.substring(7)))
            }
            if (line.startsWith("Title: ")) {
                builder.setTitle(line.substring(7))
            }
            if (line.startsWith("duration: ") && connection.isMinimumVersion(0, 22, 0)) {
                builder.setTime(line.substring(10).toFloat().roundToInt())
            }
            if (line.startsWith("Time: ") && !connection.isMinimumVersion(0, 22, 0)) { // deprecated
                builder.setTime(Integer.decode(line.substring(6)))
            }
            if (line.startsWith("Name: ")) {
                builder.setName(line.substring(6))
            }
        }

        if (exist) {
            return builder.build()
        }

        return onError()
    }

    override fun onError(): PlaylistEntity? = null
}
