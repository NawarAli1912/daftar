package com.daftar.app.kernel.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey val id: String,
    val name: String,
    val phone: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val voided: Boolean = false,
)

@Entity(
    tableName = "ledger_entries",
    indices = [Index("customerId"), Index("happenedAt"), Index("updatedAt"), Index("saleId")],
)
data class LedgerEntryEntity(
    @PrimaryKey val id: String,
    val kind: String,
    val customerId: String?,
    val amount: Long,
    val happenedAt: Long,
    val updatedAt: Long,
    val voided: Boolean = false,
    val saleId: String? = null,
    // D37: optional type tag on payments; attribution stored per D36
    val itemTypeId: String? = null,
    val askedUnit: Long? = null,
    val attributedSourceId: String? = null,
)

// Reminders (Flow 2): one active row per customer holds when to next chase the debt.
// dueEpochDay is a date (LocalDate.toEpochDay), not a timestamp — reminders are day-grained.
@Entity(tableName = "reminders", indices = [Index("customerId"), Index("updatedAt")])
data class ReminderEntity(
    @PrimaryKey val id: String,
    val customerId: String,
    val dueEpochDay: Long,
    val createdAt: Long,
    val updatedAt: Long,
    val voided: Boolean = false,
)

@Entity(tableName = "item_types", indices = [Index(value = ["name"], unique = true)])
data class ItemTypeEntity(
    @PrimaryKey val id: String,
    val name: String,
    val askingPrice: Long,
    val createdAt: Long,
    val updatedAt: Long,
    val voided: Boolean = false,
)

@Entity(tableName = "sales", indices = [Index("customerId"), Index("happenedAt"), Index("updatedAt")])
data class SaleEntity(
    @PrimaryKey val id: String,
    val customerId: String?,
    val happenedAt: Long,
    val updatedAt: Long,
    val voided: Boolean = false,
)

@Entity(tableName = "stock_sources", indices = [Index("updatedAt")])
data class StockSourceEntity(
    @PrimaryKey val id: String,
    val kind: String,
    val label: String,
    val costUsd: Long?,
    val costLocal: Long?,
    val arrivedAt: Long,
    val updatedAt: Long,
    val voided: Boolean = false,
)

@Entity(tableName = "intake_lines", indices = [Index("sourceId"), Index("updatedAt")])
data class IntakeLineEntity(
    @PrimaryKey val id: String,
    val sourceId: String,
    val itemTypeId: String,
    val typeName: String,
    val qty: Int,
    val unitCost: Long?,
    val pricePoint: Long,
    val updatedAt: Long,
    val voided: Boolean = false,
)

@Entity(tableName = "sale_lines", indices = [Index("saleId"), Index("updatedAt")])
data class SaleLineEntity(
    @PrimaryKey val id: String,
    val saleId: String,
    val itemTypeId: String,
    val typeName: String,
    val qty: Int,
    val askedUnit: Long,
    val agreedUnit: Long,
    val updatedAt: Long,
    val voided: Boolean = false,
    // D36: attribution computed once at save, stored; null = غير محدد
    val attributedSourceId: String? = null,
)
