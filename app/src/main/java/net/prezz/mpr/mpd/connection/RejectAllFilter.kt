package net.prezz.mpr.mpd.connection

object RejectAllFilter : Filter {

    override fun accepts(line: String): Boolean {
        return false
    }
}
