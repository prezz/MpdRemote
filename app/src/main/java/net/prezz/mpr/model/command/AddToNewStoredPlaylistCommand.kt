package net.prezz.mpr.model.command

import net.prezz.mpr.model.LibraryEntity

class AddToNewStoredPlaylistCommand(val playlistName: String?, val entities: Array<LibraryEntity>) : Command {

    constructor(playlistName: String?, entity: LibraryEntity) : this(playlistName, arrayOf(entity))
}
