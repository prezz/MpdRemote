package net.prezz.mpr.mpd.command

import android.util.Log
import kotlinx.coroutines.launch
import net.prezz.mpr.model.FutureTaskHandleImpl
import net.prezz.mpr.model.TaskHandle
import net.prezz.mpr.mpd.MpdPartitionProvider
import net.prezz.mpr.mpd.connection.MpdConnection
import java.util.concurrent.atomic.AtomicBoolean

abstract class MpdConnectionCommand<Param, Result>(
    private val param: Param,
    private val partitionProvider: MpdPartitionProvider,
) : MpdCommand() {

    interface MpdConnectionCommandReceiver<Result> {
        fun receive(result: Result)
    }

    fun execute(connection: MpdConnection, commandReceiver: MpdConnectionCommandReceiver<Result>): TaskHandle {
        val cancelled = AtomicBoolean(false)
        val job = scope.launch {
            try {
                synchronized(lock) {
                    try {
                        connection.connect()
                        if (!connection.setPartition(partitionProvider.getPartition())) {
                            partitionProvider.onInvalidPartition()
                        }

                        val result = doExecute(connection, param)
                        postResult(cancelled, result, commandReceiver)
                    } finally {
                        connection.disconnect()
                    }
                }
            } catch (ex: Exception) {
                Log.e(MpdConnectionCommand::class.java.name, "error executing command", ex)
                val result = onError()
                postResult(cancelled, result, commandReceiver)
            }
        }

        return FutureTaskHandleImpl(cancelled, job)
    }

    protected fun getPartition(): String = partitionProvider.getPartition()

    @Throws(Exception::class)
    protected abstract fun doExecute(connection: MpdConnection, param: Param): Result

    protected abstract fun onError(): Result

    private fun postResult(cancelled: AtomicBoolean, result: Result, commandReceiver: MpdConnectionCommandReceiver<Result>) {
        handler.post {
            if (!cancelled.get()) {
                commandReceiver.receive(result)
            }
        }
    }
}
