package net.prezz.mpr.mpd.command

import net.prezz.mpr.model.PartitionEntity
import net.prezz.mpr.mpd.MpdPartitionProvider
import net.prezz.mpr.mpd.connection.MpdConnection
import java.util.ArrayList

class MpdGetPartitionsCommand(partitionProvider: MpdPartitionProvider) :
    MpdConnectionCommand<Void?, Array<PartitionEntity>>(null, partitionProvider) {

    @Throws(Exception::class)
    override fun doExecute(connection: MpdConnection, param: Void?): Array<PartitionEntity> {

        if (connection.isMinimumVersion(0, 22, 0)) {

            val lines = connection.writeResponseCommand("listpartitions\n")

            val partitions = ArrayList<String>()

            for (line in lines) {
                if (line.startsWith("partition: ")) {
                    partitions.add(line.substring(11))
                }
            }

            val result = ArrayList<PartitionEntity>()

            try {
                for (partition in partitions) {
                    if (connection.setPartition(partition)) {
                        val isClientPartition = super.getPartition() == partition

                        val outputs = MpdGetOutputsCommand.getOutputs(connection)
                        val outputNames = ArrayList<String>()
                        for (output in outputs) {
                            if (output.plugin != "dummy") {
                                outputNames.add(output.outputName)
                            }
                        }
                        result.add(PartitionEntity(isClientPartition, partition, outputNames))
                    }
                }
            } finally {
                connection.setPartition(super.getPartition())
            }

            return result.toTypedArray()
        }

        return arrayOf()
    }

    override fun onError(): Array<PartitionEntity> = arrayOf()
}
