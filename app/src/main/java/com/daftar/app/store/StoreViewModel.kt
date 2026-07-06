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

// Undo payload — the entry id plus each shelf item's `sold` before it, so ↺ تراجع
// removes the entry (its totals derive from it) and restores the shelf.
data class Undo(val id: String, val before: List<Pair<String, Int>>)

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
    val customers: List<Customer> = emptyList(),
    val today: Long = 0,      // epoch-day, refreshed each launch/foreground
    val viewedDay: Long = 0,  // which day the اليوم page is showing (default = today)
    val usdRate: Long = 1500, // editable USD→local rate used for bale cost/profit
    // sale
    val lines: List<SaleLine> = emptyList(),
    val pay: String = "full",           // full | partial | trial
    val partialPaid: Long = 0,          // how much she paid now when pay == partial
    val saleCustomerId: String? = null, // who this sale/payment is for (null = نقدي)
    val undo: Undo? = null,
    // customer picker + inline add
    val custPickerOpen: Boolean = false,
    val custNewOpen: Boolean = false,
    val custNewName: String = "",
    val custNewPhone: String = "",
    val custNewDebt: Long = 0,
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
    // returns
    val returnAmount: Long = 5_000,
    val returnItemId: String? = null,
    // entry detail (view / void a past entry) + customer detail
    val detailEntryId: String? = null,
    val detailCustomerId: String? = null,
)

