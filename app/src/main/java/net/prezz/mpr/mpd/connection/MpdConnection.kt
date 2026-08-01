package net.prezz.mpr.mpd.connection

import android.util.Log
import net.prezz.mpr.mpd.MpdSettings
import java.io.BufferedInputStream
import java.io.BufferedWriter
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStreamWriter
import java.net.InetSocketAddress
import java.net.Socket
import java.util.LinkedList

class MpdConnection(private val settings: MpdSettings, private val readTimeout: Int) {

    private var socket: Socket? = null
    private var writer: BufferedWriter? = null
    private var inputStream: BufferedInputStream? = null
    private var version: IntArray? = null

    constructor(settings: MpdSettings) : this(settings, 30000)

    fun isConnected(): Boolean {
        return socket != null && socket!!.isConnected
    }

    @Throws(IOException::class)
    fun connect() {
        if (!isConnected()) {
            try {
                val host = settings.getMpdHost()
                val port = settings.getMpdPort().toInt()
                socket = Socket()
                socket!!.connect(InetSocketAddress(host, port), 10000)
                socket!!.soTimeout = readTimeout

                writer = BufferedWriter(OutputStreamWriter(socket!!.getOutputStream(), "UTF-8"))
                inputStream = BufferedInputStream(socket!!.getInputStream())

                val greeting = readLine() ?: throw IOException("No response from MPD server")
                val split = greeting.split(" ").toTypedArray()

                if (split.size != 3) {
                    throw IOException("Invalid MPD server response")
                }

                if ("OK" != split[0]) {
                    throw IOException("Invalid MPD server response")
                }

                if ("MPD" != split[1]) {
                    throw IOException("Invalid MPD server response")
                }

                version = parseVersion(split[2])

                val password = settings.getMpdPassword()
                if (password != null && password.isNotEmpty()) {
                    writeCommand("password \"${escape(password)}\"\n")
                    val response = readLine()
                    if (OK != response) {
                        throw IOException("Invalid MPD password")
                    }
                }
            } catch (ex: IOException) {
                Log.e(MpdConnection::class.java.name, "failed to establish connection to MPD server", ex)
                disconnect()
                throw ex
            }
        }
    }

    @Throws(IOException::class)
    fun setPartition(partition: String?): Boolean {
        if (partition != null && isConnected() && isMinimumVersion(0, 22, 0)) {
            writeCommand("partition \"${escape(partition)}\"\n")
            val response = readLine()
            if (OK != response) {
                return false
            }
        }

        return true
    }

    fun disconnect() {
        if (isConnected()) {
            try {
                writer!!.close()
            } catch (ex: IOException) {
                Log.e(MpdConnection::class.java.name, "error closing write", ex)
            }

            try {
                inputStream!!.close()
            } catch (ex: IOException) {
                Log.e(MpdConnection::class.java.name, "error closing reader", ex)
            }

            try {
                socket!!.close()
            } catch (ex: IOException) {
                Log.e(MpdConnection::class.java.name, "error closing connection", ex)
            }
        }

        writer = null
        inputStream = null
        socket = null
        version = null
    }

    fun isMinimumVersion(major: Int, minor: Int, point: Int): Boolean {
        try {
            val v = version
            if (v == null || v.size != 3) {
                return false
            }

            if (v[0] < major) {
                return false
            }

            if (v[0] == major && v[1] < minor) {
                return false
            }

            if (v[0] == major && v[1] == minor && v[2] < point) {
                return false
            }
        } catch (ex: Exception) {
            Log.e(MpdConnection::class.java.name, "unable to parse Mpd version number")
            return false
        }

        return true
    }

    @Throws(IOException::class)
    fun writeResponseCommand(command: String?): Array<String> {
        return writeResponseCommand(command, null)
    }

    @Throws(IOException::class)
    fun writeResponseCommand(command: String?, filter: Filter?): Array<String> {
        if (command != null && command.isNotEmpty() && isConnected()) {
            writer!!.write(command)
            writer!!.flush()
            return readResponse(OK, ACK, filter) ?: arrayOf()
        }

        return arrayOf()
    }

