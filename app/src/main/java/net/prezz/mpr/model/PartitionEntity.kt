package net.prezz.mpr.model

data class PartitionEntity(
    val clientPartition: Boolean,
    val partitionName: String,
    val outputs: List<String>
) {

    override fun toString(): String {
        return partitionName
    }
}
