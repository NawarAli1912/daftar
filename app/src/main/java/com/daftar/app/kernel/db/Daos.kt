package com.daftar.app.kernel.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {

    @Insert
    suspend fun insert(customer: CustomerEntity)

    @Query("SELECT * FROM customers WHERE voided = 0 ORDER BY name")
    fun observeAll(): Flow<List<CustomerEntity>>
}

@Dao
interface LedgerDao {

    @Insert
    suspend fun insert(entry: LedgerEntryEntity)

    @Query("UPDATE ledger_entries SET voided = 1, updatedAt = :now WHERE id = :id")
    suspend fun voidEntry(id: String, now: Long)

    @Query("SELECT * FROM ledger_entries WHERE voided = 0")
    fun observeAll(): Flow<List<LedgerEntryEntity>>

    @Query(
        "SELECT * FROM ledger_entries WHERE voided = 0 " +
            "AND happenedAt >= :dayStart AND happenedAt < :dayEnd ORDER BY happenedAt DESC"
    )
    fun observeDay(dayStart: Long, dayEnd: Long): Flow<List<LedgerEntryEntity>>
}
