package com.daftar.app.kernel.db

import androidx.room.Entity
import androidx.room.PrimaryKey

// D52 store slice persistence. Each list row carries `seq` = its index in the
// in-memory list, so load order (and "newest first" for the day book) is preserved.

@Entity(tableName = "store_sources")
data class SourceRow(
    @PrimaryKey val id: String,
    val kind: String,
    val label: String,
    val costUsd: Long?,
    val debt: Long = 0, // supplier credit — MARKET shops only
    val seq: Int,
)

@Entity(tableName = "store_shelf")
data class ShelfRow(
    @PrimaryKey val id: String,
    val name: String,
    val tasira: Long,
    val shelved: Int,
    val sold: Int,
    val counted: Int?,
    val sourceId: String?,
    val buy: Long?,
    val seq: Int,
)

@Entity(tableName = "store_entries")
data class EntryRow(
    @PrimaryKey val id: String,
    val t: String,
    val d: String,
    val amt: String,
    val cls: String,
    val customerId: String?,
    val debtDelta: Long,
    val day: Long,
    val saleAmount: Long,
    val cashAmount: Long,
    val stockDelta: String,
    val trialAmount: Long,
    val lines: String,
    // D68 supplier payments (v16): the shop paid and the money-out amount
    val sourceId: String? = null,
    val moneyOut: Long = 0,
    val seq: Int,
)

@Entity(tableName = "store_customers")
data class CustomerRow(
    @PrimaryKey val id: String,
    val name: String,
    val phone: String?,
    val openingDebt: Long,
    val dueEpochDay: Long?,
    val seq: Int,
)

// Single-row app meta. Day totals are derived from the entries now, not stored.
@Entity(tableName = "store_meta")
data class StoreMetaRow(
    @PrimaryKey val id: Int = 0,
    val seeded: Boolean,
    val usdRate: Long = 1500,
)
