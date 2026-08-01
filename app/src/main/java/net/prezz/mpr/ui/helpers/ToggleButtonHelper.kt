package net.prezz.mpr.ui.helpers

import android.app.Activity
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.util.TypedValue
import android.widget.ImageButton
import net.prezz.mpr.R

object ToggleButtonHelper {

    fun toggleButton(activity: Activity, button: ImageButton, toggled: Boolean) {
        val attr = if (toggled) R.attr.redFocusColor else R.attr.iconColor

        val typedValue = TypedValue()
        activity.theme.resolveAttribute(attr, typedValue, true)
        button.colorFilter = PorterDuffColorFilter(typedValue.data, PorterDuff.Mode.SRC_IN)
    }
}
