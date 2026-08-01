package net.prezz.mpr.model

data class AudioOutput(
    val outputId: String,
    val outputName: String,
    val plugin: String,
    val enabled: Boolean
)
