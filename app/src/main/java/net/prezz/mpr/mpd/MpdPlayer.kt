package net.prezz.mpr.mpd

import net.prezz.mpr.model.AudioOutput
import net.prezz.mpr.model.LibraryEntity
import net.prezz.mpr.model.MusicPlayer
import net.prezz.mpr.model.PartitionEntity
import net.prezz.mpr.model.PlaylistEntity
import net.prezz.mpr.model.ResponseReceiver
import net.prezz.mpr.model.ResponseResult
import net.prezz.mpr.model.SearchResult
import net.prezz.mpr.model.Statistics
import net.prezz.mpr.model.StatusListener
import net.prezz.mpr.model.StoredPlaylistEntity
import net.prezz.mpr.model.TaskHandle
import net.prezz.mpr.model.UriEntity
import net.prezz.mpr.model.command.Command
import net.prezz.mpr.mpd.command.MpdClearPlayDataCommand
import net.prezz.mpr.mpd.command.MpdConnectionCommand.MpdConnectionCommandReceiver
import net.prezz.mpr.mpd.command.MpdDatabaseCommand.MpdDatabaseCommandReceiver
import net.prezz.mpr.mpd.command.MpdDeleteLocalLibraryDatabaseCommand
import net.prezz.mpr.mpd.command.MpdExportPlayDataCommand
import net.prezz.mpr.mpd.command.MpdGetAlbumsCommand
import net.prezz.mpr.mpd.command.MpdGetArtistsCommand
import net.prezz.mpr.mpd.command.MpdGetFilteredAlbumsAndTitlesCommand
import net.prezz.mpr.mpd.command.MpdGetFilteredTracksAndTitlesCommand
import net.prezz.mpr.mpd.command.MpdGetGenresCommand
import net.prezz.mpr.mpd.command.MpdGetHideableUriFolders
import net.prezz.mpr.mpd.command.MpdGetOutputsCommand
import net.prezz.mpr.mpd.command.MpdGetPartitionsCommand
import net.prezz.mpr.mpd.command.MpdGetPlaylistCommand
import net.prezz.mpr.mpd.command.MpdGetPlaylistDetailsCommand
import net.prezz.mpr.mpd.command.MpdGetPlaylistEntityCommand
import net.prezz.mpr.mpd.command.MpdGetStatisticsCommand
import net.prezz.mpr.mpd.command.MpdGetStoredPlaylistsCommand
import net.prezz.mpr.mpd.command.MpdGetUriCommand
import net.prezz.mpr.mpd.command.MpdImportPlayDataCommand
import net.prezz.mpr.mpd.command.MpdSearchLibraryCommand
import net.prezz.mpr.mpd.command.MpdSendControlCommands
import net.prezz.mpr.mpd.command.MpdUpdatePlayDataCommand
import net.prezz.mpr.mpd.connection.MpdConnection
import net.prezz.mpr.mpd.database.MpdLibraryDatabaseHelper
import net.prezz.mpr.ui.ApplicationActivator
import java.util.SortedSet

class MpdPlayer(settings: MpdSettings) : MusicPlayer {

    private val connection: MpdConnection = MpdConnection(settings)
    private val monitor: MpdStatusMonitor = MpdStatusMonitor(settings)
    private val partitionStore: MpdPartitionStore = MpdPartitionStore(ApplicationActivator.context, settings)
    private val databaseHelper: MpdLibraryDatabaseHelper = MpdLibraryDatabaseHelper(ApplicationActivator.context, settings.getName())

    override fun dispose() {
        monitor.setStatusListener(null, null)
        databaseHelper.close()
    }

    override fun setStatusListener(listener: StatusListener?) {
        monitor.setStatusListener(listener, partitionStore)
    }

    override fun deleteLocalLibraryDatabase(responseReceiver: ResponseReceiver<Boolean>): TaskHandle {
        val command = MpdDeleteLocalLibraryDatabaseCommand()
        return command.execute(databaseHelper, connection, object : MpdDatabaseCommandReceiver<Boolean> {
            override fun build() {
                // the delete local library command should not rebuild the database
            }

            override fun receive(result: Boolean) {
                responseReceiver.receiveResponse(result)
            }
        })
    }

    override fun getHideableUriFolders(responseReceiver: ResponseReceiver<Array<String>>): TaskHandle {
        val command = MpdGetHideableUriFolders()
        return command.execute(databaseHelper, connection, object : MpdDatabaseCommandReceiver<Array<String>> {
            override fun build() {
                responseReceiver.buildingDatabase()
            }

            override fun receive(result: Array<String>) {
                responseReceiver.receiveResponse(result)
            }
        })
    }

    override fun getArtistsFromLibrary(entity: LibraryEntity, responseReceiver: ResponseReceiver<Array<LibraryEntity>>): TaskHandle {
        val command = MpdGetArtistsCommand(entity)
        return command.execute(databaseHelper, connection, object : MpdDatabaseCommandReceiver<Array<LibraryEntity>> {
            override fun build() {
                responseReceiver.buildingDatabase()
            }

            override fun receive(result: Array<LibraryEntity>) {
                responseReceiver.receiveResponse(result)
            }
        })
    }

