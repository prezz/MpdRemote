package net.prezz.mpr.ui.mpd

import android.content.Context
import net.prezz.mpr.model.servers.ServerConfigurationService
import net.prezz.mpr.mpd.MpdSettings

class MpdPlayerSettings protected constructor(
    private val name: String,
    private val mpdHost: String,
    private val mpdPort: String,
    private val mpdPassword: String?,
    private val mpdStreamingUrl: String?,
) : MpdSettings {

    override fun getName(): String {
        return name
    }

    override fun getMpdHost(): String {
        return mpdHost
    }

    override fun getMpdPort(): String {
        return mpdPort
    }

    override fun getMpdPassword(): String? {
        return mpdPassword
    }

    fun getMpdStreamingUrl(): String? {
        return mpdStreamingUrl
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }

        if (other is MpdPlayerSettings) {

            if (mpdHost != other.mpdHost) {
                return false
            }
            if (mpdPort != other.mpdPort) {
                return false
            }
            if (mpdPassword != other.mpdPassword) {
                return false
            }
            if (mpdStreamingUrl != other.mpdStreamingUrl) {
                return false
            }

            return true
        }

        return false
    }

    companion object {
        @JvmStatic
        fun create(context: Context): MpdPlayerSettings {
            val selectedConfiguration = ServerConfigurationService.getSelectedServerConfiguration()

            val name = selectedConfiguration.name!!
            val mpdHost = selectedConfiguration.host!!
            val mpdPort = selectedConfiguration.port!!
            val mpdPassword = selectedConfiguration.password
            val mpdStreamingUrl = selectedConfiguration.streaming

            return MpdPlayerSettings(name, mpdHost, mpdPort, mpdPassword, mpdStreamingUrl)
        }
    }
}
