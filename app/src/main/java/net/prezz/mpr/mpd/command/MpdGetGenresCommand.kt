package net.prezz.mpr.mpd.command

import net.prezz.mpr.model.LibraryEntity
import net.prezz.mpr.model.LibraryEntity.Tag
import net.prezz.mpr.mpd.database.MpdLibraryDatabaseHelper

class MpdGetGenresCommand(entity: LibraryEntity) :
    MpdDatabaseCommand<LibraryEntity, Array<LibraryEntity>>(entity) {

    @Throws(Exception::class)
    override fun doExecute(databaseHelper: MpdLibraryDatabaseHelper, entity: LibraryEntity): Array<LibraryEntity> {
        val entityBuilder = LibraryEntity.createBuilder()

        val c = databaseHelper.selectGenres(entity)

        try {
            var i = 0
            val result = arrayOfNulls<LibraryEntity>(c.count)
            if (c.moveToFirst()) {
                do {
                    val genre = c.getString(0)
                    val metaCount = c.getInt(1)
                    result[i++] = entityBuilder.clear().setTag(Tag.GENRE).setGenre(genre).setUriEntity(entity.getUriEntity()).setUriFilter(entity.getUriFilter()).setMetaCount(metaCount).build()
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
