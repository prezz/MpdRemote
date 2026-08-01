package net.prezz.mpr.ui.helpers

import android.content.Context
import android.content.res.Resources
import android.widget.Toast

class Boast private constructor(private val toast: Toast) {

    fun cancel() {
        toast.cancel()
    }

    @JvmOverloads
    fun show(cancelCurrent: Boolean = true) {
        synchronized(lock) {
            if (cancelCurrent) {
                lastBoast?.cancel()
            }
            lastBoast = this

            toast.setMargin(toast.horizontalMargin, 0.05f)
            toast.show()
        }
    }

    companion object {
        private val lock = Any()
        private var lastBoast: Boast? = null

        fun makeText(context: Context, text: CharSequence): Boast {
            // Use the application context so the retained lastBoast can never hold on to an Activity.
            return Boast(Toast.makeText(context.applicationContext, text, Toast.LENGTH_SHORT))
        }

        @Throws(Resources.NotFoundException::class)
        fun makeText(context: Context, resId: Int): Boast {
            return Boast(Toast.makeText(context.applicationContext, resId, Toast.LENGTH_SHORT))
        }
    }
}
