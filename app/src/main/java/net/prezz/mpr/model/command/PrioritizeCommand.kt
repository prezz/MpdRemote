package net.prezz.mpr.model.command

import net.prezz.mpr.model.LibraryEntity

class PrioritizeCommand(val entities: Array<LibraryEntity>) : Command {

    constructor(entity: LibraryEntity) : this(arrayOf(entity))
}
