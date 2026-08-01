package net.prezz.mpr.mpd.connection

interface Filter {

    fun accepts(line: String): Boolean
}
