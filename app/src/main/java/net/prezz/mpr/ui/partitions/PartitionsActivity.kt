package net.prezz.mpr.ui.partitions

import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import net.prezz.mpr.R
import net.prezz.mpr.Utils
import net.prezz.mpr.databinding.ActivityPartitionsBinding
import net.prezz.mpr.model.AudioOutput
import net.prezz.mpr.model.MusicPlayerControl
import net.prezz.mpr.model.PartitionEntity
import net.prezz.mpr.model.ResponseReceiver
import net.prezz.mpr.model.ResponseResult
import net.prezz.mpr.model.TaskHandle
import net.prezz.mpr.model.command.Command
import net.prezz.mpr.model.command.CreatePartitionCommand
import net.prezz.mpr.model.command.DeletePartitionCommand
import net.prezz.mpr.model.command.MoveOutputToPartitionCommand
import net.prezz.mpr.mpd.MpdPartitionProvider
import net.prezz.mpr.service.PlaybackService
import net.prezz.mpr.ui.adapter.PartitionAdapterEntity
import net.prezz.mpr.ui.adapter.PartitionArrayAdapter
import net.prezz.mpr.ui.helpers.Boast
import net.prezz.mpr.ui.helpers.VolumeButtonsHelper
import net.prezz.mpr.ui.helpers.setupToolbar
import net.prezz.mpr.ui.state.DataState

class PartitionsActivity : AppCompatActivity(), OnItemClickListener {

    private lateinit var binding: ActivityPartitionsBinding

    private val refreshResponseReceiver = RefreshEntitiesResponseReceiver()

    private var adapterEntities: Array<PartitionAdapterEntity>? = null
    private var updating = false
    private var updatingPartitionsHandle: TaskHandle = TaskHandle.NULL_HANDLE
    private var assignOutputsHandle: TaskHandle = TaskHandle.NULL_HANDLE
    private var switchPartitionHandle: TaskHandle = TaskHandle.NULL_HANDLE

    private var notSupportedDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityPartitionsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupToolbar(showUpButton = true)

        binding.partitionsAddButton.setOnClickListener { onCreatePartitionClick(it) }

        val dataState = DataState.get(this)
        // restore entities if loaded into memory again (or after rotation)
        @Suppress("UNCHECKED_CAST")
        (dataState.getData(PARTITIONS_SAVED_INSTANCE_STATE, null) as? Array<PartitionAdapterEntity>)?.let {
            adapterEntities = it
        }

