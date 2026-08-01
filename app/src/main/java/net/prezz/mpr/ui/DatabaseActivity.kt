package net.prezz.mpr.ui

import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import net.prezz.mpr.R
import net.prezz.mpr.databinding.ActivityDatabaseBinding
import net.prezz.mpr.model.MusicPlayerControl
import net.prezz.mpr.model.ResponseReceiver
import net.prezz.mpr.model.Statistics
import net.prezz.mpr.model.command.UpdateLibraryCommand
import net.prezz.mpr.ui.helpers.Boast
import net.prezz.mpr.ui.helpers.VolumeButtonsHelper
import net.prezz.mpr.ui.helpers.setupToolbar

class DatabaseActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDatabaseBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityDatabaseBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupToolbar(showUpButton = true)

        binding.databaseButtonUpdate.setOnClickListener { onUpdateClick(it) }
        binding.databaseButtonRefresh.setOnClickListener { onRefreshClick(it) }
        binding.databaseButtonDownload.setOnClickListener { onDeleteClick(it) }

        refresh()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (VolumeButtonsHelper.handleKeyDown(this, keyCode, event)) {
            return true
        }

        return super.onKeyDown(keyCode, event)
    }

    private fun onUpdateClick(view: View) {
        MusicPlayerControl.sendControlCommand(UpdateLibraryCommand())

        binding.databaseTextRunning.text = getString(R.string.database_refresh_text_running)
    }

    private fun onRefreshClick(view: View) {
        refresh()
    }

    private fun onDeleteClick(view: View) {
        MusicPlayerControl.deleteLocalLibraryDatabase(object : ResponseReceiver<Boolean>() {
            override fun receiveResponse(response: Boolean) {
                if (response) {
                    Boast.makeText(this@DatabaseActivity, R.string.database_delete_toast).show()
                }
            }
        })
    }

    fun refresh() {
        MusicPlayerControl.getStatistics(object : ResponseReceiver<Statistics?>() {
            override fun receiveResponse(response: Statistics?) {
                if (response != null) {
                    binding.databaseTextStatistics.text = getString(R.string.database_refresh_text_statistics, response.getArtists() ?: 0, response.getAlbums() ?: 0, response.getSongs() ?: 0)

                    if (response.getUpdatingJob() != null) {
                        binding.databaseTextRunning.text = getString(R.string.database_refresh_text_running)
                    } else {
                        binding.databaseTextRunning.text = ""
                    }
                }
            }
        })
    }
}
