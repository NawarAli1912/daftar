package com.daftar.app.kernel.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface StoreDao {
    @Query("SELECT * FROM store_meta WHERE id = 0") suspend fun meta(): StoreMetaRow?
    @Query("SELECT * FROM store_sources ORDER BY seq ASC") suspend fun sources(): List<SourceRow>
    @Query("SELECT * FROM store_shelf ORDER BY seq ASC") suspend fun shelf(): List<ShelfRow>
    @Query("SELECT * FROM store_entries ORDER BY seq ASC") suspend fun entries(): List<EntryRow>
    @Query("SELECT * FROM store_customers ORDER BY seq ASC") suspend fun customers(): List<CustomerRow>
    @Query("SELECT * FROM bale_expenses ORDER BY seq ASC") suspend fun baleExpenses(): List<BaleExpenseRow>

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putMeta(m: StoreMetaRow)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putSources(rows: List<SourceRow>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putShelf(rows: List<ShelfRow>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putEntries(rows: List<EntryRow>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putCustomers(rows: List<CustomerRow>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putBaleExpenses(rows: List<BaleExpenseRow>)

    @Query("DELETE FROM store_sources") suspend fun clearSources()
    @Query("DELETE FROM store_shelf") suspend fun clearShelf()
    @Query("DELETE FROM store_entries") suspend fun clearEntries()
    @Query("DELETE FROM store_customers") suspend fun clearCustomers()
    @Query("DELETE FROM bale_expenses") suspend fun clearBaleExpenses()

    // Whole-snapshot rewrite — the store data is small (tens of rows), so replacing it
    // atomically on each change is simpler and safer than diffing.
    @Transaction
    suspend fun replaceAll(
        meta: StoreMetaRow,
        sources: List<SourceRow>,
        shelf: List<ShelfRow>,
        entries: List<EntryRow>,
        customers: List<CustomerRow>,
        baleExpenses: List<BaleExpenseRow>,
    ) {
        putMeta(meta)
        clearSources(); putSources(sources)
        clearShelf(); putShelf(shelf)
        clearEntries(); putEntries(entries)
        clearCustomers(); putCustomers(customers)
        clearBaleExpenses(); putBaleExpenses(baleExpenses)
    }
}
