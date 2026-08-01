package net.prezz.mpr.model

import java.io.Serializable

class PlayerStatus(
    val connected: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    var playlistVersion: Int = -1,
    var consume: Boolean = false,
    var random: Boolean = false,
    var repeat: Boolean = false,
    var state: PlayerState = PlayerState.STOP,
    var currentSong: Int = -1,
    var nextSong: Int = -1,
    var volume: Int = 0,
    var elapsedTime: Int = 0,
    var totalTime: Int = 0,
    var partition: String = "",
    var audioOutputs: Array<AudioOutput> = arrayOf()
) : Serializable {

    companion object {
        private const val serialVersionUID = -8096344131114099250L
    }
}
