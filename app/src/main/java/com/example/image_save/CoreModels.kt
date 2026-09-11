package com.example.image_save

import java.util.UUID

enum class CareType(val label: String, val background: Int) {
    BABY("婴儿", R.drawable.livingroom),
    CAT("猫", R.drawable.livingroom),
    DOG("狗", R.drawable.livingroom)
}

enum class PurchaseStatus { PENDING, AUTO_DEBT }

enum class RecordType(val label: String) {
    DIRECT_SAVING("直接存钱"),
    VIRTUAL_DEBT("虚拟借贷"),
    DEBT_REPAYMENT("借贷偿还")
}

data class ProductConfig(
    val id: String,
    val careType: CareType,
    val name: String,
    val unit: String,
    val durationMillis: Long,
    val priceCents: Long
)

data class CareProfile(
    val id: String = UUID.randomUUID().toString(),
    val type: CareType,
    val name: String,
    val createdAt: Long
)

data class InventoryState(
    val itemId: String,
    val quantity: Int,
    val startedAt: Long,
    val depletedAt: Long
)

data class PurchaseEvent(
    val id: String = UUID.randomUUID().toString(),
    val itemId: String,
    val triggeredAt: Long,
    val amountCents: Long,
    val status: PurchaseStatus
)

data class Debt(
    val id: String = UUID.randomUUID().toString(),
    val sourceEventId: String,
    val itemId: String,
    val createdAt: Long,
    val originalAmountCents: Long,
    val repaidAmountCents: Long = 0
) {
    val remainingAmountCents: Long get() = originalAmountCents - repaidAmountCents
}

data class LedgerRecord(
    val id: String = UUID.randomUUID().toString(),
    val createdAt: Long,
    val itemId: String,
    val itemName: String,
    val amountCents: Long,
    val type: RecordType,
    val debtId: String? = null
)

data class AppState(
    val profile: CareProfile? = null,
    val inventories: List<InventoryState> = emptyList(),
    val events: List<PurchaseEvent> = emptyList(),
    val debts: List<Debt> = emptyList(),
    val records: List<LedgerRecord> = emptyList()
) {
    val totalSavedCents: Long
        get() = records.filter { it.type == RecordType.DIRECT_SAVING || it.type == RecordType.DEBT_REPAYMENT }
            .sumOf { it.amountCents }
    val outstandingDebtCents: Long get() = debts.sumOf { it.remainingAmountCents }
}
