package net.prezz.mpr.ui

import android.app.Activity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import net.prezz.mpr.R
import net.prezz.mpr.ui.helpers.LyngdorfHelper
import net.prezz.mpr.ui.helpers.VolumeButtonsHelper
import net.prezz.mpr.ui.helpers.setupToolbar

class AboutActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_about)
        setupToolbar()

        findViewById<View>(R.id.about_lastfm_image).setOnClickListener { onLastfmClick(it) }
        findViewById<View>(R.id.about_source_code_text).setOnClickListener { onSourceCodeClick(it) }
        findViewById<View>(R.id.about_hidden_lyngdorf_view).setOnClickListener { onHiddenLyngdorfClick(it) }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (VolumeButtonsHelper.handleKeyDown(this, keyCode, event)) {
            return true
        }

        return super.onKeyDown(keyCode, event)
    }

    private fun onLastfmClick(view: View) {
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("http://www.last.fm"))
        startActivity(browserIntent)
    }

    private fun onSourceCodeClick(view: View) {
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/prezz/MusicPlayerRemote"))
        startActivity(browserIntent)
    }

    private fun onHiddenLyngdorfClick(view: View) {
        showHiddenLyngdorfInput(this, "Secret Lyngdorf setting", LyngdorfHelper.getLyngdorfIp(this))
    }

    private fun showHiddenLyngdorfInput(activity: Activity, titleText: String, value: String) {
        val editTextView = EditText(activity)
        editTextView.setSingleLine()
        editTextView.setText(value)

        val dialog = MaterialAlertDialogBuilder(activity).apply {
            setTitle(titleText)
            setView(editTextView)
            setPositiveButton(android.R.string.ok) { _, _ ->
                val inputText = editTextView.text.toString()
                LyngdorfHelper.setLyngdorfIp(this@AboutActivity, inputText)
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
}
