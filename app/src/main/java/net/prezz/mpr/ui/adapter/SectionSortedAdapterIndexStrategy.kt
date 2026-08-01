package net.prezz.mpr.ui.adapter

import java.util.ArrayList
import java.util.HashMap
import java.util.Locale

import android.annotation.SuppressLint

object SectionSortedAdapterIndexStrategy : AdapterIndexStrategy {

    @SuppressLint("DefaultLocale")
    override fun createSectionIndexes(inEntities: Array<AdapterEntity>, outSectionsList: ArrayList<String>, outPositionForSection: ArrayList<Int>, outSectionForPosition: ArrayList<Int>) {
        val sectionsMap = HashMap<Key, Int>()

        var section = 0
        for (i in inEntities.indices) {
            val entity = inEntities[i]
            if (entity is SectionAdapterEntity) {
                section++
            }
            val label = if (entity is SectionAdapterEntity) "" else entity.getSectionIndexText()
            val letter = if (label.isEmpty()) "" else label.substring(0, 1).uppercase(Locale.getDefault())
            val key = Key(section, letter)
            if (!sectionsMap.containsKey(key)) {
                sectionsMap.put(key, sectionsMap.size)
                outSectionsList.add(letter)
            }
            if (entity is SectionAdapterEntity) {
                section++
            }
        }

        // Calculate the section for each position in the list.
        section = 0
        for (i in inEntities.indices) {
            val entity = inEntities[i]
            if (entity is SectionAdapterEntity) {
                section++
            }
            val label = if (entity is SectionAdapterEntity) "" else entity.getSectionIndexText()
            val letter = if (label.isEmpty()) "" else label.substring(0, 1).uppercase(Locale.getDefault())
            outSectionForPosition.add(sectionsMap.get(Key(section, letter))!!)
            if (entity is SectionAdapterEntity) {
                section++
            }
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

    private class Key(private val section: Int, private val letter: String) {

        override fun equals(other: Any?): Boolean {
            if (this === other) {
                return true
            }

            if (other is Key) {
                return this.section == other.section && this.letter == other.letter
            }

            return false
        }

        override fun hashCode(): Int {
            return section xor letter.hashCode()
        }
    }
}
