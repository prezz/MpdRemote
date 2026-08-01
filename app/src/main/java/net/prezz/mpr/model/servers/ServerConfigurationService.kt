package net.prezz.mpr.model.servers

import androidx.core.content.edit

import java.io.File

import net.prezz.mpr.mpd.database.MpdLibraryDatabaseHelper
import net.prezz.mpr.ui.ApplicationActivator
import net.prezz.mpr.ui.helpers.UriFilterHelper

import android.content.Context

import androidx.preference.PreferenceManager

object ServerConfigurationService {

    const val SELECTED_SERVER_CONFIGURATION_KEY = "selectedServerKey"
    private val databaseHelper = ServerConfigurationDatabaseHelper(ApplicationActivator.context)

    fun getSelectedServerConfiguration(): ServerConfiguration {
        val context = ApplicationActivator.context

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val selectedServerId = sharedPreferences.getInt(SELECTED_SERVER_CONFIGURATION_KEY, 0)

        val serverConfigurations = getServerConfigurations()
        if (serverConfigurations.size > 0) {
            for (configuration in serverConfigurations) {
                if (configuration.id == selectedServerId) {
                    return configuration
                }
            }

            return serverConfigurations[0]
        }

        return ServerConfiguration("", "", "6600", "", "")
    }

    fun setSelectedServerConfiguration(configuration: ServerConfiguration) {
        val context = ApplicationActivator.context

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        sharedPreferences.edit(commit = true) {
            putInt(SELECTED_SERVER_CONFIGURATION_KEY, configuration.id)
        }
    }

    fun getServerConfigurations(): Array<ServerConfiguration> {
        val c = databaseHelper.getServers()

        try {
            var i = 0
            val result = arrayOfNulls<ServerConfiguration>(c.count)
            if (c.moveToFirst()) {
                do {
                    val id = c.getInt(0)
                    val name = c.getString(1)
                    val host = c.getString(2)
                    val port = c.getString(3)
                    val password = c.getString(4)
                    val streaming = c.getString(5)
                    result[i++] = ServerConfiguration(id, name, host, port, password, streaming)
                } while (c.moveToNext())
            }
            @Suppress("UNCHECKED_CAST")
            return result as Array<ServerConfiguration>
        } finally {
            c.close()
            databaseHelper.close()
        }
    }

    fun addServerConfiguration(configuration: ServerConfiguration) {
        databaseHelper.addServer(configuration.name, configuration.host, configuration.port, configuration.password, configuration.streaming)
        databaseHelper.close()

        val serverConfigurations = getServerConfigurations()
        if (serverConfigurations.size == 1) {
            setSelectedServerConfiguration(serverConfigurations[0])
        }
    }

    fun updateServerConfiguration(configuration: ServerConfiguration) {
        val oldHost = getHost(configuration.id)
        databaseHelper.updateServer(configuration.id, configuration.name, configuration.host, configuration.port, configuration.password, configuration.streaming)
        databaseHelper.close()
        if (oldHost != configuration.host) {
            deleteOrphanLibraryDatabase(oldHost)
        }
    }

    fun deleteServerConfiguration(configuration: ServerConfiguration) {
        val context = ApplicationActivator.context

        val oldHost = getHost(configuration.id)
        databaseHelper.deleteServer(configuration.id)
        databaseHelper.close()
        deleteOrphanLibraryDatabase(oldHost)
        UriFilterHelper.removeUriFilter(context, oldHost)

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val selectedServerId = sharedPreferences.getInt(SELECTED_SERVER_CONFIGURATION_KEY, 0)
        if (selectedServerId == configuration.id) {
            val serverConfigurations = getServerConfigurations()
            if (serverConfigurations.size > 0) {
                setSelectedServerConfiguration(serverConfigurations[0])
            }
        }
    }

    private fun getHost(id: Int): String? {
        var host: String? = null

        try {
            val c = databaseHelper.getServerHost(id)
            try {
                if (c.moveToFirst()) {
                    host = c.getString(0)
                }
            } finally {
                c.close()
                //don't close databaseHelper here, this will be done by update and delete
            }
        } catch (ex: Exception) {
        }

        return host
    }

    private fun deleteOrphanLibraryDatabase(host: String?) {
        if (host != null) {
            try {
                val context = ApplicationActivator.context
                val dbToDelete: File? = context.getDatabasePath(host + MpdLibraryDatabaseHelper.LIBRARY_FILE_DB_POSTFIX)
                if (dbToDelete != null && dbToDelete.exists()) {
                    context.deleteDatabase(dbToDelete.absolutePath)
                }
            } catch (ex: Exception) {
            }
        }
    }
}
