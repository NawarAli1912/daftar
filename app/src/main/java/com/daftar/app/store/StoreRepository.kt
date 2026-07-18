package com.daftar.app.store

import com.daftar.app.kernel.db.BaleExpenseRow
import com.daftar.app.kernel.db.CustomerRow
import com.daftar.app.kernel.db.EntryRow
import com.daftar.app.kernel.db.ShelfRow
import com.daftar.app.kernel.db.SourceRow
import com.daftar.app.kernel.db.StoreDao
import com.daftar.app.kernel.db.StoreMetaRow
import javax.inject.Inject

// The persistable slice of StoreState (everything that must survive a restart —
// not the transient sheet/stepper UI state).
data class StoreSnapshot(
    val seeded: Boolean,
    val usdRate: Long,
    val sources: List<Source>,
    val shelf: List<Shelf>,
    val entries: List<DayEntry>,
    val customers: List<Customer>,
    val expenses: List<BaleExpense> = emptyList(),
)

class StoreRepository @Inject constructor(private val dao: StoreDao) {

    suspend fun load(): StoreSnapshot? {
        val meta = dao.meta() ?: return null // never seeded → caller shows onboarding
        return StoreSnapshot(
            seeded = meta.seeded,
            usdRate = meta.usdRate,
            sources = dao.sources().map { Source(it.id, Kind.valueOf(it.kind), it.label, it.costUsd, it.debt, it.countTotal, it.ratePurchase) },
            shelf = dao.shelf().map {
                Shelf(it.id, it.name, it.tasira, it.shelved, it.sold, it.counted, it.sourceId, it.buy)
            },
            entries = dao.entries().map {
                DayEntry(it.id, it.t, it.d, it.amt, it.cls, it.customerId, it.debtDelta, it.day, it.saleAmount, it.cashAmount, it.stockDelta, it.trialAmount, it.lines, it.sourceId, it.moneyOut, it.voided)
            },
            customers = dao.customers().map { Customer(it.id, it.name, it.phone, it.openingDebt, it.dueEpochDay) },
            expenses = dao.baleExpenses().map { BaleExpense(it.id, it.sourceId, it.label, it.amount) },
        )
    }

    // For the daily reminder digest (runs off the UI) — who owes and is due/overdue today.
    suspend fun loadDueDebtors(today: Long): List<Debtor> =
        load()?.let { dueDebtors(it.customers, it.entries, today) } ?: emptyList()

    suspend fun save(s: StoreSnapshot) {
        dao.replaceAll(
            meta = StoreMetaRow(0, s.seeded, s.usdRate),
            sources = s.sources.mapIndexed { i, x -> SourceRow(x.id, x.kind.name, x.label, x.cost, x.debt, x.countTotal, x.ratePurchase, i) },
            shelf = s.shelf.mapIndexed { i, x ->
                ShelfRow(x.id, x.name, x.tasira, x.shelved, x.sold, x.counted, x.sourceId, x.buy, i)
            },
            entries = s.entries.mapIndexed { i, x ->
                EntryRow(x.id, x.t, x.d, x.amt, x.cls, x.customerId, x.debtDelta, x.day, x.saleAmount, x.cashAmount, x.stockDelta, x.trialAmount, x.lines, x.sourceId, x.moneyOut, x.voided, i)
            },
            customers = s.customers.mapIndexed { i, x -> CustomerRow(x.id, x.name, x.phone, x.openingDebt, x.dueEpochDay, i) },
            baleExpenses = s.expenses.mapIndexed { i, x -> BaleExpenseRow(x.id, x.sourceId, x.label, x.amount, i) },
        )
    }
}
