package net.prezz.mpr.ui.player

import android.view.View
import net.prezz.mpr.model.PlayerStatus
import net.prezz.mpr.model.PlaylistEntity

interface PlayerFragment {

    fun statusUpdated(status: PlayerStatus)

    fun playlistUpdated(playlistEntities: Array<PlaylistEntity>)

    fun onChoiceMenuClick(view: View)

    fun forceRefresh()
}
