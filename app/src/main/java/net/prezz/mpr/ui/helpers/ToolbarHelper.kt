package net.prezz.mpr.ui.helpers

import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.NavUtils
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import net.prezz.mpr.R

/**
 * Wires up the shared @layout/view_toolbar as the activity's support action bar. Must be called
 * after setContentView. Pass showUpButton = true for activities that declare a parentActivityName;
 * a custom Toolbar (unlike the framework ActionBar) does not derive the Up affordance from the
 * manifest automatically, so it is enabled and handled explicitly here.
 */
fun AppCompatActivity.setupToolbar(showUpButton: Boolean = false) {
    val toolbar = findViewById<Toolbar>(R.id.toolbar)
    setSupportActionBar(toolbar)

    // The app bar is dark in both light and dark themes, so the status bar icons stay light.
    WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = false

    if (showUpButton) {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener {
            NavUtils.navigateUpFromSameTask(this)
        }
    }

    applyWindowInsets(toolbar)
}

/**
 * Edge-to-edge inset handling shared by every screen: the toolbar's gradient background fills
 * behind the status bar (top inset applied as toolbar padding, which grows the bar) while the
 * screen body stays clear of the side and bottom system bars.
 */
private fun AppCompatActivity.applyWindowInsets(toolbar: Toolbar) {
    val content = findViewById<View>(android.R.id.content)
    ViewCompat.setOnApplyWindowInsetsListener(content) { _, insets ->
        val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        toolbar.updatePadding(top = bars.top)
        content.updatePadding(left = bars.left, right = bars.right, bottom = bars.bottom)
        WindowInsetsCompat.CONSUMED
    }
    ViewCompat.requestApplyInsets(content)
}
