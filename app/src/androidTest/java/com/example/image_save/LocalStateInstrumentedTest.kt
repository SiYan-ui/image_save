package com.example.image_save

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LocalStateInstrumentedTest {
    @Test
    fun profileAndLedgerSurviveRepositoryReload() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("care_save_state", Context.MODE_PRIVATE).edit().clear().commit()
        val first = CareRepository(PreferencesStateStore(context))
        first.createProfile(CareType.CAT, "测试猫", 0L)
        val pending = first.reconcile(30L * 24L * 60L * 60L * 1000L, false).events.first { it.itemId == "cat_food" }
        first.confirmDirect(pending.id, 30L * 24L * 60L * 60L * 1000L)

        val reloaded = CareRepository(PreferencesStateStore(context)).snapshot()
        assertEquals("测试猫", reloaded.profile?.name)
        assertEquals(34_000L, reloaded.totalSavedCents)
        assertTrue(reloaded.records.any { it.type == RecordType.DIRECT_SAVING })
    }
}