    override fun getAlbumsFromLibrary(sortByArtist: Boolean, entity: LibraryEntity, responseReceiver: ResponseReceiver<Array<LibraryEntity>>): TaskHandle {
        val command = MpdGetAlbumsCommand(sortByArtist, entity)
        return command.execute(databaseHelper, connection, object : MpdDatabaseCommandReceiver<Array<LibraryEntity>> {
            override fun build() {
                responseReceiver.buildingDatabase()
            }

            override fun receive(result: Array<LibraryEntity>) {
                responseReceiver.receiveResponse(result)
            }
        })
    }

    override fun getGenresFromLibrary(entity: LibraryEntity, responseReceiver: ResponseReceiver<Array<LibraryEntity>>): TaskHandle {
        val command = MpdGetGenresCommand(entity)
        return command.execute(databaseHelper, connection, object : MpdDatabaseCommandReceiver<Array<LibraryEntity>> {
            override fun build() {
                responseReceiver.buildingDatabase()
            }

            override fun receive(result: Array<LibraryEntity>) {
                responseReceiver.receiveResponse(result)
            }
        })
    }

    override fun getFilteredAlbumsAndTitlesFromLibrary(entity: LibraryEntity, responseReceiver: ResponseReceiver<Array<LibraryEntity>>): TaskHandle {
        val command = MpdGetFilteredAlbumsAndTitlesCommand(entity)
        return command.execute(databaseHelper, connection, object : MpdDatabaseCommandReceiver<Array<LibraryEntity>> {
            override fun build() {
                responseReceiver.buildingDatabase()
            }

            override fun receive(result: Array<LibraryEntity>) {
                responseReceiver.receiveResponse(result)
            }
        })
    }

    override fun getFilteredTracksAndTitlesFromLibrary(entity: LibraryEntity, responseReceiver: ResponseReceiver<Array<LibraryEntity>>): TaskHandle {
        val command = MpdGetFilteredTracksAndTitlesCommand(entity)
        return command.execute(databaseHelper, connection, object : MpdDatabaseCommandReceiver<Array<LibraryEntity>> {
            override fun build() {
                responseReceiver.buildingDatabase()
            }

            override fun receive(result: Array<LibraryEntity>) {
                responseReceiver.receiveResponse(result)
            }
        })
    }

    override fun getUriFromLibrary(uriEntity: UriEntity?, uriFilter: SortedSet<String>?, responseReceiver: ResponseReceiver<Array<UriEntity>>): TaskHandle {
        val command = MpdGetUriCommand(uriEntity, uriFilter)
        return command.execute(databaseHelper, connection, object : MpdDatabaseCommandReceiver<Array<UriEntity>> {
            override fun build() {
                responseReceiver.buildingDatabase()
            }

            override fun receive(result: Array<UriEntity>) {
                responseReceiver.receiveResponse(result)
            }
        })
    }

    override fun searchLibrary(query: String, searchUri: Boolean, uriFilter: SortedSet<String>?, responseReceiver: ResponseReceiver<SearchResult>): TaskHandle {
        val command = MpdSearchLibraryCommand(query.trim(), searchUri, uriFilter)
        return command.execute(databaseHelper, connection, object : MpdDatabaseCommandReceiver<SearchResult> {
            override fun build() {
                responseReceiver.buildingDatabase()
            }

            override fun receive(result: SearchResult) {
                responseReceiver.receiveResponse(result)
            }
        })
    }

    override fun getPlaylist(responseReceiver: ResponseReceiver<Array<PlaylistEntity>>): TaskHandle {
        val command = MpdGetPlaylistCommand(partitionStore)
        return command.execute(connection, object : MpdConnectionCommandReceiver<Array<PlaylistEntity>> {
            override fun receive(result: Array<PlaylistEntity>) {
                responseReceiver.receiveResponse(result)
            }
        })
    }

    override fun getPlaylistEntity(position: Int, responseReceiver: ResponseReceiver<PlaylistEntity?>): TaskHandle {
        val command = MpdGetPlaylistEntityCommand(position, partitionStore)
        return command.execute(connection, object : MpdConnectionCommandReceiver<PlaylistEntity?> {
            override fun receive(result: PlaylistEntity?) {
                responseReceiver.receiveResponse(result)
            }
        })
    }

    override fun getStoredPlaylists(responseReceiver: ResponseReceiver<Array<StoredPlaylistEntity>>): TaskHandle {
        val command = MpdGetStoredPlaylistsCommand(partitionStore)
        return command.execute(connection, object : MpdConnectionCommandReceiver<Array<StoredPlaylistEntity>> {
            override fun receive(result: Array<StoredPlaylistEntity>) {
                responseReceiver.receiveResponse(result)
            }
        })
    }

