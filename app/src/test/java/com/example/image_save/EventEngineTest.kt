package com.example.image_save

import org.junit.Assert.assertTrue
import org.junit.Test

class EventEngineTest {
    @Test
    fun babyEventsTriggerAtMonthZero() {
        val creature = CreatureProfile(
            id = "baby-1",
            name = "小宝",
            type = CreatureType.BABY_BOY,
            birthTimeMillis = System.currentTimeMillis() - 0L
        )

        val events = EventEngine().getTriggeredEvents(creature, 0)

        assertTrue(events.any { it.title.contains("奶粉") || it.title.contains("纸尿裤") })
    }

    @Test
    fun catEventsTriggerAtMonthTwo() {
        val creature = CreatureProfile(
            id = "cat-1",
            name = "小橘",
            type = CreatureType.CAT,
            birthTimeMillis = System.currentTimeMillis() - 60L * 24L * 60L * 60L * 1000L
        )

        val events = EventEngine().getTriggeredEvents(creature, 2)

        assertTrue(events.any { it.title.contains("疫苗") })
    }

    @Test
    fun dogEventsTriggerAtMonthFour() {
        val creature = CreatureProfile(
            id = "dog-1",
            name = "小柴",
            type = CreatureType.DOG,
            birthTimeMillis = System.currentTimeMillis() - 120L * 24L * 60L * 60L * 1000L
        )

        val events = EventEngine().getTriggeredEvents(creature, 4)

        assertTrue(events.any { it.title.contains("狂犬") || it.title.contains("疫苗") })
    }
}
