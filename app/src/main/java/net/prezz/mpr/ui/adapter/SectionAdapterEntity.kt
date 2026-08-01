package net.prezz.mpr.ui.adapter

class SectionAdapterEntity(private val section: String) : AdapterEntity {

    override fun getSectionIndexText(): String {
        return getText()
    }

    override fun getText(): String {
        return section
    }

    override fun toString(): String {
        return section
    }

    companion object {
        private const val serialVersionUID = -5553831187058499677L
    }
}
