package net.prezz.mpr.mpd.command

import net.prezz.mpr.model.PlaylistEntity
import net.prezz.mpr.mpd.database.MpdLibraryDatabaseHelper
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class MpdUpdatePlayDataCommand(param: List<PlaylistEntity>) :
    MpdDatabaseCommand<List<PlaylistEntity>, Boolean>(param) {

    @Throws(Exception::class)
    override fun doExecute(databaseHelper: MpdLibraryDatabaseHelper, param: List<PlaylistEntity>): Boolean {

        for (entity in param) {
            if (entity.getArtist() == null || entity.getAlbum() == null || entity.getTitle() == null) {
                return false
            }
        }

        var result = false
        val today = LocalDate.now().format(DateTimeFormatter.ISO_DATE)

        databaseHelper.beginTransaction()
        try {
            for (entity in param) {
                val artist = entity.getArtist()
                val album = entity.getAlbum()
                val title = entity.getTitle()

                val c = databaseHelper.getPlayData(artist, album, title)
                if (c.moveToFirst()) {
                    val date = c.getString(0)
                    val count = c.getInt(1)
                    if (today != date) {
                        databaseHelper.upsertPlayData(artist, album, title, today, count + 1)
                        result = true
                    }
                } else {
                    databaseHelper.upsertPlayData(artist, album, title, today, 1)
                    result = true
                }
            }

            databaseHelper.setTransactionSuccessful()
        } finally {
            databaseHelper.endTransaction()
        }

        return result
    }

    override fun onError(): Boolean = false
}
