package net.prezz.mpr.mpd.command

import net.prezz.mpr.mpd.database.MpdLibraryDatabaseHelper

class MpdDeleteLocalLibraryDatabaseCommand : MpdDatabaseCommand<Void?, Boolean>(null, false) {

    @Throws(Exception::class)
    override fun doExecute(databaseHelper: MpdLibraryDatabaseHelper, param: Void?): Boolean {
        databaseHelper.cleanDatabase()
        return true
    }

    override fun onError(): Boolean = false
}
