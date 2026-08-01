package net.prezz.mpr.ui.adapter

import java.util.ArrayList

interface AdapterIndexStrategy {

    fun createSectionIndexes(inEntities: Array<AdapterEntity>, outSectionsList: ArrayList<String>, outPositionForSection: ArrayList<Int>, outSectionForPosition: ArrayList<Int>)
}
