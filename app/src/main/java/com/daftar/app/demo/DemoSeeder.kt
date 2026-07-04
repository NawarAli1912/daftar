package com.daftar.app.demo

import com.daftar.app.kernel.db.CustomerDao
import com.daftar.app.kernel.db.CustomerEntity
import com.daftar.app.kernel.db.DaftarDatabase
import com.daftar.app.kernel.db.IntakeLineEntity
import com.daftar.app.kernel.db.ItemTypeDao
import com.daftar.app.kernel.db.ItemTypeEntity
import com.daftar.app.kernel.db.LedgerDao
import com.daftar.app.kernel.db.LedgerEntryEntity
import com.daftar.app.kernel.db.SaleDao
import com.daftar.app.kernel.db.SaleEntity
import com.daftar.app.kernel.db.SaleLineEntity
import com.daftar.app.kernel.db.StockDao
import com.daftar.app.kernel.db.StockSourceEntity
import com.daftar.app.kernel.ledger.EntryKind
import com.daftar.app.kernel.ledger.SourceKind
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// D42/D44 demo: one tap fills the app with realistic data so the UX can be judged full.
class DemoSeeder @Inject constructor(
    private val db: DaftarDatabase,
    private val customerDao: CustomerDao,
    private val ledgerDao: LedgerDao,
    private val itemTypeDao: ItemTypeDao,
    private val saleDao: SaleDao,
    private val stockDao: StockDao,
) {
    private fun uid() = UUID.randomUUID().toString()

    // clearAllTables() is a blocking Room call — must not run on the main thread.
    suspend fun clear() = withContext(Dispatchers.IO) { db.clearAllTables() }

    suspend fun load() {
        withContext(Dispatchers.IO) { db.clearAllTables() }
        val now = System.currentTimeMillis()
        val today = now - 3 * 60 * 60 * 1000

        // Types
        val dress = ItemTypeEntity(uid(), "فستان", 8_000, now, now)
        val pants = ItemTypeEntity(uid(), "بنطال", 7_500, now, now)
        val jacket = ItemTypeEntity(uid(), "جاكيت", 10_000, now, now)
        listOf(dress, pants, jacket).forEach { itemTypeDao.insert(it) }

        // Sources: a February bale + the store's existing clothes
        val bale = StockSourceEntity(uid(), SourceKind.BALE.name, "بالة شباط", 400, 5_200_000, now - 90L * 86_400_000, now)
        val store = StockSourceEntity(uid(), SourceKind.PRE_APP.name, "بضاعة المحل", null, null, now, now)
        stockDao.insertSource(bale)
        stockDao.insertSource(store)
        stockDao.insertIntakeLine(IntakeLineEntity(uid(), bale.id, pants.id, "بنطال", 40, 3_000, 7_500, now))
        stockDao.insertIntakeLine(IntakeLineEntity(uid(), bale.id, jacket.id, "جاكيت", 25, 4_500, 10_000, now))
        stockDao.insertIntakeLine(IntakeLineEntity(uid(), store.id, dress.id, "فستان", 15, null, 8_000, now))

        // Customers with opening debts
        val samira = CustomerEntity(uid(), "سميرة", null, now, now)
        val huda = CustomerEntity(uid(), "هدى", "0912345678", now, now)
        val fatima = CustomerEntity(uid(), "فاطمة", null, now, now)
        listOf(samira, huda, fatima).forEach { customerDao.insert(it) }
        ledgerDao.insert(LedgerEntryEntity(uid(), EntryKind.OPENING_BALANCE.name, samira.id, 15_000, now - 20L * 86_400_000, now))
        ledgerDao.insert(LedgerEntryEntity(uid(), EntryKind.OPENING_BALANCE.name, fatima.id, 6_000, now - 10L * 86_400_000, now))

        // Today's book: a sale (partly paid), a standalone payment, a return
        val saleId = uid()
        saleDao.insertSale(SaleEntity(saleId, huda.id, today, today))
        saleDao.insertLines(listOf(
            SaleLineEntity(uid(), saleId, pants.id, "بنطال", 2, 7_500, 6_500, today, attributedSourceId = bale.id),
            SaleLineEntity(uid(), saleId, dress.id, "فستان", 1, 8_000, 8_000, today, attributedSourceId = store.id),
        ))
        ledgerDao.insert(LedgerEntryEntity(uid(), EntryKind.SALE.name, huda.id, 21_000, today, today, saleId = saleId))
        ledgerDao.insert(LedgerEntryEntity(uid(), EntryKind.PAYMENT.name, huda.id, 10_000, today, today, saleId = saleId))
        ledgerDao.insert(LedgerEntryEntity(uid(), EntryKind.PAYMENT.name, samira.id, 5_000, today + 600_000, today + 600_000))
        ledgerDao.insert(LedgerEntryEntity(uid(), EntryKind.RETURN.name, fatima.id, 3_000, today + 1_200_000, today + 1_200_000))
    }
}
