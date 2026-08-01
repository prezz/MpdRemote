package net.prezz.mpr.ui.helpers

import android.app.Activity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import net.prezz.mpr.R
import net.prezz.mpr.model.MusicPlayerControl
import net.prezz.mpr.model.PlaylistEntity
import net.prezz.mpr.model.ResponseReceiver
import net.prezz.mpr.ui.adapter.PlaylistAdapterEntity

object UpdatePlayDataHelper {

    fun updatePlayData(activity: Activity, adapterEntities: Array<PlaylistAdapterEntity>) {
        val playlistEntity = Array(adapterEntities.size) { adapterEntities[it].getEntity() }
        updatePlayData(activity, playlistEntity)
    }

    fun updatePlayData(activity: Activity, playlistEntities: Array<PlaylistEntity>?) {

        MaterialAlertDialogBuilder(activity).apply {
            setCancelable(true)
            setTitle(R.string.player_mark_played_header)
            setMessage(R.string.player_mark_played_message)
            setNegativeButton(android.R.string.cancel) { _, _ -> }
            setPositiveButton(android.R.string.ok) { _, _ ->
                if (playlistEntities != null && playlistEntities.isNotEmpty()) {
                    val entities = playlistEntities.toList()
                    MusicPlayerControl.updatePlayData(entities, object : ResponseReceiver<Boolean>() {
                        override fun receiveResponse(response: Boolean) {
                            if (response) {
                                Boast.makeText(activity, R.string.player_play_data_updated).show()
                            }
                        }
                    })
                }
            }
        }.create().show()
    }
}
