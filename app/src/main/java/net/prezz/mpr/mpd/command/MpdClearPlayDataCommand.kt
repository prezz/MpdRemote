package net.prezz.mpr.mpd.command

import net.prezz.mpr.mpd.database.MpdLibraryDatabaseHelper

class MpdClearPlayDataCommand : MpdDatabaseCommand<Void?, Boolean>(null) {

    @Throws(Exception::class)
    override fun doExecute(databaseHelper: MpdLibraryDatabaseHelper, param: Void?): Boolean {

        databaseHelper.beginTransaction()
        try {
            databaseHelper.clearPlayData()
            databaseHelper.setTransactionSuccessful()
        } finally {
            databaseHelper.endTransaction()
        }

        return true
    }

    override fun onError(): Boolean = false
}
