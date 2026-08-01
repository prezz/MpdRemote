package net.prezz.mpr.model

class SearchResult(
    private val libraryEntities: Array<LibraryEntity>,
    private val uriEntities: Array<UriEntity>
) {

    fun getLibraryEntities(): Array<LibraryEntity> {
        return libraryEntities
    }

    fun getUriEntities(): Array<UriEntity> {
        return uriEntities
    }
}
