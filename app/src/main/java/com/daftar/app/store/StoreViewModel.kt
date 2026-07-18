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
    val tab: String = "today",          // today | cust | appts | account
    val accountSeg: String = "sources", // sources | sum (البضاعة is its own tab since 2026-07-18)
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
    // standalone "add a customer" sheet (from the الزبائن directory, not a sale picker)
    val custAddOpen: Boolean = false,
    // editing an existing customer (v2: every record editable forever) — reuses custNew* fields
    val custEditId: String? = null,
    // محلات السوق management inside the one شراء من السوق card
    val shopAddOpen: Boolean = false,
    val shopRenameId: String? = null,
    val shopName: String = "",
    val shopDebt: Long = 0,
    // shop detail sheet (F2) + the «دفعتُ للمحل» inline pay form
    val shopId: String? = null,
    val shopPayOpen: Boolean = false,
    val shopPayAmount: Long = 0,
    // one-sheet item editing (v2: tap an item, control everything incl. its source)
    val editItemId: String? = null,
    val eiName: String = "",
    val eiTasira: Long = 0,
    val eiOnHand: Int = 0,
    val eiBuy: Long = 0,
    val eiSource: String? = null,
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
    val newSrcName: String = "", // bale name at creation (F1); empty ⇒ auto «بالة N»
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
    // editing an existing قيد: its id is held while the sheet is open so the save REPLACES it
    // (reverse the old + add the new) instead of adding a duplicate. Cleared on cancel so
    // backing out of an edit leaves the original untouched.
    val editingId: String? = null,
    // a pending destructive confirmation ("sample" | "reset") — gates the two data-wiping actions
    val confirm: String? = null,
    // F3 paper-debt catch: a دفعة would overshoot her balance → offer to record her old
    // paper-era debt as opening debt first. paperDebtAmount seeds to the shortfall.
    val paperDebtPrompt: Boolean = false,
    val paperDebtAmount: Long = 0,
    // maintainer tools sheet (sample data / wipe / sync URL) — opened only by the hidden
    // long-press on the دفتر wordmark, so الملخّص carries nothing destructive
    val maintOpen: Boolean = false,
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
        s.sources.find { it.id == id }?.label ?: "لا أعلم"

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

    // Swipe/✕ dismiss of the undo toast (F2) — clears the bar but keeps the entry.
    fun dismissUndo() {
        undoJob?.cancel()
        set { it.copy(undo = null) }
    }

    // Both of these wipe the real ledger, so they're gated behind a confirmation (askConfirm).
    fun askConfirm(kind: String) = set { it.copy(confirm = kind) }
    fun dismissConfirm() = set { it.copy(confirm = null) }

    fun openMaint() = set { it.copy(maintOpen = true) }
    fun closeMaint() = set { it.copy(maintOpen = false) }
    fun loadSample() = set {
        val d = today()
        it.copy(
            seeded = true, sources = sampleSources(), shelf = sampleShelf(),
            entries = sampleEntries(d), customers = sampleCustomers(),
            today = d, viewedDay = d, tab = "today", screen = "home", confirm = null,
        )
    }
    fun resetApp() = set {
        val d = today()
        // initialSources() keeps both defaults (قبل التطبيق + شراء من السوق) so market items
        // stay attributable after a reset.
        StoreState(sources = initialSources(), today = d, viewedDay = d)
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

    // Typed money entry (owner 2026-07-18): every money amount can be typed directly, not only
    // nudged ±500 — the steppers remain for small adjustments.
    fun setPayAmount(v: Long) = set { it.copy(payAmount = maxOf(0, v)) }
    fun setReturnAmount(v: Long) = set { it.copy(returnAmount = maxOf(0, v)) }
    fun setPartialPaid(v: Long) = set { it.copy(partialPaid = maxOf(0, v)) }
    fun setPaperDebtAmount(v: Long) = set { it.copy(paperDebtAmount = maxOf(0, v)) }
    fun setCustNewDebt(v: Long) = set { it.copy(custNewDebt = maxOf(0, v)) }
    fun setNewPrice(v: Long) = set { it.copy(newPrice = maxOf(0, v)) }
    fun setAiTasira(v: Long) = set { it.copy(aiTasira = maxOf(0, v)) }
    fun setAiBuy(v: Long) = set { it.copy(aiBuy = maxOf(0, v)) }
    fun setEiTasira(v: Long) = set { it.copy(eiTasira = maxOf(0, v)) }
    fun setEiBuy(v: Long) = set { it.copy(eiBuy = maxOf(0, v)) }
    fun setShopPayAmount(v: Long) = set { it.copy(shopPayAmount = maxOf(0, v)) }
    fun setShopDebt(v: Long) = set { it.copy(shopDebt = maxOf(0, v)) }
    fun setLinePrice(i: Int, v: Long) = set { st ->
        st.copy(lines = st.lines.mapIndexed { j, l -> if (j == i) l.copy(price = maxOf(0, v)) else l })
    }

    // ── nav ──
    fun setTab(tab: String) = set { it.copy(tab = tab, screen = "home", editingId = null) }
    // Page the day book back/forward; never past today.
    fun dayStep(d: Int) = set { it.copy(viewedDay = minOf(it.today, it.viewedDay + d)) }

    // Re-derive the real day on lifecycle resume — the day book must roll past midnight
    // without a relaunch (entries already stamp the real day; the view was the stale part).
    fun refreshToday() = set {
        val now = today()
        if (now == it.today) it
        else it.copy(viewedDay = rollViewedDay(it.today, it.viewedDay, now), today = now)
    }
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
    fun openAddSource() = set { it.copy(screen = "addsrc", addSrcKind = Kind.BALE, newCost = 400, newSrcName = "") }
    fun closeAddSource() = set { it.copy(screen = "home", addSrcKind = null) }
    fun pickKind(k: Kind) = set { it.copy(addSrcKind = k) }
    fun costStep(d: Int) = set { it.copy(newCost = maxOf(0, it.newCost + d * 50)) }
    fun setNewSrcName(v: String) = set { it.copy(newSrcName = v) }
    fun saveSource() = set {
        val k = it.addSrcKind ?: Kind.BALE
        val n = it.sources.count { x -> x.kind == k } + 1
        val id = "s" + System.currentTimeMillis()
        // F1: her name if she gave one, the running «بالة N» otherwise
        val label = it.newSrcName.trim().ifEmpty { k.label + " " + n }
        it.copy(
            sources = it.sources + Source(id, k, label, it.newCost),
            screen = "home", addSrcKind = null, newSrcName = "", accountSeg = "sources", tab = "account",
        )
    }

    // ── add item to shelf ──
    // v2: new items default to غير محدد — she resolves the source later if she remembers.
    fun openAddItem() = set {
        it.copy(screen = "additem", aiName = "", aiTasira = 5_000, aiCount = 1, aiSource = "none", aiBuy = 3_000)
    }
    // A shop's "+ صنف": the item arrives pre-attributed to that محل.
    fun openAddItemFor(sourceId: String) = set {
        it.copy(screen = "additem", aiName = "", aiTasira = 5_000, aiCount = 1, aiSource = sourceId, aiBuy = 3_000)
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
        it.copy(shelf = it.shelf + item, screen = "home", tab = "shelf")
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
    // From the الزبائن directory: a dedicated create-customer sheet (NOT the sale-time picker).
    fun openAddCustomer() = set { it.copy(custAddOpen = true, custNewName = "", custNewPhone = "", custNewDebt = 0) }
    fun closeAddCustomer() = set { it.copy(custAddOpen = false) }
    fun saveNewCustomer() = set {
        val nm = it.custNewName.trim().ifEmpty { "زبونة" }
        val c = Customer("c" + System.currentTimeMillis(), nm, it.custNewPhone.trim().ifEmpty { null }, it.custNewDebt)
        it.copy(customers = it.customers + c, custAddOpen = false)
    }
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

    // ── edit a customer (v2: every record editable forever; opening-debt edits re-normalize dues) ──
    fun startEditCustomer(id: String) = set { s ->
        val c = s.customers.find { it.id == id } ?: return@set s
        s.copy(custEditId = id, custNewName = c.name, custNewPhone = c.phone ?: "", custNewDebt = c.openingDebt)
    }
    fun cancelEditCustomer() = set { it.copy(custEditId = null) }
    fun saveEditCustomer() = set { s ->
        val id = s.custEditId ?: return@set s
        s.copy(
            customers = s.customers.map { c ->
                if (c.id == id) c.copy(
                    name = s.custNewName.trim().ifEmpty { c.name },
                    phone = s.custNewPhone.trim().ifEmpty { null },
                    openingDebt = s.custNewDebt,
                ) else c
            },
            custEditId = null,
        )
    }

    // ── محلات السوق: shops inside the one شراء من السوق card (v2 decision 11) ──
    fun toggleShopAdd() = set { it.copy(shopAddOpen = !it.shopAddOpen, shopName = "", shopDebt = 0) }
    fun setShopName(v: String) = set { it.copy(shopName = v) }
    fun shopDebtStep(d: Int) = set { it.copy(shopDebt = maxOf(0, it.shopDebt + d * 500)) }
    fun addShop() = set {
        val nm = it.shopName.trim().ifEmpty { "محل" }
        it.copy(
            sources = it.sources + Source("s" + System.currentTimeMillis(), Kind.MARKET, nm, null, it.shopDebt),
            shopAddOpen = false, shopName = "",
        )
    }
    fun startRenameShop(id: String) = set { s ->
        s.copy(shopRenameId = id, shopName = s.sources.find { it.id == id }?.label ?: "")
    }
    fun saveRenameShop() = set { s ->
        s.copy(
            sources = s.sources.map { if (it.id == s.shopRenameId) it.copy(label = s.shopName.trim().ifEmpty { it.label }) else it },
            shopRenameId = null, shopName = "",
        )
    }
    // She paid the shop / took more on credit — one number, hers to adjust.
    fun shopOwedStep(id: String, d: Int) = set { s ->
        s.copy(sources = s.sources.map { if (it.id == id) it.copy(debt = maxOf(0, it.debt + d * 500)) else it })
    }

    // ── shop detail (F2) ──
    fun openShop(id: String) = set { it.copy(shopId = id, shopPayOpen = false, shopPayAmount = 0, shopRenameId = null) }
    fun closeShop() = set { it.copy(shopId = null, shopPayOpen = false, shopRenameId = null) }
    fun toggleShopPay() = set { it.copy(shopPayOpen = !it.shopPayOpen, shopPayAmount = 0) }
    fun shopPayStep(d: Int) = set { s ->
        val src = s.sources.find { it.id == s.shopId } ?: return@set s
        val cap = maxOf(0, shopDebtNow(src, s.entries)) // she can't pay more than she owes
        s.copy(shopPayAmount = (s.shopPayAmount + d * 500).coerceIn(0, cap))
    }
    // D68: «دفعتُ للمحل» is a voidable money-out قيد, not a silent edit. cashAmount stays 0
    // so قبضنا اليوم never counts it; the shop's debt derives down via shopDebtNow.
    fun saveShopPay() = set { s ->
        val src = s.sources.find { it.id == s.shopId } ?: return@set s
        val amt = minOf(s.shopPayAmount, maxOf(0, shopDebtNow(src, s.entries)))
        if (amt <= 0) return@set s
        val entry = DayEntry(
            id = "e" + System.currentTimeMillis(),
            t = "دفعة للمحل — ${src.label}", d = "الآن · تسديد من دينه علينا",
            amt = "− " + fmt(amt), cls = "neg",
            day = today(), sourceId = src.id, moneyOut = amt,
        )
        s.copy(entries = listOf(entry) + s.entries, shopPayOpen = false, shopPayAmount = 0)
    }

    // ── one-sheet item editing: tap an item, control everything incl. re-pointing its source ──
    fun openEditItem(id: String) = set { s ->
        val x = s.shelf.find { it.id == id } ?: return@set s
        s.copy(editItemId = id, eiName = x.name, eiTasira = x.tasira, eiOnHand = x.onHand, eiBuy = x.buy ?: 0, eiSource = x.sourceId)
    }
    fun closeEditItem() = set { it.copy(editItemId = null) }
    fun setEiName(v: String) = set { it.copy(eiName = v) }
    fun eiTasiraStep(d: Int) = set { it.copy(eiTasira = maxOf(0, it.eiTasira + d * 500)) }
    fun eiOnHandStep(d: Int) = set { it.copy(eiOnHand = maxOf(0, it.eiOnHand + d)) }
    fun eiBuyStep(d: Int) = set { it.copy(eiBuy = maxOf(0, it.eiBuy + d * 500)) }
    fun eiPickSource(sid: String?) = set { it.copy(eiSource = sid) }
    fun saveEditItem() = set { s ->
        val id = s.editItemId ?: return@set s
        s.copy(
            shelf = s.shelf.map { x ->
                if (x.id == id) x.copy(
                    name = s.eiName.trim().ifEmpty { x.name },
                    tasira = s.eiTasira,
                    // she edits what she SEES on the shelf; the stored count shifts by the same amount
                    shelved = maxOf(0, x.shelved + (s.eiOnHand - x.onHand)),
                    buy = if (s.eiBuy > 0) s.eiBuy else null,
                    sourceId = s.eiSource,
                ) else x
            },
            editItemId = null,
        )
    }

    // ── payment (D37) ──
    // F3 picker-first: a دفعة opens on «لمن؟» — نقدي is a deliberate choice, never a
    // forgotten default (the silent-no-op trap: recording a payment without the customer).
    fun openPay() = set { it.copy(screen = "pay", payAmount = 5_000, payTypeId = null, saleCustomerId = null, editingId = null, custPickerOpen = true) }
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
        if (amt <= 0) return // a payment of nothing isn't a قيد
        // F3: a NEW payment that overshoots her balance would flip it to لها — usually her
        // paper-era debt was never entered. Catch it and offer to record the old debt first.
        val cust0 = s.saleCustomerId?.let { id -> s.customers.find { it.id == id } }
        if (s.editingId == null && cust0 != null) {
            val short = paperDebtShortfall(customerBalance(cust0, s.entries), amt)
            if (short != null) {
                set { it.copy(paperDebtPrompt = true, paperDebtAmount = short) }
                return
            }
        }
        commitPay()
    }

    fun paperDebtStep(d: Int) = set { it.copy(paperDebtAmount = maxOf(0, it.paperDebtAmount + d * 500)) }
    fun closePaperDebt() = set { it.copy(paperDebtPrompt = false) }
    // «نعم»: record her old paper debt as opening debt, THEN apply the payment (lands at 0+).
    fun confirmPaperDebt() {
        val cid = s.saleCustomerId ?: return
        val add = s.paperDebtAmount
        set { st ->
            st.copy(
                customers = st.customers.map { if (it.id == cid) it.copy(openingDebt = it.openingDebt + add) else it },
                paperDebtPrompt = false,
            )
        }
        commitPay()
    }
    // «لا، هي لها»: apply the payment as-is; the balance may go negative (shop owes her).
    fun declinePaperDebt() {
        set { it.copy(paperDebtPrompt = false) }
        commitPay()
    }

    private fun commitPay() {
        val amt = s.payAmount
        if (amt <= 0) return
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
        val editingId = s.editingId
        val before = if (tid != null) listOf(tid to (s.shelf.find { it.id == tid }?.sold ?: 0)) else emptyList()
        val u = if (editingId == null) Undo(entry.id, before) else null
        set {
            val r = if (editingId != null) reversed(it, editingId) else it
            r.copy(
                screen = "home", entries = listOf(entry) + r.entries, viewedDay = it.today,
                shelf = if (tid != null) r.shelf.map { x -> if (x.id == tid) x.copy(sold = x.sold + 1) else x } else r.shelf,
                undo = u, editingId = null,
            )
        }
        if (u != null) armUndo(u)
    }

    // ── returns (إرجاع) — value credited back to the customer's balance ──
    fun openReturn() = set { it.copy(screen = "return", returnAmount = 5_000, returnItemId = null, saleCustomerId = null, editingId = null) }
    fun returnAmountStep(d: Int) = set { it.copy(returnAmount = maxOf(0, it.returnAmount + d * 500)) }
    fun returnPickItem(id: String) = set {
        val item = it.shelf.find { x -> x.id == id }
        val already = it.returnItemId == id
        it.copy(returnItemId = if (already) null else id, returnAmount = if (already) it.returnAmount else (item?.tasira ?: it.returnAmount))
    }
    fun saveReturn() {
        val amt = s.returnAmount
        if (amt <= 0) return // nothing to credit back
        val iid = s.returnItemId
        val item = iid?.let { id -> s.shelf.find { it.id == id } }
        val cust = s.saleCustomerId?.let { id -> s.customers.find { it.id == id } }
        val entry = DayEntry(
            id = "e" + System.currentTimeMillis(),
            t = "إرجاع" + (item?.let { " — " + it.name } ?: "") + " — " + (cust?.name ?: "نقدي"),
            d = "الآن · قيمة تُعاد للرصيد" + (item?.let { " · أُعيد للمحل" } ?: ""),
            amt = "↩ " + fmt(amt), cls = "amber",
            customerId = s.saleCustomerId,
            debtDelta = -amt, // a return credits her balance
            day = today(), saleAmount = 0, cashAmount = 0,
            stockDelta = if (iid != null) encodeStock(mapOf(iid to -1)) else "", // item back on the shelf
        )
        val editingId = s.editingId
        val before = if (iid != null) listOf(iid to (s.shelf.find { it.id == iid }?.sold ?: 0)) else emptyList()
        val u = if (editingId == null) Undo(entry.id, before) else null
        set {
            val r = if (editingId != null) reversed(it, editingId) else it
            r.copy(
                screen = "home", entries = listOf(entry) + r.entries, viewedDay = it.today,
                shelf = if (iid != null) r.shelf.map { x -> if (x.id == iid) x.copy(sold = maxOf(0, x.sold - 1)) else x } else r.shelf,
                undo = u, editingId = null,
            )
        }
        if (u != null) armUndo(u)
    }

    // ── entry detail: view a past entry and void it (reverses debt, totals, stock) ──
    fun openEntry(id: String) = set { it.copy(detailEntryId = id) }
    fun closeEntry() = set { it.copy(detailEntryId = null) }
    // Remove an entry and put back the shelf stock it had moved. Shared by void and by
    // edit-replace (a save that's editing an old قيد reverses it first, then adds the new one).
    private fun reversed(state: StoreState, id: String): StoreState {
        val e = state.entries.find { it.id == id } ?: return state
        val deltas = decodeStock(e.stockDelta)
        return state.copy(
            entries = state.entries.filterNot { it.id == id },
            shelf = state.shelf.map { x -> deltas.find { d -> d.first == x.id }?.let { d -> x.copy(sold = maxOf(0, x.sold - d.second)) } ?: x },
        )
    }
    // D71 soft delete: voiding KEEPS the قيد (struck-through) but reverses its effects — its
    // money is excluded by the derivations (!voided), and its stock is put back here. Restore
    // re-applies both. Nothing is ever destroyed.
    private fun applyStock(shelf: List<Shelf>, stockDelta: String, sign: Int): List<Shelf> {
        val deltas = decodeStock(stockDelta)
        return shelf.map { x -> deltas.find { it.first == x.id }?.let { d -> x.copy(sold = maxOf(0, x.sold + sign * d.second)) } ?: x }
    }
    fun voidEntry(id: String) = set { s ->
        val e = s.entries.find { it.id == id } ?: return@set s
        if (e.voided) return@set s
        // withdrawals move `shelved` (not `sold`) — reverse via applyWithdraw so pieces come back
        val shelf = if (e.cls == "withdraw") applyWithdraw(s.shelf, e.stockDelta, restore = true) else applyStock(s.shelf, e.stockDelta, -1)
        s.copy(
            entries = s.entries.map { if (it.id == id) it.copy(voided = true) else it },
            shelf = shelf, // items back on the shelf / in the shop
            detailEntryId = null, undo = null,
        )
    }
    fun restoreEntry(id: String) = set { s ->
        val e = s.entries.find { it.id == id } ?: return@set s
        if (!e.voided) return@set s
        val shelf = if (e.cls == "withdraw") applyWithdraw(s.shelf, e.stockDelta, restore = false) else applyStock(s.shelf, e.stockDelta, 1)
        s.copy(
            entries = s.entries.map { if (it.id == id) it.copy(voided = false) else it },
            shelf = shelf, // re-apply its stock effect
            detailEntryId = null,
        )
    }

    // Edit an old قيد: reopen the matching sheet pre-filled and mark it for replacement. The
    // original is left in place — the save reverses it then adds the corrected one — so backing
    // out of the edit (✕ / back / تبويب آخر) leaves the entry exactly as it was (no data loss).
    fun editEntry(id: String) = set {
        val e = it.entries.find { x -> x.id == id } ?: return@set it
        if (e.moneyOut > 0) return@set it // supplier payments are void-and-redo, not editable
        if (e.cls == "withdraw") return@set it // withdrawals are void-and-redo, not editable (F6)
        val deltas = decodeStock(e.stockDelta)
        val base = it.copy(editingId = id, detailEntryId = null, undo = null, addNewOpen = false)
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
            else -> it // nothing editable to reopen — leave the entry untouched
        }
    }

    // ── customer detail: her balance + history + record a payment for her ──
    fun openCustomer(id: String) = set { it.copy(detailCustomerId = id) }
    fun closeCustomer() = set { it.copy(detailCustomerId = null) }
    // She kept THIS أمانة (v2: per-trial resolution): the trial قيد becomes a firm sale —
    // the original is replaced by a conversion entry carrying the same stock effect, so the
    // goods stay out exactly once and voiding the conversion restores everything.
    fun convertTrialEntry(id: String) = set { s ->
        val e = s.entries.find { it.id == id } ?: return@set s
        if (e.trialAmount <= 0) return@set s
        val cust = s.customers.find { it.id == e.customerId }
        val conversion = DayEntry(
            id = "e" + System.currentTimeMillis(),
            t = "تحويل أمانة إلى بيع — " + (cust?.name ?: "زبونة"),
            d = "الآن · أصبحت ديناً على الزبونة",
            amt = fmt(e.trialAmount), cls = "ink",
            customerId = e.customerId,
            debtDelta = e.trialAmount, // now owed
            day = today(), saleAmount = e.trialAmount, cashAmount = 0,
            stockDelta = e.stockDelta, // goods stay out; a future void restores them
            lines = e.lines,
        )
        s.copy(entries = listOf(conversion) + s.entries.filterNot { it.id == id }, viewedDay = s.today, detailEntryId = null)
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
    fun openSale() = set { it.copy(screen = "sale", lines = emptyList(), pay = "full", partialPaid = 0, addNewOpen = false, saleCustomerId = null, editingId = null) }
    fun partialStep(d: Int) = set { it.copy(partialPaid = maxOf(0, it.partialPaid + d * 500)) }
    fun closeSheet() = set { it.copy(screen = "home", addNewOpen = false, editingId = null) }
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
        // F1/D11: an أمانة is lent to a specific customer — never نقدي. Block the save and
        // nudge the picker open («لمن الأمانة؟») instead of silently recording an ownerless trial.
        if (trialRequiresCustomer(s.pay, s.saleCustomerId)) {
            set { it.copy(custPickerOpen = true) }
            return
        }
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
        val editingId = s.editingId
        // A fresh sale gets an undo toast; an edit-replace commits directly (re-void to change again).
        val u = if (editingId == null) Undo(entry.id, s.shelf.map { it.id to it.sold }) else null
        set {
            val r = if (editingId != null) reversed(it, editingId) else it
            r.copy(
                screen = "home", lines = emptyList(), entries = listOf(entry) + r.entries,
                viewedDay = it.today,
                shelf = r.shelf.map { x -> soldMap[x.id]?.let { q -> x.copy(sold = x.sold + q) } ?: x },
                undo = u, editingId = null,
            )
        }
        if (u != null) armUndo(u)
    }
    fun undoSale() = set {
        val u = it.undo ?: return@set it
        val e = it.entries.find { x -> x.id == u.id }
        val shelf = if (e != null && e.cls == "withdraw") {
            applyWithdraw(it.shelf, e.stockDelta, restore = true) // withdrawals move `shelved`, not `sold`
        } else {
            it.shelf.map { x -> u.before.find { b -> b.first == x.id }?.let { b -> x.copy(sold = b.second) } ?: x }
        }
        it.copy(entries = it.entries.filterNot { x -> x.id == u.id }, shelf = shelf, undo = null)
    }

    // ── F6/D73: أخذت لنفسي / هدية — goods leave the محل with zero money, their own bucket ──
    fun openWithdraw() = set { it.copy(screen = "withdraw", lines = emptyList(), addNewOpen = false, saleCustomerId = null, editingId = null) }
    // withdraw stepper never exceeds في المحل, counting other lines of the same item (F6 fix)
    fun withdrawQtyStep(i: Int, d: Int) = set {
        val l = it.lines.getOrNull(i) ?: return@set it
        val others = it.lines.filterIndexed { j, x -> j != i && x.shelfId == l.shelfId }.sumOf { x -> x.qty }
        val cap = maxOf(1, (it.shelf.find { x -> x.id == l.shelfId }?.onHand ?: 1) - others)
        it.copy(lines = it.lines.mapIndexed { j, x -> if (j == i) x.copy(qty = (x.qty + d).coerceIn(1, cap)) else x })
    }
    fun saveWithdrawal() {
        // cap at what the shop holds (D73) — the invariant that keeps حذف an exact round-trip
        val lines = capWithdrawLines(s.shelf, s.lines)
        if (lines.isEmpty()) return
        val names = lines.joinToString(" + ") { it.name }
        val pieces = lines.sumOf { it.qty }
        val outMap = HashMap<String, Int>()
        lines.forEach { l -> outMap[l.shelfId] = (outMap[l.shelfId] ?: 0) + l.qty }
        val entry = DayEntry(
            id = "e" + System.currentTimeMillis(),
            t = "أخذت لنفسي / هدية" + (if (names.isNotEmpty()) " — $names" else ""),
            d = "الآن · خرجت من المحل بلا مبلغ",
            amt = "$pieces قطعة",
            cls = "withdraw",
            day = today(),
            // no money at all: not a sale, not debt, not a trial (D73)
            saleAmount = 0, cashAmount = 0, debtDelta = 0, trialAmount = 0,
            stockDelta = encodeStock(outMap),
            lines = encodeLines(lines),
        )
        val u = Undo(entry.id, s.shelf.map { it.id to it.sold })
        set {
            it.copy(
                screen = "home", lines = emptyList(), entries = listOf(entry) + it.entries,
                viewedDay = it.today,
                shelf = applyWithdraw(it.shelf, entry.stockDelta, restore = false), // pieces leave the shop
                undo = u, editingId = null,
            )
        }
        armUndo(u)
    }
}
