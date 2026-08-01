package net.prezz.mpr.mpd.command

import net.prezz.mpr.mpd.database.MpdLibraryDatabaseHelper
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class MpdExportPlayDataCommand(offset: Int, limit: Int) :
    MpdDatabaseCommand<MpdExportPlayDataCommand.Param, String>(Param(offset, limit)) {

    class Param(val offset: Int, val limit: Int)

    @Throws(Exception::class)
    override fun doExecute(databaseHelper: MpdLibraryDatabaseHelper, param: Param): String {

        val csvFormat = CSVFormat.DEFAULT.builder()
            .setHeader("Artist", "Album", "Title", "Date", "Count")
            .setSkipHeaderRecord(false)
            .get()

        val csvStringBuilder = StringBuilder()
        val csvPrinter = CSVPrinter(csvStringBuilder, csvFormat)

        val c = databaseHelper.exportPlayData(param.offset, param.limit)
        try {
            if (c.moveToFirst()) {
                do {
                    val artist = c.getString(0)
                    val album = c.getString(1)
                    val title = c.getString(2)
                    val playDate = LocalDate.parse(c.getString(3), DateTimeFormatter.ISO_DATE)
                    val playCount = c.getInt(4)

                    csvPrinter.printRecord(artist, album, title, playDate.toString(), playCount)
                } while (c.moveToNext())
            }
        } finally {
            c.close()
        }

        return csvStringBuilder.toString()
    }

    override fun onError(): String = "Export failed"
}
