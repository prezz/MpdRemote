package net.prezz.mpr.mpd.command

import net.prezz.mpr.model.LibraryEntity
import net.prezz.mpr.model.LibraryEntity.Tag
import net.prezz.mpr.mpd.database.MpdLibraryDatabaseHelper
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit.DAYS
import java.util.ArrayList
import java.util.HashMap

class MpdGetFilteredAlbumsAndTitlesCommand(entity: LibraryEntity) :
    MpdDatabaseCommand<LibraryEntity, Array<LibraryEntity>>(entity) {

    @Throws(Exception::class)
    override fun doExecute(databaseHelper: MpdLibraryDatabaseHelper, entity: LibraryEntity): Array<LibraryEntity> {

        val playDataMap = HashMap<String, PlayData>()
        if (entity.getArtist() != null) {
            val today = LocalDate.now()
            val c = databaseHelper.selectAlbumPlayData(entity.getArtist())
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
        }

        val entityBuilder = LibraryEntity.createBuilder()
        val result = ArrayList<LibraryEntity>()
        var c = databaseHelper.selectFilteredAlbumsWithStatistics(entity)
        try {
            if (c.moveToFirst()) {
                do {
                    val album = c.getString(0)
                    val metaAlbum = c.getString(1)
                    val artist = c.getString(2)
                    val metaCompilation = c.getInt(3) > 1
                    val metaCount = c.getInt(4)
                    val metaLength = c.getInt(5)
                    val b = entityBuilder.clear().setTag(Tag.ALBUM).setGenre(entity.getGenre()).setArtist(entity.getArtist()).setAlbumArtist(entity.getAlbumArtist()).setComposer(entity.getComposer())
                        .setAlbum(album).setUriEntity(entity.getUriEntity()).setMetaAlbum(metaAlbum).setMetaArtist(artist).setLookupArtist(artist).setLookupAlbum(album).setMetaCompilation(metaCompilation).setMetaCount(metaCount)
                        .setMetaLength(metaLength).setUriFilter(entity.getUriFilter())

                    val playData = playDataMap.getOrDefault(album, PlayData(null, null))
                    b.setPlayedDaysAgo(playData.daysAgo)
                    b.setPlayedCount(playData.playCount)

                    result.add(b.build())
                } while (c.moveToNext())
            }
        } finally {
            c.close()
        }

        c = databaseHelper.selectFilteredArtistTitles(entity)
        try {
            if (c.moveToFirst()) {
                do {
                    val title = c.getString(0)
                    val album = c.getString(1)
                    val metaLength = c.getInt(2)
                    val metaYear = if (c.isNull(3)) null else c.getInt(3)
                    val metaGenre = c.getString(4)
                    result.add(entityBuilder.clear().setTag(Tag.TITLE).setGenre(entity.getGenre()).setArtist(entity.getArtist()).setAlbumArtist(entity.getAlbumArtist()).setComposer(entity.getComposer())
                        .setTitle(title).setAlbum(album).setUriEntity(entity.getUriEntity()).setMetaLength(metaLength).setMetaYear(metaYear).setMetaGenre(metaGenre).setUriFilter(entity.getUriFilter()).build())
                } while (c.moveToNext())
            }
        } finally {
            c.close()
        }

        return result.toTypedArray()
    }

    override fun onError(): Array<LibraryEntity> = arrayOf()

    private class PlayData(val daysAgo: Int?, val playCount: Int?)
}
