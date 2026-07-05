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
    PRE_APP("قبل التطبيق"),
    BALE("بالة"),
    MARKET("شراء من السوق"),
}

data class Source(
    val id: String,
    val kind: Kind,
    val label: String,
    val cost: Long? = null, // USD — BALE only; PRE_APP/MARKET carry none
)

data class Shelf(
    val id: String,
    val name: String,
    val tasira: Long,          // التسعيرة — the offer price
    val shelved: Int,          // count put on the shelf
    val sold: Int,
    val counted: Int? = null,  // two-tier: counted into the package (null ⇒ == shelved)
    val sourceId: String? = null, // null ⇒ غير محدد (the red dot)
    val buy: Long? = null,     // MARKET per-unit buy price (local)
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
)

fun encodeStock(delta: Map<String, Int>): String =
    delta.entries.filter { it.value != 0 }.joinToString(",") { "${it.key}:${it.value}" }

fun decodeStock(s: String): List<Pair<String, Int>> =
    if (s.isBlank()) emptyList() else s.split(",").mapNotNull {
        val p = it.split(":"); if (p.size == 2) p[0] to (p[1].toIntOrNull() ?: 0) else null
    }

fun entriesForDay(entries: List<DayEntry>, day: Long): List<DayEntry> =
    entries.filter { it.day == day }

fun salesForDay(entries: List<DayEntry>, day: Long): Long =
    entries.filter { it.day == day }.sumOf { it.saleAmount }

fun cashForDay(entries: List<DayEntry>, day: Long): Long =
    entries.filter { it.day == day }.sumOf { it.cashAmount }

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

// A named debtor. Balance = openingDebt + Σ(debtDelta of her entries). Positive = she owes.
data class Customer(
    val id: String,
    val name: String,
    val phone: String? = null,
    val openingDebt: Long = 0,
)

fun customerBalance(c: Customer, entries: List<DayEntry>): Long =
    c.openingDebt + entries.filter { it.customerId == c.id }.sumOf { it.debtDelta }

// Who owes the shop right now, largest first — the المواعيد list and the daily digest.
data class Debtor(val customer: Customer, val balance: Long)

fun debtors(customers: List<Customer>, entries: List<DayEntry>): List<Debtor> =
    customers.map { Debtor(it, customerBalance(it, entries)) }
        .filter { it.balance > 0 }
        .sortedByDescending { it.balance }

// The daily digest notification copy (title + body), kept pure so it can be tested
// without the worker/emulator.
fun digestTitleAndBody(due: List<Debtor>): Pair<String, String> {
    val total = due.sumOf { it.balance }
    val title = "${due.size} زبائن عليهن ديون — إجمالي ${fmt(total)}"
    val body = due.joinToString("، ") { it.customer.name + " (" + fmt(it.balance) + ")" }
    return title to body
}

data class SaleLine(
    val shelfId: String,
    val name: String,
    val tasira: Long,
    val price: Long,
    val qty: Int,
) {
    val haggled: Boolean get() = price != tasira
}

data class SetupChip(val name: String, val price: Long)
data class SetupPick(val name: String, val price: Long, val qty: Int)

data class OnbCard(val icon: String, val eyebrow: String, val title: String, val body: String)

data class StaticRow(val name: String, val sub: String, val amt: String, val cls: String)

fun fmt(n: Long): String = String.format(Locale.US, "%,d", n)
fun fmt(n: Int): String = fmt(n.toLong())

val SETUP_CHIPS = listOf(
    SetupChip("فستان", 10_000),
    SetupChip("بنطال", 7_500),
    SetupChip("قميص", 5_000),
    SetupChip("جاكيت", 12_000),
    SetupChip("طقم أطفال", 8_000),
    SetupChip("بلوزة", 4_000),
    SetupChip("تنورة", 6_000),
)

