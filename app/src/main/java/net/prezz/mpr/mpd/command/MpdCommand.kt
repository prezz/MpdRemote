package net.prezz.mpr.mpd.command

import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executors

abstract class MpdCommand {

    companion object {
        // A single-thread dispatcher preserves V1's newSingleThreadExecutor() semantics:
        // MPD commands are executed one at a time, serialized on a dedicated daemon thread.
        private val commandDispatcher = Executors.newSingleThreadExecutor { r ->
            Executors.defaultThreadFactory().newThread(r).apply { isDaemon = true }
        }.asCoroutineDispatcher()

        internal val scope = CoroutineScope(commandDispatcher + SupervisorJob())

        // Results are posted back to the main thread, exactly as in V1.
        internal val handler = Handler(Looper.getMainLooper())

        internal val lock = Any()
    }
}
