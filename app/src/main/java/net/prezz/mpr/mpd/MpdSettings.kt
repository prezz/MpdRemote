package net.prezz.mpr.mpd

interface MpdSettings {

    fun getName(): String

    fun getMpdHost(): String

    fun getMpdPort(): String

    fun getMpdPassword(): String?
}