    override fun getPlaylistDetails(storedPlaylist: StoredPlaylistEntity, responseReceiver: ResponseReceiver<Array<PlaylistEntity>>): TaskHandle {
        val command = MpdGetPlaylistDetailsCommand(storedPlaylist, partitionStore)
        return command.execute(connection, object : MpdConnectionCommandReceiver<Array<PlaylistEntity>> {
            override fun receive(result: Array<PlaylistEntity>) {
                responseReceiver.receiveResponse(result)
            }
        })
    }

    override fun getOutputs(defaultPartition: Boolean, responseReceiver: ResponseReceiver<Array<AudioOutput>>): TaskHandle {
        val command = MpdGetOutputsCommand(if (defaultPartition) SpecificPartitionProvider(MpdPartitionProvider.DEFAULT_PARTITION) else partitionStore)
        return command.execute(connection, object : MpdConnectionCommandReceiver<Array<AudioOutput>> {
            override fun receive(result: Array<AudioOutput>) {
                responseReceiver.receiveResponse(result)
            }
        })
    }

    override fun getStatistics(responseReceiver: ResponseReceiver<Statistics?>): TaskHandle {
        val command = MpdGetStatisticsCommand(partitionStore)
        return command.execute(connection, object : MpdConnectionCommandReceiver<Statistics?> {
            override fun receive(result: Statistics?) {
                responseReceiver.receiveResponse(result)
            }
        })
    }

    override fun getPartitions(responseReceiver: ResponseReceiver<Array<PartitionEntity>>): TaskHandle {
        val command = MpdGetPartitionsCommand(partitionStore)
        return command.execute(connection, object : MpdConnectionCommandReceiver<Array<PartitionEntity>> {
            override fun receive(result: Array<PartitionEntity>) {
                responseReceiver.receiveResponse(result)
            }
        })
    }

    override fun switchPartition(partition: String, responseReceiver: ResponseReceiver<Array<PartitionEntity>>): TaskHandle {
        val command = MpdGetPartitionsCommand(SpecificPartitionProvider(partition))
        return command.execute(connection, object : MpdConnectionCommandReceiver<Array<PartitionEntity>> {
            override fun receive(result: Array<PartitionEntity>) {
                val partitions = HashSet<String>(result.size)
                for (entity in result) {
                    partitions.add(entity.partitionName)
                }

                if (partitions.contains(partition)) {
                    partitionStore.putPartition(partition)
                    monitor.switchPartition(partitionStore)
                }

                responseReceiver.receiveResponse(result)
            }
        })
    }

    override fun updatePlayData(entities: List<PlaylistEntity>, responseReceiver: ResponseReceiver<Boolean>): TaskHandle {
        val command = MpdUpdatePlayDataCommand(entities)
        return command.execute(databaseHelper, connection, object : MpdDatabaseCommandReceiver<Boolean> {
            override fun build() {
                responseReceiver.buildingDatabase()
            }

            override fun receive(result: Boolean) {
                responseReceiver.receiveResponse(result)
            }
        })
    }

    override fun clearPlayData(responseReceiver: ResponseReceiver<Boolean>): TaskHandle {
        val command = MpdClearPlayDataCommand()
        return command.execute(databaseHelper, connection, object : MpdDatabaseCommandReceiver<Boolean> {
            override fun build() {
                responseReceiver.buildingDatabase()
            }

            override fun receive(result: Boolean) {
                responseReceiver.receiveResponse(result)
            }
        })
    }

    override fun exportPlayData(offset: Int, limit: Int, responseReceiver: ResponseReceiver<String>): TaskHandle {
        val command = MpdExportPlayDataCommand(offset, limit)
        return command.execute(databaseHelper, connection, object : MpdDatabaseCommandReceiver<String> {
            override fun build() {
                responseReceiver.buildingDatabase()
            }

            override fun receive(result: String) {
                responseReceiver.receiveResponse(result)
            }
        })
    }

    override fun importPlayData(csvData: String, responseReceiver: ResponseReceiver<Boolean>): TaskHandle {
        val command = MpdImportPlayDataCommand(csvData)
        return command.execute(databaseHelper, connection, object : MpdDatabaseCommandReceiver<Boolean> {
            override fun build() {
                responseReceiver.buildingDatabase()
            }

            override fun receive(result: Boolean) {
                responseReceiver.receiveResponse(result)
            }
        })
    }

    override fun sendControlCommands(commands: List<Command>, responseReceiver: ResponseReceiver<ResponseResult>): TaskHandle {
        val command = MpdSendControlCommands(commands, partitionStore)
        return command.execute(connection, object : MpdConnectionCommandReceiver<ResponseResult> {
            override fun receive(result: ResponseResult) {
                responseReceiver.receiveResponse(result)
            }
        })
    }

    private class SpecificPartitionProvider(private var partition: String) : MpdPartitionProvider {

        override fun getPartition(): String {
            return partition
        }

        override fun onInvalidPartition() {
            this.partition = MpdPartitionProvider.DEFAULT_PARTITION
        }
    }
}
