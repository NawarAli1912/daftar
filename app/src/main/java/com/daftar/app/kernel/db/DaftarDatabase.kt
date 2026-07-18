package com.daftar.app.kernel.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        SourceRow::class,
        ShelfRow::class,
        EntryRow::class,
        StoreMetaRow::class,
        CustomerRow::class,
        BaleExpenseRow::class,
    ],
    version = 21,
    exportSchema = true,
)
abstract class DaftarDatabase : RoomDatabase() {
    abstract fun storeDao(): StoreDao
}
