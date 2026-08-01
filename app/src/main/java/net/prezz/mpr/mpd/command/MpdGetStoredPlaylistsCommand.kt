package net.prezz.mpr.mpd.command

import net.prezz.mpr.model.StoredPlaylistEntity
import net.prezz.mpr.mpd.MpdPartitionProvider
import net.prezz.mpr.mpd.connection.MpdConnection
import java.util.ArrayList
import java.util.Comparator

class MpdGetStoredPlaylistsCommand(partitionProvider: MpdPartitionProvider) :
    MpdConnectionCommand<Void?, Array<StoredPlaylistEntity>>(null, partitionProvider) {

    @Throws(Exception::class)
    override fun doExecute(connection: MpdConnection, param: Void?): Array<StoredPlaylistEntity> {
        val lines = connection.writeResponseCommand("listplaylists\n")

        val result = ArrayList<StoredPlaylistEntity>()

        for (line in lines) {
            if (line.startsWith("playlist: ")) {
                val playlistName = line.substring(10)
                result.add(StoredPlaylistEntity(playlistName))
            }
        }

        result.sortWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.playlistName })

        return result.toTypedArray()
    }

    override fun onError(): Array<StoredPlaylistEntity> = arrayOf()
}
