package com.daftar.app.store

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// Undo payload — snapshot of each shelf item's `sold` before the entry, so ↺ تراجع restores it.
data class Undo(val id: String, val dS: Long, val dC: Long, val before: List<Pair<String, Int>>)

// The whole app state, mirroring the prototype Component.state 1:1.
data class StoreState(
    val seeded: Boolean = false,
    val onb: Int = 0,
    val setupList: List<SetupPick> = emptyList(),
    val tab: String = "today",          // today | cust | appts | account
    val accountSeg: String = "shelf",   // shelf | sources | sum
    val shelfFilter: String = "all",    // all | unspec
    val screen: String = "home",        // home | chooser | sale | pay | addsrc | additem | package
    val sources: List<Source> = initialSources(),
    val shelf: List<Shelf> = emptyList(),
    val entries: List<DayEntry> = emptyList(),
    val salesToday: Long = 0,
    val cashToday: Long = 0,
    // sale
    val lines: List<SaleLine> = emptyList(),
    val pay: String = "full",           // full | partial | trial
    val undo: Undo? = null,
    val addNewOpen: Boolean = false,
    val newName: String = "",
    val newPrice: Long = 5_000,
    // specify / add-source / add-item / package / payment transient state
    val specifyId: String? = null,
    val addSrcKind: Kind? = null,
    val newCost: Long = 400,
    val aiName: String = "",
    val aiTasira: Long = 5_000,
    val aiCount: Int = 1,
    val aiSource: String = PRE_ID,
    val aiBuy: Long = 3_000,
    val pkgId: String? = null,
    val pkgAddOpen: Boolean = false,
    val payAmount: Long = 5_000,
    val payTypeId: String? = null,
)

