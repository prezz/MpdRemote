package net.prezz.mpr.ui.adapter

import net.prezz.mpr.model.LibraryEntity

abstract class LibraryAdapterEntity(private val entity: LibraryEntity) : AdapterEntity {

    override fun getSectionIndexText(): String {
        return getText()
    }

    fun getEntity(): LibraryEntity {
        return entity
    }

    abstract fun getSubText(): String

    abstract fun getTime(): String

    abstract fun getData(): String

    companion object {
        private const val serialVersionUID = -2010311452515165465L
    }
}
