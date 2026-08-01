package net.prezz.mpr.model

interface TaskHandle {

    fun cancelTask()

    companion object {
        @JvmField
        val NULL_HANDLE: TaskHandle = object : TaskHandle {
            override fun cancelTask() {
            }
        }
    }
}
