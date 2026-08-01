package net.prezz.mpr.model

import kotlinx.coroutines.Job
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicBoolean

class FutureTaskHandleImpl : TaskHandle {

    private val cancelled: AtomicBoolean?
    private val job: Job?
    private val future: Future<*>?

    // MPD command variant. Cancelling a blocking socket read is not possible (the connection is
    // shared and serialized across commands, so closing it mid-flight would corrupt the next
    // command), and the read timeouts already bound the worst case. Instead the shared `cancelled`
    // flag is checked at result-delivery time so a cancelled command never calls back into a
    // stopped/destroyed activity, even if it was already mid-flight when cancelled.
    constructor(cancelled: AtomicBoolean, job: Job) {
        this.cancelled = cancelled
        this.job = job
        this.future = null
    }

    constructor(future: Future<*>) {
        this.cancelled = null
        this.future = future
        this.job = null
    }

    override fun cancelTask() {
        cancelled?.set(true)
        job?.cancel()
        future?.cancel(false)
    }
}
