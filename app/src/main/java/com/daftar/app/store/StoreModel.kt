package com.daftar.app.store

import java.util.Locale

// ── D51 model, ported 1:1 from docs/design-sessions/V2/daftar-app-v2.html ──
// البضاعة is the shelf (the only thing a sale suggests from); المصادر are provenance + cost;
// stock is priced with التسعيرة (the offer price). Numbers render Western with thousands
// separators exactly as the prototype (toLocaleString('en-US')).

const val PRE_ID = "src_pre" // قبل التطبيق — the default "before the app" provenance
const val MKT_ID = "src_mkt" // شراء من السوق — market pickings
const val USD_RATE = 1500L   // BALE cost is entered in USD; local ≈ USD × 1500

enum class Kind(val label: String) {
    PRE_APP("تحديد لاحقاً"),
    BALE("بالة"),
    MARKET("شراء من السوق"),
}

data class Source(
    val id: String,
    val kind: Kind,
    val label: String,
    val cost: Long? = null, // USD — BALE only; PRE_APP/MARKET carry none
    // MARKET shops only (v2 decision 11): what SHE owes this shop — they sell her on
    // credit; one editable number she adjusts when she pays or takes more.
    val debt: Long = 0,
    // BALE only: the TOTAL pieces she counted at purchase (e.g. 175) — naming item types
    // allocates against it, the remainder stays visible. null for buckets/shops and legacy bales.
    val countTotal: Int? = null,
    // BALE only: the USD→local rate FROZEN at purchase. A bale's cost is locked to the rate the
    // day she bought it; the global «سعر صرف اليوم» is only the default for new bales. null for
    // buckets/shops and legacy bales — those fall back to the live global rate (migration backfill
    // semantics: no data write, the code default IS the old behaviour).
    val ratePurchase: Long? = null,
)

// A bale-owned expense (كوي, نقل، …): a label + SYP amount deducted FULLY from that bale's
// profit. Bale-owned and freely editable (add/remove without a confirm), so it never touches
// the ledger's undo/void machinery.
data class BaleExpense(
    val id: String,
    val sourceId: String,
    val label: String,
    val amount: Long,
)

// Everything spent on a bale — deducted in full from its profit (D-bale-expenses).
fun baleExpensesTotal(sourceId: String, expenses: List<BaleExpense>): Long =
    expenses.filter { it.sourceId == sourceId }.sumOf { it.amount }

// The quick-pick labels the expense add-row offers: «كوي» always, plus every distinct label
// she has already used on ANY bale (so her own vocabulary comes back).
fun expenseLabelChips(expenses: List<BaleExpense>): List<String> =
    (listOf("كوي") + expenses.map { it.label }).distinct()

// SLICE 4 allocation: a bale stores a TOTAL counted piece count; naming item types allocates
// against it. Allocated = Σ cnt of its shelf items; the remainder («متبقي غير مصنّف») may go
// negative when she names more than she counted (a warning, never a block).
fun baleAllocated(sourceId: String, shelf: List<Shelf>): Int =
    shelf.filter { it.sourceId == sourceId }.sumOf { it.cnt }

fun baleUnallocated(countTotal: Int, allocated: Int): Int = countTotal - allocated

// The bale page's one USD summary line: SYP profit converted at the bale's FROZEN rate (the
// live global rate for legacy bales). Rounded to whole dollars; null with no cost basis or rate.
fun baleUsdProfit(profit: Long?, rate: Long): Long? =
    if (profit == null || rate <= 0L) null else Math.round(profit.toDouble() / rate)

// محلات السوق: the shops living inside the one شراء من السوق card (never the system row).
fun marketShops(sources: List<Source>): List<Source> =
    sources.filter { it.kind == Kind.MARKET && it.id != MKT_ID }

fun marketDebtTotal(sources: List<Source>): Long =
    sources.filter { it.kind == Kind.MARKET }.sumOf { it.debt }