        updateEntities()
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)

        val listView = findListView()
        listView.onItemClickListener = this
        listView.setOnItemLongClickListener { _, _, position, _ ->
            showContextMenu(position)
            true
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        val dataState = DataState.get(this)
        dataState.setData(PARTITIONS_SAVED_INSTANCE_STATE, adapterEntities)

        super.onSaveInstanceState(outState)
    }

    override fun onStop() {
        super.onStop()

        // Cancel any in-flight requests so their callbacks don't touch a stopped activity.
        updatingPartitionsHandle.cancelTask()
        assignOutputsHandle.cancelTask()
        switchPartitionHandle.cancelTask()
    }

    override fun onDestroy() {
        super.onDestroy()

        // Avoid leaking the dialog window if the activity is destroyed while it is showing.
        notSupportedDialog?.dismiss()
        notSupportedDialog = null
    }

    private fun showContextMenu(position: Int) {
        val entity = adapterEntities?.get(position) ?: return
        val partitionEntity = entity.getEntity()
        // The last item (delete) is only offered for deletable partitions, matching the old menu.
        val allItems = resources.getStringArray(R.array.partitions_context_menu)
        val menuItems = if (canDelete(partitionEntity)) allItems else allItems.copyOf(allItems.size - 1)
        MaterialAlertDialogBuilder(this)
            .setTitle(entity.getText())
            .setItems(menuItems) { _, which ->
                when (which) {
                    0 -> selectPartition(partitionEntity)
                    1 -> assignOutput(partitionEntity)
                    2 -> deletePartition(partitionEntity)
                }
            }
            .show()
    }

    override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        val entity = adapterEntities?.get(position) ?: return
        val partitionEntity = entity.getEntity()

        selectPartition(partitionEntity)
    }

    private fun onCreatePartitionClick(view: View) {
        if (adapterEntities != null) {
            createPartition()
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (VolumeButtonsHelper.handleKeyDown(this, keyCode, event)) {
            return true
        }

        return super.onKeyDown(keyCode, event)
    }

    private fun updateEntities() {
        val existing = adapterEntities
        if (existing != null) {
            createEntityAdapter(existing)
        } else if (!updating) {
            showUpdatingIndicator()
            updatingPartitionsHandle.cancelTask()
            updatingPartitionsHandle = MusicPlayerControl.getPartitions(object : ResponseReceiver<Array<PartitionEntity>>() {
                override fun receiveResponse(response: Array<PartitionEntity>) {
                    val entities = createAdapterEntities(response)
                    adapterEntities = entities
                    createEntityAdapter(entities)
                    hideUpdatingIndicator()

                    if (entities.isEmpty()) {
                        showNotSupported()
                    }
                }
            })
        }
    }

    private fun createAdapterEntities(entities: Array<PartitionEntity>): Array<PartitionAdapterEntity> {
        return Array(entities.size) { PartitionAdapterEntity(entities[it]) }
    }

    private fun createEntityAdapter(adapterEntities: Array<PartitionAdapterEntity>) {
        val listView = findListView()
        val adapter = createAdapter(adapterEntities)
        listView.adapter = adapter
    }

    private fun createAdapter(adapterEntities: Array<PartitionAdapterEntity>): PartitionArrayAdapter {
        return PartitionArrayAdapter(this, android.R.layout.simple_list_item_2, ArrayList(adapterEntities.asList()))
    }

    private fun findListView(): ListView {
        return binding.partitionsListViewBrowse
    }

    private fun showUpdatingIndicator() {
        updating = true
        binding.partitionsProgressBarLoad.visibility = View.VISIBLE
    }

    private fun hideUpdatingIndicator() {
        updating = false
        binding.partitionsProgressBarLoad.visibility = View.GONE
    }

    private fun createPartition() {
        val editTextView = EditText(this)
        editTextView.setSingleLine()

        val dialog = MaterialAlertDialogBuilder(this).apply {
            setTitle(R.string.partitions_create_button)
            setView(editTextView)
            setPositiveButton(android.R.string.ok) { _, _ ->
                val partitionName = editTextView.text.toString()
                if (isValidPartitionName(partitionName)) {
                    MusicPlayerControl.sendControlCommand(CreatePartitionCommand(partitionName), refreshResponseReceiver)
                } else {
                    Boast.makeText(this@PartitionsActivity, R.string.partitions_invalid_name_toast).show()
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

    private fun selectPartition(partitionEntity: PartitionEntity) {
        if (!partitionEntity.clientPartition) {
            PlaybackService.stop()
            switchPartitionHandle.cancelTask()
            switchPartitionHandle = MusicPlayerControl.switchPartition(partitionEntity.partitionName, object : ResponseReceiver<Array<PartitionEntity>>() {
                override fun receiveResponse(partitionEntities: Array<PartitionEntity>) {
                    refreshEntities(partitionEntities)
                }
            })
        }
    }

    private fun assignOutput(partitionEntity: PartitionEntity) {
        val preAssigned = HashSet(partitionEntity.outputs)

        assignOutputsHandle.cancelTask()
        assignOutputsHandle = MusicPlayerControl.getOutputs(true, object : ResponseReceiver<Array<AudioOutput>>() {
            override fun receiveResponse(response: Array<AudioOutput>) {
                val assignable = ArrayList<AudioOutput>()
                for (output in response) {
                    if (!preAssigned.contains(output.outputName)) {
                        assignable.add(output)
                    }
                }

                val items = Array(assignable.size) { assignable[it].outputName }
                val postChecked = BooleanArray(assignable.size)

                MaterialAlertDialogBuilder(this@PartitionsActivity).apply {
                    setTitle(R.string.partitions_assign_outputs)
                    setMultiChoiceItems(items, postChecked) { _, which, isChecked ->
                        postChecked[which] = isChecked
                    }
                    setPositiveButton(android.R.string.ok) { _, _ ->
                        val commands = ArrayList<Command>()
                        for (i in assignable.indices) {
                            if (postChecked[i]) {
                                commands.add(MoveOutputToPartitionCommand(items[i], partitionEntity.partitionName))
                            }
                        }
                        if (commands.isNotEmpty()) {
                            MusicPlayerControl.sendControlCommands(commands, refreshResponseReceiver)
                        }
                    }
                }.create().show()
            }
        })
    }

    private fun deletePartition(partitionEntity: PartitionEntity) {
        val commands = ArrayList<Command>()

        for (output in partitionEntity.outputs) {
            commands.add(MoveOutputToPartitionCommand(output, MpdPartitionProvider.DEFAULT_PARTITION))
        }
        commands.add(DeletePartitionCommand(partitionEntity.partitionName))

        MusicPlayerControl.sendControlCommands(commands, refreshResponseReceiver)
    }

    private fun getPartitionNames(adapterEntities: Array<PartitionAdapterEntity>): Array<String> {
        return Array(adapterEntities.size) { adapterEntities[it].getEntity().partitionName }
    }

    private fun isValidPartitionName(name: String): Boolean {
        if (name.isEmpty()) {
            return false
        }
        if (name.contains(" ")) {
            return false
        }
        if (getPartitionNames(adapterEntities ?: return false).asList().contains(name)) {
            return false
        }
        return true
    }

    private fun canDelete(partitionEntity: PartitionEntity): Boolean {
        if (partitionEntity.partitionName == MpdPartitionProvider.DEFAULT_PARTITION) {
            return false
        }
        if (partitionEntity.clientPartition) {
            return false
        }
        return true
    }

    private fun refreshEntities(entities: Array<PartitionEntity>) {
        val newEntities = createAdapterEntities(entities)
        adapterEntities = newEntities
        val listView = findListView()
        @Suppress("UNCHECKED_CAST")
        val arrayAdapter = listView.adapter as ArrayAdapter<PartitionAdapterEntity>
        arrayAdapter.setNotifyOnChange(false)
        arrayAdapter.clear()
        arrayAdapter.addAll(*newEntities)
        arrayAdapter.notifyDataSetChanged()
    }

    private fun showNotSupported() {
        if (notSupportedDialog?.isShowing == true) {
            return
        }

        notSupportedDialog = MaterialAlertDialogBuilder(this).apply {
            setCancelable(false)
            setTitle(R.string.library_not_supported_header)
            setMessage(R.string.library_not_supported_message)
            setPositiveButton(android.R.string.ok) { _, _ -> }
        }.create().also { it.show() }
    }

    private inner class RefreshEntitiesResponseReceiver : ResponseReceiver<ResponseResult>() {

        override fun receiveResponse(response: ResponseResult) {
            if (!response.isSuccess) {
                Boast.makeText(this@PartitionsActivity, R.string.partitions_server_error_toast).show()
            }

            updatingPartitionsHandle.cancelTask()
            updatingPartitionsHandle = MusicPlayerControl.getPartitions(object : ResponseReceiver<Array<PartitionEntity>>() {
                override fun receiveResponse(response: Array<PartitionEntity>) {
                    refreshEntities(response)
                }
            })
        }
    }

    companion object {
        private const val PARTITIONS_SAVED_INSTANCE_STATE = "partitions"
    }
}
