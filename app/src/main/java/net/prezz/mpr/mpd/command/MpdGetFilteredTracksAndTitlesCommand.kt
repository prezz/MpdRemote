package net.prezz.mpr.mpd.command

import net.prezz.mpr.model.LibraryEntity
import net.prezz.mpr.model.LibraryEntity.Tag
import net.prezz.mpr.mpd.database.MpdLibraryDatabaseHelper

class MpdGetFilteredTracksAndTitlesCommand(entity: LibraryEntity) :
    MpdDatabaseCommand<LibraryEntity, Array<LibraryEntity>>(entity) {

    @Throws(Exception::class)
    override fun doExecute(databaseHelper: MpdLibraryDatabaseHelper, entity: LibraryEntity): Array<LibraryEntity> {
        val entityBuilder = LibraryEntity.createBuilder()
        val c = databaseHelper.selectFilteredAlbumTitles(entity)

        try {
            var i = 0
            val result = arrayOfNulls<LibraryEntity>(c.count)
            if (c.moveToFirst()) {
                do {
                    val disc = if (c.isNull(0)) null else c.getInt(0)
                    val track = if (c.isNull(1)) null else c.getInt(1)
                    val title = c.getString(2)
                    val artist = c.getString(3)
                    val metaArtist = if (track == null) c.getString(4) else artist
                    val albumArtist = c.getString(5)
                    val metaAlbumArtist = if (track == null) c.getString(6) else albumArtist
                    val composer = c.getString(7)
                    val metaLength = c.getInt(8)
                    val metaYear = if (c.isNull(9)) null else c.getInt(9)
                    val metaGenre = c.getString(10)
                    result[i++] = entityBuilder.clear().setTag(Tag.TITLE).setGenre(entity.getGenre()).setAlbum(entity.getAlbum()).setArtist(artist)
                        .setMetaArtist(metaArtist).setAlbumArtist(albumArtist).setMetaAlbumArtist(metaAlbumArtist).setComposer(composer).setTitle(title)
                        .setUriEntity(entity.getUriEntity()).setMetaDisc(disc).setMetaTrack(track).setMetaLength(metaLength).setMetaYear(metaYear).setMetaGenre(metaGenre)
                        .setUriFilter(entity.getUriFilter()).build()
                } while (c.moveToNext())
            }
            @Suppress("UNCHECKED_CAST")
            return result as Array<LibraryEntity>
        } finally {
            c.close()
        }
    }

    override fun onError(): Array<LibraryEntity> = arrayOf()
}
