package com.example.image_save

import java.util.UUID

class CareRepository(private val store: StateStore) {
    private var state: AppState = store.load()

    @Synchronized fun snapshot(): AppState = state

    @Synchronized fun createProfile(type: CareType, name: String, now: Long): AppState {
        require(state.profile == null) { "只能养育一个对象" }
        require(name.isNotBlank()) { "名称不能为空" }
        val inventories = ProductCatalog.forType(type).map { InventoryState(it.id, 1, now, now + it.durationMillis) }
        state = AppState(CareProfile(type = type, name = name.trim(), createdAt = now), inventories)
        persist()
        return state
    }

    @Synchronized fun reconcile(now: Long, autoConvertDueEvents: Boolean): AppState {
        if (state.profile == null) return state
        val inventories = state.inventories.toMutableList()
        val events = state.events.toMutableList()
        val debts = state.debts.toMutableList()
        val records = state.records.toMutableList()
        for (index in inventories.indices) {
            var inventory = inventories[index]
            var guard = 0
            while (now >= inventory.depletedAt && guard++ < 1000) {
                val config = ProductCatalog.get(inventory.itemId)
                val existing = events.firstOrNull { it.itemId == inventory.itemId && it.triggeredAt == inventory.depletedAt }
                if (existing != null && existing.status == PurchaseStatus.PENDING) {
                    if (!autoConvertDueEvents) break
                    val debt = Debt(sourceEventId = existing.id, itemId = config.id, createdAt = now, originalAmountCents = existing.amountCents)
                    debts += debt
                    records += LedgerRecord(createdAt = now, itemId = config.id, itemName = config.name, amountCents = config.priceCents, type = RecordType.VIRTUAL_DEBT, debtId = debt.id)
                    events[events.indexOf(existing)] = existing.copy(status = PurchaseStatus.AUTO_DEBT)
                    inventory = inventory.copy(startedAt = inventory.depletedAt, depletedAt = inventory.depletedAt + config.durationMillis)
                    inventories[index] = inventory
                    continue
                }
                if (existing == null) {
                    val event = PurchaseEvent(itemId = inventory.itemId, triggeredAt = inventory.depletedAt, amountCents = config.priceCents, status = if (autoConvertDueEvents) PurchaseStatus.AUTO_DEBT else PurchaseStatus.PENDING)
                    events += event
                    if (autoConvertDueEvents) {
                        val debt = Debt(sourceEventId = event.id, itemId = config.id, createdAt = now, originalAmountCents = config.priceCents)
                        debts += debt
                        records += LedgerRecord(createdAt = now, itemId = config.id, itemName = config.name, amountCents = config.priceCents, type = RecordType.VIRTUAL_DEBT, debtId = debt.id)
                        inventory = inventory.copy(startedAt = inventory.depletedAt, depletedAt = inventory.depletedAt + config.durationMillis)
                        inventories[index] = inventory
                        continue
                    }
                }
                break
            }
        }
        state = state.copy(inventories = inventories, events = events, debts = debts, records = records)
        persist()
        return state
    }

    @Synchronized fun confirmDirect(eventId: String, now: Long): AppState {
        val event = state.events.firstOrNull { it.id == eventId && it.status == PurchaseStatus.PENDING } ?: return state
        val config = ProductCatalog.get(event.itemId)
        val inventory = state.inventories.first { it.itemId == event.itemId }
        val updatedInventory = inventory.copy(startedAt = event.triggeredAt, depletedAt = event.triggeredAt + config.durationMillis)
        state = state.copy(
            inventories = state.inventories.map { if (it.itemId == event.itemId) updatedInventory else it },
            events = state.events.filterNot { it.id == event.id },
            records = state.records + LedgerRecord(createdAt = now, itemId = config.id, itemName = config.name, amountCents = config.priceCents, type = RecordType.DIRECT_SAVING)
        )
        persist()
        return state
    }

    @Synchronized fun repay(debtId: String, amountCents: Long, now: Long): AppState {
        require(amountCents > 0) { "偿还金额必须大于 0" }
        val debt = state.debts.firstOrNull { it.id == debtId } ?: error("借贷不存在")
        require(amountCents <= debt.remainingAmountCents) { "偿还金额不能超过剩余借贷" }
        val updated = debt.copy(repaidAmountCents = debt.repaidAmountCents + amountCents)
        val config = ProductCatalog.get(debt.itemId)
        state = state.copy(
            debts = state.debts.map { if (it.id == debtId) updated else it },
            records = state.records + LedgerRecord(createdAt = now, itemId = config.id, itemName = config.name, amountCents = amountCents, type = RecordType.DEBT_REPAYMENT, debtId = debtId)
        )
        persist()
        return state
    }

    private fun persist() = store.save(state)
}
