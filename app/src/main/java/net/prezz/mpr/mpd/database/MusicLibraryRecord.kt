package net.prezz.mpr.mpd.database


// KOTLIN-REVIEW: This is a Java bean - private fields + getX()/setX() pairs + clear(). Idiomatic
// Kotlin would make these public `var` constructor properties (dropping the accessors). Deferred to a
// TYPE-AWARE IDE refactor because getArtist/getAlbum/getTitle/getDisc/getTrack/getGenre collide with
// LibraryEntity/PlaylistEntity, so a textual getter->property swap is unsafe.
// NOTE: getAlbumArtist()/getMetaAlbumArtist()/getComposer() are NOT plain accessors - they contain
// logic (return null when equal to artist/albumArtist). Keep that as custom `get()` accessors on the
// properties; do not let the refactor flatten them into stored fields.
class MusicLibraryRecord {

    private var artist: String? = null
    private var metaArtist: String? = null
    private var albumArtist: String? = null
    private var metaAlbumArtist: String? = null
    private var composer: String? = null
    private var album: String? = null
    private var metaAlbum: String? = null
    private var title: String? = null
    private var disc: Int? = null
    private var track: Int? = null
    private var genre: String? = null
    private var year: Int? = null
    private var length: Int? = null
    private var uri: String? = null

    init {
        clear()
    }

    fun clear() {
        this.artist = ""
        this.metaArtist = ""
        this.albumArtist = null
        this.metaAlbumArtist = null
        this.composer = null
        this.album = ""
        this.metaAlbum = ""
        this.title = ""
        this.disc = null
        this.track = null
        this.genre = ""
        this.year = null
        this.length = null
        this.uri = ""
    }

    fun getArtist(): String? {
        return artist
    }

    fun setArtist(artist: String?) {
        this.artist = artist
    }

    fun getMetaArtist(): String? {
        return metaArtist
    }

    fun setMetaArtist(metaArtist: String?) {
        this.metaArtist = metaArtist
    }

    fun getAlbumArtist(): String? {
        return if (albumArtist == artist) null else albumArtist
    }

    fun setAlbumArtist(albumArtist: String?) {
        this.albumArtist = albumArtist
    }

    fun getMetaAlbumArtist(): String? {
        return if (metaAlbumArtist == metaArtist) null else metaAlbumArtist
    }

    fun setMetaAlbumArtist(metaAlbumArtist: String?) {
        this.metaAlbumArtist = metaAlbumArtist
    }

    fun getComposer(): String? {
        return if (composer == artist || composer == albumArtist) null else composer
    }

    fun setComposer(composer: String?) {
        this.composer = composer
    }

    fun getAlbum(): String? {
        return album
    }

    fun setAlbum(album: String?) {
        this.album = album
    }

    fun getMetaAlbum(): String? {
        return metaAlbum
    }

    fun setMetaAlbum(metaAlbum: String?) {
        this.metaAlbum = metaAlbum
    }

    fun getTitle(): String? {
        return title
    }

    fun setTitle(title: String?) {
        this.title = title
    }

    fun getDisc(): Int? {
        return disc
    }

    fun setDisc(disc: Int?) {
        this.disc = disc
    }

    fun getTrack(): Int? {
        return track
    }

    fun setTrack(track: Int?) {
        this.track = track
    }

    fun getGenre(): String? {
        return genre
    }

    fun setGenre(genre: String?) {
        this.genre = genre
    }

    fun getYear(): Int? {
        return year
    }

    fun setYear(year: Int?) {
        this.year = year
    }

    fun getLength(): Int? {
        return length
    }

    fun setLength(length: Int?) {
        this.length = length
    }

    fun getUri(): String? {
        return uri
    }

    fun setUri(uri: String?) {
        this.uri = uri
    }
}
