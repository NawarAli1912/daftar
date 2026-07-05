package com.daftar.app.store

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
    val salesToday: Long,
    val cashToday: Long,
    val sources: List<Source>,
    val shelf: List<Shelf>,
    val entries: List<DayEntry>,
)

class StoreRepository @Inject constructor(private val dao: StoreDao) {

    suspend fun load(): StoreSnapshot? {
        val meta = dao.meta() ?: return null // never seeded → caller shows onboarding
        return StoreSnapshot(
            seeded = meta.seeded,
            salesToday = meta.salesToday,
            cashToday = meta.cashToday,
            sources = dao.sources().map { Source(it.id, Kind.valueOf(it.kind), it.label, it.costUsd) },
            shelf = dao.shelf().map {
                Shelf(it.id, it.name, it.tasira, it.shelved, it.sold, it.counted, it.sourceId, it.buy)
            },
            entries = dao.entries().map { DayEntry(it.id, it.t, it.d, it.amt, it.cls) },
        )
    }

    suspend fun save(s: StoreSnapshot) {
        dao.replaceAll(
            meta = StoreMetaRow(0, s.seeded, s.salesToday, s.cashToday),
            sources = s.sources.mapIndexed { i, x -> SourceRow(x.id, x.kind.name, x.label, x.cost, i) },
            shelf = s.shelf.mapIndexed { i, x ->
                ShelfRow(x.id, x.name, x.tasira, x.shelved, x.sold, x.counted, x.sourceId, x.buy, i)
            },
            entries = s.entries.mapIndexed { i, x -> EntryRow(x.id, x.t, x.d, x.amt, x.cls, null, i) },
        )
    }
}
