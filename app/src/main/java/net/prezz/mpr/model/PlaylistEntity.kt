package net.prezz.mpr.model

import java.io.Serializable

import net.prezz.mpr.model.UriEntity.UriType

// KOTLIN-REVIEW: This class still uses Java-style getters (fun getX()) instead of `val` properties.
// Deferred deliberately: the getter names (getArtist/getAlbum/getTitle/getName/getDisc/getTrack/
// getTime/getPriority/getUriEntity/getId/getPosition) are NOT unique - they also exist on
// LibraryEntity, MusicLibraryRecord, and (getTime/getPriority) the whole AdapterEntity UI hierarchy.
// A safe conversion therefore needs a TYPE-AWARE refactor (IntelliJ: right-click a getter ->
// "Refactor > Convert Java-style getter to property", or rename member), NOT a textual find/replace,
// because a text swap can silently retarget a same-named getter on a different type and still compile.
// The Builder below should then collapse into a data class with default (= null) constructor args;
// see the notes on Builder.clear() and its loop reuse before doing so.
class PlaylistEntity private constructor(
    private val id: Int?,
    private val position: Int?,
    private val priority: Int?,
    private val artist: String?,
    private val album: String?,
    private val disc: Int?,
    private val track: Int?,
    private val title: String?,
    private val time: Int?,
    private val name: String?,
    private val uriEntity: UriEntity
) : Serializable {

    fun getId(): Int? {
        return id
    }

    fun getPosition(): Int? {
        return position
    }

    fun getPriority(): Int? {
        return priority
    }

    fun getArtist(): String? {
        return artist
    }

    fun getAlbum(): String? {
        return album
    }

    fun getDisc(): Int? {
        return disc
    }

    fun getTrack(): Int? {
        return track
    }

    fun getTitle(): String? {
        return title
    }

    fun getTime(): Int? {
        return time
    }

    fun getName(): String? {
        return name
    }

    fun getUriEntity(): UriEntity {
        return uriEntity
    }

    override fun toString(): String {
        return "$id, $position, $priority, $artist, $album, $disc $track $title $time $name $uriEntity"
    }

    // KOTLIN-REVIEW: Hand-rolled Java Builder. Idiomatic Kotlin would drop this entirely in favour of
    // a data class with default constructor args + named-argument call sites (and move build()'s
    // UriEntity wrapping into an init block / factory).
    // TWO behaviour-sensitive caveats before converting:
    //   1. clear() below is buggy: it resets every field EXCEPT `name`, so a `name` set on one entity
    //      leaks into the next. A single builder instance is reused across rows in the MpdGetPlaylist*
    //      commands (builder.clear() per "file:" line, builder.build() per entry), so state persists
    //      between iterations by design - a naive rewrite to per-row construction must preserve/repair
    //      exactly which fields survive a clear().
    //   2. Because of that reuse, replacing the builder with immutable named-arg construction changes
    //      when/how fields are reset; verify each MpdGetPlaylist*Command loop still produces identical
    //      entities.
    class Builder {
        private var id: Int? = null
        private var position: Int? = null
        private var priority: Int? = null
        private var artist: String? = null
        private var album: String? = null
        private var disc: Int? = null
        private var track: Int? = null
        private var title: String? = null
        private var time: Int? = null
        private var name: String? = null
        private var uri: String? = null

        fun clear(): Builder {
            id = null
            position = null
            priority = null
            artist = null
            album = null
            disc = null
            track = null
            title = null
            time = null
            uri = null
            // KOTLIN-REVIEW: BUG - `name` is not reset here (unlike every other field), so it leaks
            // from one built entity to the next when this builder is reused in a loop. If keeping the
            // builder, add `name = null`; if converting to per-row construction, this is fixed for free.
            return this
        }

        fun setId(id: Int?): Builder {
            this.id = id
            return this
        }

        fun setPosition(position: Int?): Builder {
            this.position = position
            return this
        }

        fun setPriority(priority: Int?): Builder {
            this.priority = priority
            return this
        }

        fun setArtist(artist: String?): Builder {
            this.artist = artist
            return this
        }

        fun setAlbum(album: String?): Builder {
            this.album = album
            return this
        }

        fun setDisc(disc: Int?): Builder {
            this.disc = disc
            return this
        }

        fun setTrack(track: Int?): Builder {
            this.track = track
            return this
        }

        fun setTitle(title: String?): Builder {
            this.title = title
            return this
        }

        fun setTime(time: Int?): Builder {
            this.time = time
            return this
        }

        fun setName(name: String?): Builder {
            this.name = name
            return this
        }

        fun setUri(uri: String?): Builder {
            this.uri = uri
            return this
        }

        fun build(): PlaylistEntity {
            val uriEntity = UriEntity(UriType.FILE, UriEntity.FileType.PLAYLIST, "", uri ?: "")
            return PlaylistEntity(id, position, priority, artist, album, disc, track, title, time, name, uriEntity)
        }
    }

    companion object {
        private const val serialVersionUID = 1363338950259562011L

        fun createBuilder(): Builder {
            return Builder()
        }
    }
}
