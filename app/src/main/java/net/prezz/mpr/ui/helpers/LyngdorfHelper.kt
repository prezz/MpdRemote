package net.prezz.mpr.ui.helpers

import androidx.core.content.edit

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.preference.PreferenceManager
import java.io.BufferedWriter
import java.io.IOException
import java.io.OutputStreamWriter
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.Executors

object LyngdorfHelper {

    private const val LYNGDORF_IP_KEY = "lyngdorfIpKey"
    private val executor = Executors.newSingleThreadExecutor()
    private val handler = Handler(Looper.getMainLooper())

    private var lyngdorfIp: String? = null

    fun setLyngdorfIp(context: Context, ip: String?) {
        lyngdorfIp = null

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        sharedPreferences.edit(commit = true) {
            if (ip == null || ip.isEmpty()) {
                remove(LYNGDORF_IP_KEY)
            } else {
                putString(LYNGDORF_IP_KEY, ip)
            }
        }
    }

    fun getLyngdorfIp(context: Context): String {
        lyngdorfIp?.let { return it }

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        return sharedPreferences.getString(LYNGDORF_IP_KEY, "")!!.also { lyngdorfIp = it }
    }

    fun volumeDown(context: Context): Boolean {
        if (getLyngdorfIp(context).isEmpty()) {
            return false
        }

        sendLyngdorfCommand(context, "!VOLDN\n", Callback(context))
        return true
    }

    fun volumeUp(context: Context): Boolean {
        if (getLyngdorfIp(context).isEmpty()) {
            return false
        }

        sendLyngdorfCommand(context, "!VOLUP\n", Callback(context))
        return true
    }

    private fun sendLyngdorfCommand(context: Context, command: String, callback: Callback) {
        executor.submit {
            try {
                Socket().use { socket ->
                    socket.connect(InetSocketAddress(lyngdorfIp, 84), 1000)
                    socket.soTimeout = 1000

                    BufferedWriter(OutputStreamWriter(socket.getOutputStream(), "UTF-8")).use { writer ->
                        writer.write(command)
                        writer.flush()
                    }
                }
                doCallback(true, callback)
            } catch (ex: IOException) {
                Log.e(LyngdorfHelper::class.java.name, "error writing to Lyngdorf", ex)
                doCallback(false, callback)
            }
        }
    }

    private fun doCallback(result: Boolean, callback: Callback) {
        handler.post {
            callback.call(result)
        }
    }

    private class Callback(private val context: Context) {

        fun call(success: Boolean) {
            if (!success) {
                Boast.makeText(context, "Error writing to Lyngdorf").show()
            }
        }
    }
}
