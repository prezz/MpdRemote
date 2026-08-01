package net.prezz.mpr.mpd.command

import android.database.Cursor
import net.prezz.mpr.model.UriEntity
import net.prezz.mpr.model.UriEntity.FileType
import net.prezz.mpr.model.UriEntity.UriType
import net.prezz.mpr.mpd.database.MpdLibraryDatabaseHelper
import java.util.Collections
import java.util.SortedSet
import java.util.TreeSet

class MpdGetUriCommand(uriEntity: UriEntity?, uriFilter: SortedSet<String>?) :
    MpdDatabaseCommand<MpdGetUriCommand.Param, Array<UriEntity>>(Param(uriEntity, uriFilter)) {

    class Param(val uriEntity: UriEntity?, uriFilter: SortedSet<String>?) {
        val uriFilter: SortedSet<String>? = if (uriFilter != null) Collections.unmodifiableSortedSet(uriFilter) else null
    }

    @Throws(Exception::class)
    override fun doExecute(databaseHelper: MpdLibraryDatabaseHelper, param: Param): Array<UriEntity> {
        val uriEntity = param.uriEntity
        val uriFilter = param.uriFilter

        val uriSet = TreeSet<UriEntity>(MpdCommandHelper.getUriComparator())

        val uriPath = uriEntity?.getFullUriPath(true) ?: ""

        var c = if (uriPath.isEmpty()) databaseHelper.selectMusicEntitiesRootUri(uriFilter) else databaseHelper.selectMusicEntitiesUri(uriPath)
        try {
            addToSet(uriSet, c, uriPath, FileType.MUSIC)
        } finally {
            c.close()
        }

        c = if (uriPath.isEmpty()) databaseHelper.selectPlaylistEntitiesRootUri(uriFilter) else databaseHelper.selectPlaylistEntitiesUri(uriPath)
        try {
            addToSet(uriSet, c, uriPath, FileType.PLAYLIST)
        } finally {
            c.close()
        }

        return uriSet.toTypedArray()
    }

    override fun onError(): Array<UriEntity> = arrayOf()

    private fun addToSet(uriSet: TreeSet<UriEntity>, c: Cursor, uriPath: String, fileType: FileType) {
        if (c.moveToFirst()) {
            do {
                val uri = c.getString(0)

                if (uri.contains(UriEntity.DIR_SEPERATOR)) {
                    val dir = uri.substring(0, uri.indexOf(UriEntity.DIR_SEPERATOR))
                    uriSet.add(UriEntity(UriType.DIRECTORY, FileType.NA, uriPath, dir))
                } else {
                    uriSet.add(UriEntity(UriType.FILE, fileType, uriPath, uri))
                }
            } while (c.moveToNext())
        }
    }
}