data class Shelf(
    val id: String,
    val name: String,
    val tasira: Long,          // التسعيرة — the offer price
    val shelved: Int,          // count put on the shelf
    val sold: Int,
    val counted: Int? = null,  // two-tier: counted into the package (null ⇒ == shelved)
    val sourceId: String? = null, // null ⇒ غير محدد (the red dot)
    val buy: Long? = null,     // MARKET per-unit buy price (local)
    val finished: Boolean = false, // «خلصت» — retired from sale suggestions, independent of onHand (2026-07-19)
) {
    val onHand: Int get() = shelved - sold
    val cnt: Int get() = counted ?: shelved
    val inPkg: Int get() = maxOf(0, cnt - shelved) // still in the bale, not yet shelved
    val unspecified: Boolean get() = sourceId == null
}

// A day-book row (already rendered — title/subtitle/amount as the prototype builds them).
// customerId + debtDelta carry the ledger truth under the display strings: debtDelta is
// how much this entry moved that customer's balance (+ = new debt, − = payment).
// day = the epoch-day it was recorded on; saleAmount/cashAmount feed the day's totals.
data class DayEntry(
    val id: String,
    val t: String,
    val d: String,
    val amt: String,
    val cls: String, // "ink" | "pos" | "amber"
    val customerId: String? = null,
    val debtDelta: Long = 0,
    val day: Long = 0,
    val saleAmount: Long = 0,
    val cashAmount: Long = 0,
    // How this entry changed each shelf item's `sold` count ("id:delta,id:delta"),
    // so voiding it can put the stock back. Sales add sold (+), returns subtract (−).
    val stockDelta: String = "",
    // أمانة (on-trust) value: goods lent to try, not a firm sale or hard debt — tracked
    // here separately so it can be followed up, returned (void), or later sold.
    val trialAmount: Long = 0,
    // The sold lines ("shelfId|name|price|qty;…"), so the sale detail can show each item
    // and let it be attributed to its source.
    val lines: String = "",
    // D68 «دفعتُ للمحل»: money-out to a supplier. sourceId names the shop, moneyOut the
    // amount. cashAmount stays 0 — money-out never counts in قبضنا اليوم.
    val sourceId: String? = null,
    val moneyOut: Long = 0,
    // D71 soft delete: a voided قيد stays visible (struck-through) but counts for nothing.
    val voided: Boolean = false,
)

data class SoldLine(val shelfId: String, val name: String, val price: Long, val qty: Int)

fun encodeLines(lines: List<SaleLine>): String =
    lines.joinToString(";") { "${it.shelfId}|${it.name}|${it.price}|${it.qty}" }

fun decodeLines(s: String): List<SoldLine> =
    if (s.isBlank()) emptyList() else s.split(";").mapNotNull {
        val p = it.split("|")
        if (p.size == 4) SoldLine(p[0], p[1], p[2].toLongOrNull() ?: 0, p[3].toIntOrNull() ?: 0) else null
    }

fun encodeStock(delta: Map<String, Int>): String =
    delta.entries.filter { it.value != 0 }.joinToString(",") { "${it.key}:${it.value}" }

fun decodeStock(s: String): List<Pair<String, Int>> =
    if (s.isBlank()) emptyList() else s.split(",").mapNotNull {
        val p = it.split(":"); if (p.size == 2) p[0] to (p[1].toIntOrNull() ?: 0) else null
    }

fun entriesForDay(entries: List<DayEntry>, day: Long): List<DayEntry> =
    entries.filter { it.day == day }

// D71 soft delete: a voided قيد stays in the book (struck-through) but counts for nothing.
// Every money/debt derivation skips voided rows; a sale's stock effect is separately reversed
// when it's voided, so البضاعة stays correct too.
fun salesForDay(entries: List<DayEntry>, day: Long): Long =
    entries.filter { it.day == day && !it.voided }.sumOf { it.saleAmount }

fun cashForDay(entries: List<DayEntry>, day: Long): Long =
    entries.filter { it.day == day && !it.voided }.sumOf { it.cashAmount }

// Arabic label for a day, relative to today.
fun dayLabel(day: Long, today: Long): String = when (day) {
    today -> "اليوم"
    today - 1 -> "أمس"
    else -> {
        val d = java.time.LocalDate.ofEpochDay(day)
        val names = arrayOf("الاثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت", "الأحد")
        "${names[d.dayOfWeek.value - 1]} ${d.dayOfMonth}/${d.monthValue}"
    }
}

