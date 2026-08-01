package net.prezz.mpr.model.external.cache

import net.prezz.mpr.Utils
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class CoverCacheDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, "CoverCache.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE album_cover_url (artist TEXT, album TEXT, url TEXT, PRIMARY KEY(artist, album))")
        db.execSQL("CREATE TABLE album_cover_file (url TEXT, filename TEXT, PRIMARY KEY(url))")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
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

    fun getCoverUrl(artist: String?, album: String?): Cursor {
        val db = this.readableDatabase
        return db.rawQuery("SELECT url FROM album_cover_url WHERE artist='${Utils.fixDatabaseQuery(artist)}' AND album ='${Utils.fixDatabaseQuery(album)}'", null)
    }

    fun insertCoverUrl(artist: String?, album: String?, url: String?) {
        val url_values = ContentValues()
        url_values.put("artist", artist)
        url_values.put("album", album)
        url_values.put("url", url)

        val db = this.writableDatabase
        db.insert("album_cover_url", null, url_values)
    }

    fun deleteCoverUrl(artist: String?, album: String?) {
        val db = this.writableDatabase
        db.execSQL("DELETE FROM album_cover_url WHERE artist='${Utils.fixDatabaseQuery(artist)}' AND album ='${Utils.fixDatabaseQuery(album)}'")
    }

    fun getUrlUseCount(url: String?): Cursor {
        val db = this.readableDatabase
        return db.rawQuery("SELECT count(*) FROM album_cover_url WHERE url='${Utils.fixDatabaseQuery(url)}'", null)
    }

    fun getCoverFile(url: String?): Cursor {
        val db = this.readableDatabase
        return db.rawQuery("SELECT filename FROM album_cover_file WHERE url='${Utils.fixDatabaseQuery(url)}'", null)
    }

    fun insertCoverFile(coverUrl: String?, coverFile: String?) {
        val file_values = ContentValues()
        file_values.put("url", coverUrl)
        file_values.put("filename", coverFile)

        val db = this.writableDatabase
        db.insert("album_cover_file", null, file_values)
    }

    fun deleteCoverFile(url: String?) {
        val db = this.writableDatabase
        db.execSQL("DELETE FROM album_cover_file WHERE url='${Utils.fixDatabaseQuery(url)}'")
    }
}
