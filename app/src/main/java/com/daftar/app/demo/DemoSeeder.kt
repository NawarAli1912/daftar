package com.daftar.app.demo

import com.daftar.app.kernel.db.CustomerDao
import com.daftar.app.kernel.db.CustomerEntity
import com.daftar.app.kernel.db.DaftarDatabase
import com.daftar.app.kernel.db.IntakeLineEntity
import com.daftar.app.kernel.db.ItemTypeDao
import com.daftar.app.kernel.db.ItemTypeEntity
import com.daftar.app.kernel.db.LedgerDao
import com.daftar.app.kernel.db.LedgerEntryEntity
import com.daftar.app.kernel.db.ReminderDao
import com.daftar.app.kernel.db.ReminderEntity
import com.daftar.app.kernel.db.SaleDao
import com.daftar.app.kernel.db.SaleEntity
import com.daftar.app.kernel.db.SaleLineEntity
import com.daftar.app.kernel.db.StockDao
import com.daftar.app.kernel.db.StockSourceEntity
import com.daftar.app.kernel.ledger.EntryKind
import com.daftar.app.kernel.ledger.SourceKind
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// D42/D44 demo: one tap fills the app with realistic data so the UX can be judged full.
class DemoSeeder @Inject constructor(
    private val db: DaftarDatabase,
    private val customerDao: CustomerDao,
    private val ledgerDao: LedgerDao,
    private val reminderDao: ReminderDao,
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

        // Reminders: سميرة overdue, هدى due soon; فاطمة falls through to the default (1st of next month)
        val todayDate = LocalDate.now()
        reminderDao.insert(ReminderEntity(uid(), samira.id, todayDate.minusDays(3).toEpochDay(), now, now))
        reminderDao.insert(ReminderEntity(uid(), huda.id, todayDate.plusDays(2).toEpochDay(), now, now))

        // A full book across the last three days plus today — flip back through it
        // from the day-book's ‹ › navigation. Balances land at: سميرة 11k, هدى 11k, فاطمة 8k.

        // ── 3 days ago ──
        sale(huda.id, at(3, 10), paid = 10_000, listOf(Line(jacket, 1, 10_000, 10_000, bale.id)))
        entry(EntryKind.PAYMENT, samira.id, 3_000, at(3, 12))
        sale(null, at(3, 16), paid = 16_000, listOf(Line(dress, 2, 8_000, 8_000, store.id)))

        // ── 2 days ago ──
        sale(fatima.id, at(2, 11), paid = 4_000, listOf(Line(pants, 2, 7_500, 7_000, bale.id)))
        sale(null, at(2, 15), paid = 7_500, listOf(Line(pants, 1, 7_500, 7_500, bale.id)))

        // ── yesterday ──
        entry(EntryKind.PAYMENT, fatima.id, 5_000, at(1, 10))
        sale(samira.id, at(1, 14), paid = 6_000, listOf(Line(jacket, 1, 10_000, 10_000, bale.id)))

        // ── today ──
        sale(
            huda.id, at(0, 12), paid = 10_000,
            listOf(
                Line(pants, 2, 7_500, 6_500, bale.id),
                Line(dress, 1, 8_000, 8_000, store.id),
            ),
        )
        entry(EntryKind.PAYMENT, samira.id, 5_000, at(0, 13))
        entry(EntryKind.RETURN, fatima.id, 3_000, at(0, 14))
    }

    private data class Line(
        val type: ItemTypeEntity,
        val qty: Int,
        val asked: Long,
        val agreed: Long,
        val sourceId: String,
    )

    private fun at(daysAgo: Int, hour: Int): Long =
        LocalDate.now().minusDays(daysAgo.toLong())
            .atTime(hour, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private suspend fun sale(customerId: String?, time: Long, paid: Long, lines: List<Line>) {
        val saleId = uid()
        saleDao.insertSale(SaleEntity(saleId, customerId, time, time))
        saleDao.insertLines(
            lines.map {
                SaleLineEntity(
                    uid(), saleId, it.type.id, it.type.name, it.qty, it.asked, it.agreed, time,
                    attributedSourceId = it.sourceId,
                )
            }
        )
        val total = lines.sumOf { it.qty * it.agreed }
        ledgerDao.insert(LedgerEntryEntity(uid(), EntryKind.SALE.name, customerId, total, time, time, saleId = saleId))
        if (paid > 0) {
            ledgerDao.insert(LedgerEntryEntity(uid(), EntryKind.PAYMENT.name, customerId, paid, time, time, saleId = saleId))
        }
    }

    private suspend fun entry(kind: EntryKind, customerId: String?, amount: Long, time: Long) {
        ledgerDao.insert(LedgerEntryEntity(uid(), kind.name, customerId, amount, time, time))
    }
}
