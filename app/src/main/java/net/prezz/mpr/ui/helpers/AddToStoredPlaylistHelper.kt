package net.prezz.mpr.ui.helpers

import android.app.Activity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.view.WindowManager
import android.widget.EditText
import net.prezz.mpr.R
import net.prezz.mpr.model.LibraryEntity
import net.prezz.mpr.model.MusicPlayerControl
import net.prezz.mpr.model.ResponseReceiver
import net.prezz.mpr.model.ResponseResult
import net.prezz.mpr.model.StoredPlaylistEntity
import net.prezz.mpr.model.UriEntity
import net.prezz.mpr.model.command.AddToNewStoredPlaylistCommand
import net.prezz.mpr.model.command.AddToStoredPlaylistCommand
import net.prezz.mpr.model.command.AddUriToNewStoredPlaylistCommand
import net.prezz.mpr.model.command.AddUriToStoredPlaylistCommand
import net.prezz.mpr.model.command.Command

object AddToStoredPlaylistHelper {

    fun addToStoredPlaylist(activity: Activity, displayText: String, entity: LibraryEntity) {
        addToStoredPlaylist(activity, displayText, LibraryEntityCommandFactory(entity))
    }

    fun addUriToStoredPlaylist(activity: Activity, displayText: String, entity: UriEntity) {
        addToStoredPlaylist(activity, displayText, UriEntityCommandFactory(entity))
    }

    private fun addToStoredPlaylist(activity: Activity, displayText: String, commandFactory: CommandFactory) {
        MusicPlayerControl.getStoredPlaylists(object : ResponseReceiver<Array<StoredPlaylistEntity>>() {
            override fun receiveResponse(response: Array<StoredPlaylistEntity>) {
                // The response is delivered asynchronously; bail out if the activity is gone
                // to avoid a BadTokenException when showing the dialog on a dead window.
                if (activity.isFinishing || activity.isDestroyed) {
                    return
                }

                val items = arrayOfNulls<String>(response.size + 1)
                items[0] = activity.getString(R.string.library_new_stored_playlist)
                for (i in response.indices) {
                    items[i + 1] = response[i].playlistName
                }

                MaterialAlertDialogBuilder(activity).apply {
                    setTitle(R.string.library_add_to_stored_playlist)
                    setItems(items) { _, which ->
                        if (which == 0) {
                            addToNewStoredPlaylist(activity, displayText, commandFactory)
                        } else {
                            MusicPlayerControl.sendControlCommand(commandFactory.createAddToCommand(response[which - 1]))
                            Boast.makeText(activity, displayText).show()
                        }
                    }
                }.create().show()
            }
        })
    }

    private fun addToNewStoredPlaylist(activity: Activity, displayText: String, commandFactory: CommandFactory) {
        val editTextView = EditText(activity)
        editTextView.setSingleLine()

        val dialog = MaterialAlertDialogBuilder(activity).apply {
            setTitle(R.string.library_new_stored_playlist)
            setView(editTextView)
            setPositiveButton(android.R.string.ok) { _, _ ->
                val saveName = editTextView.text.toString()
                if (saveName.isNotEmpty()) {
                    MusicPlayerControl.sendControlCommand(commandFactory.createAddToNewCommand(saveName), object : ResponseReceiver<ResponseResult>() {
                        override fun receiveResponse(response: ResponseResult) {
                            if (!response.isSuccess) {
                                Boast.makeText(activity, R.string.library_new_stores_playlist_server_error).show()
                            }
                        }
                    })
                    Boast.makeText(activity, displayText).show()
                }
            }
            setNegativeButton(android.R.string.cancel) { _, _ -> }
        }.create()
        editTextView.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
            }
        }
        dialog.show()
    }

    private interface CommandFactory {
        fun createAddToCommand(storedPlaylist: StoredPlaylistEntity): Command
        fun createAddToNewCommand(playlistName: String): Command
    }

    private class LibraryEntityCommandFactory(private val entity: LibraryEntity) : CommandFactory {

        override fun createAddToCommand(storedPlaylist: StoredPlaylistEntity): Command {
            return AddToStoredPlaylistCommand(storedPlaylist, entity)
        }

        override fun createAddToNewCommand(playlistName: String): Command {
            return AddToNewStoredPlaylistCommand(playlistName, entity)
        }
    }

    private class UriEntityCommandFactory(private val entity: UriEntity) : CommandFactory {

        override fun createAddToCommand(storedPlaylist: StoredPlaylistEntity): Command {
            return AddUriToStoredPlaylistCommand(storedPlaylist, entity)
        }

        override fun createAddToNewCommand(playlistName: String): Command {
            return AddUriToNewStoredPlaylistCommand(playlistName, entity)
        }
    }
}
