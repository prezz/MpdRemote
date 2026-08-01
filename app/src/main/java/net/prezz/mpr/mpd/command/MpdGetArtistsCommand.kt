package net.prezz.mpr.mpd.command

import net.prezz.mpr.model.LibraryEntity
import net.prezz.mpr.model.LibraryEntity.Tag
import net.prezz.mpr.mpd.database.MpdLibraryDatabaseHelper
import java.util.ArrayList

class MpdGetArtistsCommand(entity: LibraryEntity) :
    MpdDatabaseCommand<LibraryEntity, Array<LibraryEntity>>(entity) {

    @Throws(Exception::class)
    override fun doExecute(databaseHelper: MpdLibraryDatabaseHelper, entity: LibraryEntity): Array<LibraryEntity> {
        val entityBuilder = LibraryEntity.createBuilder()

        val libraryEntities = ArrayList<LibraryEntity>()

        var c = databaseHelper.selectArtists(entity)
        try {
            if (c.moveToFirst()) {
                do {
                    val artist = c.getString(0)
                    val metaArtist = c.getString(1)
                    val metaCount = c.getInt(2)
                    libraryEntities.add(entityBuilder.clear().setTag(Tag.ARTIST).setArtist(artist).setUriEntity(entity.getUriEntity()).setMetaArtist(metaArtist).setMetaCount(metaCount).setUriFilter(entity.getUriFilter()).build())
                } while (c.moveToNext())
            }
        } finally {
            c.close()
        }

        c = databaseHelper.selectAlbumArtists(entity)
        try {
            if (c.moveToFirst()) {
                do {
                    val albumArtist = c.getString(0)
                    val metaAlbumArtist = c.getString(1)
                    val metaCount = c.getInt(2)
                    libraryEntities.add(entityBuilder.clear().setTag(Tag.ALBUM_ARTIST).setAlbumArtist(albumArtist).setUriEntity(entity.getUriEntity()).setMetaAlbumArtist(metaAlbumArtist).setMetaCount(metaCount).setUriFilter(entity.getUriFilter()).build())
                } while (c.moveToNext())
            }
        } finally {
            c.close()
        }

        c = databaseHelper.selectComposers(entity)
        try {
            if (c.moveToFirst()) {
                do {
                    val composer = c.getString(0)
                    val metaCount = c.getInt(1)
                    libraryEntities.add(entityBuilder.clear().setTag(Tag.COMPOSER).setComposer(composer).setUriEntity(entity.getUriEntity()).setMetaCount(metaCount).setUriFilter(entity.getUriFilter()).build())
                } while (c.moveToNext())
            }
        } finally {
            c.close()
        }

        return libraryEntities.toTypedArray()
    }

    override fun onError(): Array<LibraryEntity> = arrayOf()
}