// Rolling the day book forward when the app resumes after midnight (left open overnight).
// The viewed page follows only if it sat on the old today — a deliberately flipped-back
// past page stays put.
fun rollViewedDay(today: Long, viewedDay: Long, newToday: Long): Long =
    if (viewedDay == today) newToday else viewedDay

// The day book never shows a future page: a jump (calendar picker) or a step is clamped to
// ≤ today. There's no lower bound — she can page back as far as her ledger goes.
fun clampViewedDay(today: Long, day: Long): Long = minOf(today, day)

// A named debtor. Balance = openingDebt + Σ(debtDelta of her entries). Positive = she owes.
data class Customer(
    val id: String,
    val name: String,
    val phone: String? = null,
    val openingDebt: Long = 0,
)

// The standing catch-all customer: an entry with no named customer files under her (replaces the
// old نقدي = null phantom — 2026-07-18 domain redesign). A real customer with a fixed id so
// balance/attribution work uniformly, and an entry can be reassigned to a named customer later.
// Always present in the roster via ensureGeneric().
const val GENERIC_ID = "c_unspecified"

fun genericCustomer(): Customer = Customer(GENERIC_ID, "زبونة غير محددة")

fun ensureGeneric(customers: List<Customer>): List<Customer> =
    if (customers.any { it.id == GENERIC_ID }) customers else listOf(genericCustomer()) + customers

fun customerBalance(c: Customer, entries: List<DayEntry>): Long =
    c.openingDebt + entries.filter { it.customerId == c.id && !it.voided }.sumOf { it.debtDelta }

// What she'd owe the instant a دفعة is saved WITH the selected تجريب kept: her live balance plus
// the value of the trial she decided to keep (kept → becomes debt just before the payment reduces
// it). Drives the دفعة context line and the «سداد كامل» quick-fill, and the paper-debt catch is
// measured against this (so keeping a 10,000 تجريب and paying 10,000 doesn't falsely fire).
fun payOwedWithKept(cust: Customer, entries: List<DayEntry>, keptTrialId: String?): Long {
    val kept = keptTrialId
        ?.let { id -> entries.find { it.id == id } }
        ?.takeIf { it.trialAmount > 0 && !it.voided }?.trialAmount ?: 0L
    return customerBalance(cust, entries) + kept
}

// F3 paper-debt catch. A new دفعة larger than her recorded balance would flip it negative
// (لها — the shop owing her), which in the first trial days almost always means her paper-era
// debt was never entered. This returns the opening debt to record so the payment lands her at
// zero; null when the payment fits and no prompt is needed. Declining records nothing and lets
// the balance legitimately go negative.
fun paperDebtShortfall(balanceBefore: Long, payment: Long): Long? =
    if (payment > balanceBefore) payment - balanceBefore else null

// أمانة still out with her (goods on trust, not yet firm debt). The net is the source of
// truth for totals — a keep-&-pay قيد nets a kept trial with a matching −trialAmount, so this
// already excludes what she decided to keep.
fun customerTrial(c: Customer, entries: List<DayEntry>): Long =
    entries.filter { it.customerId == c.id && !it.voided }.sumOf { it.trialAmount }

// The trials STILL open with a customer — her un-voided +trial قيود minus those a keep-&-pay
// settlement already netted (each settlement carries −trialAmount of the kept قيد). Drives the
// per-trial resolve UI (أبقتها/أعادتها) and the دفعة chips, so a trial she already kept no longer
// offers to be resolved again. `entries` is newest-first; settlements pool by value and cover the
// matching positive trials exactly (a settlement always zeroes a whole trial).
fun openTrialEntries(customerId: String?, entries: List<DayEntry>): List<DayEntry> {
    if (customerId == null) return emptyList()
    val mine = entries.filter { it.customerId == customerId && !it.voided && it.trialAmount != 0L }
    var settled = mine.filter { it.trialAmount < 0 }.sumOf { -it.trialAmount }
    val open = ArrayList<DayEntry>()
    for (e in mine.filter { it.trialAmount > 0 }) {
        if (settled >= e.trialAmount) settled -= e.trialAmount else open.add(e)
    }
    return open
}

