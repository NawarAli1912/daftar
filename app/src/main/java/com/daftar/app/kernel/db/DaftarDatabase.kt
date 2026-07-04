package com.daftar.app.kernel.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        CustomerEntity::class,
        LedgerEntryEntity::class,
        ReminderEntity::class,
        ItemTypeEntity::class,
        SaleEntity::class,
        SaleLineEntity::class,
        StockSourceEntity::class,
        IntakeLineEntity::class,
    ],
    version = 6,
    exportSchema = true,
)
abstract class DaftarDatabase : RoomDatabase() {
    abstract fun customerDao(): CustomerDao
    abstract fun ledgerDao(): LedgerDao
    abstract fun reminderDao(): ReminderDao
    abstract fun itemTypeDao(): ItemTypeDao
    abstract fun saleDao(): SaleDao
    abstract fun stockDao(): StockDao
}
