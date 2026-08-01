package net.prezz.mpr.mpd.command

import net.prezz.mpr.model.LibraryEntity
import net.prezz.mpr.model.PlayerState
import net.prezz.mpr.model.ResponseResult
import net.prezz.mpr.model.UriEntity
import net.prezz.mpr.model.UriEntity.UriType
import net.prezz.mpr.model.command.AddToNewStoredPlaylistCommand
import net.prezz.mpr.model.command.AddToPlaylistCommand
import net.prezz.mpr.model.command.AddToStoredPlaylistCommand
import net.prezz.mpr.model.command.AddUriToNewStoredPlaylistCommand
import net.prezz.mpr.model.command.AddUriToPlaylistCommand
import net.prezz.mpr.model.command.AddUriToStoredPlaylistCommand
import net.prezz.mpr.model.command.ClearPlaylistCommand
import net.prezz.mpr.model.command.Command
import net.prezz.mpr.model.command.ConsumeCommand
import net.prezz.mpr.model.command.CreatePartitionCommand
import net.prezz.mpr.model.command.DeleteFromPlaylistCommand
import net.prezz.mpr.model.command.DeleteFromStoredPlaylistCommand
import net.prezz.mpr.model.command.DeleteMultipleFromPlaylistCommand
import net.prezz.mpr.model.command.DeletePartitionCommand
import net.prezz.mpr.model.command.DeleteStoredPlaylistCommand
import net.prezz.mpr.model.command.LoadStoredPlaylistCommand
import net.prezz.mpr.model.command.MoveInPlaylistCommand
import net.prezz.mpr.model.command.MoveInStoredPlaylistCommand
import net.prezz.mpr.model.command.MoveOutputToPartitionCommand
import net.prezz.mpr.model.command.NextCommand
import net.prezz.mpr.model.command.PauseCommand
import net.prezz.mpr.model.command.PlayCommand
import net.prezz.mpr.model.command.PlayPauseCommand
import net.prezz.mpr.model.command.PreviousCommand
import net.prezz.mpr.model.command.PrioritizeCommand
import net.prezz.mpr.model.command.PrioritizeUriCommand
import net.prezz.mpr.model.command.RandomCommand
import net.prezz.mpr.model.command.RepeatCommand
import net.prezz.mpr.model.command.SaveCurrentPlaylistCommand
import net.prezz.mpr.model.command.SeekCommand
import net.prezz.mpr.model.command.ShuffleCommand
import net.prezz.mpr.model.command.StopCommand
import net.prezz.mpr.model.command.ToggleOutputCommand
import net.prezz.mpr.model.command.UnprioritizeCommand
import net.prezz.mpr.model.command.UpdateLibraryCommand
import net.prezz.mpr.model.command.UpdatePrioritiesCommand
import net.prezz.mpr.model.command.VolumeDownCommand
import net.prezz.mpr.model.command.VolumeUpCommand
import net.prezz.mpr.mpd.MpdPartitionProvider
import net.prezz.mpr.mpd.connection.Filter
import net.prezz.mpr.mpd.connection.MpdConnection
import net.prezz.mpr.mpd.connection.RejectAllFilter
import java.io.IOException
import java.util.ArrayList
import java.util.TreeMap