// إرجاع suggests what SHE actually took: the lines across her un-voided قيود (sales and تجريب),
// newest قيد first, one chip per shelf item (deduplicated), capped. Each keeps the price she was
// recorded at so a return pre-fills that value. With no customer the caller falls back to الرف.
fun customerTakenLines(customerId: String?, entries: List<DayEntry>, max: Int = 10): List<SoldLine> {
    if (customerId == null) return emptyList()
    val seen = HashSet<String>()
    val out = ArrayList<SoldLine>()
    for (e in entries.filter { it.customerId == customerId && !it.voided }) {
        for (l in decodeLines(e.lines)) {
            if (seen.add(l.shelfId)) out.add(l)
            if (out.size >= max) return out
        }
    }
    return out
}

// An أمانة is goods lent to a *specific* customer to try — it is never نقدي (D11/FR-5).
// A trial with no customer must be blocked at capture and the picker nudged open.
fun trialRequiresCustomer(pay: String, customerId: String?): Boolean =
    pay == "trial" && customerId == null

// A personal/gift withdrawal (D73) removes pieces from the shop *and* from stock without being a
// sale — so it decrements `shelved` (and `counted` when tracked), never `sold`, keeping revenue
// (sold × tasira) and the "still in bale" count untouched. restore=true reverses it (void/undo).
fun applyWithdraw(shelf: List<Shelf>, stockDelta: String, restore: Boolean): List<Shelf> {
    val deltas = decodeStock(stockDelta)
    val f = if (restore) 1 else -1
    return shelf.map { x ->
        deltas.find { it.first == x.id }?.let { d ->
            x.copy(
                shelved = maxOf(0, x.shelved + f * d.second),
                counted = x.counted?.let { c -> maxOf(0, c + f * d.second) },
            )
        } ?: x
    }
}

// الحساب bucket (D73): what left the shop for herself or as gifts — pieces, and their value at
// asking price (تقريباً). Voided withdrawals don't count; withdrawals never touch sales/profit.
fun withdrawnPieces(entries: List<DayEntry>): Int =
    entries.filter { it.cls == "withdraw" && !it.voided }
        .sumOf { decodeStock(it.stockDelta).sumOf { d -> d.second } }

fun withdrawnValue(entries: List<DayEntry>): Long =
    entries.filter { it.cls == "withdraw" && !it.voided }
        .sumOf { decodeLines(it.lines).sumOf { l -> l.price * l.qty } }

// A withdrawal can never take more than the shop holds (D73): cap lines at في المحل, summing
// across lines of the same item, so the recorded delta equals what actually left — and a later
// حذف restores exactly that (no phantom stock). Lines capped to nothing are dropped.
fun capWithdrawLines(shelf: List<Shelf>, lines: List<SaleLine>): List<SaleLine> {
    val remaining = HashMap<String, Int>()
    shelf.forEach { remaining[it.id] = maxOf(0, it.onHand) }
    return lines.mapNotNull { l ->
        val r = remaining[l.shelfId] ?: 0
        val q = minOf(l.qty, r)
        if (q <= 0) null else { remaining[l.shelfId] = r - q; l.copy(qty = q) }
    }
}

// Who owes the shop right now, largest first — the المواعيد list and the daily digest.
data class Debtor(val customer: Customer, val balance: Long)

fun debtors(customers: List<Customer>, entries: List<DayEntry>): List<Debtor> =
    customers.map { Debtor(it, customerBalance(it, entries)) }
        .filter { it.balance > 0 }
        .sortedByDescending { it.balance }

// Everyone worth chasing: owes money and/or has أمانة out. Most urgent = biggest (debt + trial).
data class FollowUp(val customer: Customer, val debt: Long, val trial: Long)

fun followUps(customers: List<Customer>, entries: List<DayEntry>): List<FollowUp> =
    customers.map { FollowUp(it, customerBalance(it, entries), customerTrial(it, entries)) }
        .filter { it.debt > 0 || it.trial > 0 }
        .sortedByDescending { it.debt + it.trial }

data class SaleLine(
    val shelfId: String,
    val name: String,
    val tasira: Long,
    val price: Long,
    val qty: Int,
) {
    val haggled: Boolean get() = price != tasira
}

data class StaticRow(val name: String, val sub: String, val amt: String, val cls: String)

