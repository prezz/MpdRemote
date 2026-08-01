package net.prezz.mpr.model.servers

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class ServerConfigurationDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, "Servers.db", null, 4) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE server (id INTEGER PRIMARY KEY, name TEXT, host TEXT, port TEXT, password TEXT, streaming TEXT)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE server ADD COLUMN output TEXT DEFAULT \"\"")
        }
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE server ADD COLUMN streaming TEXT DEFAULT \"\"")
        }
        if (oldVersion < 4) {
            db.execSQL("CREATE TABLE server_migrate (id INTEGER PRIMARY KEY, name TEXT, host TEXT, port TEXT, password TEXT, streaming TEXT)")
            db.execSQL("INSERT INTO server_migrate (id, name, host, port, password, streaming) SELECT id, name, host, port, password, streaming FROM server")
            db.execSQL("DROP TABLE server")
            db.execSQL("ALTER TABLE server_migrate RENAME TO server")
        }
    }

    fun getServerHost(id: Int): Cursor {
        val db = this.readableDatabase
        return db.rawQuery("SELECT host FROM server WHERE id=$id", null)
    }

    fun getServers(): Cursor {
        val db = this.readableDatabase
        return db.rawQuery("SELECT * FROM server ORDER BY name COLLATE NOCASE asc", null)
    }

    fun addServer(name: String?, host: String?, port: String?, password: String?, streaming: String?) {
        val values = ContentValues()
        values.put("name", name)
        values.put("host", host)
        values.put("port", port)
        values.put("password", password)
        values.put("streaming", streaming)

        val db = this.writableDatabase
        db.insert("server", null, values)
    }

    fun updateServer(id: Int?, name: String?, host: String?, port: String?, password: String?, streaming: String?) {
        val values = ContentValues()
        values.put("name", name)
        values.put("host", host)
        values.put("port", port)
        values.put("password", password)
        values.put("streaming", streaming)

        val db = this.writableDatabase
        db.update("server", values, "id=$id", null)
    }

    fun deleteServer(id: Int) {
        val db = this.writableDatabase
        db.execSQL("DELETE FROM server WHERE id=$id")
    }
}
