package net.prezz.mpr.mpd

interface MpdPartitionProvider {

    fun getPartition(): String

    fun onInvalidPartition()

    companion object {
        const val DEFAULT_PARTITION = "default"
    }
}
