package com.example.image_save

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CareRepositoryTest {
    private val day = 24L * 60L * 60L * 1000L

    @Test
    fun creationGivesOneInventoryForEachApplicableProduct() {
        val repository = CareRepository(InMemoryStateStore())
        val state = repository.createProfile(CareType.CAT, "小橘", 1_000L)

        assertEquals(listOf("cat_food", "cat_litter"), state.inventories.map { it.itemId })
        assertTrue(state.inventories.all { it.quantity == 1 })
    }

    @Test
    fun foregroundReconcileCreatesPendingEventWithoutSaving() {
        val repository = CareRepository(InMemoryStateStore())
        repository.createProfile(CareType.CAT, "小橘", 0L)
        val state = repository.reconcile(30 * day, autoConvertDueEvents = false)

        assertTrue(state.events.any { it.itemId == "cat_food" && it.status == PurchaseStatus.PENDING })
        assertEquals(0L, state.totalSavedCents)
    }

    @Test
    fun directConfirmationAddsConfiguredPriceAndStartsNextCycle() {
        val repository = CareRepository(InMemoryStateStore())
        repository.createProfile(CareType.CAT, "小橘", 0L)
        val pending = repository.reconcile(30 * day, false).events.first { it.itemId == "cat_food" }
        val state = repository.confirmDirect(pending.id, 30 * day)

        assertEquals(34_000L, state.totalSavedCents)
        assertTrue(state.events.none { it.id == pending.id })
        assertEquals(60 * day, state.inventories.first { it.itemId == "cat_food" }.depletedAt)
    }

    @Test
    fun backgroundReconcileCreatesOneDebtForEachMissedCycle() {
        val store = InMemoryStateStore()
        val repository = CareRepository(store)
        repository.createProfile(CareType.DOG, "小柴", 0L)
        val state = repository.reconcile(91 * day, autoConvertDueEvents = true)

        assertEquals(3, state.debts.size)
        assertEquals(255_000L, state.outstandingDebtCents)
        assertEquals(0L, state.totalSavedCents)
        assertEquals(3, state.records.count { it.type == RecordType.VIRTUAL_DEBT })
    }

    @Test
    fun debtCanBePartiallyRepaidAndPersistsAcrossStoreReload() {
        val store = InMemoryStateStore()
        val repository = CareRepository(store)
        repository.createProfile(CareType.CAT, "小橘", 0L)
        val debt = repository.reconcile(31 * day, true).debts.first { it.itemId == "cat_food" }
        val partial = repository.repay(debt.id, 12_345L, 31 * day)

        assertEquals(25_155L, partial.outstandingDebtCents)
        assertEquals(12_345L, partial.totalSavedCents)
        assertEquals(1, partial.records.count { it.type == RecordType.DEBT_REPAYMENT })
        assertEquals(partial, CareRepository(store).snapshot())
    }

    @Test
    fun pendingForegroundEventBecomesDebtWhenBackgroundAlarmRuns() {
        val repository = CareRepository(InMemoryStateStore())
        repository.createProfile(CareType.CAT, "小橘", 0L)
        repository.reconcile(30 * day, autoConvertDueEvents = false)
        val state = repository.reconcile(30 * day + 1, autoConvertDueEvents = true)

        assertTrue(state.events.single { it.itemId == "cat_food" }.status == PurchaseStatus.AUTO_DEBT)
        assertEquals(1, state.debts.count { it.itemId == "cat_food" })
        assertEquals(34_000L, state.outstandingDebtCents - 3_500L)
    }
}
