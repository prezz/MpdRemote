package net.prezz.mpr.ui.helpers

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import net.prezz.mpr.R

/**
 * Drives light/dark via AppCompat's DayNight night mode. The stored preference is one of
 * "light" / "dark" / "system"; applying a mode recreates the running activities automatically.
 */
object NightModeHelper {

    private const val THEME_LIGHT = "light"
    private const val THEME_DARK = "dark"
    private const val THEME_SYSTEM = "system"

    fun applyFromPreferences(context: Context) {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        val mode = preferences.getString(context.getString(R.string.settings_interface_theme_mode_key), THEME_SYSTEM)
        apply(mode)
    }

    fun apply(mode: String?) {
        val nightMode = when (mode) {
            THEME_LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            THEME_DARK -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(nightMode)
    }
}
