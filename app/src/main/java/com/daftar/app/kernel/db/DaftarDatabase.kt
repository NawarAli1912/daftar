package com.daftar.app.kernel.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [CustomerEntity::class, LedgerEntryEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class DaftarDatabase : RoomDatabase() {
    abstract fun customerDao(): CustomerDao
    abstract fun ledgerDao(): LedgerDao
}
