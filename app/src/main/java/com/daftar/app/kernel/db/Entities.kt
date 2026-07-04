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
)
