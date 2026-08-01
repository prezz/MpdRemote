package net.prezz.mpr.model

import java.io.Serializable
import java.util.Collections
import java.util.SortedSet


// KOTLIN-REVIEW: Still uses Java-style getters (fun getX()) instead of `val` properties. Deferred
// because the getter names (getArtist/getAlbum/getTitle/getGenre/getUriEntity/...) collide with
// PlaylistEntity, MusicLibraryRecord, and the AdapterEntity hierarchy, so converting safely needs a
// TYPE-AWARE IDE refactor (IntelliJ "Convert Java-style getter to property" / rename member), not a
// textual replace (a text swap could retarget a same-named getter on another type and still compile).
// The Builder below should then become a data class with default (= null) args - but see the Builder
// note: its setMeta* methods carry real truncation logic that must move into init/factory code.
class LibraryEntity private constructor(
    private val tag: Tag?,
    private val artist: String?,
    private val albumArtist: String?,
    private val composer: String?,
    private val album: String?,
    private val title: String?,
    private val genre: String?,
    private val uriEntity: UriEntity?,
    private val metaDisc: Int?,
    private val metaTrack: Int?,
    private val metaArtist: String?,
    private val metaAlbumArtist: String?,
    private val metaAlbum: String?,
    private val metaGenre: String?,
    private val metaCount: Int?,
    private val metaYear: Int?,
    private val metaLength: Int?,
    private val metaCompilation: Boolean?,
    private val lookupArtist: String?,
    private val lookupAlbum: String?,
    private val uriFilter: SortedSet<String>?,
    private val playedDaysAgo: Int?,
    private val playedCount: Int?
) : Serializable {

    //order is important as it impacts the priority used in sorting
    enum class Tag { ARTIST, ALBUM_ARTIST, COMPOSER, ALBUM, TITLE, GENRE }

    fun getTag(): Tag? {
        return tag
    }

    fun getArtist(): String? {
        return artist
    }

    fun getAlbumArtist(): String? {
        return albumArtist
    }

    fun getComposer(): String? {
        return composer
    }

    fun getAlbum(): String? {
        return album
    }

    fun getTitle(): String? {
        return title
    }

    fun getGenre(): String? {
        return genre
    }

    fun getUriEntity(): UriEntity? {
        return uriEntity
    }

    fun getMetaDisc(): Int? {
        return metaDisc
    }

    fun getMetaTrack(): Int? {
        return metaTrack
    }

    fun getMetaArtist(): String? {
        return metaArtist
    }

    fun getMetaAlbumArtist(): String? {
        return metaAlbumArtist
    }

    fun getMetaAlbum(): String? {
        return metaAlbum
    }

    fun getMetaGenre(): String? {
        return metaGenre
    }

    fun getMetaCount(): Int? {
        return metaCount
    }

    fun getMetaYear(): Int? {
        return metaYear
    }

    fun getMetaLength(): Int? {
        return metaLength
    }

    fun getMetaCompilation(): Boolean? {
        return metaCompilation
    }

    fun getLookupArtist(): String? {
        return lookupArtist
    }

    fun getLookupAlbum(): String? {
        return lookupAlbum
    }

    fun getPlayedDaysAgo(): Int? {
        return playedDaysAgo
    }

    fun getPlayedCount(): Int? {
        return playedCount
    }

    fun getUriFilter(): SortedSet<String>? {
        return if (uriFilter != null) Collections.unmodifiableSortedSet(uriFilter) else null
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }

        if (other is LibraryEntity) {

            if (this.tag != other.tag) {
                return false
            }
            if (this.artist != other.artist) {
                return false
            }
            if (this.albumArtist != other.albumArtist) {
                return false
            }
            if (this.composer != other.composer) {
                return false
            }
            if (this.album != other.album) {
                return false
            }
            if (this.title != other.title) {
                return false
            }
            if (this.genre != other.genre) {
                return false
            }
            if (this.uriEntity != other.uriEntity) {
                return false
            }
            if (this.metaDisc != other.metaDisc) {
                return false
            }
            if (this.metaTrack != other.metaTrack) {
                return false
            }
            if (this.metaArtist != other.metaArtist) {
                return false
            }
            if (this.metaAlbumArtist != other.metaAlbumArtist) {
                return false
            }
            if (this.metaAlbum != other.metaAlbum) {
                return false
            }
            if (this.metaGenre != other.metaGenre) {
                return false
            }
            if (this.metaCount != other.metaCount) {
                return false
            }
            if (this.metaYear != other.metaYear) {
                return false
            }
            if (this.metaLength != other.metaLength) {
                return false
            }
            if (this.metaCompilation != other.metaCompilation) {
                return false
            }
            if (this.lookupArtist != other.lookupArtist) {
                return false
            }
            if (this.lookupAlbum != other.lookupAlbum) {
                return false
            }
            if (this.uriFilter != other.uriFilter) {
                return false
            }
            if (this.playedDaysAgo != other.playedDaysAgo) {
                return false
            }
            if (this.playedCount != other.playedCount) {
                return false
            }

            return true
        }

        return false
    }

    override fun hashCode(): Int {
        var hash = 0

        hash = 31 * hash + (tag?.hashCode() ?: 0)
        hash = 31 * hash + (artist?.hashCode() ?: 0)
        hash = 31 * hash + (albumArtist?.hashCode() ?: 0)
        hash = 31 * hash + (composer?.hashCode() ?: 0)
        hash = 31 * hash + (album?.hashCode() ?: 0)
        hash = 31 * hash + (title?.hashCode() ?: 0)
        hash = 31 * hash + (genre?.hashCode() ?: 0)
        hash = 31 * hash + (uriEntity?.hashCode() ?: 0)
        hash = 31 * hash + (metaDisc?.hashCode() ?: 0)
        hash = 31 * hash + (metaTrack?.hashCode() ?: 0)
        hash = 31 * hash + (metaArtist?.hashCode() ?: 0)
        hash = 31 * hash + (metaAlbumArtist?.hashCode() ?: 0)
        hash = 31 * hash + (metaAlbum?.hashCode() ?: 0)
        hash = 31 * hash + (metaGenre?.hashCode() ?: 0)
        hash = 31 * hash + (metaCount?.hashCode() ?: 0)
        hash = 31 * hash + (metaYear?.hashCode() ?: 0)
        hash = 31 * hash + (metaLength?.hashCode() ?: 0)
        hash = 31 * hash + (metaCompilation?.hashCode() ?: 0)
        hash = 31 * hash + (lookupArtist?.hashCode() ?: 0)
        hash = 31 * hash + (lookupAlbum?.hashCode() ?: 0)
        hash = 31 * hash + (uriFilter?.hashCode() ?: 0)
        hash = 31 * hash + (playedDaysAgo?.hashCode() ?: 0)
        hash = 31 * hash + (playedCount?.hashCode() ?: 0)

        return hash
    }

    override fun toString(): String {
        return "$tag $artist, $album, $title, $genre, $metaDisc, $metaTrack"
    }

    // KOTLIN-REVIEW: Hand-rolled Java Builder -> replace with a data class using default (= null) args
    // + named-argument call sites. NOT a purely mechanical move: setMetaArtist/setMetaAlbumArtist/
    // setMetaAlbum/setMetaGenre truncate to MAX_META_SIZE (see below). That truncation must be
    // preserved - move it into an init block (or a factory/secondary constructor), do not drop it.
    class Builder {
        private var tag: Tag? = null
        private var artist: String? = null
        private var albumArtist: String? = null
        private var composer: String? = null
        private var album: String? = null
        private var title: String? = null
        private var genre: String? = null
        private var uriEntity: UriEntity? = null
        private var metaDisc: Int? = null
        private var metaTrack: Int? = null
        private var metaArtist: String? = null
        private var metaAlbumArtist: String? = null
        private var metaAlbum: String? = null
        private var metaGenre: String? = null
        private var metaCount: Int? = null
        private var metaYear: Int? = null
        private var metaLength: Int? = null
        private var metaCompilation: Boolean? = null
        private var lookupArtist: String? = null
        private var lookupAlbum: String? = null
        private var uriFilter: SortedSet<String>? = null
        private var playedDaysAgo: Int? = null
        private var playedCount: Int? = null

        fun clear(): Builder {
            tag = null
            artist = null
            albumArtist = null
            composer = null
            album = null
            title = null
            genre = null
            uriEntity = null
            metaDisc = null
            metaTrack = null
            metaArtist = null
            metaAlbumArtist = null
            metaAlbum = null
            metaGenre = null
            metaCount = null
            metaYear = null
            metaLength = null
            metaCompilation = null
            lookupArtist = null
            lookupAlbum = null
            uriFilter = null
            playedDaysAgo = null
            playedCount = null
            return this
        }

        fun setTag(tag: Tag?): Builder {
            this.tag = tag
            return this
        }

        fun setArtist(artist: String?): Builder {
            this.artist = artist
            return this
        }

        fun setAlbumArtist(albumArtist: String?): Builder {
            this.albumArtist = albumArtist
            return this
        }

        fun setComposer(composer: String?): Builder {
            this.composer = composer
            return this
        }

        fun setAlbum(album: String?): Builder {
            this.album = album
            return this
        }

        fun setTitle(title: String?): Builder {
            this.title = title
            return this
        }

        fun setGenre(genre: String?): Builder {
            this.genre = genre
            return this
        }

        fun setUriEntity(uriEntity: UriEntity?): Builder {
            this.uriEntity = uriEntity
            return this
        }

        fun setMetaDisc(metaDisc: Int?): Builder {
            this.metaDisc = metaDisc
            return this
        }

        fun setMetaTrack(metaTrack: Int?): Builder {
            this.metaTrack = metaTrack
            return this
        }

        fun setMetaArtist(metaArtist: String?): Builder {
            this.metaArtist = if (metaArtist != null && metaArtist.length > MAX_META_SIZE) metaArtist.substring(0, MAX_META_SIZE) else metaArtist
            return this
        }

        fun setMetaAlbumArtist(metaAlbumArtist: String?): Builder {
            this.metaAlbumArtist = if (metaAlbumArtist != null && metaAlbumArtist.length > MAX_META_SIZE) metaAlbumArtist.substring(0, MAX_META_SIZE) else metaAlbumArtist
            return this
        }

        fun setMetaAlbum(metaAlbum: String?): Builder {
            this.metaAlbum = if (metaAlbum != null && metaAlbum.length > MAX_META_SIZE) metaAlbum.substring(0, MAX_META_SIZE) else metaAlbum
            return this
        }

        fun setMetaGenre(metaGenre: String?): Builder {
            this.metaGenre = if (metaGenre != null && metaGenre.length > MAX_META_SIZE) metaGenre.substring(0, MAX_META_SIZE) else metaGenre
            return this
        }

        fun setMetaCount(metaCount: Int?): Builder {
            this.metaCount = metaCount
            return this
        }

        fun setMetaYear(metaYear: Int?): Builder {
            this.metaYear = metaYear
            return this
        }

        fun setMetaLength(metaLength: Int?): Builder {
            this.metaLength = metaLength
            return this
        }

        fun setMetaCompilation(metaCompilation: Boolean?): Builder {
            this.metaCompilation = metaCompilation
            return this
        }

        fun setLookupArtist(lookupArtist: String?): Builder {
            this.lookupArtist = lookupArtist
            return this
        }

        fun setLookupAlbum(lookupAlbum: String?): Builder {
            this.lookupAlbum = lookupAlbum
            return this
        }

        fun setUriFilter(uriFilter: SortedSet<String>?): Builder {
            this.uriFilter = uriFilter
            return this
        }

        fun setPlayedDaysAgo(playedDaysAgo: Int?): Builder {
            this.playedDaysAgo = playedDaysAgo
            return this
        }

        fun setPlayedCount(playedCount: Int?): Builder {
            this.playedCount = playedCount
            return this
        }

        fun build(): LibraryEntity {
            return LibraryEntity(tag, artist, albumArtist, composer, album, title, genre, uriEntity, metaDisc, metaTrack,
                metaArtist, metaAlbumArtist, metaAlbum, metaGenre, metaCount, metaYear, metaLength, metaCompilation,
                lookupArtist, lookupAlbum, uriFilter, playedDaysAgo, playedCount)
        }

        companion object {
            private const val MAX_META_SIZE = 200
        }
    }

    companion object {
        private const val serialVersionUID = -3383200228964358924L

        fun createBuilder(): Builder {
            return Builder()
        }
    }
}
