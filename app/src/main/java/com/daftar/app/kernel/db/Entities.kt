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
    indices = [Index("customerId"), Index("happenedAt"), Index("updatedAt")],
)
data class LedgerEntryEntity(
    @PrimaryKey val id: String,
    val kind: String,
    val customerId: String?,
    val amount: Long,
    val happenedAt: Long,
    val updatedAt: Long,
    val voided: Boolean = false,
)
