package net.prezz.mpr.model

import net.prezz.mpr.model.command.Command
import java.util.SortedSet

object MusicPlayerControl {

    private var musicPlayer: MusicPlayer = NullPlayer()

    @JvmStatic
    fun setMusicPlayer(musicPlayer: MusicPlayer?) {
        MusicPlayerControl.musicPlayer.dispose()
        MusicPlayerControl.musicPlayer = musicPlayer ?: NullPlayer()
    }

    @JvmStatic
    fun setStatusListener(listener: StatusListener?) {
        musicPlayer.setStatusListener(listener)
    }

    @JvmStatic
    fun deleteLocalLibraryDatabase(responseReceiver: ResponseReceiver<Boolean>) = musicPlayer.deleteLocalLibraryDatabase(responseReceiver)

    @JvmStatic
    fun getHideableUriFolders(responseReceiver: ResponseReceiver<Array<String>>) = musicPlayer.getHideableUriFolders(responseReceiver)

    @JvmStatic
    fun getArtistsFromLibrary(entity: LibraryEntity, responseReceiver: ResponseReceiver<Array<LibraryEntity>>) = musicPlayer.getArtistsFromLibrary(entity, responseReceiver)

    @JvmStatic
    fun getAlbumsFromLibrary(sortByArtist: Boolean, entity: LibraryEntity, responseReceiver: ResponseReceiver<Array<LibraryEntity>>) = musicPlayer.getAlbumsFromLibrary(sortByArtist, entity, responseReceiver)

    @JvmStatic
    fun getGenresFromLibrary(entity: LibraryEntity, responseReceiver: ResponseReceiver<Array<LibraryEntity>>) = musicPlayer.getGenresFromLibrary(entity, responseReceiver)

    @JvmStatic
    fun getFilteredAlbumsAndTitlesFromLibrary(entity: LibraryEntity, responseReceiver: ResponseReceiver<Array<LibraryEntity>>) = musicPlayer.getFilteredAlbumsAndTitlesFromLibrary(entity, responseReceiver)

    @JvmStatic
    fun getFilteredTracksAndTitlesFromLibrary(entity: LibraryEntity, responseReceiver: ResponseReceiver<Array<LibraryEntity>>) = musicPlayer.getFilteredTracksAndTitlesFromLibrary(entity, responseReceiver)

    @JvmStatic
    fun getUriFromLibrary(uriEntity: UriEntity?, uriFilter: SortedSet<String>?, responseReceiver: ResponseReceiver<Array<UriEntity>>) = musicPlayer.getUriFromLibrary(uriEntity, uriFilter, responseReceiver)

    @JvmStatic
    fun searchLibrary(query: String, searchUri: Boolean, uriFilter: SortedSet<String>?, responseReceiver: ResponseReceiver<SearchResult>) = musicPlayer.searchLibrary(query, searchUri, uriFilter, responseReceiver)

    @JvmStatic
    fun getPlaylist(responseReceiver: ResponseReceiver<Array<PlaylistEntity>>) = musicPlayer.getPlaylist(responseReceiver)

    @JvmStatic
    fun getPlaylistEntity(position: Int, responseReceiver: ResponseReceiver<PlaylistEntity?>) = musicPlayer.getPlaylistEntity(position, responseReceiver)

    @JvmStatic
    fun getStoredPlaylists(responseReceiver: ResponseReceiver<Array<StoredPlaylistEntity>>) = musicPlayer.getStoredPlaylists(responseReceiver)

    @JvmStatic
    fun getPlaylistDetails(storedPlaylist: StoredPlaylistEntity, responseReceiver: ResponseReceiver<Array<PlaylistEntity>>) = musicPlayer.getPlaylistDetails(storedPlaylist, responseReceiver)

    @JvmStatic
    fun getOutputs(defaultPartition: Boolean, responseReceiver: ResponseReceiver<Array<AudioOutput>>) = musicPlayer.getOutputs(defaultPartition, responseReceiver)

    @JvmStatic
    fun getStatistics(responseReceiver: ResponseReceiver<Statistics?>) = musicPlayer.getStatistics(responseReceiver)

    @JvmStatic
    fun getPartitions(responseReceiver: ResponseReceiver<Array<PartitionEntity>>) = musicPlayer.getPartitions(responseReceiver)

    @JvmStatic
    fun switchPartition(partition: String, responseReceiver: ResponseReceiver<Array<PartitionEntity>>) = musicPlayer.switchPartition(partition, responseReceiver)

    @JvmStatic
    fun updatePlayData(entities: List<PlaylistEntity>, responseReceiver: ResponseReceiver<Boolean>) = musicPlayer.updatePlayData(entities, responseReceiver)

    @JvmStatic
    fun clearPlayData(responseReceiver: ResponseReceiver<Boolean>) = musicPlayer.clearPlayData(responseReceiver)

    @JvmStatic
    fun exportPlayData(offset: Int, limit: Int, responseReceiver: ResponseReceiver<String>) = musicPlayer.exportPlayData(offset, limit, responseReceiver)

    @JvmStatic
    fun importPlayData(csvData: String, responseReceiver: ResponseReceiver<Boolean>) = musicPlayer.importPlayData(csvData, responseReceiver)

    @JvmStatic
    fun sendControlCommand(command: Command) {
        sendControlCommands(listOf(command), object : ResponseReceiver<ResponseResult>() {
            override fun receiveResponse(response: ResponseResult) {
            }
        })
    }

    @JvmStatic
    fun sendControlCommand(command: Command, responseReceiver: ResponseReceiver<ResponseResult>) = sendControlCommands(listOf(command), responseReceiver)

    @JvmStatic
    fun sendControlCommands(commands: List<Command>) {
        sendControlCommands(commands, object : ResponseReceiver<ResponseResult>() {
            override fun receiveResponse(response: ResponseResult) {
            }
        })
    }

    @JvmStatic
    fun sendControlCommands(commands: List<Command>, responseReceiver: ResponseReceiver<ResponseResult>) = musicPlayer.sendControlCommands(commands, responseReceiver)
}
