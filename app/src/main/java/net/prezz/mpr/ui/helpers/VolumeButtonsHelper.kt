package net.prezz.mpr.ui.helpers

import android.content.Context
import android.view.KeyEvent
import androidx.preference.PreferenceManager
import net.prezz.mpr.R
import net.prezz.mpr.model.MusicPlayerControl
import net.prezz.mpr.model.ResponseReceiver
import net.prezz.mpr.model.ResponseResult
import net.prezz.mpr.model.command.VolumeDownCommand
import net.prezz.mpr.model.command.VolumeUpCommand
import net.prezz.mpr.service.StreamingService

object VolumeButtonsHelper {

    fun handleKeyDown(context: Context, keyCode: Int, event: KeyEvent): Boolean {

        if (!StreamingService.isStarted()) {
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
            val resources = context.resources
            val enabled = sharedPreferences.getBoolean(resources.getString(R.string.settings_behavior_override_volume_buttons_key), true)

            if (enabled) {
                when (keyCode) {
                    KeyEvent.KEYCODE_VOLUME_UP -> {
                        if (!LyngdorfHelper.volumeUp(context)) {
                            MusicPlayerControl.sendControlCommand(VolumeUpCommand(getVolumeAmount(context)), object : ResponseReceiver<ResponseResult>() {
                                override fun receiveResponse(response: ResponseResult) {
                                    if (response.isSuccess) {
                                        val volume = response.getResponseValue(ResponseResult.ValueType.VOLUME) as Int?
                                        if (volume != null) {
                                            val text = if (volume != -1) context.getString(R.string.general_volume_text_format, volume) else context.getString(R.string.general_volume_text_no_mixer)
                                            Boast.makeText(context, text).show()
                                        }
                                    }
                                }
                            })
                        }
                        return true
                    }

                    KeyEvent.KEYCODE_VOLUME_DOWN -> {
                        if (!LyngdorfHelper.volumeDown(context)) {
                            MusicPlayerControl.sendControlCommand(VolumeDownCommand(getVolumeAmount(context)), object : ResponseReceiver<ResponseResult>() {
                                override fun receiveResponse(response: ResponseResult) {
                                    if (response.isSuccess) {
                                        val volume = response.getResponseValue(ResponseResult.ValueType.VOLUME) as Int?
                                        if (volume != null) {
                                            val text = if (volume != -1) context.getString(R.string.general_volume_text_format, volume) else context.getString(R.string.general_volume_text_no_mixer)
                                            Boast.makeText(context, text).show()
                                        }
                                    }
                                }
                            })
                        }
                        return true
                    }
                }
            }
        }

        return false
    }

    fun getVolumeAmount(context: Context): Int {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val resources = context.resources
        return sharedPreferences.getString(resources.getString(R.string.settings_volume_control_amount_key), "1")!!.toInt()
    }
}
