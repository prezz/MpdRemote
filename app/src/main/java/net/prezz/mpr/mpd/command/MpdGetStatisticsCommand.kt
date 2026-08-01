package net.prezz.mpr.mpd.command

import net.prezz.mpr.model.Statistics
import net.prezz.mpr.mpd.MpdPartitionProvider
import net.prezz.mpr.mpd.connection.MpdConnection

class MpdGetStatisticsCommand(partitionProvider: MpdPartitionProvider) :
    MpdConnectionCommand<Void?, Statistics?>(null, partitionProvider) {

    @Throws(Exception::class)
    override fun doExecute(connection: MpdConnection, param: Void?): Statistics? {
        val lines = connection.writeResponseCommandList(arrayOf("stats\n", "status\n"))

        val result = Statistics()

        for (line in lines[0]!!) {
            if (line.startsWith("artists: ")) {
                result.setArtists(Integer.decode(line.substring(9)))
            }
            if (line.startsWith("albums: ")) {
                result.setAlbums(Integer.decode(line.substring(8)))
            }
            if (line.startsWith("songs: ")) {
                result.setSongs(Integer.decode(line.substring(7)))
            }
        }

        for (line in lines[1]!!) {
            if (line.startsWith("updating_db: ")) {
                result.setUpdatingJob(Integer.decode(line.substring(13)))
            }
        }

        return result
    }

    override fun onError(): Statistics? = null
}
