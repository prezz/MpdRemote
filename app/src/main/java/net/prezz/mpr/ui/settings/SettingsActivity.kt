package net.prezz.mpr.ui.settings

import android.os.Bundle
import android.view.KeyEvent
import androidx.appcompat.app.AppCompatActivity
import net.prezz.mpr.R
import net.prezz.mpr.databinding.ActivitySettingsBinding
import net.prezz.mpr.ui.helpers.VolumeButtonsHelper
import net.prezz.mpr.ui.helpers.setupToolbar

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupToolbar(showUpButton = true)

        if (savedInstanceState == null) {
            val fragment = supportFragmentManager.findFragmentByTag(FRAGMENT_TAG) ?: SettingsFragment()

            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings_fragment_container, fragment, FRAGMENT_TAG)
                .commit()
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (VolumeButtonsHelper.handleKeyDown(this, keyCode, event)) {
            return true
        }

        return super.onKeyDown(keyCode, event)
    }

    companion object {
        private const val FRAGMENT_TAG = "settings_fragment"
    }
}