@HiltViewModel
class StoreViewModel @Inject constructor(
    private val repo: StoreRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(StoreState())
    val state: StateFlow<StoreState> = _state.asStateFlow()

    private val s get() = _state.value
    private fun set(f: (StoreState) -> StoreState) = _state.update(f)

    init {
        // Load the persisted ledger, then keep the DB in step with the persistable
        // slice of state (transient sheet/stepper changes don't touch disk).
        viewModelScope.launch {
            repo.load()?.let { snap ->
                _state.update {
                    it.copy(
                        seeded = snap.seeded, salesToday = snap.salesToday, cashToday = snap.cashToday,
                        sources = snap.sources, shelf = snap.shelf, entries = snap.entries,
                    )
                }
            }
            state.map {
                StoreSnapshot(it.seeded, it.salesToday, it.cashToday, it.sources, it.shelf, it.entries)
            }.distinctUntilChanged().drop(1).collect { repo.save(it) }
        }
    }

    private fun srcLabel(id: String?): String =
        s.sources.find { it.id == id }?.label ?: "غير محدد"

    fun sourceLabelFor(id: String?): String = srcLabel(id)

    private fun total(): Long = s.lines.sumOf { it.price * it.qty }

    private var undoJob: Job? = null
    private fun armUndo(u: Undo) {
        undoJob?.cancel()
        undoJob = viewModelScope.launch {
            delay(10_000)
            set { if (it.undo?.id == u.id) it.copy(undo = null) else it }
        }
    }

    // ── onboarding ──
    fun onbNext() = set { it.copy(onb = it.onb + 1) }
    fun skipOnb() = set { it.copy(onb = 3) }
    fun setupAdd(name: String, price: Long) = set {
        if (it.setupList.any { p -> p.name == name }) it
        else it.copy(setupList = it.setupList + SetupPick(name, price, 10))
    }
    fun enterApp() = set {
        val items = it.setupList.mapIndexed { i, x ->
            Shelf("u$i", x.name, x.price, shelved = x.qty, sold = 0, sourceId = PRE_ID)
        }
        it.copy(
            seeded = true, shelf = items,
            tab = if (items.isNotEmpty()) "account" else "today", accountSeg = "shelf",
        )
    }
    fun loadSample() = set {
        it.copy(
            seeded = true, sources = sampleSources(), shelf = sampleShelf(),
            entries = sampleEntries(), salesToday = 68_500, cashToday = 54_000,
            tab = "today", screen = "home",
        )
    }
    fun resetApp() = set {
        StoreState(sources = listOf(Source(PRE_ID, Kind.PRE_APP, "قبل التطبيق", null)))
    }

    // ── nav ──
    fun setTab(tab: String) = set { it.copy(tab = tab, screen = "home") }
    fun setSeg(seg: String) = set { it.copy(accountSeg = seg) }
    fun setFilter(f: String) = set { it.copy(shelfFilter = f) }

    // ── shelf edits ──
    fun tasiraStep(id: String, d: Int) = set {
        it.copy(shelf = it.shelf.map { x -> if (x.id == id) x.copy(tasira = maxOf(0, x.tasira + d * 500)) else x })
    }
    fun onhandStep(id: String, d: Int) = set {
        it.copy(shelf = it.shelf.map { x -> if (x.id == id) x.copy(shelved = maxOf(0, x.shelved + d)) else x })
    }
    fun reconcile(id: String) = set {
        it.copy(shelf = it.shelf.map { x -> if (x.id == id) x.copy(shelved = x.sold) else x })
    }
    fun openSpecify(id: String) = set { it.copy(specifyId = id) }
    fun closeSpecify() = set { it.copy(specifyId = null) }
    fun pickSource(sid: String) = set {
        it.copy(
            shelf = it.shelf.map { x -> if (x.id == it.specifyId) x.copy(sourceId = if (sid == "none") null else sid) else x },
            specifyId = null,
        )
    }

    // ── add source ──
    fun openAddSource() = set { it.copy(screen = "addsrc", addSrcKind = Kind.BALE, newCost = 400) }
    fun closeAddSource() = set { it.copy(screen = "home", addSrcKind = null) }
    fun pickKind(k: Kind) = set { it.copy(addSrcKind = k) }
    fun costStep(d: Int) = set { it.copy(newCost = maxOf(0, it.newCost + d * 50)) }
    fun saveSource() = set {
        val k = it.addSrcKind ?: Kind.BALE
        val n = it.sources.count { x -> x.kind == k } + 1
        val id = "s" + System.currentTimeMillis()
        it.copy(
            sources = it.sources + Source(id, k, k.label + " " + n, it.newCost),
            screen = "home", addSrcKind = null, accountSeg = "sources", tab = "account",
        )
    }

    // ── add item to shelf ──
    fun openAddItem() = set {
        it.copy(screen = "additem", aiName = "", aiTasira = 5_000, aiCount = 1, aiSource = PRE_ID, aiBuy = 3_000)
    }
    fun closeAddItem() = set { it.copy(screen = "home") }
    fun setAiName(v: String) = set { it.copy(aiName = v) }
    fun aiTasiraStep(d: Int) = set { it.copy(aiTasira = maxOf(0, it.aiTasira + d * 500)) }
    fun aiCountStep(d: Int) = set { it.copy(aiCount = maxOf(1, it.aiCount + d)) }
    fun aiBuyStep(d: Int) = set { it.copy(aiBuy = maxOf(0, it.aiBuy + d * 500)) }
    fun aiPickSource(sid: String) = set { it.copy(aiSource = sid) }
    fun saveAiItem() = set {
        val nm = it.aiName.trim().ifEmpty { "صنف" }
        val id = "a" + System.currentTimeMillis()
        val src = it.aiSource
        val item = Shelf(
            id = id, name = nm, tasira = it.aiTasira, shelved = it.aiCount, counted = it.aiCount,
            sold = 0, sourceId = if (src == "none") null else src,
            buy = if (src == MKT_ID) it.aiBuy else null,
        )
        it.copy(shelf = it.shelf + item, screen = "home", accountSeg = "shelf", tab = "account")
    }

    // ── package (count & shelve) ──
    fun openPackage(id: String) = set { it.copy(screen = "package", pkgId = id, pkgAddOpen = false) }
    fun closePackage() = set { it.copy(screen = "home", pkgAddOpen = false) }
    fun shelveStep(id: String, d: Int) = set {
        it.copy(shelf = it.shelf.map { x -> if (x.id == id) x.copy(shelved = maxOf(0, minOf(x.cnt, x.shelved + d))) else x })
    }
    fun shelveAll(id: String) = set {
        it.copy(shelf = it.shelf.map { x -> if (x.id == id) x.copy(shelved = x.cnt) else x })
    }
    fun togglePkgAdd() = set {
        it.copy(pkgAddOpen = !it.pkgAddOpen, aiName = "", aiTasira = 5_000, aiCount = 5)
    }
    fun savePkgCount() = set {
        val nm = it.aiName.trim().ifEmpty { "صنف" }
        val id = "p" + System.currentTimeMillis()
        it.copy(
            shelf = it.shelf + Shelf(id, nm, it.aiTasira, shelved = 0, counted = it.aiCount, sold = 0, sourceId = it.pkgId),
            pkgAddOpen = false,
        )
    }

    // ── payment (D37) ──
    fun openPay() = set { it.copy(screen = "pay", payAmount = 5_000, payTypeId = null) }
    fun payAmountStep(d: Int) = set { it.copy(payAmount = maxOf(0, it.payAmount + d * 500)) }
    fun payPickType(id: String) = set {
        val it0 = it
        val item = it0.shelf.find { x -> x.id == id }
        val already = it0.payTypeId == id
        it0.copy(
            payTypeId = if (already) null else id,
            payAmount = if (already) it0.payAmount else (item?.tasira ?: it0.payAmount),
        )
    }
    fun savePay() {
        val amt = s.payAmount
        val tid = s.payTypeId
        val item = tid?.let { id -> s.shelf.find { it.id == id } }
        val entry = DayEntry(
            id = "e" + System.currentTimeMillis(),
            t = "دفعة" + (item?.let { " — " + it.name } ?: "") + " — نقدي",
            d = "الآن" + (item?.let { " · نوع: " + it.name + " · يُسند لمصدره" } ?: " · على الرصيد"),
            amt = "+ " + fmt(amt), cls = "pos",
        )
        val before = if (tid != null) listOf(tid to (s.shelf.find { it.id == tid }?.sold ?: 0)) else emptyList()
        set {
            it.copy(
                screen = "home", entries = listOf(entry) + it.entries, cashToday = it.cashToday + amt,
                shelf = if (tid != null) it.shelf.map { x -> if (x.id == tid) x.copy(sold = x.sold + 1) else x } else it.shelf,
                undo = Undo(entry.id, 0, amt, before),
            )
        }
        armUndo(Undo(entry.id, 0, amt, before))
    }

    // ── sale ──
    fun openChooser() = set { it.copy(screen = "chooser") }
    fun openSale() = set { it.copy(screen = "sale", lines = emptyList(), pay = "full", addNewOpen = false) }
    fun closeSheet() = set { it.copy(screen = "home", addNewOpen = false) }
    fun addLine(id: String) = set {
        val item = it.shelf.find { x -> x.id == id } ?: return@set it
        it.copy(lines = it.lines + SaleLine(id, item.name, item.tasira, item.tasira, 1))
    }
    fun priceStep(i: Int, d: Int) = set {
        it.copy(lines = it.lines.mapIndexed { j, l -> if (j == i) l.copy(price = maxOf(0, l.price + d * 500)) else l })
    }
    fun qtyStep(i: Int, d: Int) = set {
        it.copy(lines = it.lines.mapIndexed { j, l -> if (j == i) l.copy(qty = maxOf(1, l.qty + d)) else l })
    }
    fun setPay(mode: String) = set { it.copy(pay = mode) }
    fun toggleAddNew() = set { it.copy(addNewOpen = !it.addNewOpen, newName = "", newPrice = 5_000) }
    fun setNewName(v: String) = set { it.copy(newName = v) }
    fun newPriceStep(d: Int) = set { it.copy(newPrice = maxOf(0, it.newPrice + d * 500)) }
    fun addNewItem() = set {
        val nm = it.newName.trim().ifEmpty { "صنف جديد" }
        val id = "n" + System.currentTimeMillis()
        it.copy(
            shelf = it.shelf + Shelf(id, nm, it.newPrice, shelved = 1, sold = 0, sourceId = null),
            lines = it.lines + SaleLine(id, nm, it.newPrice, it.newPrice, 1),
            addNewOpen = false, newName = "",
        )
    }
    fun saveSale() {
        val total = total()
        val isTrial = s.pay == "trial"
        val paid = when {
            s.pay == "full" -> total
            isTrial -> 0
            else -> Math.round(total / 2.0)
        }
        val names = s.lines.joinToString(" + ") { it.name }
        val entry = DayEntry(
            id = "e" + System.currentTimeMillis(),
            t = (if (isTrial) "أمانة" else "بيع") + " نقدي" + (if (names.isNotEmpty()) " — $names" else ""),
            d = "الآن · " + when {
                s.pay == "full" -> "مدفوع كامل"
                isTrial -> "أمانة"
                else -> "دفعت " + fmt(paid) + " والباقي دين"
            },
            amt = if (isTrial) "أمانة" else fmt(total),
            cls = if (isTrial) "amber" else "ink",
        )
        val sold = HashMap<String, Int>()
        s.lines.forEach { l -> sold[l.shelfId] = (sold[l.shelfId] ?: 0) + l.qty }
        val before = s.shelf.map { it.id to it.sold }
        val u = Undo(entry.id, if (isTrial) 0 else total, paid, before)
        set {
            it.copy(
                screen = "home", lines = emptyList(), entries = listOf(entry) + it.entries,
                shelf = it.shelf.map { x -> sold[x.id]?.let { q -> x.copy(sold = x.sold + q) } ?: x },
                salesToday = it.salesToday + (if (isTrial) 0 else total),
                cashToday = it.cashToday + paid, undo = u,
            )
        }
        armUndo(u)
    }
    fun undoSale() = set {
        val u = it.undo ?: return@set it
        it.copy(
            entries = it.entries.filterNot { e -> e.id == u.id },
            salesToday = it.salesToday - u.dS, cashToday = it.cashToday - u.dC,
            shelf = it.shelf.map { x -> u.before.find { b -> b.first == x.id }?.let { b -> x.copy(sold = b.second) } ?: x },
            undo = null,
        )
    }
}