fun fmt(n: Long): String = String.format(Locale.US, "%,d", n)
fun fmt(n: Int): String = fmt(n.toLong())

// Short in-use tips (shown on the day book, dismissible) instead of a first-run demo splash.
val USAGE_TIPS = listOf(
    "اضغطي على أي قيد في الدفتر لرؤية تفاصيله أو إلغائه",
    "في البيع، «دفعت جزءاً» تسألك كم دفعت — والباقي يُسجَّل ديناً",
    "من «البضاعة» أضيفي أصنافك — ووجّهي كل صنف لبالته إن عرفتِها",
    "«المواعيد» تذكّرك بالديون المستحقة، ويمكنك تأجيل التذكير بضغطة",
    "«تجريب» بضاعة عند الزبونة لم تُبَع بعد — لا تُحسب ديناً حتى تقرّر",
)

// الزبائن / المواعيد are unchanged from v1 — the prototype shows them as static samples.
val CUST_ROWS = listOf(
    StaticRow("أم محمد", "موعد التسديد: أول آب", "6,500", "debt"),
    StaticRow("سميرة", "دفعت اليوم", "لا شيء", "paid"),
    StaticRow("فاطمة", "دين قديم", "35,000", "debt"),
)
val APPT_ROWS = listOf(
    StaticRow("أم محمد", "مستحق اليوم", "تذكير", ""),
    StaticRow("خالدة", "تجريب — 3 قطع", "تجريب", ""),
    StaticRow("فاطمة", "متأخر 3 أيام", "متأخر", ""),
)

fun initialSources() = listOf(
    Source(PRE_ID, Kind.PRE_APP, "تحديد لاحقاً", null),
    Source(MKT_ID, Kind.MARKET, "شراء من السوق", null),
)

fun sampleSources() = listOf(
    Source(PRE_ID, Kind.PRE_APP, "تحديد لاحقاً", null),
    Source(MKT_ID, Kind.MARKET, "شراء من السوق", null),
    Source("s_dr", Kind.BALE, "بالة فساتين", 400),
    Source("s_pc", Kind.BALE, "بالة بنطال + قميص", 400),
)

fun sampleShelf() = listOf(
    Shelf("h1", "فستان", 10_000, 34, 12, counted = 50, sourceId = "s_dr"),
    Shelf("h2", "بنطال", 7_500, 40, 28, sourceId = "s_pc"),
    Shelf("h3", "بنطال", 5_000, 22, 25, sourceId = PRE_ID),
    Shelf("h4", "قميص", 5_000, 22, 9, counted = 30, sourceId = "s_pc"),
    Shelf("h5", "جاكيت", 12_000, 6, 3, sourceId = PRE_ID),
    Shelf("h6", "طقم أطفال", 8_000, 35, 11, sourceId = null),
    Shelf("h7", "بلوزة", 4_000, 30, 11, sourceId = PRE_ID),
    Shelf("h8", "تنورة", 6_000, 18, 18, sourceId = null),
    Shelf("h9", "حقيبة", 9_000, 5, 2, sourceId = MKT_ID, buy = 4_000),
)

// Sample debtors — balances match the prototype's static rows once the sample
// entries below apply: أم محمد 6,500 (0 + 6,500 debt), سميرة 0 (10,000 − 10,000 paid), فاطمة 35,000.
fun sampleCustomers() = listOf(
    Customer("c_um", "أم محمد", null, 0),
    Customer("c_sam", "سميرة", null, 10_000),
    Customer("c_fat", "فاطمة", null, 35_000),
)

fun sampleEntries(today: Long) = listOf(
    DayEntry("e1", "بيع — أم محمد — فستان + بنطال", "11:40 · دفعت 10,000 والباقي دين", "16,500", "ink", "c_um", 6_500, today, saleAmount = 16_500, cashAmount = 10_000, lines = "h1|فستان|10000|1;h2|بنطال|6500|1"),
    DayEntry("e2", "دفعة — سميرة", "11:15 · عن دين قديم", "+ 10,000", "pos", "c_sam", -10_000, today, saleAmount = 0, cashAmount = 10_000),
    DayEntry("e3", "بيع نقدي — قميص", "9:50", "4,000 ✓", "pos", null, 0, today, saleAmount = 4_000, cashAmount = 4_000, lines = "h4|قميص|4000|1"),
)

