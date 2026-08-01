package net.prezz.mpr

object Utils {

    fun shortHashCode(vararg values: Any): Int {
        var hash = 0
        for (v in values) {
            hash = hash xor v.hashCode()
        }

        hash = hash and 0x7FFFFFFF
        val l = hash shr 16
        val r = hash and 0xFFFF

        return l xor r
    }

    fun fixDatabaseQuery(input: String?): String? {
        if (input != null && input.contains("'")) {
            val sb = StringBuilder()
            for (c in input.toCharArray()) {
                if ('\'' == c) {
                    sb.append("'")
                }
                sb.append(c)
            }

            return sb.toString()
        }
        return input
    }

    fun moveInsignificantWordsLast(input: String?): String? {
        if (input != null) {
            if (input.lowercase().startsWith("the ")) {
                val the = input.substring(0, 3)
                val remaining = input.substring(4)
                return "$remaining, $the"
            }
        }

        return input
    }
}
