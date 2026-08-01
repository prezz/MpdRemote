package net.prezz.mpr.ui.adapter

import java.io.Serializable

interface AdapterEntity : Serializable {

    fun getSectionIndexText(): String

    fun getText(): String
}