@HiltViewModel
class StoreViewModel @Inject constructor(
    private val repo: StoreRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(StoreState())
    val state: StateFlow<StoreState> = _state.asStateFlow()

    private val s get() = _state.value
    // Every state change re-normalizes due dates (idempotent + cheap) so a new debt gets a
    // due date and a paid-off customer loses hers, no matter which handler ran.
    private fun set(f: (StoreState) -> StoreState) = _state.update { s0 ->
        val s1 = f(s0)
        s1.copy(customers = normalizeDues(s1.customers, s1.entries, s1.today))
    }

    init {
        // Start on the real current day, then load the persisted ledger and keep the DB in
        // step with the persistable slice (transient sheet/stepper changes don't touch disk).
        val now = today()
        _state.update { it.copy(today = now, viewedDay = now) }
        viewModelScope.launch {
            repo.load()?.let { snap ->
                _state.update {
                    it.copy(
                        seeded = snap.seeded, usdRate = snap.usdRate, sources = snap.sources, shelf = snap.shelf,
                        entries = snap.entries, customers = normalizeDues(snap.customers, snap.entries, it.today),
                    )
                }
            }
            state.map {
                StoreSnapshot(it.seeded, it.usdRate, it.sources, it.shelf, it.entries, it.customers)
            }.distinctUntilChanged().drop(1).collect { repo.save(it) }
        }
    }

    private fun today(): Long = java.time.LocalDate.now().toEpochDay()

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
        val d = today()
        it.copy(
            seeded = true, sources = sampleSources(), shelf = sampleShelf(),
            entries = sampleEntries(d), customers = sampleCustomers(),
            today = d, viewedDay = d, tab = "today", screen = "home",
        )
    }
    fun resetApp() = set {
        val d = today()
        StoreState(sources = listOf(Source(PRE_ID, Kind.PRE_APP, "قبل التطبيق", null)), today = d, viewedDay = d)
    }

    // ── backup: export/restore the whole ledger as JSON (the persist collector saves imports) ──
    fun exportJson(): String = s.let { snapshotToJson(StoreSnapshot(it.seeded, it.usdRate, it.sources, it.shelf, it.entries, it.customers)) }
    fun importJson(json: String): Boolean = try {
        val snap = snapshotFromJson(json)
        set {
            it.copy(
                seeded = true, usdRate = snap.usdRate, sources = snap.sources, shelf = snap.shelf,
                entries = snap.entries, customers = snap.customers, screen = "home", tab = "today", viewedDay = it.today,
            )
        }
        true
    } catch (e: Exception) {
        false
    }

    // The editable "today's rate" — one number that recomputes all bale profit.
    fun setUsdRate(v: Long) = set { it.copy(usdRate = maxOf(0, v)) }

    // ── nav ──
    fun setTab(tab: String) = set { it.copy(tab = tab, screen = "home") }
    // Page the day book back/forward; never past today.
    fun dayStep(d: Int) = set { it.copy(viewedDay = minOf(it.today, it.viewedDay + d)) }
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

    // ── customers ──
    fun openCustPicker() = set { it.copy(custPickerOpen = true, custNewOpen = false) }
    fun openAddCustomer() = set { it.copy(custPickerOpen = true, custNewOpen = true, custNewName = "", custNewPhone = "", custNewDebt = 0) }
    fun closeCustPicker() = set { it.copy(custPickerOpen = false, custNewOpen = false) }
    fun pickCustomer(id: String?) = set { it.copy(saleCustomerId = id, custPickerOpen = false, custNewOpen = false) }
    fun toggleCustNew() = set { it.copy(custNewOpen = !it.custNewOpen, custNewName = "", custNewPhone = "", custNewDebt = 0) }
    fun setCustNewName(v: String) = set { it.copy(custNewName = v) }
    fun setCustNewPhone(v: String) = set { it.copy(custNewPhone = v) }
    fun custNewDebtStep(d: Int) = set { it.copy(custNewDebt = maxOf(0, it.custNewDebt + d * 500)) }
    fun addCustomer() = set {
        val nm = it.custNewName.trim().ifEmpty { "زبونة" }
        val c = Customer("c" + System.currentTimeMillis(), nm, it.custNewPhone.trim().ifEmpty { null }, it.custNewDebt)
        it.copy(customers = it.customers + c, saleCustomerId = c.id, custPickerOpen = false, custNewOpen = false)
    }

    // ── payment (D37) ──
    fun openPay() = set { it.copy(screen = "pay", payAmount = 5_000, payTypeId = null, saleCustomerId = null) }
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
        val cust = s.saleCustomerId?.let { id -> s.customers.find { it.id == id } }
        val entry = DayEntry(
            id = "e" + System.currentTimeMillis(),
            t = "دفعة" + (item?.let { " — " + it.name } ?: "") + " — " + (cust?.name ?: "نقدي"),
            d = "الآن" + (item?.let { " · نوع: " + it.name + " · يُسند لمصدره" } ?: " · على الرصيد"),
            amt = "+ " + fmt(amt), cls = "pos",
            customerId = s.saleCustomerId,
            debtDelta = -amt, // a payment reduces the customer's debt
            day = today(), saleAmount = 0, cashAmount = amt,
            stockDelta = if (tid != null) encodeStock(mapOf(tid to 1)) else "",
        )
        val before = if (tid != null) listOf(tid to (s.shelf.find { it.id == tid }?.sold ?: 0)) else emptyList()
        val u = Undo(entry.id, before)
        set {
            it.copy(
                screen = "home", entries = listOf(entry) + it.entries, viewedDay = it.today,
                shelf = if (tid != null) it.shelf.map { x -> if (x.id == tid) x.copy(sold = x.sold + 1) else x } else it.shelf,
                undo = u,
            )
        }
        armUndo(u)
    }

    // ── returns (إرجاع) — value credited back to the customer's balance ──
    fun openReturn() = set { it.copy(screen = "return", returnAmount = 5_000, returnItemId = null, saleCustomerId = null) }
    fun returnAmountStep(d: Int) = set { it.copy(returnAmount = maxOf(0, it.returnAmount + d * 500)) }
    fun returnPickItem(id: String) = set {
        val item = it.shelf.find { x -> x.id == id }
        val already = it.returnItemId == id
        it.copy(returnItemId = if (already) null else id, returnAmount = if (already) it.returnAmount else (item?.tasira ?: it.returnAmount))
    }
    fun saveReturn() {
        val amt = s.returnAmount
        val iid = s.returnItemId
        val item = iid?.let { id -> s.shelf.find { it.id == id } }
        val cust = s.saleCustomerId?.let { id -> s.customers.find { it.id == id } }
        val entry = DayEntry(
            id = "e" + System.currentTimeMillis(),
            t = "إرجاع" + (item?.let { " — " + it.name } ?: "") + " — " + (cust?.name ?: "نقدي"),
            d = "الآن · قيمة تُعاد للرصيد" + (item?.let { " · أُعيد للرف" } ?: ""),
            amt = "↩ " + fmt(amt), cls = "amber",
            customerId = s.saleCustomerId,
            debtDelta = -amt, // a return credits her balance
            day = today(), saleAmount = 0, cashAmount = 0,
            stockDelta = if (iid != null) encodeStock(mapOf(iid to -1)) else "", // item back on the shelf
        )
        val before = if (iid != null) listOf(iid to (s.shelf.find { it.id == iid }?.sold ?: 0)) else emptyList()
        val u = Undo(entry.id, before)
        set {
            it.copy(
                screen = "home", entries = listOf(entry) + it.entries, viewedDay = it.today,
                shelf = if (iid != null) it.shelf.map { x -> if (x.id == iid) x.copy(sold = maxOf(0, x.sold - 1)) else x } else it.shelf,
                undo = u,
            )
        }
        armUndo(u)
    }

    // ── entry detail: view a past entry and void it (reverses debt, totals, stock) ──
    fun openEntry(id: String) = set { it.copy(detailEntryId = id) }
    fun closeEntry() = set { it.copy(detailEntryId = null) }
    fun voidEntry(id: String) = set {
        val e = it.entries.find { x -> x.id == id } ?: return@set it
        val deltas = decodeStock(e.stockDelta)
        it.copy(
            entries = it.entries.filterNot { x -> x.id == id },
            shelf = it.shelf.map { x -> deltas.find { d -> d.first == x.id }?.let { d -> x.copy(sold = maxOf(0, x.sold - d.second)) } ?: x },
            detailEntryId = null, undo = null,
        )
    }

    // Edit an old قيد: reverse it (like void), then reopen the matching sheet pre-filled so the
    // owner can fix anything — add a customer, change the amount, the items, the pay type.
    fun editEntry(id: String) = set {
        val e = it.entries.find { x -> x.id == id } ?: return@set it
        val deltas = decodeStock(e.stockDelta)
        val shelfBack = it.shelf.map { x -> deltas.find { d -> d.first == x.id }?.let { d -> x.copy(sold = maxOf(0, x.sold - d.second)) } ?: x }
        val entriesBack = it.entries.filterNot { x -> x.id == id }
        val base = it.copy(entries = entriesBack, shelf = shelfBack, detailEntryId = null, undo = null, addNewOpen = false)
        when {
            e.lines.isNotEmpty() -> base.copy(
                screen = "sale", saleCustomerId = e.customerId,
                lines = decodeLines(e.lines).map { l ->
                    SaleLine(l.shelfId, l.name, it.shelf.find { s -> s.id == l.shelfId }?.tasira ?: l.price, l.price, l.qty)
                },
                pay = if (e.trialAmount > 0) "trial" else if (e.debtDelta > 0) "partial" else "full",
                partialPaid = e.cashAmount,
            )
            e.t.startsWith("دفعة") -> base.copy(
                screen = "pay", payAmount = e.cashAmount,
                payTypeId = deltas.firstOrNull()?.first, saleCustomerId = e.customerId,
            )
            e.t.startsWith("إرجاع") -> base.copy(
                screen = "return", returnAmount = -e.debtDelta,
                returnItemId = deltas.firstOrNull()?.first, saleCustomerId = e.customerId,
            )
            else -> base // e.g. a trial-conversion: nothing to reopen, just reverse it
        }
    }

    // ── customer detail: her balance + history + record a payment for her ──
    fun openCustomer(id: String) = set { it.copy(detailCustomerId = id) }
    fun closeCustomer() = set { it.copy(detailCustomerId = null) }
    // She kept the أمانة: turn her outstanding trial into a firm (owed) sale.
    fun convertTrial(id: String) {
        val cust = s.customers.find { it.id == id } ?: return
        val t = customerTrial(cust, s.entries)
        if (t <= 0) return
        val entry = DayEntry(
            id = "e" + System.currentTimeMillis(),
            t = "تحويل أمانة إلى بيع — " + cust.name,
            d = "الآن · أصبحت ديناً على الزبونة",
            amt = fmt(t), cls = "ink",
            customerId = id,
            debtDelta = t,       // now owed
            trialAmount = -t,    // cancels the outstanding trial
            day = today(), saleAmount = t, cashAmount = 0,
        )
        set { it.copy(entries = listOf(entry) + it.entries, viewedDay = it.today) }
    }
    fun payThisCustomer(id: String) = set {
        it.copy(screen = "pay", payAmount = 5_000, payTypeId = null, saleCustomerId = id, detailCustomerId = null)
    }
    // FR-3.2: one tap pushes her reminder out (from today); the digest reschedules naturally.
    fun snooze(id: String, days: Int) = set {
        it.copy(customers = it.customers.map { c -> if (c.id == id) c.copy(dueEpochDay = it.today + days) else c })
    }

    // ── sale ──
    fun openChooser() = set { it.copy(screen = "chooser") }
    fun openSale() = set { it.copy(screen = "sale", lines = emptyList(), pay = "full", partialPaid = 0, addNewOpen = false, saleCustomerId = null) }
    fun partialStep(d: Int) = set { it.copy(partialPaid = maxOf(0, it.partialPaid + d * 500)) }
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
    fun setPay(mode: String) = set {
        val total = it.lines.sumOf { l -> l.price * l.qty }
        // seed a sensible starting amount (half) the first time she picks جزءاً
        it.copy(pay = mode, partialPaid = if (mode == "partial" && it.partialPaid == 0L) total / 2 else it.partialPaid)
    }
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
        if (s.lines.isEmpty()) return // tighten: a sale needs at least one item
        val total = total()
        val isTrial = s.pay == "trial"
        val paid = when {
            s.pay == "full" -> total
            isTrial -> 0
            else -> minOf(s.partialPaid, total) // exactly what she paid (never more than the total)
        }
        val names = s.lines.joinToString(" + ") { it.name }
        val cust = s.saleCustomerId?.let { id -> s.customers.find { it.id == id } }
        val prefix = if (isTrial) "أمانة" else "بيع"
        val who = cust?.name ?: "نقدي"
        val soldMap = HashMap<String, Int>()
        s.lines.forEach { l -> soldMap[l.shelfId] = (soldMap[l.shelfId] ?: 0) + l.qty }
        val entry = DayEntry(
            id = "e" + System.currentTimeMillis(),
            t = (if (cust != null) "$prefix — $who" else "$prefix نقدي") + (if (names.isNotEmpty()) " — $names" else ""),
            d = "الآن · " + when {
                s.pay == "full" -> "مدفوع كامل"
                isTrial -> "أمانة — قد تُعاد"
                else -> "دفعت " + fmt(paid) + " والباقي دين"
            },
            amt = if (isTrial) "أمانة " + fmt(total) else fmt(total),
            cls = if (isTrial) "amber" else "ink",
            customerId = s.saleCustomerId,
            // أمانة is not a firm sale/debt yet — it's tracked as trial, not debt.
            debtDelta = if (isTrial) 0 else total - paid,
            trialAmount = if (isTrial) total else 0,
            day = today(),
            saleAmount = if (isTrial) 0 else total, // أمانة is excluded from the day's sales
            cashAmount = paid,
            stockDelta = encodeStock(soldMap),
            lines = encodeLines(s.lines),
        )
        val before = s.shelf.map { it.id to it.sold }
        val u = Undo(entry.id, before)
        set {
            it.copy(
                screen = "home", lines = emptyList(), entries = listOf(entry) + it.entries,
                viewedDay = it.today,
                shelf = it.shelf.map { x -> soldMap[x.id]?.let { q -> x.copy(sold = x.sold + q) } ?: x },
                undo = u,
            )
        }
        armUndo(u)
    }
    fun undoSale() = set {
        val u = it.undo ?: return@set it
        it.copy(
            entries = it.entries.filterNot { e -> e.id == u.id },
            shelf = it.shelf.map { x -> u.before.find { b -> b.first == x.id }?.let { b -> x.copy(sold = b.second) } ?: x },
            undo = null,
        )
    }
}
