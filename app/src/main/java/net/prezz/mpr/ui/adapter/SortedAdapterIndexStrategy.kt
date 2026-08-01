package net.prezz.mpr.ui.adapter

import java.util.ArrayList
import java.util.HashMap
import java.util.Locale

import android.annotation.SuppressLint

object SortedAdapterIndexStrategy : AdapterIndexStrategy {

    @SuppressLint("DefaultLocale")
    override fun createSectionIndexes(inEntities: Array<AdapterEntity>, outSectionsList: ArrayList<String>, outPositionForSection: ArrayList<Int>, outSectionForPosition: ArrayList<Int>) {
        val sectionsMap = HashMap<String, Int>()

        for (i in inEntities.indices) {
            val label = inEntities[i].getSectionIndexText()
            val letter = if (label.isEmpty()) "" else label.substring(0, 1).uppercase(Locale.getDefault())
            if (!sectionsMap.containsKey(letter)) {
                sectionsMap.put(letter, sectionsMap.size)
                outSectionsList.add(letter)
            }
        }

        // Calculate the section for each position in the list.
        for (i in inEntities.indices) {
            val label = inEntities[i].getSectionIndexText()
            val letter = if (label.isEmpty()) "" else label.substring(0, 1).uppercase(Locale.getDefault())
            outSectionForPosition.add(sectionsMap.get(letter)!!)
        }

        // Calculate the first position where each section begins.
        for (i in 0 until sectionsMap.size) {
            outPositionForSection.add(0)
        }
        for (i in 0 until sectionsMap.size) {
            for (j in inEntities.indices) {
                if (i == outSectionForPosition.get(j).toInt()) {
                    outPositionForSection.set(i, j)
                    break
                }
            }
        }

        //finally add position just past the last element such scrolling for the last section can be correctly calculated
        outPositionForSection.add(inEntities.size)
    }
}
