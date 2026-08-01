package net.prezz.mpr.mpd.command

import android.util.Log
import kotlinx.coroutines.launch
import net.prezz.mpr.model.FutureTaskHandleImpl
import net.prezz.mpr.model.TaskHandle
import net.prezz.mpr.mpd.connection.MpdConnection
import net.prezz.mpr.mpd.database.MpdDatabaseBuilder
import net.prezz.mpr.mpd.database.MpdLibraryDatabaseHelper
import java.util.concurrent.atomic.AtomicBoolean

abstract class MpdDatabaseCommand<Param, Result> : MpdCommand {

    interface MpdDatabaseCommandReceiver<Result> {
        fun build()
        fun receive(result: Result)
    }

    private val param: Param
    private val rebuild: Boolean

    constructor(param: Param) {
        this.param = param
        this.rebuild = true
    }

    constructor(param: Param, rebuild: Boolean) {
        this.param = param
        this.rebuild = rebuild
    }

    fun execute(
        databaseHelper: MpdLibraryDatabaseHelper,
        connection: MpdConnection,
        commandReceiver: MpdDatabaseCommandReceiver<Result>,
    ): TaskHandle {
        val cancelled = AtomicBoolean(false)
        val job = scope.launch {
            try {
                synchronized(lock) {
                    if (rebuild && databaseHelper.getRowCount() == 0) {
                        postBuild(cancelled, commandReceiver)
                        try {
                            connection.connect()
                            MpdDatabaseBuilder.buildDatabase(connection, databaseHelper)
                        } finally {
                            connection.disconnect()
                        }
                    }
                }

                val result = doExecute(databaseHelper, param)
                postResult(cancelled, result, commandReceiver)
            } catch (ex: Exception) {
                Log.e(MpdDatabaseCommand::class.java.name, "error executing command", ex)
                val result = onError()
                postResult(cancelled, result, commandReceiver)
            } finally {
                databaseHelper.close()
            }
        }

        return FutureTaskHandleImpl(cancelled, job)
    }

    @Throws(Exception::class)
    protected abstract fun doExecute(databaseHelper: MpdLibraryDatabaseHelper, param: Param): Result

    protected abstract fun onError(): Result

    private fun postBuild(cancelled: AtomicBoolean, commandReceiver: MpdDatabaseCommandReceiver<Result>) {
        handler.post {
            if (!cancelled.get()) {
                commandReceiver.build()
            }
        }
    }

    private fun postResult(cancelled: AtomicBoolean, result: Result, commandReceiver: MpdDatabaseCommandReceiver<Result>) {
        handler.post {
            if (!cancelled.get()) {
                commandReceiver.receive(result)
            }
        }
    }
}
