package net.prezz.mpr.model.command

import net.prezz.mpr.model.StoredPlaylistEntity

class MoveInStoredPlaylistCommand(val storedPlaylist: StoredPlaylistEntity, val from: Int, val to: Int) : Command
