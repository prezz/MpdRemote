package net.prezz.mpr.mpd

import androidx.core.content.edit

import android.content.Context
import androidx.preference.PreferenceManager

class MpdPartitionStore(private val context: Context, mpdSettings: MpdSettings) : MpdPartitionProvider {

    private val lock = Any()
    private val preferenceKey: String = mpdSettings.getMpdHost() + mpdSettings.getMpdPort() + PREFERENCE_PARTITION_POSTFIX
    private var partition: String? = null

    fun putPartition(partition: String?) {
        synchronized(lock) {
            this.partition = null // force load in get method

            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
            sharedPreferences.edit(commit = true) {
                if (partition == null) {
                    remove(preferenceKey)
                } else {
                    putString(preferenceKey, partition)
                }
            }
        }
    }

    override fun getPartition(): String {
        synchronized(lock) {
            if (partition == null) {
                val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
                partition = sharedPreferences.getString(preferenceKey, MpdPartitionProvider.DEFAULT_PARTITION)
            }
        }

        return partition!!
    }

    override fun onInvalidPartition() {
        putPartition(null)
    }

    companion object {
        private const val PREFERENCE_PARTITION_POSTFIX = "_client_partition"
    }
}
