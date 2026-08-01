package net.prezz.mpr.ui

import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.os.Bundle
import android.text.InputType
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.core.view.MenuProvider
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import net.prezz.mpr.R
import net.prezz.mpr.databinding.ActivityPlayDataBinding
import net.prezz.mpr.model.MusicPlayerControl
import net.prezz.mpr.model.ResponseReceiver
import net.prezz.mpr.ui.helpers.Boast
import net.prezz.mpr.ui.helpers.VolumeButtonsHelper
import net.prezz.mpr.ui.helpers.setupToolbar

class PlayDataActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlayDataBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityPlayDataBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupToolbar(showUpButton = true)

        addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.play_data, menu)
            }

            override fun onMenuItemSelected(item: MenuItem): Boolean {
                return when (item.itemId) {
                    R.id.play_data_action_clear -> {
                        clearPlayData()
                        true
                    }
                    else -> false
                }
            }
        }, this)

        binding.playDataExportButton.setOnClickListener { onExportClick(it) }
        binding.playDataImportButton.setOnClickListener { onImportClick(it) }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (VolumeButtonsHelper.handleKeyDown(this, keyCode, event)) {
            return true
        }

        return super.onKeyDown(keyCode, event)
    }

    private fun onImportClick(view: View) {
        MaterialAlertDialogBuilder(this).apply {
            setCancelable(true)
            setTitle(R.string.play_data_import_header)
            setMessage(R.string.play_data_import_message)
            setNegativeButton(android.R.string.cancel) { _, _ -> }
            setPositiveButton(android.R.string.ok) { _, _ ->
                MusicPlayerControl.importPlayData(binding.playDataCsvText.text.toString(), object : ResponseReceiver<Boolean>() {
                    override fun receiveResponse(response: Boolean) {
                        if (response) {
                            Boast.makeText(this@PlayDataActivity, R.string.play_data_import_success_toast).show()
                        } else {
                            Boast.makeText(this@PlayDataActivity, R.string.play_data_import_failed_toast).show()
                        }
                    }
                })
            }
        }.create().show()
    }

    private fun onExportClick(view: View) {
        val defaultLimit = 10000

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(40, 10, 40, 10)

        val pageText = EditText(this)
        pageText.setHint(R.string.play_data_export_page_hint)
        pageText.inputType = InputType.TYPE_CLASS_NUMBER
        layout.addView(pageText)

        val limitText = EditText(this)
        limitText.hint = getString(R.string.play_data_export_limit_hint, defaultLimit)
        limitText.inputType = InputType.TYPE_CLASS_NUMBER
        layout.addView(limitText)

        MaterialAlertDialogBuilder(this).apply {
            setTitle(R.string.play_data_export_button)
            setView(layout)
            setPositiveButton(android.R.string.ok) { _, _ ->
                val pageString = pageText.text.toString()
                val limitString = limitText.text.toString()
                val page = if (pageString.isBlank()) 0 else pageString.toInt()
                val limit = if (limitString.isBlank()) defaultLimit else limitString.toInt()
                val offset = page * limit

                MusicPlayerControl.exportPlayData(offset, limit, object : ResponseReceiver<String>() {
                    override fun receiveResponse(response: String) {
                        binding.playDataCsvText.setText(response)

                        val lines = response.lines().size.toLong() - 1
                        Boast.makeText(this@PlayDataActivity, getString(R.string.play_data_export_count_toast, lines)).show()
                    }
                })
            }
        }.create().show()
    }

    private fun clearPlayData() {
        MaterialAlertDialogBuilder(this).apply {
            setCancelable(true)
            setTitle(R.string.play_data_clear_header)
            setMessage(R.string.play_data_clear_message)
            setNegativeButton(android.R.string.cancel) { _, _ -> }
            setPositiveButton(android.R.string.ok) { _, _ ->
                MusicPlayerControl.clearPlayData(object : ResponseReceiver<Boolean>() {
                    override fun receiveResponse(response: Boolean) {
                        if (response) {
                            Boast.makeText(this@PlayDataActivity, R.string.play_data_clear_success_toast).show()
                        } else {
                            Boast.makeText(this@PlayDataActivity, R.string.play_data_clear_failed_toast).show()
                        }
                    }
                })
            }
        }.create().show()
    }
}
