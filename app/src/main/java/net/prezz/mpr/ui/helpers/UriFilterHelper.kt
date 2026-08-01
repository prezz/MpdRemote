package net.prezz.mpr.ui.helpers

import androidx.core.content.edit

import android.app.Activity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.content.Context
import net.prezz.mpr.R
import net.prezz.mpr.model.MusicPlayerControl
import net.prezz.mpr.model.ResponseReceiver
import net.prezz.mpr.model.TaskHandle
import net.prezz.mpr.model.servers.ServerConfigurationService
import androidx.preference.PreferenceManager
import java.util.SortedSet
import java.util.TreeSet

class UriFilterHelper(owner: Activity, private val uriFilterChangedListener: UriFilterChangedListener) {

    fun interface UriFilterChangedListener {
        fun entityFilterChanged()
    }

    private val activity: Activity = owner
    private var getAllUriFoldersHandle: TaskHandle = TaskHandle.NULL_HANDLE

    fun setUriFilter() {

        getAllUriFoldersHandle.cancelTask()
        getAllUriFoldersHandle = MusicPlayerControl.getHideableUriFolders(object : ResponseReceiver<Array<String>>() {
            override fun receiveResponse(items: Array<String>) {

                val visible = getUriFilter()
                items.sortWith(SortComparator())

                val checked = BooleanArray(items.size)
                for (i in items.indices) {
                    checked[i] = visible.contains(items[i])
                }

                MaterialAlertDialogBuilder(activity).apply {
                    setTitle(R.string.library_visibility_folder_header)
                    setMultiChoiceItems(items, checked) { _, which, isChecked ->
                        checked[which] = isChecked
                    }
                    setPositiveButton(android.R.string.ok) { _, _ ->
                        val values: SortedSet<String> = TreeSet()
                        for (i in items.indices) {
                            if (checked[i]) {
                                values.add(items[i])
                            }
                        }
                        saveUriFilter(values)
                        uriFilterChangedListener.entityFilterChanged()
                    }
                }.create().show()
            }
        })
    }

    fun getUriFilter(): SortedSet<String> {
        val host = ServerConfigurationService.getSelectedServerConfiguration().host

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity)
        return TreeSet(sharedPreferences.getStringSet(PREFERENCE_LIBRARY_VISIBLE_FOLDERS + host, emptySet()))
    }

    private fun saveUriFilter(values: SortedSet<String>) {
        val host = ServerConfigurationService.getSelectedServerConfiguration().host

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity)
        sharedPreferences.edit(commit = true) {
            putStringSet(PREFERENCE_LIBRARY_VISIBLE_FOLDERS + host, values)
        }
    }

    private class SortComparator : Comparator<String> {

        override fun compare(s1: String, s2: String): Int {
            return s1.compareTo(s2, ignoreCase = true)
        }
    }

    companion object {
        private const val PREFERENCE_LIBRARY_VISIBLE_FOLDERS = "library_visible_folders"

        @JvmStatic
        fun removeUriFilter(context: Context, host: String?) {
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
            sharedPreferences.edit(commit = true) {
                remove(PREFERENCE_LIBRARY_VISIBLE_FOLDERS + host)
            }
        }
    }
}
