package net.prezz.mpr.mpd.command

import net.prezz.mpr.model.LibraryEntity
import net.prezz.mpr.model.LibraryEntity.Tag
import net.prezz.mpr.model.SearchResult
import net.prezz.mpr.model.UriEntity
import net.prezz.mpr.model.UriEntity.FileType
import net.prezz.mpr.model.UriEntity.UriType
import net.prezz.mpr.mpd.database.MpdLibraryDatabaseHelper
import java.util.ArrayList
import java.util.Collections
import java.util.Locale
import java.util.SortedSet
import java.util.TreeSet

class MpdSearchLibraryCommand(queury: String, searchUri: Boolean, uriFilter: SortedSet<String>?) :
    MpdDatabaseCommand<MpdSearchLibraryCommand.Param, SearchResult>(Param(queury, searchUri, uriFilter)) {

    class Param(val query: String, val searchUri: Boolean, uriFilter: SortedSet<String>?) {
        val uriFilter: SortedSet<String>? = if (uriFilter != null) Collections.unmodifiableSortedSet(uriFilter) else null
    }

    @Throws(Exception::class)
    override fun doExecute(databaseHelper: MpdLibraryDatabaseHelper, param: Param): SearchResult {
        val uriFilter = param.uriFilter

        val entityBuilder = LibraryEntity.createBuilder()

        val libraryEntities = ArrayList<LibraryEntity>()

        var c = databaseHelper.findArtists(param.query, uriFilter)
        try {
            if (c.moveToFirst()) {
                do {
                    val artist = c.getString(0)
                    val metaArtist = c.getString(1)
                    val metaCount = c.getInt(2)
                    libraryEntities.add(entityBuilder.clear().setTag(Tag.ARTIST).setArtist(artist).setMetaArtist(metaArtist).setMetaCount(metaCount).setUriFilter(uriFilter).build())
                } while (c.moveToNext())
            }
        } finally {
            c.close()
        }

        c = databaseHelper.findAlbumArtists(param.query, uriFilter)
        try {
            if (c.moveToFirst()) {
                do {
                    val albumArtist = c.getString(0)
                    val metaAlbumArtist = c.getString(1)
                    val metaCount = c.getInt(2)
                    libraryEntities.add(entityBuilder.clear().setTag(Tag.ALBUM_ARTIST).setAlbumArtist(albumArtist).setMetaAlbumArtist(metaAlbumArtist).setMetaCount(metaCount).setUriFilter(uriFilter).build())
                } while (c.moveToNext())
            }
        } finally {
            c.close()
        }

        c = databaseHelper.findComposers(param.query, uriFilter)
        try {
            if (c.moveToFirst()) {
                do {
                    val composers = c.getString(0)
                    val metaCount = c.getInt(1)
                    libraryEntities.add(entityBuilder.clear().setTag(Tag.COMPOSER).setComposer(composers).setMetaCount(metaCount).setUriFilter(uriFilter).build())
                } while (c.moveToNext())
            }
        } finally {
            c.close()
        }

        c = databaseHelper.findAlbums(param.query, uriFilter)
        try {
            if (c.moveToFirst()) {
                do {
                    val album = c.getString(0)
                    val metaAlbum = c.getString(1)
                    val artist = c.getString(2)
                    val metaArtist = if (c.isNull(3)) "" else c.getString(3)
                    libraryEntities.add(entityBuilder.clear().setTag(Tag.ALBUM).setAlbum(album).setMetaAlbum(metaAlbum).setMetaArtist(metaArtist).setLookupArtist(artist).setUriFilter(uriFilter).build())
                } while (c.moveToNext())
            }
        } finally {
            c.close()
        }

        c = databaseHelper.findTitles(param.query, uriFilter)
        try {
            if (c.moveToFirst()) {
                do {
                    val title = c.getString(0)
                    val artist = c.getString(1)
                    val album = c.getString(2)
                    libraryEntities.add(entityBuilder.clear().setTag(Tag.TITLE).setTitle(title).setArtist(artist).setAlbum(album).setUriFilter(uriFilter).build())
                } while (c.moveToNext())
            }
        } finally {
            c.close()
        }

        val uriEntities = TreeSet<UriEntity>(MpdCommandHelper.getUriComparator())

        if (param.searchUri) {
            c = databaseHelper.findUri(param.query, uriFilter)
            try {
                if (c.moveToFirst()) {
                    val lowerCaseQuery = param.query.lowercase(Locale.US)
                    do {
                        val uri = c.getString(0)
                        // KOTLIN-REVIEW: cast to java.lang.String forces Java's String.split, which
                        // (a) treats the separator as a REGEX and (b) drops trailing empty segments.
                        // Kotlin's uri.split(DIR_SEPERATOR) treats it as a literal and KEEPS trailing
                        // empties, so it is not a safe drop-in here (a path ending in "/" would yield an
                        // extra "" element). To convert: use uri.split(DIR_SEPERATOR) and, if trailing
                        // empties matter, append .dropLastWhile { it.isEmpty() } (see UriEntity.getUriFilname
                        // which handles exactly this). DIR_SEPERATOR is "/" so regex vs literal is a no-op.
                        val split = (uri as java.lang.String).split(UriEntity.DIR_SEPERATOR)

                        val sb = StringBuilder()
                        for (i in split.indices) {
                            val section = split[i]
                            if (section.lowercase(Locale.US).contains(lowerCaseQuery)) {
                                if (i == split.size - 1) {
                                    uriEntities.add(UriEntity(UriType.FILE, FileType.MUSIC, sb.toString(), section))
                                } else {
                                    uriEntities.add(UriEntity(UriType.DIRECTORY, FileType.NA, sb.toString(), section))
                                }
                            }

                            sb.append(section)
                            sb.append(UriEntity.DIR_SEPERATOR)
                        }

                    } while (c.moveToNext())
                }
            } finally {
                c.close()
            }
        }

        return SearchResult(libraryEntities.toTypedArray(), uriEntities.toTypedArray())
    }

    override fun onError(): SearchResult = SearchResult(arrayOf(), arrayOf())
}