class MpdSendControlCommands(commands: List<Command>, partitionProvider: MpdPartitionProvider) :
    MpdConnectionCommand<List<Command>, ResponseResult>(commands, partitionProvider) {

    @Throws(Exception::class)
    override fun doExecute(connection: MpdConnection, commands: List<Command>): ResponseResult {
        if (commands.isEmpty()) {
            return ResponseResult(true)
        }

        var volume: Int? = null
        var playerState: PlayerState? = null
        for (command in commands) {
            if (command is AddToNewStoredPlaylistCommand) {
                val playlistName = command.playlistName
                val entities = command.entities
                addToStoredPlaylist(connection, playlistName, entities)
            }
            if (command is AddToPlaylistCommand) {
                val entities = command.entities
                addToPlaylist(connection, entities)
            }
            if (command is AddToStoredPlaylistCommand) {
                val storedPlaylist = command.storedPlaylist
                val entities = command.entities
                addToStoredPlaylist(connection, storedPlaylist.playlistName, entities)
            }
            if (command is AddUriToNewStoredPlaylistCommand) {
                val playlistName = command.playlistName
                val entities = command.entities
                addToStoredPlaylist(connection, playlistName, entities)
            }
            if (command is AddUriToPlaylistCommand) {
                val entities = command.entities
                addToPlaylist(connection, entities)
            }
            if (command is AddUriToStoredPlaylistCommand) {
                val storedPlaylist = command.storedPlaylist
                val entities = command.entities
                addToStoredPlaylist(connection, storedPlaylist.playlistName, entities)
            }
            if (command is ClearPlaylistCommand) {
                connection.writeResponseCommand("clear\n", RejectAllFilter)
            }
            if (command is ConsumeCommand) {
                val consume = command.consume
                connection.writeResponseCommand("consume ${if (consume) "1" else "0"}\n", RejectAllFilter)
            }
            if (command is CreatePartitionCommand) {
                if (connection.isMinimumVersion(0, 22, 0)) {
                    val name = command.name
                    connection.writeResponseCommand("newpartition \"${MpdCommandHelper.escape(name)}\"\n", RejectAllFilter)
                }
            }
            if (command is DeleteFromPlaylistCommand) {
                val pos = command.pos
                connection.writeResponseCommand("delete $pos\n", RejectAllFilter)
            }
            if (command is DeleteFromStoredPlaylistCommand) {
                val name = command.storedPlaylist.playlistName
                val pos = command.pos
                connection.writeResponseCommand("playlistdelete \"${MpdCommandHelper.escape(name)}\" $pos\n", RejectAllFilter)
            }
            if (command is DeleteMultipleFromPlaylistCommand) {
                val commandList = ArrayList<String>()
                for (id in command.identifiers) {
                    commandList.add("deleteid $id\n")
                }
                connection.writeResponseCommandList(commandList.toTypedArray(), RejectAllFilter)
            }
            if (command is DeletePartitionCommand) {
                if (connection.isMinimumVersion(0, 22, 0)) {
                    val name = command.name
                    connection.writeResponseCommand("delpartition \"${MpdCommandHelper.escape(name)}\"\n", RejectAllFilter)
                }
            }
            if (command is DeleteStoredPlaylistCommand) {
                val entity = command.entity
                connection.writeResponseCommand("rm \"${MpdCommandHelper.escape(entity.playlistName)}\"\n", RejectAllFilter)
            }
            if (command is LoadStoredPlaylistCommand) {
                val entity = command.entity
                connection.writeResponseCommand("load \"${MpdCommandHelper.escape(entity.playlistName)}\"\n", RejectAllFilter)
            }
            if (command is MoveInPlaylistCommand) {
                val id = command.id
                val to = command.to
                connection.writeResponseCommand("moveid $id $to\n", RejectAllFilter)
            }
            if (command is MoveInStoredPlaylistCommand) {
                val name = command.storedPlaylist.playlistName
                val from = command.from
                val to = command.to
                connection.writeResponseCommand("playlistmove \"${MpdCommandHelper.escape(name)}\" $from $to\n", RejectAllFilter)
            }
            if (command is MoveOutputToPartitionCommand) {
                if (connection.isMinimumVersion(0, 22, 0)) {
                    val outputName = command.outputName
                    val partition = command.partition
                    try {
                        if (connection.setPartition(partition)) {
                            connection.writeResponseCommand("moveoutput \"${MpdCommandHelper.escape(outputName)}\"\n", RejectAllFilter)
                        } else {
                            throw IOException("Invalid partition.")
                        }
                    } finally {
                        connection.setPartition(super.getPartition())
                    }
                }
            }
            if (command is NextCommand) {
                connection.writeResponseCommand("next\n", RejectAllFilter)
            }
            if (command is PauseCommand) {
                val resume = command.resume
                connection.writeResponseCommand("pause ${if (resume) "0" else "1"}\n", RejectAllFilter)
            }
            if (command is PlayCommand) {
                val id = command.id
                if (id != null) {
                    connection.writeResponseCommand("playid $id\n", RejectAllFilter)
                } else {
                    connection.writeResponseCommand("play\n", RejectAllFilter)
                }
            }
            if (command is PlayPauseCommand) {
                val state = getState(connection)
                if ("play" == state) {
                    playerState = PlayerState.PAUSE
                    connection.writeResponseCommand("pause 1\n", RejectAllFilter)
                } else if ("stop" == state) {
                    playerState = PlayerState.PLAY
                    connection.writeResponseCommand("play\n", RejectAllFilter)
                } else if ("pause" == state) {
                    playerState = PlayerState.PLAY
                    connection.writeResponseCommand("pause 0\n", RejectAllFilter)
                }
            }
            if (command is PreviousCommand) {
                connection.writeResponseCommand("previous\n", RejectAllFilter)
            }
            if (command is PrioritizeCommand) {
                val entities = command.entities
                prioritize(connection, entities)
            }
            if (command is PrioritizeUriCommand) {
                val entities = command.entities
                prioritize(connection, entities)
            }
            if (command is RandomCommand) {
                val random = command.random
                connection.writeResponseCommand("random ${if (random) "1" else "0"}\n", RejectAllFilter)
            }
            if (command is RepeatCommand) {
                val repeat = command.repeat
                connection.writeResponseCommand("repeat ${if (repeat) "1" else "0"}\n", RejectAllFilter)
            }
            if (command is UpdateLibraryCommand) {
                connection.writeResponseCommand("update\n", RejectAllFilter)
            }
            if (command is SaveCurrentPlaylistCommand) {
                val name = command.name
                connection.writeResponseCommand("save \"${MpdCommandHelper.escape(name)}\"\n", RejectAllFilter)
            }
            if (command is SeekCommand) {
                val id = command.id
                val pos = command.position
                connection.writeResponseCommand("seekid $id $pos\n", RejectAllFilter)
            }
            if (command is ShuffleCommand) {
                connection.writeResponseCommand("shuffle\n", RejectAllFilter)
            }
            if (command is StopCommand) {
                connection.writeResponseCommand("stop\n", RejectAllFilter)
            }
            if (command is ToggleOutputCommand) {
                val outputId = command.outputId
                val enable = command.enabled
                val cmd = if (enable) "enableoutput %s\n" else "disableoutput %s\n"
                connection.writeResponseCommand(String.format(cmd, outputId), RejectAllFilter)
            }
            if (command is UnprioritizeCommand) {
                connection.writeResponseCommand("prio 0 ${command.from}:${command.to}\n")
            }
            if (command is UpdatePrioritiesCommand) {
                updatePriorities(connection)
            }
            if (command is VolumeDownCommand) {
                val amount = command.amount
                val currentVolume = getVolume(connection)
                if (currentVolume != null) {
                    if (currentVolume == -1) {
                        volume = currentVolume
                    } else {
                        volume = maxOf(0, currentVolume - amount)
                        if (currentVolume > 0) {
                            connection.writeResponseCommand("setvol $volume\n", RejectAllFilter)
                        }
                    }
                }
            }
            if (command is VolumeUpCommand) {
                val amount = command.amount
                val currentVolume = getVolume(connection)
                if (currentVolume != null) {
                    if (currentVolume == -1) {
                        volume = currentVolume
                    } else {
                        volume = minOf(100, currentVolume + amount)
                        if (currentVolume < 100) {
                            connection.writeResponseCommand("setvol $volume\n", RejectAllFilter)
                        }
                    }
                }
            }
        }

        val responseResult = ResponseResult(true)
        if (volume != null) {
            responseResult.putResponseValue(ResponseResult.ValueType.VOLUME, volume)
        }
        if (playerState != null) {
            responseResult.putResponseValue(ResponseResult.ValueType.PLAYER_STATE, playerState)
        }
        return responseResult
    }

    override fun onError(): ResponseResult = ResponseResult(false)

    @Throws(IOException::class)
    private fun getState(connection: MpdConnection): String? {
        val response = connection.writeResponseCommand("status\n")
        for (line in response) {
            if (line.startsWith("state: ")) {
                return line.substring(7)
            }
        }
        return null
    }

    @Throws(IOException::class)
    private fun getVolume(connection: MpdConnection): Int? {
        val response = connection.writeResponseCommand("status\n")
        for (line in response) {
            if (line.startsWith("volume: ")) {
                return Integer.decode(line.substring(8))
            }
        }
        return null
    }

    @Throws(IOException::class)
    private fun addToStoredPlaylist(connection: MpdConnection, playlistName: String?, entities: Array<LibraryEntity>) {
        val findCommands = ArrayList<String>()
        for (i in entities.indices) {
            findCommands.addAll(MpdCommandHelper.createQuery("find", entities[i]))
        }

        connection.writeCommandList(findCommands.toTypedArray())
        val files = connection.readResponse(FILE_FILTER)!!

        val commands = ArrayList<String>()
        for (file in files) {
            commands.add("playlistadd \"${MpdCommandHelper.escape(playlistName.orEmpty())}\" \"${MpdCommandHelper.escape(file.substring(6))}\"\n")
        }

        connection.writeResponseCommandList(commands.toTypedArray(), RejectAllFilter)
    }

    @Throws(IOException::class)
    private fun addToStoredPlaylist(connection: MpdConnection, playlistName: String?, entities: Array<UriEntity>) {
        val commands = ArrayList<String>()
        for (uri in entities) {
            commands.add("playlistadd \"${MpdCommandHelper.escape(playlistName.orEmpty())}\" \"${MpdCommandHelper.escape(uri.getFullUriPath(false))}\"\n")
        }

        connection.writeResponseCommandList(commands.toTypedArray(), RejectAllFilter)
    }

    @Throws(IOException::class)
    private fun addToPlaylist(connection: MpdConnection, entities: Array<LibraryEntity>) {
        if (connection.isMinimumVersion(0, 16, 0)) {
            val commands = ArrayList<String>()

            for (i in entities.indices) {
                commands.addAll(MpdCommandHelper.createQuery("findadd", entities[i]))
            }

            connection.writeResponseCommandList(commands.toTypedArray(), RejectAllFilter)
        } else {
            val findCommands = ArrayList<String>()
            for (i in entities.indices) {
                findCommands.addAll(MpdCommandHelper.createQuery("find", entities[i]))
            }

            connection.writeCommandList(findCommands.toTypedArray())
            val files = connection.readResponse(FILE_FILTER)!!

            val commands = ArrayList<String>()
            for (file in files) {
                commands.add("add \"${MpdCommandHelper.escape(file.substring(6))}\"\n")
            }

            connection.writeResponseCommandList(commands.toTypedArray(), RejectAllFilter)
        }
    }

    @Throws(IOException::class)
    private fun addToPlaylist(connection: MpdConnection, entities: Array<UriEntity>) {
        val commands = ArrayList<String>()
        for (uri in entities) {
            commands.add("add \"${MpdCommandHelper.escape(uri.getFullUriPath(false))}\"\n")
        }

        connection.writeResponseCommandList(commands.toTypedArray(), RejectAllFilter)
    }

    @Throws(IOException::class)
    private fun prioritize(connection: MpdConnection, entities: Array<LibraryEntity>) {
        val result = searchPlaylist(connection)
        val addPosition = result.destinationPosition

        val files = findFiles(connection, entities)
        val addedToPlaylist = addFilesToPlaylistWithId(connection, files, addPosition)

        setPriorities(connection, result, addedToPlaylist)
    }

    @Throws(IOException::class)
    private fun prioritize(connection: MpdConnection, entities: Array<UriEntity>) {
        val result = searchPlaylist(connection)
        val addPosition = result.destinationPosition

        val files = findFiles(connection, entities)
        val addedToPlaylist = addFilesToPlaylistWithId(connection, files, addPosition)

        setPriorities(connection, result, addedToPlaylist)
    }

    @Throws(IOException::class)
    private fun updatePriorities(connection: MpdConnection) {
        if (connection.isMinimumVersion(0, 17, 0)) {
            val result = searchPlaylist(connection)
            val commands = ArrayList<String>()
            reprioritize(result, commands)
            connection.writeResponseCommandList(commands.toTypedArray(), RejectAllFilter)
        }
    }

    @Throws(IOException::class)
    private fun searchPlaylist(connection: MpdConnection): SearchResult {
        var playing = -1

        val statusLines = connection.writeResponseCommand("status\n")
        for (line in statusLines) {
            if (line.startsWith("song: ")) {
                playing = line.substring(6).toInt()
            }
        }

        connection.writeCommand("playlistinfo\n")

        val unprioritizeMap = TreeMap<Int, Int>()
        val reprioritizeMap = TreeMap<Int, Int>()
        var position: Int? = null
        var id: Int? = null
        var prioritized = false
        var reprioritize = true
        while (true) {
            val current = connection.readLine() ?: break
            if (current.startsWith(MpdConnection.OK)) {
                break
            }
            if (current.startsWith(MpdConnection.ACK)) {
                throw IOException("Error reading MPD response: $current")
            }

            if (current.startsWith("file: ")) {
                if (prioritized) {
                    unprioritizeMap[position!!] = id!!
                }

                if (position != null) {
                    val pos = position
                    if (pos > playing && !prioritized) {
                        reprioritize = false
                    }
                    if (pos >= playing && prioritized && reprioritize) {
                        reprioritizeMap[position!!] = id!!
                    }
                }
                id = null
                position = null
                prioritized = false
            }
            if (current.startsWith("Id: ")) {
                id = Integer.decode(current.substring(4))
            }
            if (current.startsWith("Pos: ")) {
                position = Integer.decode(current.substring(5))
            }
            if (current.startsWith("Prio: ")) {
                prioritized = true
            }
        }
        if (prioritized) {
            unprioritizeMap[position!!] = id!!
        }
        if (position != null) {
            val pos = position
            if (pos > playing && !prioritized) {
                reprioritize = false
            }
            if (pos > playing && prioritized && reprioritize) {
                reprioritizeMap[position!!] = id!!
            }
        }

        var end = playing
        if (!reprioritizeMap.isEmpty()) {
            end = reprioritizeMap.lastKey()
        }

        val searchResult = SearchResult()
        searchResult.destinationPosition = end + 1
        searchResult.unprioritizeMap = unprioritizeMap
        searchResult.reprioritizeMap = reprioritizeMap
        return searchResult
    }

    @Throws(IOException::class)
    private fun findFiles(connection: MpdConnection, entities: Array<LibraryEntity>): Array<String> {
        val findCommands = ArrayList<String>()
        for (i in entities.indices) {
            findCommands.addAll(MpdCommandHelper.createQuery("find", entities[i]))
        }
        connection.writeCommandList(findCommands.toTypedArray())
        val files = connection.readResponse(FILE_FILTER)!!
        for (i in files.indices) {
            files[i] = files[i].substring(6)
        }
        return files
    }

    @Throws(IOException::class)
    private fun findFiles(connection: MpdConnection, entities: Array<UriEntity>): Array<String> {
        val result = ArrayList<String>()

        for (i in entities.indices) {
            if (entities[i].uriType == UriType.FILE) {
                result.add(entities[i].getFullUriPath(false))
            } else {
                val listCommand = "listall \"${MpdCommandHelper.escape(entities[i].getFullUriPath(false))}\"\n"
                val files = connection.writeResponseCommand(listCommand, FILE_FILTER)
                for (j in files.indices) {
                    result.add(files[j].substring(6))
                }
            }
        }

        return result.toTypedArray()
    }

    @Throws(IOException::class)
    private fun addFilesToPlaylistWithId(connection: MpdConnection, files: Array<String>, position: Int): List<Int> {
        var position = position
        val addCommands = arrayOfNulls<String>(files.size)
        for (i in files.indices) {
            addCommands[i] = "addid \"${MpdCommandHelper.escape(files[i])}\" ${position++}\n"
        }
        @Suppress("UNCHECKED_CAST")
        connection.writeCommandList(addCommands as Array<String>)
        val idLines = connection.readResponse(ID_FILTER)!!

        val result = ArrayList<Int>(idLines.size)
        for (line in idLines) {
            val id = line.substring(4)
            result.add(Integer.decode(id))
        }
        return result
    }

    @Throws(IOException::class)
    private fun setPriorities(connection: MpdConnection, result: SearchResult, addedToPlaylist: List<Int>) {
        if (connection.isMinimumVersion(0, 17, 0)) {
            val priorityCommands = ArrayList<String>()
            var priority = reprioritize(result, priorityCommands)
            for (id in addedToPlaylist) {
                if (priority > 0) {
                    priorityCommands.add("prioid ${priority--} $id\n")
                }
            }
            connection.writeResponseCommandList(priorityCommands.toTypedArray(), RejectAllFilter)
        }
    }

    @Throws(IOException::class)
    private fun reprioritize(searchResult: SearchResult, priorityCommands: MutableList<String>): Int {
        for (id in searchResult.unprioritizeMap!!.values) {
            priorityCommands.add("prioid 0 $id\n")
        }

        var priority = 255
        for (id in searchResult.reprioritizeMap!!.values) {
            priorityCommands.add("prioid ${priority--} $id\n")
        }
        return priority
    }

    private class SearchResult {
        var destinationPosition = 0
        var unprioritizeMap: TreeMap<Int, Int>? = null
        var reprioritizeMap: TreeMap<Int, Int>? = null
    }

    companion object {
        private val FILE_FILTER: Filter = object : Filter {
            override fun accepts(line: String): Boolean {
                return line.startsWith("file: ")
            }
        }

        private val ID_FILTER: Filter = object : Filter {
            override fun accepts(line: String): Boolean {
                return line.startsWith("Id: ")
            }
        }
    }
}
