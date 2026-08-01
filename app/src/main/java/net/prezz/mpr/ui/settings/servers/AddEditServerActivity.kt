package net.prezz.mpr.ui.settings.servers

import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import android.widget.TextView.OnEditorActionListener
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.materialswitch.MaterialSwitch
import net.prezz.mpr.R
import net.prezz.mpr.databinding.ActivityAddEditServerBinding
import net.prezz.mpr.model.servers.ServerConfiguration
import net.prezz.mpr.model.servers.ServerConfigurationService
import net.prezz.mpr.ui.helpers.VolumeButtonsHelper
import net.prezz.mpr.ui.helpers.setupToolbar

class AddEditServerActivity : AppCompatActivity(), OnEditorActionListener {

    private lateinit var binding: ActivityAddEditServerBinding
    private var editConfiguration: ServerConfiguration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAddEditServerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupToolbar()

        val config = intent.extras?.getSerializable(CONFIGURATION_ARGUMENT_KEY, ServerConfiguration::class.java)
        editConfiguration = config
        if (config != null) {
            setTitle(R.string.add_edit_server_edit_title)
            binding.addEditServerNameText.setText(config.name)
            binding.addEditServerHostText.setText(config.host)
            binding.addEditServerPortText.setText(config.port)
            binding.addEditServerPasswordText.setText(config.password)
            setStreamingViewUrl(config.streaming)
        } else {
            setTitle(R.string.add_edit_server_add_title)
            binding.addEditServerPortText.setText("6600")
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)

        binding.addEditServerPasswordText.setOnEditorActionListener(this)

        binding.addEditServerStreamingUrlSwitch.setOnClickListener { onStreamingClick(it) }
        binding.addEditServerSaveButton.setOnClickListener { onSaveClick(it) }

        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
    }

    override fun onEditorAction(v: TextView?, actionId: Int, event: KeyEvent?): Boolean {
        if (actionId == EditorInfo.IME_ACTION_DONE) {
            if (addServer()) {
                setResult(RESULT_OK)
                finish()
            }
            return true
        }

        return false
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (VolumeButtonsHelper.handleKeyDown(this, keyCode, event)) {
            return true
        }

        return super.onKeyDown(keyCode, event)
    }

    private fun onStreamingClick(view: View) {
        val streamingSwitch = view as MaterialSwitch
        if (streamingSwitch.isChecked) {
            val host = binding.addEditServerHostText.text.toString()
            val port = binding.addEditServerPortText.text.toString()

            val sb = StringBuilder()
            if (!host.startsWith("http")) {
                sb.append("http://")
            }
            sb.append(host)

            try {
                val portNumber = port.toInt()
                val delta = portNumber - 6600
                sb.append(":" + (8000 + delta))
            } catch (ex: NumberFormatException) {
                sb.append(":8000")
            }

            setStreamingViewUrl(sb.toString())
        } else {
            setStreamingViewUrl("")
        }
    }

    private fun onSaveClick(view: View) {
        if (addServer()) {
            setResult(RESULT_OK)
            finish()
        }
    }

    private fun addServer(): Boolean {
        if (!isFinishing) {
            var name = binding.addEditServerNameText.text.toString()
            val host = binding.addEditServerHostText.text.toString()
            val port = binding.addEditServerPortText.text.toString()
            val password = binding.addEditServerPasswordText.text.toString()
            val streaming = binding.addEditServerStreamingUrlText.text.toString()

            if ("" == name) {
                name = host
            }

            if (host.isNotEmpty() && port.isNotEmpty()) {
                val config = editConfiguration
                if (config != null) {
                    val id = config.id
                    ServerConfigurationService.updateServerConfiguration(ServerConfiguration(id, name, host, port, password, streaming))
                } else {
                    ServerConfigurationService.addServerConfiguration(ServerConfiguration(name, host, port, password, streaming))
                }
                return true
            }
        }

        return false
    }

    private fun setStreamingViewUrl(url: String?) {
        val streamingSwitch = binding.addEditServerStreamingUrlSwitch
        val streamingText = binding.addEditServerStreamingUrlText

        if (url.isNullOrEmpty()) {
            streamingSwitch.isChecked = false
            streamingText.visibility = View.GONE
            streamingText.setText("")
        } else {
            streamingSwitch.isChecked = true
            streamingText.visibility = View.VISIBLE
            streamingText.setText(url)
        }
    }

    companion object {
        const val CONFIGURATION_ARGUMENT_KEY = "serverConfiguration"
    }
}
