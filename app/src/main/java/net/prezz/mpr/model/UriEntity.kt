package net.prezz.mpr.model

import java.io.Serializable

data class UriEntity(
    val uriType: UriType,
    val fileType: FileType,
    val parentUriPath: String,
    val uriPath: String
) : Serializable {

    enum class UriType { DIRECTORY, FILE }
    enum class FileType { NA, MUSIC, PLAYLIST }

    fun getFullUriPath(appendDirSeperator: Boolean): String {
        val sb = StringBuilder()
        sb.append(parentUriPath)
        sb.append(uriPath)
        if (appendDirSeperator && uriType == UriType.DIRECTORY) {
            sb.append(DIR_SEPERATOR)
        }
        return sb.toString()
    }

    fun getUriFilname(): String {
        if (uriType == UriType.FILE) {
            // Mirror Java's String.split(regex), which drops trailing empty segments, so a path
            // ending in a separator still yields the last real segment (e.g. "a/b/" -> "b").
            // Kotlin's split keeps trailing empties, hence the explicit dropLastWhile.
            val sections = uriPath.split(DIR_SEPERATOR).dropLastWhile { it.isEmpty() }
            return if (sections.isEmpty()) "" else sections[sections.size - 1]
        }

        return ""
    }

    companion object {
        private const val serialVersionUID = -6990570522929131413L

        const val DIR_SEPERATOR = "/"
    }
}
