package net.prezz.mpr.ui.adapter

import net.prezz.mpr.model.PartitionEntity

class PartitionAdapterEntity(private val entity: PartitionEntity) : AdapterEntity {

    fun getEntity(): PartitionEntity {
        return entity
    }

    override fun getSectionIndexText(): String {
        return getText()
    }

    override fun getText(): String {
        return entity.partitionName
    }

    fun getSubText(): String {
        val sb = StringBuilder()

        val outputs = entity.outputs

        for (i in outputs.indices) {
            if (sb.isNotEmpty()) {
                sb.append(", ")
            }
            sb.append(outputs[i])
        }

        return sb.toString()
    }
}
