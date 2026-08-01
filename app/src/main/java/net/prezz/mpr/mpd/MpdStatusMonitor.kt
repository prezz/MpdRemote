package net.prezz.mpr.mpd

import kotlin.math.roundToInt

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import net.prezz.mpr.Utils
import net.prezz.mpr.model.AudioOutput
import net.prezz.mpr.model.PlayerState
import net.prezz.mpr.model.PlayerStatus
import net.prezz.mpr.model.StatusListener
import net.prezz.mpr.mpd.connection.MpdConnection
import net.prezz.mpr.mpd.connection.RejectAllFilter
import net.prezz.mpr.service.PlaybackService
import java.util.ArrayList
import java.util.concurrent.Executors

/**
 * Port of V1's Handler-based status monitor to coroutines.
 *
 * The blocking `idle` polling loop runs on a background (cached) coroutine dispatcher; status
 * updates are dispatched back to the main thread (replacing the V1 Handler) where the registered
 * [StatusListener] is notified. Statuses are additionally exposed through [statusFlow].
 */
class MpdStatusMonitor(private val settings: MpdSettings) {

    private val backgroundDispatcher = Executors.newCachedThreadPool { r ->
        Executors.defaultThreadFactory().newThread(r).apply { isDaemon = true }
    }.asCoroutineDispatcher()
    private val backgroundScope = CoroutineScope(backgroundDispatcher + SupervisorJob())
    private val mainScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val statusSink = MutableSharedFlow<PlayerStatus>(replay = 1, extraBufferCapacity = 16)
    val statusFlow: Flow<PlayerStatus> = statusSink.asSharedFlow()

    private var monitor: Monitor? = null
    private var statusListener: StatusListener? = null
    private var partitionProvider: MpdPartitionProvider? = null

    fun switchPartition(partitionProvider: MpdPartitionProvider) {
        setStatusListener(statusListener, partitionProvider)
    }

    fun setStatusListener(listener: StatusListener?, partitionProvider: MpdPartitionProvider?) {
        monitor?.abort()
        monitor = null

        this.statusListener = listener
        this.partitionProvider = partitionProvider

        if (statusListener != null && partitionProvider != null) {
            val m = Monitor(settings, partitionProvider.getPartition())
            monitor = m
            backgroundScope.launch { m.run() }
        }
    }

    // Replaces V1 handleMessage(INVALID_PARTITION_EVENT): runs on the main thread.
    private fun dispatchInvalidPartitionEvent() {
        mainScope.launch {
            val provider = partitionProvider
            provider?.onInvalidPartition()

            if (statusListener != null && provider != null) {
                val m = Monitor(settings, provider.getPartition())
                monitor = m
                backgroundScope.launch { m.run() }
            }
        }
    }

