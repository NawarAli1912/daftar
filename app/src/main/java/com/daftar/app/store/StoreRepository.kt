package com.daftar.app.store

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
)

class StoreRepository @Inject constructor(private val dao: StoreDao) {

    suspend fun load(): StoreSnapshot? {
        val meta = dao.meta() ?: return null // never seeded → caller shows onboarding
        return StoreSnapshot(
            seeded = meta.seeded,
            usdRate = meta.usdRate,
            sources = dao.sources().map { Source(it.id, Kind.valueOf(it.kind), it.label, it.costUsd) },
            shelf = dao.shelf().map {
                Shelf(it.id, it.name, it.tasira, it.shelved, it.sold, it.counted, it.sourceId, it.buy)
            },
            entries = dao.entries().map {
                DayEntry(it.id, it.t, it.d, it.amt, it.cls, it.customerId, it.debtDelta, it.day, it.saleAmount, it.cashAmount, it.stockDelta, it.trialAmount)
            },
            customers = dao.customers().map { Customer(it.id, it.name, it.phone, it.openingDebt) },
        )
    }

    // For the daily reminder digest (runs off the UI) — who owes, largest first.
    suspend fun loadDebtors(): List<Debtor> =
        load()?.let { debtors(it.customers, it.entries) } ?: emptyList()

    suspend fun save(s: StoreSnapshot) {
        dao.replaceAll(
            meta = StoreMetaRow(0, s.seeded, s.usdRate),
            sources = s.sources.mapIndexed { i, x -> SourceRow(x.id, x.kind.name, x.label, x.cost, i) },
            shelf = s.shelf.mapIndexed { i, x ->
                ShelfRow(x.id, x.name, x.tasira, x.shelved, x.sold, x.counted, x.sourceId, x.buy, i)
            },
            entries = s.entries.mapIndexed { i, x ->
                EntryRow(x.id, x.t, x.d, x.amt, x.cls, x.customerId, x.debtDelta, x.day, x.saleAmount, x.cashAmount, x.stockDelta, x.trialAmount, i)
            },
            customers = s.customers.mapIndexed { i, x -> CustomerRow(x.id, x.name, x.phone, x.openingDebt, i) },
        )
    }
}
