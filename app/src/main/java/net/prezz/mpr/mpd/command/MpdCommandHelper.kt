package net.prezz.mpr.mpd.command

import net.prezz.mpr.model.LibraryEntity
import net.prezz.mpr.model.UriEntity
import net.prezz.mpr.mpd.connection.MpdConnection

object MpdCommandHelper {

    private val URI_COMPARATOR = UriComparator()

    // KOTLIN-REVIEW: Integer.decode (used here and at other MPD-parsing sites across mpd/command,
    // mpd/database) was intentionally NOT converted to Kotlin's String.toInt(). They differ:
    // Integer.decode also parses 0x/# hex and leading-zero OCTAL (e.g. decode("010") == 8), whereas
    // toInt() is strictly base-10 and would throw / parse differently. Only switch to .toInt() after
    // confirming MPD always sends plain decimal here (it does for Disc/Track/Id/Pos/counts); then
    // `value.substring(...).toInt()` is the idiomatic replacement.
    fun getDecimalNumber(value: String): Int? {
        if (value.isNotEmpty()) {
            try {
                val idx = value.indexOf('/')
                return if (idx != -1) Integer.decode(value.substring(0, idx)) else Integer.decode(value)
            } catch (ex: NumberFormatException) {
            }
        }

        return null
    }

    fun createQuery(prefix: String, entity: LibraryEntity): List<String> {

        if (entity.getUriEntity() != null) {
            val uriEntity = entity.getUriEntity()!!
            return listOf(createQuery(prefix, uriEntity.getFullUriPath(false), entity))
        }

        val uriFilter = entity.getUriFilter()
        if (uriFilter != null && uriFilter.size > 0) {

            val result = ArrayList<String>()
            for (filter in uriFilter) {
                var uri: String? = filter
                if (!uri.isNullOrEmpty() && uri!!.endsWith(UriEntity.DIR_SEPERATOR)) {
                    uri = uri.substring(0, uri.length - 1)
                }
                result.add(createQuery(prefix, uri, entity))
            }
            return result.toList()
        }

        return listOf(createQuery(prefix, null, entity))
    }

    fun createQuery(prefix: String, uriPath: String?, entity: LibraryEntity): String {
        val stringBuilder = StringBuilder()

        stringBuilder.append(prefix)

        if (uriPath != null) {
            stringBuilder.append(" base \"")
            stringBuilder.append(escape(uriPath))
            stringBuilder.append("\"")
        }

        if (entity.getArtist() != null) {
            stringBuilder.append(" Artist \"")
            stringBuilder.append(escape(entity.getArtist()!!))
            stringBuilder.append("\"")
        }

        if (entity.getAlbumArtist() != null) {
            stringBuilder.append(" AlbumArtist \"")
            stringBuilder.append(escape(entity.getAlbumArtist()!!))
            stringBuilder.append("\"")
        }

        if (entity.getComposer() != null) {
            stringBuilder.append(" Composer \"")
            stringBuilder.append(escape(entity.getComposer()!!))
            stringBuilder.append("\"")
        }

        if (entity.getAlbum() != null) {
            stringBuilder.append(" Album \"")
            stringBuilder.append(escape(entity.getAlbum()!!))
            stringBuilder.append("\"")
        }

        if (entity.getGenre() != null) {
            stringBuilder.append(" Genre \"")
            stringBuilder.append(escape(entity.getGenre()!!))
            stringBuilder.append("\"")
        }

        if (entity.getTitle() != null) {
            stringBuilder.append(" Title \"")
            stringBuilder.append(escape(entity.getTitle()!!))
            stringBuilder.append("\"")
        }

        stringBuilder.append("\n")

        return stringBuilder.toString()
    }

    fun escape(input: String?): String {
        return MpdConnection.escape(input.orEmpty())
    }

    fun getUriComparator(): Comparator<UriEntity> {
        return URI_COMPARATOR
    }

    private class UriComparator : Comparator<UriEntity> {

        override fun compare(lhs: UriEntity, rhs: UriEntity): Int {
            if (lhs.uriType == UriEntity.UriType.DIRECTORY && rhs.uriType != UriEntity.UriType.DIRECTORY) {
                return -1
            }
            if (lhs.uriType != UriEntity.UriType.DIRECTORY && rhs.uriType == UriEntity.UriType.DIRECTORY) {
                return 1
            }

            return lhs.uriPath.compareTo(rhs.uriPath, ignoreCase = true)
        }
    }
}
