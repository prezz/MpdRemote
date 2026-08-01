package net.prezz.mpr.ui.settings.servers

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import net.prezz.mpr.R
import net.prezz.mpr.databinding.ActivityServersBinding
import net.prezz.mpr.model.servers.ServerConfiguration
import net.prezz.mpr.model.servers.ServerConfigurationService
import net.prezz.mpr.ui.helpers.VolumeButtonsHelper
import net.prezz.mpr.ui.helpers.setupToolbar

class ServersActivity : AppCompatActivity(), OnItemClickListener, ActivityResultCallback<ActivityResult> {

    private lateinit var binding: ActivityServersBinding
    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent>
    private var serverConfigurations: Array<ServerConfiguration> = emptyArray()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityServersBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupToolbar()

        activityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult(), this)

        binding.serversAddButton.setOnClickListener { onAddServerClick(it) }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)

        val listView = findListView()
        listView.onItemClickListener = this
        listView.setOnItemLongClickListener { _, _, position, _ ->
            showContextMenu(position)
            true
        }

        val adapter = ArrayAdapter<ServerConfiguration>(this, R.layout.view_list_item_single_line, ArrayList())
        listView.adapter = adapter

        updateListView()
    }

    private fun showContextMenu(position: Int) {
        val configuration = serverConfigurations[position]
        val menuItems = resources.getStringArray(R.array.servers_context_menu)
        MaterialAlertDialogBuilder(this)
            .setTitle(configuration.toString())
            .setItems(menuItems) { _, which ->
                when (which) {
                    0 -> {
                        ServerConfigurationService.deleteServerConfiguration(configuration)
                        updateListView()
                    }
                }
            }
            .show()
    }

    override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        val configuration = serverConfigurations[position]
        val intent = Intent(this, AddEditServerActivity::class.java)
        val args = Bundle()
        args.putSerializable(AddEditServerActivity.CONFIGURATION_ARGUMENT_KEY, configuration)
        intent.putExtras(args)
        activityResultLauncher.launch(intent)
    }

    private fun onAddServerClick(view: View) {
        val intent = Intent(this, AddEditServerActivity::class.java)
        val args = Bundle()
        args.putSerializable(AddEditServerActivity.CONFIGURATION_ARGUMENT_KEY, null)
        intent.putExtras(args)
        activityResultLauncher.launch(intent)
    }

    override fun onActivityResult(result: ActivityResult) {
        if (result.resultCode == RESULT_OK) {
            updateListView()
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (VolumeButtonsHelper.handleKeyDown(this, keyCode, event)) {
            return true
        }

        return super.onKeyDown(keyCode, event)
    }

    private fun updateListView() {
        serverConfigurations = ServerConfigurationService.getServerConfigurations()

        val listView = findListView()
        @Suppress("UNCHECKED_CAST")
        val arrayAdapter = listView.adapter as ArrayAdapter<ServerConfiguration>
        arrayAdapter.setNotifyOnChange(false)
        arrayAdapter.clear()
        arrayAdapter.addAll(*serverConfigurations)
        arrayAdapter.notifyDataSetChanged()
    }

    private fun findListView(): ListView {
        return binding.serversListViewBrowse
    }
}
