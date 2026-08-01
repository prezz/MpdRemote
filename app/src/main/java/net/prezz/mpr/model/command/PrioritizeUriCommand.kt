package net.prezz.mpr.model.command

import net.prezz.mpr.model.UriEntity

class PrioritizeUriCommand(val entities: Array<UriEntity>) : Command {

    constructor(entity: UriEntity) : this(arrayOf(entity))
}
