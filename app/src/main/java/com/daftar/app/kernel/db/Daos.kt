package com.daftar.app.kernel.db

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
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

@Dao
interface ItemTypeDao {

    @Insert
    suspend fun insert(type: ItemTypeEntity)

    @Query("SELECT * FROM item_types WHERE voided = 0 ORDER BY name")
    fun observeAll(): Flow<List<ItemTypeEntity>>
}

@Dao
interface StockDao {

    @Insert
    suspend fun insertSource(source: StockSourceEntity)

    @Insert
    suspend fun insertIntakeLine(line: IntakeLineEntity)

    @Query("SELECT * FROM stock_sources WHERE voided = 0 ORDER BY arrivedAt DESC")
    fun observeSources(): Flow<List<StockSourceEntity>>

    @Query("SELECT * FROM stock_sources WHERE kind = :kind AND voided = 0 LIMIT 1")
    suspend fun findByKind(kind: String): StockSourceEntity?

    @Query("SELECT * FROM intake_lines WHERE voided = 0")
    fun observeIntakeLines(): Flow<List<IntakeLineEntity>>
}

data class SaleWithLines(
    @Embedded val sale: SaleEntity,
    @Relation(parentColumn = "id", entityColumn = "saleId")
    val lines: List<SaleLineEntity>,
)

@Dao
interface SaleDao {

    @Query("SELECT * FROM sale_lines WHERE voided = 0")
    fun observeAllLines(): Flow<List<SaleLineEntity>>

    @Insert
    suspend fun insertSale(sale: SaleEntity)

    @Insert
    suspend fun insertLines(lines: List<SaleLineEntity>)

    @Transaction
    @Query(
        "SELECT * FROM sales WHERE voided = 0 " +
            "AND happenedAt >= :dayStart AND happenedAt < :dayEnd ORDER BY happenedAt DESC"
    )
    fun observeDay(dayStart: Long, dayEnd: Long): Flow<List<SaleWithLines>>

    @Query("UPDATE sales SET voided = 1, updatedAt = :now WHERE id = :saleId")
    suspend fun voidSaleRow(saleId: String, now: Long)

    @Query("UPDATE sale_lines SET voided = 1, updatedAt = :now WHERE saleId = :saleId")
    suspend fun voidSaleLines(saleId: String, now: Long)

    @Query("UPDATE ledger_entries SET voided = 1, updatedAt = :now WHERE saleId = :saleId")
    suspend fun voidSaleLedger(saleId: String, now: Long)

    @Transaction
    suspend fun voidSaleCascade(saleId: String, now: Long) {
        voidSaleRow(saleId, now)
        voidSaleLines(saleId, now)
        voidSaleLedger(saleId, now)
    }
}
