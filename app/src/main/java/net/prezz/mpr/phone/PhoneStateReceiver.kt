package net.prezz.mpr.phone

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.telephony.TelephonyManager
import androidx.preference.PreferenceManager
import net.prezz.mpr.R
import net.prezz.mpr.model.MusicPlayerControl
import net.prezz.mpr.model.command.PauseCommand

class PhoneStateReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (pauseOnIncommingCall(context) && hasPhoneStatePermission(context)) {
            val bundle = intent.extras
            if (bundle != null) {
                val state = bundle.getString(TelephonyManager.EXTRA_STATE)
                val shouldPause = TelephonyManager.EXTRA_STATE_RINGING.equals(state, ignoreCase = true) ||
                    TelephonyManager.EXTRA_STATE_OFFHOOK.equals(state, ignoreCase = true)
                if (shouldPause) {
                    MusicPlayerControl.sendControlCommand(PauseCommand(false))
                }
            }
        }
    }

    private fun pauseOnIncommingCall(context: Context): Boolean {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val resources = context.resources
        return sharedPreferences.getBoolean(resources.getString(R.string.settings_behavior_pause_on_phonecall_key), false)
    }

    private fun hasPhoneStatePermission(context: Context): Boolean {
        return context.checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
    }
}