// ── derived (mirrors renderVals) ──

data class SourceView(
    val id: String,
    val label: String,
    val kindLabel: String,
    val remain: Int,
    val sold: Int,
    val isBale: Boolean,
    val inPkg: Int,
    val costFmt: String,
    val revFmt: String,
    val profitFmt: String,
    val profit: Long?, // null ⇒ no cost basis (قبل التطبيق)
    // raw values so the one شراء من السوق card can combine its shops (v2 decision 11)
    val kind: Kind,
    val costLocal: Long?,
    val revenue: Long,
    val debt: Long,
    // BALE only (slice 2/3/4): the frozen rate, counted total, and expenses folded into cost.
    val cost: Long?,          // raw USD cost, so the bale page can render «$275 + expenses»
    val ratePurchase: Long?,  // the frozen rate (null ⇒ legacy bale on the live rate)
    val countTotal: Int?,     // pieces counted at purchase (null ⇒ legacy/non-bale)
    val expensesTotal: Long,  // Σ this bale's expenses, already inside costLocal
)

fun revenueBySource(shelf: List<Shelf>): Map<String, Long> {
    val m = HashMap<String, Long>()
    for (x in shelf) {
        val k = x.sourceId ?: "_"
        m[k] = (m[k] ?: 0L) + x.sold.toLong() * x.tasira
    }
    return m
}

// V3 merge (owner decision): the two no-source buckets read as ONE «بضاعة قديمة — بلا مصدر».
// An item belongs to that merged bucket when it has no source (null), sits in the PRE_ID bucket,
// OR carries a legacy MARKET/shop sourceId (شراء من السوق is hidden this release — its data stays
// intact but folds into old stock in the UI). Only BALE-sourced items keep their own group.
fun isOldStockNoSource(item: Shelf, sources: List<Source>): Boolean {
    if (item.sourceId == null || item.sourceId == PRE_ID) return true
    return sources.find { it.id == item.sourceId }?.kind != Kind.BALE
}

// V3 bale framing (owner: negative numbers frustrate). Instead of a «− N» loss, a bale that
// hasn't earned back its cost shows how much CAPITAL is still to recover, as a positive number;
// once recovered it shows profit. costLocal is the bale's inclusive cost (USD×frozen rate +
// expenses). null costLocal ⇒ no cost basis (caller shows «—»). At exactly cost, recovered=true,
// profit=+0 (green). No surface ever renders a negative sign.
data class BaleFraming(
    val recovered: Boolean,           // revenue ≥ inclusive cost
    val sold: Boolean,                // any pieces sold (drives the pre-recovery status line)
    val remainingToRecover: Long?,    // pre-recovery: cost − revenue (≥ 0); post: null
    val profit: Long?,                // post-recovery: revenue − cost (≥ 0); pre: null
    val statusLine: String,
)

fun baleFraming(revenue: Long, costLocal: Long?, sold: Int): BaleFraming? {
    if (costLocal == null) return null
    return if (revenue >= costLocal) BaleFraming(
        recovered = true, sold = sold > 0,
        remainingToRecover = null, profit = revenue - costLocal,
        statusLine = "ربحت هذه البالة ✓",
    ) else BaleFraming(
        recovered = false, sold = sold > 0,
        remainingToRecover = costLocal - revenue, profit = null,
        statusLine = if (sold == 0) "لم يبدأ البيع بعد" else "في مرحلة استرداد رأس المال",
    )
}

fun remainBySource(shelf: List<Shelf>): Map<String, Int> {
    val m = HashMap<String, Int>()
    for (x in shelf) {
        val k = x.sourceId ?: "_"
        m[k] = (m[k] ?: 0) + maxOf(0, x.cnt - x.sold)
    }
    return m
}

// D68: a shop's current debt is derived — stored (entered) debt minus what supplier-payment
// entries paid. Voiding a payment removes its entry, so the debt restores by construction
// (the same never-stored discipline as customer balances).
fun supplierPaid(entries: List<DayEntry>, sourceId: String): Long =
    entries.filter { it.sourceId == sourceId && !it.voided }.sumOf { it.moneyOut }

