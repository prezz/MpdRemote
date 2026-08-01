package net.prezz.mpr.model

import net.prezz.mpr.model.command.Command
import java.util.SortedSet

interface MusicPlayer {

    fun dispose()

    fun setStatusListener(listener: StatusListener?)

    fun deleteLocalLibraryDatabase(responseReceiver: ResponseReceiver<Boolean>): TaskHandle

    fun getHideableUriFolders(responseReceiver: ResponseReceiver<Array<String>>): TaskHandle

    fun getArtistsFromLibrary(entity: LibraryEntity, responseReceiver: ResponseReceiver<Array<LibraryEntity>>): TaskHandle

    fun getAlbumsFromLibrary(sortByArtist: Boolean, entity: LibraryEntity, responseReceiver: ResponseReceiver<Array<LibraryEntity>>): TaskHandle

    fun getGenresFromLibrary(entity: LibraryEntity, responseReceiver: ResponseReceiver<Array<LibraryEntity>>): TaskHandle

    fun getFilteredAlbumsAndTitlesFromLibrary(entity: LibraryEntity, responseReceiver: ResponseReceiver<Array<LibraryEntity>>): TaskHandle

    fun getFilteredTracksAndTitlesFromLibrary(entity: LibraryEntity, responseReceiver: ResponseReceiver<Array<LibraryEntity>>): TaskHandle

    fun getUriFromLibrary(uriEntity: UriEntity?, uriFilter: SortedSet<String>?, responseReceiver: ResponseReceiver<Array<UriEntity>>): TaskHandle

    fun searchLibrary(query: String, searchUri: Boolean, uriFilter: SortedSet<String>?, responseReceiver: ResponseReceiver<SearchResult>): TaskHandle

    fun getPlaylist(responseReceiver: ResponseReceiver<Array<PlaylistEntity>>): TaskHandle

    fun getPlaylistEntity(position: Int, responseReceiver: ResponseReceiver<PlaylistEntity?>): TaskHandle

    fun getStoredPlaylists(responseReceiver: ResponseReceiver<Array<StoredPlaylistEntity>>): TaskHandle

    fun getPlaylistDetails(storedPlaylist: StoredPlaylistEntity, responseReceiver: ResponseReceiver<Array<PlaylistEntity>>): TaskHandle

    fun getOutputs(defaultPartition: Boolean, responseReceiver: ResponseReceiver<Array<AudioOutput>>): TaskHandle

    fun getStatistics(responseReceiver: ResponseReceiver<Statistics?>): TaskHandle

    fun getPartitions(responseReceiver: ResponseReceiver<Array<PartitionEntity>>): TaskHandle

    fun switchPartition(partition: String, responseReceiver: ResponseReceiver<Array<PartitionEntity>>): TaskHandle

    fun updatePlayData(entities: List<PlaylistEntity>, responseReceiver: ResponseReceiver<Boolean>): TaskHandle

    fun clearPlayData(responseReceiver: ResponseReceiver<Boolean>): TaskHandle

    fun exportPlayData(offset: Int, limit: Int, responseReceiver: ResponseReceiver<String>): TaskHandle

    fun importPlayData(csvData: String, responseReceiver: ResponseReceiver<Boolean>): TaskHandle

    fun sendControlCommands(commands: List<Command>, responseReceiver: ResponseReceiver<ResponseResult>): TaskHandle
}
