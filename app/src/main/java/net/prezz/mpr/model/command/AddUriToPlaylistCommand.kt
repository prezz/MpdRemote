package net.prezz.mpr.model.command

import net.prezz.mpr.model.UriEntity

class AddUriToPlaylistCommand(val entities: Array<UriEntity>) : Command {

    constructor(entity: UriEntity) : this(arrayOf(entity))
}
