package net.prezz.mpr.mpd.command

import net.prezz.mpr.model.AudioOutput
import net.prezz.mpr.mpd.MpdPartitionProvider
import net.prezz.mpr.mpd.connection.MpdConnection
import java.io.IOException
import java.util.ArrayList

class MpdGetOutputsCommand(partitionProvider: MpdPartitionProvider) :
    MpdConnectionCommand<Void?, Array<AudioOutput>>(null, partitionProvider) {

    @Throws(Exception::class)
    override fun doExecute(connection: MpdConnection, param: Void?): Array<AudioOutput> {
        return getOutputs(connection)
    }

    override fun onError(): Array<AudioOutput> = arrayOf()

    companion object {
        @Throws(Exception::class)
        fun getOutputs(connection: MpdConnection): Array<AudioOutput> {
            val lines = connection.writeResponseCommand("outputs\n")

            val result = ArrayList<AudioOutput>()

            var add = false
            var outputId: String? = null
            var outputName: String? = null
            var plugin = ""
            var outputEnabled: Boolean? = null
            for (line in lines) {
                if (line.startsWith(MpdConnection.OK)) {
                    break
                }
                if (line.startsWith(MpdConnection.ACK)) {
                    throw IOException("Error reading MPD response: $line")
                }

                if (line.startsWith("outputid: ")) {
                    if (add) {
                        result.add(AudioOutput(outputId ?: "", outputName ?: "", plugin, outputEnabled ?: false))
                    }
                    add = false
                    outputId = line.substring(10)
                    outputName = null
                    plugin = ""
                    outputEnabled = null
                }

                if (line.startsWith("outputname: ")) {
                    outputName = line.substring(12)
                }

                if (line.startsWith("plugin: ")) {
                    plugin = line.substring(8)
                }

                if (line.startsWith("outputenabled: ")) {
                    val s = line.substring(15)
                    outputEnabled = "1" == s
                    add = true
                }
            }
            if (add) {
                result.add(AudioOutput(outputId ?: "", outputName ?: "", plugin, outputEnabled ?: false))
            }

            return result.toTypedArray()
        }
    }
}
