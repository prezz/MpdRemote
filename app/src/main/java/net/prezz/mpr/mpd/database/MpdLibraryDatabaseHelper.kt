package net.prezz.mpr.mpd.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

import net.prezz.mpr.Utils
import net.prezz.mpr.model.LibraryEntity
import net.prezz.mpr.model.UriEntity

import java.util.SortedSet

class MpdLibraryDatabaseHelper(context: Context, databaseName: String) :
    SQLiteOpenHelper(context, databaseName + LIBRARY_FILE_DB_POSTFIX, null, 10) {

    override fun onCreate(db: SQLiteDatabase) {
        doCreate(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS music_entities")
        db.execSQL("DROP TABLE IF EXISTS playlist_entities")
//        db.execSQL("DROP TABLE IF EXISTS play_data"); // <-- careful, you might loos all play data
        doCreate(db)
    }

    fun cleanDatabase() {
        val db = this.writableDatabase
        db.execSQL("DROP TABLE IF EXISTS music_entities")
        db.execSQL("DROP TABLE IF EXISTS playlist_entities")
        doCreate(db)
    }

    fun beginTransaction() {
        val db = this.writableDatabase
        db.beginTransaction()
    }

    fun setTransactionSuccessful() {
        val db = this.writableDatabase
        db.setTransactionSuccessful()
    }

    fun endTransaction() {
        val db = this.writableDatabase
        db.endTransaction()
    }

    fun addMusicEntity(record: MusicLibraryRecord) {
        val values = ContentValues()
        values.put("artist", record.getArtist())
        values.put("meta_artist", record.getMetaArtist())
        values.put("album_artist", record.getAlbumArtist())
        values.put("meta_album_artist", record.getMetaAlbumArtist())
        values.put("composer", record.getComposer())
        values.put("album", record.getAlbum())
        values.put("meta_album", record.getMetaAlbum())
        values.put("title", record.getTitle())
        values.put("disc", record.getDisc())
        values.put("track", record.getTrack())
        values.put("genre", record.getGenre())
        values.put("year", record.getYear())
        values.put("length", record.getLength())
        values.put("uri", record.getUri())

        val db = this.writableDatabase
        db.insert("music_entities", null, values)
    }

    fun addPlaylistEntity(uri: String) {
        val values = ContentValues()
        values.put("uri", uri)

        val db = this.writableDatabase
        db.insert("playlist_entities", null, values)
    }

    fun getRowCount(): Int {
        var result = 0

        val db = this.readableDatabase
        var c = db.rawQuery("SELECT count(*) FROM music_entities", null)
        try {
            if (c.moveToFirst()) {
                result += if (c.isNull(0)) 0 else c.getInt(0)
            }
        } finally {
            c.close()
        }

        c = db.rawQuery("SELECT count(*) FROM playlist_entities", null)
        try {
            if (c.moveToFirst()) {
                result += if (c.isNull(0)) 0 else c.getInt(0)
            }
        } finally {
            c.close()
        }

        return result
    }

    fun selectAlbums(orderByArtist: Boolean, entity: LibraryEntity?): Cursor {
        val db = this.readableDatabase
        return if (orderByArtist) {
            db.rawQuery("SELECT album, meta_album, group_concat(a, ', '), group_concat(ma, ', ') g, sum(l), count(a) FROM (SELECT DISTINCT album, meta_album, artist a, meta_artist ma, sum(length) l FROM music_entities ${buildFilter(null, entity)} GROUP BY album, meta_album, artist, meta_artist ORDER BY track, meta_artist, title) GROUP BY album ORDER BY g COLLATE NOCASE asc, album COLLATE NOCASE asc", null)
        } else {
            db.rawQuery("SELECT album, meta_album, group_concat(a, ', '), group_concat(ma, ', '), sum(l), count(a) FROM (SELECT DISTINCT album, meta_album, artist a, meta_artist ma, sum(length) l FROM music_entities ${buildFilter(null, entity)} GROUP BY album, meta_album, artist, meta_artist ORDER BY track, meta_artist, title) GROUP BY meta_album ORDER BY meta_album COLLATE NOCASE asc", null)
        }
    }

    fun selectArtists(entity: LibraryEntity?): Cursor {
        val db = this.readableDatabase
        return db.rawQuery("SELECT artist, meta_artist, count(title) FROM music_entities ${buildFilter(null, entity)} GROUP BY meta_artist ORDER BY meta_artist COLLATE NOCASE asc", null)
    }

    fun selectAlbumArtists(entity: LibraryEntity?): Cursor {
        val db = this.readableDatabase
        return db.rawQuery("SELECT album_artist, meta_album_artist, count(title) FROM music_entities ${buildFilter("WHERE album_artist IS NOT NULL", entity)} GROUP BY meta_album_artist ORDER BY meta_album_artist COLLATE NOCASE asc", null)
    }

    fun selectComposers(entity: LibraryEntity?): Cursor {
        val db = this.readableDatabase
        return db.rawQuery("SELECT composer, count(title) FROM music_entities ${buildFilter("WHERE composer IS NOT NULL", entity)} GROUP BY composer ORDER BY composer COLLATE NOCASE asc", null)
    }

    fun selectGenres(entity: LibraryEntity?): Cursor {
        val db = this.readableDatabase
        return db.rawQuery("SELECT genre, count(a) FROM (SELECT genre, album a FROM music_entities ${buildFilter(null, entity)} GROUP BY genre, album) GROUP BY genre ORDER BY genre COLLATE NOCASE asc", null)
    }

    fun selectFilteredAlbumsWithStatistics(entity: LibraryEntity?): Cursor {
        val db = this.readableDatabase
        return db.rawQuery("SELECT album, meta_album, group_concat(a, ', '), count(a), sum(t), sum(l) FROM (SELECT DISTINCT album, meta_album, artist a, count(title) t, sum(length) l FROM music_entities ${buildFilter(null, entity)} GROUP BY album, meta_album, artist) GROUP BY meta_album ORDER BY meta_album COLLATE NOCASE asc", null)
    }

    fun selectFilteredArtistTitles(entity: LibraryEntity?): Cursor {
        val db = this.readableDatabase
        return db.rawQuery("SELECT title, album, length, year, genre FROM music_entities ${buildFilter(null, entity)} ORDER BY title COLLATE NOCASE asc", null)
    }

    fun selectFilteredArtists(entity: LibraryEntity?): Cursor {
        val db = this.readableDatabase
        return db.rawQuery("SELECT artist, meta_artist, count(t) FROM (SELECT artist, meta_artist, title t FROM music_entities ${buildFilter(null, entity)}) GROUP BY meta_artist ORDER BY meta_artist COLLATE NOCASE asc", null)
    }

    fun selectFilteredAlbumArtists(entity: LibraryEntity?): Cursor {
        val db = this.readableDatabase
        return db.rawQuery("SELECT album_artist, meta_album_artist, count(t) FROM (SELECT album_artist, meta_album_artist, title t FROM music_entities ${buildFilter(null, entity)} AND album_artist NOT NULL) GROUP BY meta_album_artist ORDER BY meta_album_artist COLLATE NOCASE asc", null)
    }

    fun selectFilteredComposers(entity: LibraryEntity?): Cursor {
        val db = this.readableDatabase
        return db.rawQuery("SELECT composer, count(t) FROM (SELECT composer, title t FROM music_entities ${buildFilter(null, entity)} AND composer NOT NULL) GROUP BY composer ORDER BY composer COLLATE NOCASE asc", null)
    }

    fun selectFilteredAlbumTitles(entity: LibraryEntity?): Cursor {
        val db = this.readableDatabase
        return db.rawQuery("SELECT disc, track, title, artist, meta_artist, album_artist, meta_album_artist, composer, length, year, genre FROM music_entities ${buildFilter(null, entity)} ORDER BY disc, track, meta_artist COLLATE NOCASE asc, title COLLATE NOCASE asc", null)
    }

    fun selectMusicEntitiesRootUri(uriFilter: SortedSet<String>?): Cursor {
        val db = this.readableDatabase
        return db.rawQuery("SELECT uri FROM music_entities ${buildFilter(null, uriFilter)}", null)
    }

    fun selectMusicEntitiesUri(uriPath: String): Cursor {
        val db = this.readableDatabase
        return db.rawQuery(String.format("SELECT SUBSTR(uri, %s) FROM music_entities WHERE uri LIKE '%s%%'", uriPath.length + 1, Utils.fixDatabaseQuery(uriPath)), null)
    }

    fun selectPlaylistEntitiesRootUri(uriFilter: SortedSet<String>?): Cursor {
        val db = this.readableDatabase
        return db.rawQuery("SELECT uri FROM playlist_entities ${buildFilter(null, uriFilter)}", null)
    }

    fun selectPlaylistEntitiesUri(uriPath: String): Cursor {
        val db = this.readableDatabase
        return db.rawQuery(String.format("SELECT SUBSTR(uri, %s) FROM playlist_entities WHERE uri LIKE '%s%%'", uriPath.length + 1, Utils.fixDatabaseQuery(uriPath)), null)
    }

    fun findArtists(query: String, uriFilter: SortedSet<String>?): Cursor {
        val db = this.readableDatabase
        val filter = buildFilter(String.format("WHERE artist LIKE '%%%s%%'", Utils.fixDatabaseQuery(query)), uriFilter)
        return db.rawQuery("SELECT artist, meta_artist, count(title) c FROM music_entities $filter GROUP BY meta_artist ORDER BY meta_artist COLLATE NOCASE asc", null)
    }

    fun findAlbumArtists(query: String, uriFilter: SortedSet<String>?): Cursor {
        val db = this.readableDatabase
        val filter = buildFilter(String.format("WHERE album_artist IS NOT NULL AND album_artist LIKE '%%%s%%'", Utils.fixDatabaseQuery(query)), uriFilter)
        return db.rawQuery("SELECT album_artist, meta_album_artist, count(title) c FROM music_entities $filter GROUP BY meta_album_artist ORDER BY meta_album_artist COLLATE NOCASE asc", null)
    }

    fun findComposers(query: String, uriFilter: SortedSet<String>?): Cursor {
        val db = this.readableDatabase
        val filter = buildFilter(String.format("WHERE composer IS NOT NULL AND composer LIKE '%%%s%%'", Utils.fixDatabaseQuery(query)), uriFilter)
        return db.rawQuery("SELECT composer, count(title) c FROM music_entities $filter GROUP BY composer ORDER BY composer COLLATE NOCASE asc", null)
    }

    fun findAlbums(query: String, uriFilter: SortedSet<String>?): Cursor {
        val db = this.readableDatabase
        val filter = buildFilter(String.format("WHERE album LIKE '%%%s%%'", Utils.fixDatabaseQuery(query)), uriFilter)
        return db.rawQuery("SELECT album, meta_album, group_concat(a, ', '), group_concat(ma, ', ') FROM (SELECT DISTINCT album, meta_album, artist a, meta_artist ma FROM music_entities $filter) GROUP BY meta_album ORDER BY meta_album COLLATE NOCASE asc", null)
    }

    fun findTitles(query: String, uriFilter: SortedSet<String>?): Cursor {
        val db = this.readableDatabase
        val filter = buildFilter(String.format("WHERE title LIKE '%%%s%%'", Utils.fixDatabaseQuery(query)), uriFilter)
        return db.rawQuery("SELECT title, artist, album FROM music_entities $filter ORDER BY title COLLATE NOCASE asc", null)
    }

    fun findUri(query: String, uriFilter: SortedSet<String>?): Cursor {
        val db = this.readableDatabase
        val filter = buildFilter(String.format("WHERE uri LIKE '%%%s%%'", Utils.fixDatabaseQuery(query)), uriFilter)
        return db.rawQuery("SELECT uri FROM music_entities $filter ORDER BY uri COLLATE NOCASE asc", null)
    }

    fun findUris(artist: String?, album: String?): Cursor {
        val db = this.readableDatabase
        return if (artist.isNullOrEmpty()) {
            db.rawQuery("SELECT uri FROM music_entities WHERE album='${Utils.fixDatabaseQuery(album)}'", null)
        } else {
            db.rawQuery("SELECT uri FROM music_entities WHERE artist='${Utils.fixDatabaseQuery(artist)}' AND album='${Utils.fixDatabaseQuery(album)}'", null)
        }
    }

    fun getPlayData(artist: String?, album: String?, title: String?): Cursor {
        val db = this.readableDatabase
        return db.rawQuery("SELECT play_date, play_count FROM play_data WHERE artist='${Utils.fixDatabaseQuery(artist)}' AND album='${Utils.fixDatabaseQuery(album)}' AND title='${Utils.fixDatabaseQuery(title)}'", null)
    }

    fun selectAllAlbumPlayData(): Cursor {
        val db = this.readableDatabase
        return db.rawQuery("SELECT album, max(play_date), max(play_count) FROM play_data GROUP BY album", null)
    }

    fun selectAlbumPlayData(artist: String?): Cursor {
        val db = this.readableDatabase
        return db.rawQuery("SELECT album, max(play_date), max(play_count) FROM play_data WHERE artist = '${Utils.fixDatabaseQuery(artist)}' GROUP BY album", null)
    }

    fun upsertPlayData(artist: String?, album: String?, title: String?, date: String?, count: Int): Long {

        val values = ContentValues()
        values.put("artist", artist)
        values.put("album", album)
        values.put("title", title)
        values.put("play_date", date)
        values.put("play_count", count)

        val db = this.writableDatabase
        return db.replace("play_data", null, values)
    }

    fun exportPlayData(offset: Int, limit: Int): Cursor {
        val db = this.readableDatabase
        return db.rawQuery("SELECT artist, album, title, play_date, play_count FROM play_data order by artist, album, title LIMIT $limit OFFSET $offset", null)
    }

    fun clearPlayData() {
        val db = this.readableDatabase
        db.execSQL("DELETE FROM play_data")
    }

    private fun buildFilter(prefix: String?, entity: LibraryEntity?): String {

        val stringBuilder = StringBuilder()

        if (!prefix.isNullOrEmpty()) {
            stringBuilder.append(prefix)
        }

        if (entity == null) {
            return stringBuilder.toString()
        }

        if (entity.getArtist() != null) {
            if (stringBuilder.length == 0) {
                stringBuilder.append("WHERE")
            } else {
                stringBuilder.append(" AND")
            }

            stringBuilder.append(" artist='")
            stringBuilder.append(Utils.fixDatabaseQuery(entity.getArtist()))
            stringBuilder.append("'")
        }

        if (entity.getAlbumArtist() != null) {
            if (stringBuilder.length == 0) {
                stringBuilder.append("WHERE")
            } else {
                stringBuilder.append(" AND")
            }

            stringBuilder.append(" album_artist='")
            stringBuilder.append(Utils.fixDatabaseQuery(entity.getAlbumArtist()))
            stringBuilder.append("'")
        }

        if (entity.getComposer() != null) {
            if (stringBuilder.length == 0) {
                stringBuilder.append("WHERE")
            } else {
                stringBuilder.append(" AND")
            }

            stringBuilder.append(" composer='")
            stringBuilder.append(Utils.fixDatabaseQuery(entity.getComposer()))
            stringBuilder.append("'")
        }

        if (entity.getAlbum() != null) {
            if (stringBuilder.length == 0) {
                stringBuilder.append("WHERE")
            } else {
                stringBuilder.append(" AND")
            }

            stringBuilder.append(" album='")
            stringBuilder.append(Utils.fixDatabaseQuery(entity.getAlbum()))
            stringBuilder.append("'")
        }

        if (entity.getGenre() != null) {
            if (stringBuilder.length == 0) {
                stringBuilder.append("WHERE")
            } else {
                stringBuilder.append(" AND")
            }

            stringBuilder.append(" genre='")
            stringBuilder.append(Utils.fixDatabaseQuery(entity.getGenre()))
            stringBuilder.append("'")
        }

        if (entity.getTitle() != null) {
            if (stringBuilder.length == 0) {
                stringBuilder.append("WHERE")
            } else {
                stringBuilder.append(" AND")
            }

            stringBuilder.append(" title='")
            stringBuilder.append(Utils.fixDatabaseQuery(entity.getTitle()))
            stringBuilder.append("'")
        }

        if (entity.getUriEntity() != null) {
            if (stringBuilder.length == 0) {
                stringBuilder.append("WHERE")
            } else {
                stringBuilder.append(" AND")
            }

            val uriEntity: UriEntity = entity.getUriEntity()!!
            stringBuilder.append(String.format(" uri LIKE '%s%%'", Utils.fixDatabaseQuery(uriEntity.getFullUriPath(true))))
        }

        return buildFilter(stringBuilder.toString(), entity.getUriFilter())
    }

    private fun buildFilter(prefix: String?, uriFilter: SortedSet<String>?): String {
        val stringBuilder = StringBuilder()

        if (!prefix.isNullOrEmpty()) {
            stringBuilder.append(prefix)
        }

        if (uriFilter != null && uriFilter.size > 0) {
            val it = uriFilter.iterator()

            if (it.hasNext()) {
                if (stringBuilder.length == 0) {
                    stringBuilder.append("WHERE (")
                } else {
                    stringBuilder.append(" AND (")
                }

                stringBuilder.append("uri LIKE '")
                stringBuilder.append(Utils.fixDatabaseQuery(it.next()))
                stringBuilder.append("%'")
            }

            while (it.hasNext()) {
                stringBuilder.append(" OR uri LIKE '")
                stringBuilder.append(Utils.fixDatabaseQuery(it.next()))
                stringBuilder.append("%'")
            }

            stringBuilder.append(")")
        }

        return stringBuilder.toString()
    }

    private fun doCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE music_entities (id INTEGER PRIMARY KEY, artist TEXT, meta_artist TEXT, album_artist TEXT, meta_album_artist TEXT, composer TEXT, album TEXT, meta_album TEXT, title TEXT, disc INTEGER, track INTEGER, genre TEXT, year INTEGER, length INTEGER, uri TEXT)")
        db.execSQL("CREATE INDEX idx_artist ON music_entities (artist);")
        db.execSQL("CREATE INDEX idx_album_artist ON music_entities (album_artist);")
        db.execSQL("CREATE INDEX idx_composer ON music_entities (composer);")
        db.execSQL("CREATE INDEX idx_album ON music_entities (album);")
        db.execSQL("CREATE INDEX idx_genre ON music_entities (genre);")
        db.execSQL("CREATE INDEX idx_title ON music_entities (title);")
        db.execSQL("CREATE INDEX idx_uri ON music_entities (uri);")

        db.execSQL("CREATE TABLE playlist_entities (id INTEGER PRIMARY KEY, uri TEXT)")

        db.execSQL("CREATE TABLE IF NOT EXISTS play_data (artist TEXT, album TEXT, title TEXT, play_date TEXT, play_count INTEGER, PRIMARY KEY (artist, album, title))")
    }

    companion object {
        const val LIBRARY_FILE_DB_POSTFIX = "_library.db"
    }
}