fun shopDebtNow(src: Source, entries: List<DayEntry>): Long =
    src.debt - supplierPaid(entries, src.id)

// F4 estimated profit for untracked goods (قبل التطبيق / غير محدد — the only sources with no
// cost basis). The margin is LEARNED from items that DO carry a per-piece cost (شراء من السوق
// buy price): margin = (tasira − buy) / tasira. Same item NAME first, else the shop-wide
// average of tracked items. Applied to untracked SOLD revenue (sold × tasira).
//   • returns null  ⇒ nothing to learn from (no tracked item) → the UI shows «—», never a guess
//   • returns 0     ⇒ we CAN estimate, but no untracked pieces have sold yet
// This is its OWN number: never summed into real ربح تقريباً, never touches cash-in-hand (D70).
fun estimatedUntrackedProfit(shelf: List<Shelf>): Long? {
    fun margin(s: Shelf): Double? {
        val buy = s.buy ?: return null
        if (s.tasira <= 0 || buy < 0) return null
        return (s.tasira - buy).toDouble() / s.tasira
    }
    val tracked = shelf.mapNotNull { s -> margin(s)?.let { s.name to it } }
    if (tracked.isEmpty()) return null // no cost basis anywhere → honest «—»
    val shopWide = tracked.map { it.second }.average()
    val byName = tracked.groupBy({ it.first }, { it.second }).mapValues { it.value.average() }

    var profit = 0.0
    for (u in shelf) {
        if (!(u.sourceId == PRE_ID || u.unspecified) || u.sold <= 0) continue
        val m = byName[u.name] ?: shopWide
        profit += u.sold.toLong() * u.tasira * m
    }
    return profit.toLong()
}

// F1 per-item page stats, derived from the actual sale lines in the ledger (voided entries are
// already gone from `entries`, so this is always current truth). Profit is shown only when the
// item has a cost basis — a شراء buy price, or its share of its bale's cost.
data class ItemStats(
    val soldPieces: Int,
    val revenue: Long,        // actual money taken (Σ agreed price × qty), not tasira × sold
    val avgSellPrice: Long?,  // revenue ÷ pieces; null until something sells
    val lastSoldDay: Long?,   // epoch-day of the most recent sale containing it
    val profit: Long?,        // null ⇒ no cost basis (قبل التطبيق / غير محدد, no buy)
    val perPieceCost: Long?,  // the item's share of its bale's inclusive cost (incl. expenses), or its buy
)

fun itemStats(item: Shelf, sources: List<Source>, shelf: List<Shelf>, entries: List<DayEntry>, usdRate: Long, expenses: List<BaleExpense> = emptyList()): ItemStats {
    var qty = 0
    var rev = 0L
    var last = Long.MIN_VALUE
    for (e in entries) for (l in decodeLines(e.lines)) if (l.shelfId == item.id) {
        qty += l.qty
        rev += l.price * l.qty
        if (e.day > last) last = e.day
    }
    val perPieceCost: Long? = when {
        item.buy != null -> item.buy // شراء من السوق — real per-piece cost
        else -> {
            val src = sources.find { it.id == item.sourceId }
            if (src?.kind == Kind.BALE) {
                // a bale's cost is frozen to its purchase-day rate; legacy bales use the live rate.
                // Bale expenses (كوي، نقل…) are part of the cost basis, so the per-piece share
                // includes them — same inclusive cost sourceViews folds into costLocal.
                val baleCost = (src.cost ?: 0L) * (src.ratePurchase ?: usdRate) + baleExpensesTotal(src.id, expenses)
                val piecesInBale = shelf.filter { it.sourceId == src.id }.sumOf { it.cnt }
                if (piecesInBale > 0) baleCost / piecesInBale else null // the item's share of the bale
            } else null
        }
    }
    return ItemStats(
        soldPieces = qty,
        revenue = rev,
        avgSellPrice = if (qty > 0) rev / qty else null,
        lastSoldDay = if (qty > 0) last else null,
        profit = perPieceCost?.let { rev - it * qty },
        perPieceCost = perPieceCost,
    )
}