    private inner class Monitor(settings: MpdSettings, private val partition: String) {

        private val lock = Any()

        private val connection: MpdConnection = MpdConnection(settings, 60000 * 5)
        @Volatile
        private var running: Boolean = true
        private var connected: Boolean = false
        private var errorCount: Int = 0
        private var retryDelayMillis: Long = INITIAL_RETRY_DELAY_MILLIS
        private val connectionHash: Int = Utils.shortHashCode(settings.getMpdHost(), settings.getMpdPort(), partition)

        // Per-monitor scope for delivering status updates on the main thread. Cancelling it on
        // abort() is the coroutine equivalent of V1's Handler.removeMessages(STATUS_EVENT): it
        // discards any status that was queued for the main thread but not yet delivered, so a
        // stale status can never reach a listener that has since been replaced. It is a child of
        // mainScope so it is also torn down if the whole monitor is disposed.
        private val statusDispatchScope =
            CoroutineScope(Dispatchers.Main + SupervisorJob(mainScope.coroutineContext[Job]))

        fun abort() {
            synchronized(lock) {
                running = false
                try {
                    backgroundScope.launch { Abort(connection, partition).run() }
                } catch (ex: Exception) {
                    Log.e(MpdStatusMonitor::class.java.name, "error sending noidle command", ex)
                }

                // Equivalent to V1's removeMessages(STATUS_EVENT). Performed under `lock` so it is
                // serialized against dispatchStatus (which enqueues under the same lock): a status
                // is therefore either fully delivered before abort or discarded, never left pending.
                statusDispatchScope.cancel()
            }
        }

        fun run() {
            Log.d(Monitor::class.java.name, "starting monitor thread")

            var status = getPlayerStatus(true)
            if (status == null && running) {
                status = PlayerStatus(false)
            }
            dispatchStatus(status)

            while (running) {
                try {
                    if (!connected) {
                        status = getPlayerStatus(false)
                        dispatchStatus(status)
                    }

                    connection.connect()
                    if (!connection.setPartition(partition)) {
                        dispatchInvalidPartition()
                        running = false
                    }
                    synchronized(lock) {
                        if (!running) {
                            break
                        }
                        connection.writeCommand("idle playlist options player mixer output\n")
                    }

                    connection.readResponse(RejectAllFilter)

                    if (running) {
                        status = getPlayerStatus(false)
                        dispatchStatus(status)
                    }
                } catch (ex: Exception) {
                    Log.e(MpdStatusMonitor::class.java.name, "error in monitor thread", ex)

                    if (running) {
                        connection.disconnect()
                        if (connected && ++errorCount > MAX_ERROR_COUNT) {
                            connected = false
                            dispatchStatus(PlayerStatus(false))
                        }
                        sleepBeforeRetry()
                    }
                }
            }

            connection.disconnect()
            Log.d(Monitor::class.java.name, "exiting monitor thread")
        }

        private fun getPlayerStatus(returnOnError: Boolean): PlayerStatus? {
            while (running) {
                try {
                    connection.connect()
                    if (!connection.setPartition(partition)) {
                        return PlayerStatus(false)
                    }
                    val status = PlayerStatus(true)

                    val statusLines = connection.writeResponseCommand("status\n")
                    for (line in statusLines) {
                        if (line.startsWith("consume: ")) {
                            val s = line.substring(9)
                            status.consume = "1" == s
                        }
                        if (line.startsWith("playlist: ")) {
                            val s = line.substring(10)
                            status.playlistVersion = connectionHash + s.toInt()
                        }
                        if (line.startsWith("random: ")) {
                            val s = line.substring(8)
                            status.random = "1" == s
                        }
                        if (line.startsWith("repeat: ")) {
                            val s = line.substring(8)
                            status.repeat = "1" == s
                        }
                        if (line.startsWith("song: ")) {
                            val s = line.substring(6)
                            status.currentSong = s.toInt()
                        }
                        if (line.startsWith("nextsong: ")) {
                            val s = line.substring(10)
                            status.nextSong = s.toInt()
                        }
                        if (line.startsWith("state: ")) {
                            val s = line.substring(7)
                            if ("play" == s) {
                                status.state = PlayerState.PLAY
                            }
                            if ("stop" == s) {
                                status.state = PlayerState.STOP
                            }
                            if ("pause" == s) {
                                status.state = PlayerState.PAUSE
                            }
                        }
                        if (line.startsWith("elapsed: ") && connection.isMinimumVersion(0, 22, 0)) {
                            val s = line.substring(9)
                            status.elapsedTime = s.toFloat().roundToInt()
                        }
                        if (line.startsWith("duration: ") && connection.isMinimumVersion(0, 22, 0)) {
                            val s = line.substring(10)
                            status.totalTime = s.toFloat().roundToInt()
                        }
                        if (line.startsWith("time: ") && !connection.isMinimumVersion(0, 22, 0)) { // deprecated
                            val s = line.substring(6)
                            val split = s.split(":").toTypedArray()
                            status.elapsedTime = split[0].toInt()
                            status.totalTime = split[1].toInt()
                        }
                        if (line.startsWith("volume: ")) {
                            val s = line.substring(8)
                            status.volume = s.toInt()
                        }
                        if (line.startsWith("partition: ")) {
                            val s = line.substring(11)
                            status.partition = s
                        }
                    }

                    val outputLines = connection.writeResponseCommand("outputs\n")
                    val audioOutputs = ArrayList<AudioOutput>()
                    var outputId: String? = null
                    var outputName: String? = null
                    var plugin: String? = null
                    var outputEnabled: Boolean? = null
                    for (line in outputLines) {
                        if (line.startsWith("outputid: ")) {
                            outputId = line.substring(10)
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
                        }

                        if (outputId != null && outputName != null && plugin != null && outputEnabled != null) {
                            if (outputEnabled) {
                                audioOutputs.add(AudioOutput(outputId, outputName, plugin, outputEnabled))
                            }
                            outputId = null
                            outputName = null
                            plugin = null
                            outputEnabled = null
                        }
                    }
                    status.audioOutputs = audioOutputs.toTypedArray()

                    connected = true
                    errorCount = 0
                    retryDelayMillis = INITIAL_RETRY_DELAY_MILLIS
                    return status
                } catch (ex: Exception) {
                    Log.e(MpdStatusMonitor::class.java.name, "Error in monitor thread. Failed getting initial player status", ex)

                    if (returnOnError) {
                        connection.disconnect()
                        break
                    }
                    if (running) {
                        connection.disconnect()
                        if (connected && ++errorCount > MAX_ERROR_COUNT) {
                            connected = false
                            return PlayerStatus(false)
                        }
                        sleepBeforeRetry()
                    }
                }
            }

            return null
        }

        /**
         * Sleeps between reconnect attempts using exponential backoff (capped at
         * [MAX_RETRY_DELAY_MILLIS]) so that a persistently unreachable server no longer causes a
         * once-per-second reconnect storm. An interrupt is treated as a stop signal: the interrupt
         * flag is restored and the monitor loop is asked to exit cleanly rather than crashing the
         * background coroutine with a RuntimeException.
         */
        private fun sleepBeforeRetry() {
            val delay = retryDelayMillis
            retryDelayMillis = minOf(retryDelayMillis * 2, MAX_RETRY_DELAY_MILLIS)
            try {
                Thread.sleep(delay)
            } catch (ex: InterruptedException) {
                Thread.currentThread().interrupt()
                running = false
            }
        }

        // Replaces V1 sendMessage(STATUS_EVENT) + handleMessage(STATUS_EVENT). The launch happens
        // under `lock` while running == true (mirroring V1's guarded sendMessage); abort() cancels
        // statusDispatchScope under the same lock, so an enqueued status is never delivered late.
        private fun dispatchStatus(status: PlayerStatus?) {
            synchronized(lock) {
                if (status != null && running) {
                    statusDispatchScope.launch {
                        statusSink.tryEmit(status)
                        val listener = statusListener
                        if (listener != null) {
                            listener.statusUpdated(status)
                            if (status.connected && status.state != PlayerState.STOP) {
                                PlaybackService.start()
                            }
                        }
                    }
                }
            }
        }

        private fun dispatchInvalidPartition() {
            synchronized(lock) {
                if (running) {
                    dispatchInvalidPartitionEvent()
                }
            }
        }
    }

    private class Abort(private val connection: MpdConnection, private val partition: String) {

        fun run() {
            try {
                connection.connect()
                //connection.setPartition(partition);
                connection.writeCommand("noidle\n")
            } catch (ex: Exception) {
                Log.e(MpdStatusMonitor::class.java.name, "error sending noidle command", ex)
            }
        }
    }

    companion object {
        private const val MAX_ERROR_COUNT = 10
        private const val INITIAL_RETRY_DELAY_MILLIS = 1000L
        private const val MAX_RETRY_DELAY_MILLIS = 30000L
    }
}
