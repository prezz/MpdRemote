package net.prezz.mpr.model

import net.prezz.mpr.model.command.Command
import java.util.SortedSet

/**
 * Music player with no functionality to be used in [MusicPlayerControl]
 * to avoid null checks in every call if a music player isn't set.
 */
internal class NullPlayer : MusicPlayer {

    override fun dispose() {
    }

    override fun setStatusListener(listener: StatusListener?) {
    }

    override fun deleteLocalLibraryDatabase(responseReceiver: ResponseReceiver<Boolean>) = TaskHandle.NULL_HANDLE

    override fun getHideableUriFolders(responseReceiver: ResponseReceiver<Array<String>>) = TaskHandle.NULL_HANDLE

    override fun getArtistsFromLibrary(entity: LibraryEntity, responseReceiver: ResponseReceiver<Array<LibraryEntity>>) = TaskHandle.NULL_HANDLE

    override fun getAlbumsFromLibrary(sortByArtist: Boolean, entity: LibraryEntity, responseReceiver: ResponseReceiver<Array<LibraryEntity>>) = TaskHandle.NULL_HANDLE

    override fun getGenresFromLibrary(entity: LibraryEntity, responseReceiver: ResponseReceiver<Array<LibraryEntity>>) = TaskHandle.NULL_HANDLE

    override fun getFilteredAlbumsAndTitlesFromLibrary(entity: LibraryEntity, responseReceiver: ResponseReceiver<Array<LibraryEntity>>) = TaskHandle.NULL_HANDLE

    override fun getFilteredTracksAndTitlesFromLibrary(entity: LibraryEntity, responseReceiver: ResponseReceiver<Array<LibraryEntity>>) = TaskHandle.NULL_HANDLE

    override fun getUriFromLibrary(uriEntity: UriEntity?, uriFilter: SortedSet<String>?, responseReceiver: ResponseReceiver<Array<UriEntity>>) = TaskHandle.NULL_HANDLE

    override fun searchLibrary(query: String, searchUri: Boolean, uriFilter: SortedSet<String>?, responseReceiver: ResponseReceiver<SearchResult>) = TaskHandle.NULL_HANDLE

    override fun getPlaylist(responseReceiver: ResponseReceiver<Array<PlaylistEntity>>) = TaskHandle.NULL_HANDLE

    override fun getPlaylistEntity(position: Int, responseReceiver: ResponseReceiver<PlaylistEntity?>) = TaskHandle.NULL_HANDLE

    override fun getStoredPlaylists(responseReceiver: ResponseReceiver<Array<StoredPlaylistEntity>>) = TaskHandle.NULL_HANDLE

    override fun getPlaylistDetails(storedPlaylist: StoredPlaylistEntity, responseReceiver: ResponseReceiver<Array<PlaylistEntity>>) = TaskHandle.NULL_HANDLE

    override fun getOutputs(defaultPartition: Boolean, responseReceiver: ResponseReceiver<Array<AudioOutput>>) = TaskHandle.NULL_HANDLE

    override fun getStatistics(responseReceiver: ResponseReceiver<Statistics?>) = TaskHandle.NULL_HANDLE

    override fun getPartitions(responseReceiver: ResponseReceiver<Array<PartitionEntity>>) = TaskHandle.NULL_HANDLE

    override fun switchPartition(partition: String, responseReceiver: ResponseReceiver<Array<PartitionEntity>>) = TaskHandle.NULL_HANDLE

    override fun updatePlayData(entities: List<PlaylistEntity>, responseReceiver: ResponseReceiver<Boolean>) = TaskHandle.NULL_HANDLE

    override fun clearPlayData(responseReceiver: ResponseReceiver<Boolean>) = TaskHandle.NULL_HANDLE

    override fun exportPlayData(offset: Int, limit: Int, responseReceiver: ResponseReceiver<String>) = TaskHandle.NULL_HANDLE

    override fun importPlayData(csvData: String, responseReceiver: ResponseReceiver<Boolean>) = TaskHandle.NULL_HANDLE

    override fun sendControlCommands(commands: List<Command>, responseReceiver: ResponseReceiver<ResponseResult>) = TaskHandle.NULL_HANDLE
}