// Most-recent sale day per shelf item, read from the ledger's sale lines in one pass (voided
// entries are already gone from `entries`). Drives the new-entry picker's "recently sold first"
// order — one map for the whole shelf, instead of an itemStats() scan per item.
fun lastSoldByItem(entries: List<DayEntry>): Map<String, Long> {
    val m = HashMap<String, Long>()
    for (e in entries) for (l in decodeLines(e.lines)) {
        val prev = m[l.shelfId]
        if (prev == null || e.day > prev) m[l.shelfId] = e.day
    }
    return m
}

// The shelf items offered in the new-entry / withdraw picker, ordered for quick entry:
// most-recently-sold first, then never-sold items (most stock on hand first). Out-of-stock and
// «خلصت» (finished) items are excluded — she can't add what isn't there. When the list is long
// the caller shows a leading slice and folds the cold tail behind «المزيد».
fun pickerItems(shelf: List<Shelf>, entries: List<DayEntry>): List<Shelf> {
    val last = lastSoldByItem(entries)
    return shelf.filter { it.onHand > 0 && !it.finished }
        .sortedWith(
            compareByDescending<Shelf> { last[it.id] ?: Long.MIN_VALUE }
                .thenByDescending { it.onHand },
        )
}

fun soldBySource(shelf: List<Shelf>): Map<String, Int> {
    val m = HashMap<String, Int>()
    for (x in shelf) {
        val k = x.sourceId ?: "_"
        m[k] = (m[k] ?: 0) + x.sold
    }
    return m
}

// F1 bale-screen stats. Capital recovery — «رجّعت ثمنها؟» — is raw percent (passes 100 once
// the bale has paid for itself; the bar caps the drawing, not the number). null ⇒ nothing
// to recover against (no cost basis). Both move with today's rate, like profit (R3).
fun recoveryPct(revenue: Long, costLocal: Long?): Int? =
    if (costLocal == null || costLocal <= 0L) null else (revenue * 100 / costLocal).toInt()

// Average fetched per sold piece; null until something sells.
fun avgSoldPrice(revenue: Long, sold: Int): Long? = if (sold <= 0) null else revenue / sold

fun sourceViews(sources: List<Source>, shelf: List<Shelf>, usdRate: Long, expenses: List<BaleExpense> = emptyList()): List<SourceView> {
    val rev = revenueBySource(shelf)
    val rem = remainBySource(shelf)
    val sold = soldBySource(shelf)
    return sources.map { s ->
        val r = rev[s.id] ?: 0L
        val mkt = if (s.kind == Kind.MARKET)
            shelf.filter { it.sourceId == s.id && it.buy != null }
                .sumOf { (it.buy ?: 0L) * it.shelved } else null
        val exp = baleExpensesTotal(s.id, expenses)
        // A bale's cost is frozen to the rate the day she bought it (ratePurchase); legacy bales
        // fall back to the live global rate. Bale expenses are deducted IN FULL — folded into the
        // local cost so profit and capital-recovery both count them.
        val costLocal: Long? = when (s.kind) {
            Kind.BALE -> (s.cost ?: 0L) * (s.ratePurchase ?: usdRate) + exp
            Kind.MARKET -> mkt
            Kind.PRE_APP -> null
        }
        val profit = costLocal?.let { r - it }
        val inPkg = shelf.filter { it.sourceId == s.id }.sumOf { maxOf(0, it.cnt - it.shelved) }
        SourceView(
            id = s.id,
            label = s.label,
            kindLabel = s.kind.label,
            remain = rem[s.id] ?: 0,
            sold = sold[s.id] ?: 0,
            isBale = s.kind == Kind.BALE,
            inPkg = inPkg,
            costFmt = when (s.kind) {
                Kind.BALE -> "$" + fmt(s.cost ?: 0L)
                Kind.MARKET -> fmt(mkt ?: 0L)
                Kind.PRE_APP -> "—"
            },
            revFmt = fmt(r),
            profitFmt = if (profit == null) "—"
            else (if (profit >= 0) "+ " else "− ") + fmt(kotlin.math.abs(profit)),
            profit = profit,
            kind = s.kind,
            costLocal = costLocal,
            revenue = r,
            debt = s.debt,
            cost = s.cost,
            ratePurchase = s.ratePurchase,
            countTotal = s.countTotal,
            expensesTotal = exp,
        )
    }
}
