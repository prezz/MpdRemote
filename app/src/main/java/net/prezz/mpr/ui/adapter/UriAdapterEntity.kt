package net.prezz.mpr.ui.adapter

import net.prezz.mpr.model.UriEntity
import net.prezz.mpr.ui.ApplicationActivator
import net.prezz.mpr.R

class UriAdapterEntity(private val uriEntity: UriEntity) : AdapterEntity {

    override fun getSectionIndexText(): String {
        return uriEntity.uriPath
    }

    override fun getText(): String {
        return uriEntity.uriPath
    }

    fun getSubText(): String {
        when (uriEntity.uriType) {
            UriEntity.UriType.DIRECTORY ->
                return ApplicationActivator.context.getString(R.string.library_directory)
            UriEntity.UriType.FILE ->
                return ApplicationActivator.context.getString(if (uriEntity.fileType == UriEntity.FileType.PLAYLIST) R.string.library_file_playlist else R.string.library_file)
        }

        return ""
    }

    fun getEntity(): UriEntity {
        return uriEntity
    }

    companion object {
        private const val serialVersionUID = 6951202513706174870L
    }
}
