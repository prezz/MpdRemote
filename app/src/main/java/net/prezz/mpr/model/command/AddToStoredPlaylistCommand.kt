package net.prezz.mpr.model.command

import net.prezz.mpr.model.StoredPlaylistEntity
import net.prezz.mpr.model.LibraryEntity

class AddToStoredPlaylistCommand(val storedPlaylist: StoredPlaylistEntity, val entities: Array<LibraryEntity>) : Command {

    constructor(storedPlaylist: StoredPlaylistEntity, entity: LibraryEntity) : this(storedPlaylist, arrayOf(entity))
}
