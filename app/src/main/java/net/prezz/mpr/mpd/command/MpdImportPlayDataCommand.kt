package net.prezz.mpr.mpd.command

import net.prezz.mpr.mpd.database.MpdLibraryDatabaseHelper
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import java.io.StringReader
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class MpdImportPlayDataCommand(csvData: String) : MpdDatabaseCommand<String, Boolean>(csvData) {

    @Throws(Exception::class)
    override fun doExecute(databaseHelper: MpdLibraryDatabaseHelper, param: String): Boolean {

        val header = arrayOf("Artist", "Album", "Title", "Date", "Count")
        val csvFormat = CSVFormat.DEFAULT.builder()
            .setHeader(*header)
            .setSkipHeaderRecord(false)
            .get()

        val csvReader = StringReader(param)
        val csvParser = CSVParser.parse(csvReader, csvFormat)
        val csvRecords = csvParser.records

        if (csvRecords.isEmpty()) {
            return false
        }

        if (!csvRecords[0].values().contentEquals(header)) {
            return false
        }

        databaseHelper.beginTransaction()
        try {
            for (i in 1 until csvRecords.size) {
                val record = csvRecords[i]

                val artist = record.get(header[0])
                val album = record.get(header[1])
                val title = record.get(header[2])
                val playData = LocalDate.parse(record.get(header[3]), DateTimeFormatter.ISO_DATE)
                val playCount = isPositive(record.get(header[4]).toInt())

                databaseHelper.upsertPlayData(artist, album, title, playData.format(DateTimeFormatter.ISO_DATE), playCount)
            }

            databaseHelper.setTransactionSuccessful()
        } finally {
            databaseHelper.endTransaction()
        }

        return true
    }

    override fun onError(): Boolean = false

    companion object {
        private fun isPositive(value: Int): Int {
            if (value < 1) {
                throw IllegalArgumentException()
            }
            return value
        }
    }
}