    @Throws(IOException::class)
    fun writeResponseCommandList(commands: Array<String>): Array<Array<String>?> {
        return writeResponseCommandList(commands, null)
    }

    @Throws(IOException::class)
    fun writeResponseCommandList(commands: Array<String>, filter: Filter?): Array<Array<String>?> {
        writeCommandList(commands)

        val result = arrayOfNulls<Array<String>>(commands.size)
        for (i in commands.indices) {
            result[i] = readResponse(LIST_OK, ACK, filter)
        }

        // empty buffer
        readResponse(OK, ACK, RejectAllFilter)

        return result
    }

    @Throws(IOException::class)
    fun writeCommand(command: String?) {
        if (command != null && command.isNotEmpty() && isConnected()) {
            writer!!.write(command)
            writer!!.flush()
        }
    }

    @Throws(IOException::class)
    fun writeCommandList(commands: Array<String>) {
        val stringBuilder = StringBuilder()
        stringBuilder.append(LIST_OK_BEGIN)
        for (c in commands) {
            stringBuilder.append(c)
        }
        stringBuilder.append(LIST_OK_END)

        writeCommand(stringBuilder.toString())
    }

    @Throws(IOException::class)
    fun readResponse(filter: Filter?): Array<String>? {
        return readResponse(OK, ACK, filter)
    }

    @Throws(IOException::class)
    fun readLine(): String? {
        val bytes = ByteArrayOutputStream()

        var next: Int
        while (inputStream!!.read().also { next = it } != -1) {
            if (next == '\n'.code) {
                break
            }
            bytes.write(next)
        }

        return if (bytes.size() == 0 && next == -1) null else String(bytes.toByteArray(), Charsets.UTF_8)
    }

    @Throws(IOException::class)
    fun readBinary(buffer: ByteArray, offset: Int, length: Int): Int {
        var total = 0
        var off = offset
        var len = length

        while (len > 0) {
            val read = inputStream!!.read(buffer, off, len)
            if (read == -1) {
                break
            }

            total += read
            off += read
            len -= read
        }

        return total
    }

    @Throws(IOException::class)
    private fun readResponse(successTerminator: String, errorTerminator: String, filter: Filter?): Array<String>? {
        if (isConnected()) {
            val buffer: MutableList<String> = LinkedList()
            while (true) {
                val current = readLine() ?: break
                if (current.startsWith(successTerminator)) {
                    break
                }
                if (current.startsWith(errorTerminator)) {
                    throw IOException("Error reading MPD response: $current")
                }
                if (filter == null || filter.accepts(current)) {
                    buffer.add(current)
                }
            }
            return buffer.toTypedArray()
        }

        return null
    }

    private fun parseVersion(version: String?): IntArray? {
        try {
            if (version == null) {
                return null
            }

            val split = version.split("\\.".toRegex()).toTypedArray()
            val serverMajor = split[0].toInt()
            val serverMinor = split[1].toInt()
            val serverPoint = split[2].toInt()

            return intArrayOf(serverMajor, serverMinor, serverPoint)
        } catch (ex: Exception) {
            Log.e(MpdConnection::class.java.name, "unable to parse Mpd version number")
            return null
        }
    }

    companion object {
        const val OK = "OK"
        const val ACK = "ACK"
        const val LIST_OK = "list_OK"
        const val LIST_OK_BEGIN = "command_list_ok_begin\n"
        const val LIST_OK_END = "command_list_end\n"

        /**
         * Escapes a value for use inside a double-quoted MPD argument. Both the quote character
         * and the backslash must be prefixed with a backslash; a single pass handles both without
         * ordering concerns.
         */
        fun escape(value: String): String {
            val sb = StringBuilder(value.length)
            for (c in value) {
                if (c == '"' || c == '\\') {
                    sb.append('\\')
                }
                sb.append(c)
            }
            return sb.toString()
        }
    }
}
