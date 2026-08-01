package net.prezz.mpr.model.command

import net.prezz.mpr.model.StoredPlaylistEntity
import net.prezz.mpr.model.UriEntity

class AddUriToStoredPlaylistCommand(val storedPlaylist: StoredPlaylistEntity, val entities: Array<UriEntity>) : Command {

    constructor(storedPlaylist: StoredPlaylistEntity, entity: UriEntity) : this(storedPlaylist, arrayOf(entity))
}
