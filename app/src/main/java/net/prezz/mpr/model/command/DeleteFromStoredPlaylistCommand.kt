package net.prezz.mpr.model.command

import net.prezz.mpr.model.StoredPlaylistEntity

class DeleteFromStoredPlaylistCommand(val storedPlaylist: StoredPlaylistEntity, val pos: Int) : Command
