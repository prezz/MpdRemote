package net.prezz.mpr.model.command

import net.prezz.mpr.model.UriEntity

class AddUriToNewStoredPlaylistCommand(val playlistName: String?, val entities: Array<UriEntity>) : Command {

    constructor(playlistName: String?, entity: UriEntity) : this(playlistName, arrayOf(entity))
}
