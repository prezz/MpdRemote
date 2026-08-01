package net.prezz.mpr.mpd.command

import net.prezz.mpr.model.LibraryEntity
import net.prezz.mpr.model.LibraryEntity.Tag
import net.prezz.mpr.mpd.database.MpdLibraryDatabaseHelper
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit.DAYS
import java.util.ArrayList
import java.util.Comparator
import java.util.HashMap

class MpdGetAlbumsCommand(sortByArtist: Boolean, entity: LibraryEntity) :
    MpdDatabaseCommand<MpdGetAlbumsCommand.Param, Array<LibraryEntity>>(Param(sortByArtist, entity)) {

    class Param(val sortByArtist: Boolean, val entity: LibraryEntity)

    @Throws(Exception::class)
    override fun doExecute(databaseHelper: MpdLibraryDatabaseHelper, param: Param): Array<LibraryEntity> {

        val today = LocalDate.now()
        val playDataMap = HashMap<String, PlayData>()
        var c = databaseHelper.selectAllAlbumPlayData()
        try {
            if (c.moveToFirst()) {
                do {
                    val album = c.getString(0)
                    val playDate = c.getString(1)
                    val playCount = c.getInt(2)

                    val daysAgo = DAYS.between(LocalDate.parse(playDate, DateTimeFormatter.ISO_DATE), today).toInt()
                    playDataMap[album] = PlayData(daysAgo, playCount)
                } while (c.moveToNext())
            }
        } finally {
            c.close()
        }

        val entityBuilder = LibraryEntity.createBuilder()

        val sortByArtist = param.sortByArtist
        val entity = param.entity

        c = databaseHelper.selectAlbums(sortByArtist, entity)
        try {
            var compilationIndex = 0
            val tracks = ArrayList<LibraryEntity>()
            val compilations = ArrayList<LibraryEntity>()
            val result = ArrayList<LibraryEntity>(c.count)
            if (c.moveToFirst()) {
                do {
                    val album = c.getString(0)
                    val metaAlbum = c.getString(1)
                    val artist = c.getString(2)
                    val metaArtist = if (c.isNull(3)) "" else c.getString(3)
                    val metaLength = c.getInt(4)
                    val metaCompilation = c.getInt(5) > 1

                    if (album.isNullOrEmpty()) {
                        val b = entityBuilder.clear().setTag(Tag.ALBUM).setGenre(entity.getGenre()).setAlbum(album).setUriEntity(entity.getUriEntity()).setMetaAlbum(metaAlbum).setMetaArtist(metaArtist)
                            .setLookupArtist(artist).setLookupAlbum(album).setMetaCompilation(metaCompilation).setMetaLength(metaLength).setUriFilter(entity.getUriFilter())

                        val playData = playDataMap.getOrDefault(album, PlayData(null, null))
                        b.setPlayedDaysAgo(playData.daysAgo)
                        b.setPlayedCount(playData.playCount)

                        tracks.add(b.build())
                    } else if (sortByArtist && metaCompilation) {
                        val b = entityBuilder.clear().setTag(Tag.ALBUM).setGenre(entity.getGenre()).setAlbum(album).setUriEntity(entity.getUriEntity()).setMetaAlbum(metaAlbum).setMetaArtist(VARIOUS)
                            .setLookupArtist(VARIOUS).setLookupAlbum(album).setMetaCompilation(metaCompilation).setMetaLength(metaLength).setUriFilter(entity.getUriFilter())

                        val playData = playDataMap.getOrDefault(album, PlayData(null, null))
                        b.setPlayedDaysAgo(playData.daysAgo)
                        b.setPlayedCount(playData.playCount)

                        compilations.add(b.build())
                    } else {
                        val b = entityBuilder.clear().setTag(Tag.ALBUM).setGenre(entity.getGenre()).setAlbum(album).setUriEntity(entity.getUriEntity()).setMetaAlbum(metaAlbum).setMetaArtist(metaArtist)
                            .setLookupArtist(artist).setLookupAlbum(album).setMetaCompilation(metaCompilation).setMetaLength(metaLength).setUriFilter(entity.getUriFilter())

                        val playData = playDataMap.getOrDefault(album, PlayData(null, null))
                        b.setPlayedDaysAgo(playData.daysAgo)
                        b.setPlayedCount(playData.playCount)

                        result.add(b.build())
                        if (sortByArtist && VARIOUS.compareTo(metaArtist, ignoreCase = true) > 0) {
                            compilationIndex++
                        }
                    }
                } while (c.moveToNext())
            }

            if (sortByArtist) {
                compilations.sortWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.getAlbum()!! })

                result.addAll(compilationIndex, compilations)
            }

            result.addAll(0, tracks)

            return result.toTypedArray()
        } finally {
            c.close()
        }
    }

    override fun onError(): Array<LibraryEntity> = arrayOf()

    private class PlayData(val daysAgo: Int?, val playCount: Int?)

    companion object {
        private const val VARIOUS = "Various"
    }
}