val ONB = listOf(
    OnbCard("📒", "اليوم", "دفترك اليومي", "كل بيع ودفعة وإرجاع يُسجَّل هنا — تفتح على صفحة اليوم، الأحدث أولاً، مثل دفترك الورقي تماماً."),
    OnbCard("🧺", "البضاعة", "رفّك", "ما لديك للبيع، كل صنف بتسعيرته. البيع يقترح من الرف مباشرةً — لا كتالوج ولا باركود ولا كتابة."),
    OnbCard("📦", "المصادر", "من أين أتت؟", "سجّلي كل بالة أو شراء وكلفته لتري أيّها ربح. بضاعتك القديمة تدخل تلقائياً تحت «قبل التطبيق»."),
)

// الزبائن / المواعيد are unchanged from v1 — the prototype shows them as static samples.
val CUST_ROWS = listOf(
    StaticRow("أم محمد", "موعد التسديد: أول آب", "6,500", "debt"),
    StaticRow("سميرة", "دفعت اليوم", "لا شيء", "paid"),
    StaticRow("فاطمة", "دين قديم", "35,000", "debt"),
)
val APPT_ROWS = listOf(
    StaticRow("أم محمد", "مستحق اليوم", "تذكير", ""),
    StaticRow("خالدة", "أمانة — 3 قطع", "أمانة", ""),
    StaticRow("فاطمة", "متأخر 3 أيام", "متأخر", ""),
)

fun initialSources() = listOf(
    Source(PRE_ID, Kind.PRE_APP, "قبل التطبيق", null),
    Source(MKT_ID, Kind.MARKET, "شراء من السوق", null),
)

fun sampleSources() = listOf(
    Source(PRE_ID, Kind.PRE_APP, "قبل التطبيق", null),
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
    DayEntry("e1", "بيع — أم محمد — فستان + بنطال", "11:40 · دفعت 10,000 والباقي دين", "16,500", "ink", "c_um", 6_500, today, saleAmount = 16_500, cashAmount = 10_000),
    DayEntry("e2", "دفعة — سميرة", "11:15 · عن دين قديم", "+ 10,000", "pos", "c_sam", -10_000, today, saleAmount = 0, cashAmount = 10_000),
    DayEntry("e3", "بيع نقدي — قميص", "9:50", "4,000 ✓", "pos", null, 0, today, saleAmount = 4_000, cashAmount = 4_000),
)

// ── derived (mirrors renderVals) ──

data class SourceView(
    val id: String,
    val label: String,
    val kindLabel: String,
    val remain: Int,
    val isBale: Boolean,
    val inPkg: Int,
    val costFmt: String,
    val revFmt: String,
    val profitFmt: String,
    val profit: Long?, // null ⇒ no cost basis (قبل التطبيق)
)

fun revenueBySource(shelf: List<Shelf>): Map<String, Long> {
    val m = HashMap<String, Long>()
    for (x in shelf) {
        val k = x.sourceId ?: "_"
        m[k] = (m[k] ?: 0L) + x.sold.toLong() * x.tasira
    }
    return m
}

fun remainBySource(shelf: List<Shelf>): Map<String, Int> {
    val m = HashMap<String, Int>()
    for (x in shelf) {
        val k = x.sourceId ?: "_"
        m[k] = (m[k] ?: 0) + maxOf(0, x.cnt - x.sold)
    }
    return m
}

fun sourceViews(sources: List<Source>, shelf: List<Shelf>): List<SourceView> {
    val rev = revenueBySource(shelf)
    val rem = remainBySource(shelf)
    return sources.map { s ->
        val r = rev[s.id] ?: 0L
        val mkt = if (s.kind == Kind.MARKET)
            shelf.filter { it.sourceId == s.id && it.buy != null }
                .sumOf { (it.buy ?: 0L) * it.shelved } else null
        val costLocal: Long? = when (s.kind) {
            Kind.BALE -> (s.cost ?: 0L) * USD_RATE
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
        )
    }
}
