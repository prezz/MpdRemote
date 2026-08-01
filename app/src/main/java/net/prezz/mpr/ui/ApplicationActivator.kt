package net.prezz.mpr.ui

import android.app.Application
import android.content.Context
import net.prezz.mpr.ui.helpers.NightModeHelper

class ApplicationActivator : Application() {

    override fun onCreate() {
        super.onCreate()
        context = applicationContext
        NightModeHelper.applyFromPreferences(this)
    }

    companion object {
        @JvmStatic
        lateinit var context: Context
            private set
    }
}
