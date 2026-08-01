package net.prezz.mpr.ui.settings

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.getSystemService
import androidx.preference.CheckBoxPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import net.prezz.mpr.R
import net.prezz.mpr.model.MusicPlayerControl
import net.prezz.mpr.model.ResponseReceiver
import net.prezz.mpr.model.TaskHandle
import net.prezz.mpr.service.PlaybackService
import net.prezz.mpr.ui.AboutActivity
import net.prezz.mpr.ui.helpers.Boast
import net.prezz.mpr.ui.helpers.NightModeHelper
import net.prezz.mpr.ui.settings.servers.ServersActivity

class SettingsFragment : PreferenceFragmentCompat(), ActivityResultCallback<Map<String, Boolean>> {

    private lateinit var activityResultLauncher: ActivityResultLauncher<Array<String>>

    private var deleteDatabaseHandle: TaskHandle = TaskHandle.NULL_HANDLE

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setupSimplePreferencesScreen(rootKey)

        activityResultLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions(), this)
    }

    override fun onStop() {
        super.onStop()

        deleteDatabaseHandle.cancelTask()
    }

    override fun onActivityResult(result: Map<String, Boolean>) {

        if (result[Manifest.permission.READ_PHONE_STATE] == true) {
            val pauseOnPhonePreference = findPreference<CheckBoxPreference>(getString(R.string.settings_behavior_pause_on_phonecall_key))!!
            pauseOnPhonePreference.isChecked = true
        }

        if (result[Manifest.permission.POST_NOTIFICATIONS] == true) {
            val showNotificationPreference = findPreference<CheckBoxPreference>(getString(R.string.settings_behavior_show_notification_key))!!
            showNotificationPreference.isChecked = true
            handleNotificationPreference(true)
        }

        if (result[Manifest.permission.ACCESS_LOCAL_NETWORK] == true) {
            startActivity(Intent(activity, ServersActivity::class.java))
        }
    }

    private fun setupSimplePreferencesScreen(rootKey: String?) {
        setPreferencesFromResource(R.xml.settings_screen, rootKey)

        setupServersPreferences()
        setupThemePreferences()
        setupProperSortingPreferences()
        setupPauseOnPhoneCallPreferences()
        setupNotificationPreferences()
        setupAboutPreferences()

        // Bind the summaries of list preferences to their values so the summary reflects the
        // current selection, per the Android Design guidelines.
        bindPreferenceSummaryToValue(findPreference(getString(R.string.settings_default_player_fragment_key))!!)
        bindPreferenceSummaryToValue(findPreference(getString(R.string.settings_volume_control_amount_key))!!)
    }

    private fun bindPreferenceSummaryToValue(preference: Preference) {
        preference.onPreferenceChangeListener = bindPreferenceSummaryToValueListener

        // Trigger the listener immediately with the preference's current value.
        bindPreferenceSummaryToValueListener.onPreferenceChange(
            preference,
            PreferenceManager.getDefaultSharedPreferences(preference.context).getString(preference.key, ""),
        )
    }

    private val bindPreferenceSummaryToValueListener = Preference.OnPreferenceChangeListener { preference, value ->
        if (preference is ListPreference) {
            val stringValue = value.toString()
            val index = preference.findIndexOfValue(stringValue)
            preference.summary = if (index >= 0) preference.entries[index] else null
        }
        true
    }

    private fun setupServersPreferences() {
        val serversPreference = findPreference<Preference>(getString(R.string.settings_servers_key))!!
        serversPreference.setOnPreferenceClickListener {
            val context = requireContext()
            if (context.checkSelfPermission(Manifest.permission.ACCESS_LOCAL_NETWORK) != PackageManager.PERMISSION_GRANTED) {
                activityResultLauncher.launch(arrayOf(Manifest.permission.ACCESS_LOCAL_NETWORK))
                return@setOnPreferenceClickListener false
            }

            startActivity(Intent(activity, ServersActivity::class.java))
            true
        }
    }

    private fun setupThemePreferences() {
        val themePreference = findPreference<Preference>(getString(R.string.settings_interface_theme_mode_key))!!
        themePreference.setOnPreferenceChangeListener { _, newValue ->
            // Applying the night mode recreates the running activities automatically.
            NightModeHelper.apply(newValue as String)
            true
        }
    }

    private fun setupProperSortingPreferences() {
        val sortingPreference = findPreference<Preference>(getString(R.string.settings_library_proper_sort_key))!!
        sortingPreference.setOnPreferenceChangeListener { _, _ ->
            deleteDatabaseHandle.cancelTask()
            deleteDatabaseHandle = MusicPlayerControl.deleteLocalLibraryDatabase(object : ResponseReceiver<Boolean>() {
                override fun receiveResponse(response: Boolean) {
                    // Delivered asynchronously; the fragment may have detached, so use the nullable
                    // activity instead of requireActivity() to avoid an IllegalStateException.
                    if (response) {
                        activity?.let { Boast.makeText(it, R.string.settings_delete_library_database_toast).show() }
                    }
                }
            })
            true
        }
    }

    private fun setupPauseOnPhoneCallPreferences() {
        val pauseOnPhonePreference = findPreference<Preference>(getString(R.string.settings_behavior_pause_on_phonecall_key))!!
        pauseOnPhonePreference.setOnPreferenceChangeListener { _, newValue ->
            if (newValue == true) {
                val context = requireContext()
                if (context.checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                    activityResultLauncher.launch(arrayOf(Manifest.permission.READ_PHONE_STATE))
                    return@setOnPreferenceChangeListener false
                }
            }
            true
        }
    }

    private fun setupNotificationPreferences() {
        val notificationPreference = findPreference<Preference>(getString(R.string.settings_behavior_show_notification_key))!!
        notificationPreference.setOnPreferenceChangeListener { _, newValue ->
            if (newValue == true) {
                val context = requireContext()
                if (context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    activityResultLauncher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
                    return@setOnPreferenceChangeListener false
                }
            }

            handleNotificationPreference(newValue)
            true
        }
    }

    private fun setupAboutPreferences() {
        val version = getVersion()

        val aboutPreference = findPreference<Preference>(getString(R.string.settings_about_key))!!
        aboutPreference.summary = getString(R.string.settings_about_summary) + " " + version

        aboutPreference.setOnPreferenceClickListener {
            startActivity(Intent(activity, AboutActivity::class.java))
            true
        }
    }

    private fun handleNotificationPreference(newValue: Any?) {

        if (newValue == false) {
            PlaybackService.stop()
        }

        if (newValue == true) {
            // Route through the service's single source of truth so the channel is never created
            // here with a conflicting importance (importance is fixed at creation time).
            PlaybackService.createMediaNotificationChannel(requireActivity())
        } else {
            val notificationManager = requireActivity().getSystemService<NotificationManager>()!!
            val channelId = getString(R.string.notification_media_player_channel_id)
            notificationManager.deleteNotificationChannel(channelId)
        }
    }

    private fun getVersion(): String {
        try {
            val activity = requireActivity()
            val info = activity.packageManager.getPackageInfo(activity.applicationContext.packageName, PackageManager.PackageInfoFlags.of(0))
            return info.versionName ?: "-"
        } catch (ex: Exception) {
            Log.w(SettingsFragment::class.java.name, "unable to read app version", ex)
        }

        return "-"
    }
}
