package com.example.image_save

import android.content.Context
import android.util.Base64
import java.nio.charset.StandardCharsets

interface StateStore {
    fun load(): AppState
    fun save(state: AppState)
}

class InMemoryStateStore(initial: AppState = AppState()) : StateStore {
    private var value = initial
    override fun load(): AppState = value
    override fun save(state: AppState) { value = state }
}

class PreferencesStateStore(context: Context) : StateStore {
    private val preferences = context.getSharedPreferences("care_save_state", Context.MODE_PRIVATE)

    override fun load(): AppState {
        val encoded = preferences.getString(KEY_STATE, null) ?: return AppState()
        return runCatching {
            StateCodec.decode(String(Base64.decode(encoded, Base64.NO_WRAP), StandardCharsets.UTF_8))
        }.getOrElse { AppState() }
    }

    override fun save(state: AppState) {
        val encoded = Base64.encodeToString(StateCodec.encode(state).toByteArray(StandardCharsets.UTF_8), Base64.NO_WRAP)
        preferences.edit().putString(KEY_STATE, encoded).apply()
    }

    private companion object { const val KEY_STATE = "state" }
}

private object StateCodec {
    private const val SEP = "|"

    fun encode(state: AppState): String = buildString {
        append("V1\n")
        state.profile?.let { append("P$SEP${it.id}$SEP${it.type.name}$SEP${b(it.name)}$SEP${it.createdAt}\n") }
        state.inventories.forEach { append("I$SEP${it.itemId}$SEP${it.quantity}$SEP${it.startedAt}$SEP${it.depletedAt}\n") }
        state.events.forEach { append("E$SEP${it.id}$SEP${it.itemId}$SEP${it.triggeredAt}$SEP${it.amountCents}$SEP${it.status.name}\n") }
        state.debts.forEach { append("D$SEP${it.id}$SEP${it.sourceEventId}$SEP${it.itemId}$SEP${it.createdAt}$SEP${it.originalAmountCents}$SEP${it.repaidAmountCents}\n") }
        state.records.forEach { append("R$SEP${it.id}$SEP${it.createdAt}$SEP${it.itemId}$SEP${b(it.itemName)}$SEP${it.amountCents}$SEP${it.type.name}$SEP${it.debtId.orEmpty()}\n") }
    }

    fun decode(value: String): AppState {
        var profile: CareProfile? = null
        val inventories = mutableListOf<InventoryState>()
        val events = mutableListOf<PurchaseEvent>()
        val debts = mutableListOf<Debt>()
        val records = mutableListOf<LedgerRecord>()
        value.lineSequence().drop(1).filter { it.isNotBlank() }.forEach { line ->
            val p = line.split(SEP)
            when (p.firstOrNull()) {
                "P" -> profile = CareProfile(p[1], CareType.valueOf(p[2]), ub(p[3]), p[4].toLong())
                "I" -> inventories += InventoryState(p[1], p[2].toInt(), p[3].toLong(), p[4].toLong())
                "E" -> events += PurchaseEvent(p[1], p[2], p[3].toLong(), p[4].toLong(), PurchaseStatus.valueOf(p[5]))
                "D" -> debts += Debt(p[1], p[2], p[3], p[4].toLong(), p[5].toLong(), p[6].toLong())
                "R" -> records += LedgerRecord(p[1], p[2].toLong(), p[3], ub(p[4]), p[5].toLong(), RecordType.valueOf(p[6]), p.getOrNull(7)?.ifBlank { null })
            }
        }
        return AppState(profile, inventories, events, debts, records)
    }

    private fun b(value: String): String = Base64.encodeToString(value.toByteArray(StandardCharsets.UTF_8), Base64.NO_WRAP)
    private fun ub(value: String): String = String(Base64.decode(value, Base64.NO_WRAP), StandardCharsets.UTF_8)
}
