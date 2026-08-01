package net.prezz.mpr.model.servers

import java.io.Serializable

data class ServerConfiguration(
    val id: Int,
    val name: String?,
    val host: String?,
    val port: String?,
    val password: String?,
    val streaming: String?
) : Serializable {

    constructor(name: String?, host: String?, port: String?, password: String?, streaming: String?) :
        this(0, name, host, port, password, streaming)

    override fun toString(): String {
        return name ?: ""
    }

    companion object {
        private const val serialVersionUID = -8757270537389863648L
    }
}
