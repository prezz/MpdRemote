package net.prezz.mpr.mpd.command

import android.database.Cursor
import net.prezz.mpr.model.UriEntity
import net.prezz.mpr.mpd.database.MpdLibraryDatabaseHelper
import java.util.Comparator
import java.util.TreeSet

class MpdGetHideableUriFolders : MpdDatabaseCommand<Void?, Array<String>>(null) {

    @Throws(Exception::class)
    override fun doExecute(databaseHelper: MpdLibraryDatabaseHelper, param: Void?): Array<String> {

        val uriSet = TreeSet<String>(UriComparator())

        var c = databaseHelper.selectMusicEntitiesRootUri(TreeSet<String>())
        try {
            addToSet(uriSet, c)
        } finally {
            c.close()
        }

        c = databaseHelper.selectPlaylistEntitiesRootUri(TreeSet<String>())
        try {
            addToSet(uriSet, c)
        } finally {
            c.close()
        }

        return uriSet.toTypedArray()
    }

    override fun onError(): Array<String> = arrayOf()

    private fun addToSet(uriSet: TreeSet<String>, c: Cursor) {
        if (c.moveToFirst()) {
            do {
                var uri = c.getString(0)

                if (uri.contains(UriEntity.DIR_SEPERATOR)) {
                    uri = uri.substring(0, uri.indexOf(UriEntity.DIR_SEPERATOR) + 1)
                    uriSet.add(uri)
                }
            } while (c.moveToNext())
        }
    }

    private class UriComparator : Comparator<String> {

        override fun compare(lhs: String, rhs: String): Int {
            return lhs.compareTo(rhs, ignoreCase = true)
        }
    }
}
