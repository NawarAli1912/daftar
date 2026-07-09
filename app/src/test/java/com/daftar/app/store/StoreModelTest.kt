package com.daftar.app.store

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// Locks the D51 derivations to the V2 prototype's renderVals() math. The expected
// numbers here are exactly what the prototype's sample data renders.
class StoreModelTest {

    private val sources = sampleSources()
    private val shelf = sampleShelf()

    @Test
    fun `onHand cnt and inPkg follow the two-tier rules`() {
        val fustan = shelf.first { it.id == "h1" } // shelved 34, sold 12, counted 50
        assertEquals(22, fustan.onHand)
        assertEquals(50, fustan.cnt)
        assertEquals(16, fustan.inPkg) // 50 counted − 34 shelved
        val bantalPre = shelf.first { it.id == "h3" } // shelved 22, sold 25, no count
        assertEquals(-3, bantalPre.onHand) // oversold — kept honest
        assertEquals(22, bantalPre.cnt) // cnt falls back to shelved
        assertEquals(0, bantalPre.inPkg)
    }

    @Test
    fun `fmt renders western digits with thousands separators`() {
        assertEquals("10,000", fmt(10_000))
        assertEquals("205,000", fmt(205_000L))
        assertEquals("0", fmt(0))
    }

    @Test
    fun `revenue is attributed sold times tasira per source`() {
        val rev = revenueBySource(shelf)
        assertEquals(120_000L, rev["s_dr"]) // فستان 12 × 10,000
        assertEquals(255_000L, rev["s_pc"]) // بنطال 28×7,500 + قميص 9×5,000
        assertEquals(205_000L, rev[PRE_ID]) // 25×5,000 + 3×12,000 + 11×4,000
        assertEquals(18_000L, rev[MKT_ID]) // حقيبة 2 × 9,000
    }

    @Test
    fun `remaining on shelf is counted minus sold, clamped at zero`() {
        val rem = remainBySource(shelf)
        assertEquals(38, rem["s_dr"]) // 50 − 12
        assertEquals(33, rem["s_pc"]) // (40−28) + (30−9)
        assertEquals(22, rem[PRE_ID]) // (22−25→0) + (6−3) + (30−11)
        assertEquals(3, rem[MKT_ID]) // 5 − 2
    }

    @Test
    fun `bale profit is revenue minus cost in USD times the local rate`() {
        val dr = sourceViews(sources, shelf, 1500).first { it.id == "s_dr" }
        assertEquals(-480_000L, dr.profit) // 120,000 − 400×1500
        assertEquals("$400", dr.costFmt)
        assertEquals("120,000", dr.revFmt)
        assertEquals("− 480,000", dr.profitFmt)
        assertEquals(38, dr.remain)
        assertEquals(16, dr.inPkg)
        assertTrue(dr.isBale)
    }

    @Test
    fun `bale profit follows the editable rate`() {
        // at a realistic higher rate the same bale reads very differently
        val at1500 = sourceViews(sources, shelf, 1500).first { it.id == "s_dr" }
        val at100 = sourceViews(sources, shelf, 100).first { it.id == "s_dr" }
        assertEquals(-480_000L, at1500.profit) // 120,000 − 400×1500
        assertEquals(80_000L, at100.profit)    // 120,000 − 400×100 → profitable
    }

    @Test
    fun `market profit is revenue minus per-unit buy times shelved`() {
        val mkt = sourceViews(sources, shelf, 1500).first { it.id == MKT_ID }
        assertEquals(-2_000L, mkt.profit) // 18,000 − 4,000×5
        assertEquals("20,000", mkt.costFmt)
    }

    @Test
    fun `before-the-app source has no cost basis and no profit`() {
        val pre = sourceViews(sources, shelf, 1500).first { it.id == PRE_ID }
        assertNull(pre.profit)
        assertEquals("—", pre.costFmt)
        assertEquals("—", pre.profitFmt)
    }

    @Test
    fun `total on-hand and unspecified count match the summary`() {
        assertEquals(96, shelf.sumOf { maxOf(0, it.onHand) })
        assertEquals(2, shelf.count { it.unspecified }) // طقم أطفال + تنورة
    }

    @Test
    fun `customer balance is opening debt plus the debt movements on her entries`() {
        val customers = sampleCustomers()
        val entries = sampleEntries(0)
        val um = customers.first { it.id == "c_um" }   // 0 opening + 6,500 sale remainder
        val sam = customers.first { it.id == "c_sam" } // 10,000 opening − 10,000 paid
        val fat = customers.first { it.id == "c_fat" } // 35,000 opening, no entries
        assertEquals(6_500L, customerBalance(um, entries))
        assertEquals(0L, customerBalance(sam, entries))
        assertEquals(35_000L, customerBalance(fat, entries))
        // total owed to the shop
        assertEquals(41_500L, customers.sumOf { maxOf(0L, customerBalance(it, entries)) })
    }

    @Test
    fun `a payment reduces the balance and a partial sale adds the remainder`() {
        val c = Customer("c1", "زبونة", null, 0)
        val entries = listOf(
            DayEntry("s1", "بيع", "", "10,000", "ink", "c1", debtDelta = 4_000), // paid 6k of 10k
            DayEntry("p1", "دفعة", "", "+ 1,000", "pos", "c1", debtDelta = -1_000),
        )
        assertEquals(3_000L, customerBalance(c, entries))
    }

    @Test
    fun `debtors lists only who owes, largest first`() {
        val ds = debtors(sampleCustomers(), sampleEntries(0))
        // فاطمة 35,000 then أم محمد 6,500 (سميرة at 0 is excluded)
        assertEquals(listOf("فاطمة", "أم محمد"), ds.map { it.customer.name })
        assertEquals(35_000L, ds.first().balance)
    }

    @Test
    fun `digest copy names each debtor with her balance and the total`() {
        val ds = debtors(sampleCustomers(), sampleEntries(0))
        val (title, body) = digestTitleAndBody(ds)
        assertEquals("2 زبائن عليهن ديون — إجمالي 41,500", title)
        assertEquals("فاطمة (35,000)، أم محمد (6,500)", body)
    }

    @Test
    fun `the day book scopes entries and totals to the viewed day`() {
        val today = 20_100L
        val entries = listOf(
            DayEntry("a", "بيع", "", "10,000", "ink", day = today, saleAmount = 10_000, cashAmount = 10_000),
            DayEntry("b", "دفعة", "", "+ 5,000", "pos", day = today, saleAmount = 0, cashAmount = 5_000),
            DayEntry("c", "بيع أمس", "", "8,000", "ink", day = today - 1, saleAmount = 8_000, cashAmount = 8_000),
        )
        assertEquals(2, entriesForDay(entries, today).size)
        assertEquals(1, entriesForDay(entries, today - 1).size)
        assertEquals(10_000L, salesForDay(entries, today))     // payment adds nothing to sales
        assertEquals(15_000L, cashForDay(entries, today))      // 10,000 + 5,000
        assertEquals(8_000L, salesForDay(entries, today - 1))
        assertEquals(0L, salesForDay(entries, today - 5))      // empty day
    }

    @Test
    fun `stock delta round-trips so a voided entry can restore the shelf`() {
        assertEquals("h1:2,h4:1", encodeStock(mapOf("h1" to 2, "h4" to 1)))
        assertEquals("h9:-1", encodeStock(mapOf("h9" to -1))) // a return puts one back
        assertEquals("", encodeStock(mapOf("h1" to 0)))       // no-op deltas are dropped
        assertEquals(listOf("h1" to 2, "h4" to 1), decodeStock("h1:2,h4:1"))
        assertEquals(emptyList<Pair<String, Int>>(), decodeStock(""))
    }

    @Test
    fun `a return credits the balance like a payment`() {
        val c = Customer("c1", "زبونة", null, 10_000)
        val entries = listOf(
            DayEntry("r1", "إرجاع", "", "↩ 4,000", "amber", "c1", debtDelta = -4_000),
        )
        assertEquals(6_000L, customerBalance(c, entries)) // 10,000 owed − 4,000 returned
    }

    @Test
    fun `amana is tracked as trial, not as hard debt`() {
        val c = Customer("c1", "خالدة", null, 0)
        val entries = listOf(
            DayEntry("t1", "أمانة — خالدة", "", "أمانة 12,000", "amber", "c1", debtDelta = 0, trialAmount = 12_000),
        )
        assertEquals(0L, customerBalance(c, entries))      // not owed money yet
        assertEquals(12_000L, customerTrial(c, entries))   // but tracked as out-on-trust
    }

    @Test
    fun `follow-ups include debtors and amana-holders`() {
        val cs = listOf(
            Customer("d", "مدينة", null, 5_000),                 // owes only
            Customer("t", "أمانة-فقط", null, 0),                  // trial only
            Customer("b", "كلاهما", null, 3_000),                 // both
            Customer("z", "صفر", null, 0),                        // neither → excluded
        )
        val entries = listOf(
            DayEntry("a", "أمانة", "", "", "amber", "t", trialAmount = 8_000),
            DayEntry("a2", "أمانة", "", "", "amber", "b", trialAmount = 1_000),
        )
        val fs = followUps(cs, entries)
        assertEquals(listOf("أمانة-فقط", "مدينة", "كلاهما"), fs.map { it.customer.name }) // by debt+trial desc
        assertTrue(fs.none { it.customer.name == "صفر" })
    }

    @Test
    fun `a new debt defaults due to the 1st of next month and clears when paid off`() {
        val today = java.time.LocalDate.of(2026, 7, 6).toEpochDay()
        val firstAug = java.time.LocalDate.of(2026, 8, 1).toEpochDay()
        assertEquals(firstAug, firstOfNextMonth(today))
        // owes → gets a due date
        val owing = normalizeDues(listOf(Customer("c", "زبونة", null, 5_000)), emptyList(), today)
        assertEquals(firstAug, owing.first().dueEpochDay)
        // paid off → due date cleared
        val paid = normalizeDues(listOf(Customer("c", "زبونة", null, 0, dueEpochDay = firstAug)), emptyList(), today)
        assertNull(paid.first().dueEpochDay)
    }

    @Test
    fun `due status reads overdue, due-today, tomorrow, then in-N-days`() {
        val t = 20_000L
        assertEquals("متأخّرة 3 يوم", dueStatus(t - 3, t))
        assertEquals("مستحقة اليوم", dueStatus(t, t))
        assertEquals("غداً", dueStatus(t + 1, t))
        assertEquals("بعد 10 يوم", dueStatus(t + 10, t))
    }

    @Test
    fun `the digest chases only debts due or overdue today, so a snooze drops out`() {
        val t = 20_000L
        val cs = listOf(
            Customer("a", "متأخرة", null, 5_000, dueEpochDay = t - 2), // overdue → included
            Customer("b", "اليوم", null, 3_000, dueEpochDay = t),      // due today → included
            Customer("c", "مؤجّلة", null, 4_000, dueEpochDay = t + 7), // snoozed → excluded
        )
        val due = dueDebtors(cs, emptyList(), t)
        assertEquals(listOf("متأخرة", "اليوم"), due.map { it.customer.name }) // debtors: largest balance first
        assertTrue(due.none { it.customer.name == "مؤجّلة" })
    }

    @Test
    fun `sale lines round-trip so the detail can show and attribute each item`() {
        val lines = listOf(
            SaleLine("h1", "فستان", tasira = 10_000, price = 9_000, qty = 2),
            SaleLine("h4", "قميص", tasira = 5_000, price = 5_000, qty = 1),
        )
        val encoded = encodeLines(lines)
        assertEquals("h1|فستان|9000|2;h4|قميص|5000|1", encoded)
        val decoded = decodeLines(encoded)
        assertEquals(2, decoded.size)
        assertEquals(SoldLine("h1", "فستان", 9_000, 2), decoded[0])
        assertEquals("قميص", decoded[1].name)
        assertEquals(emptyList<SoldLine>(), decodeLines(""))
    }

    @Test
    fun `day labels read today, yesterday, then a dated weekday`() {
        val today = java.time.LocalDate.of(2026, 7, 5).toEpochDay() // a Sunday
        assertEquals("اليوم", dayLabel(today, today))
        assertEquals("أمس", dayLabel(today - 1, today))
        assertEquals("الجمعة 3/7", dayLabel(today - 2, today)) // Fri 3 July
    }

    @Test
    fun `market shops are the market sources except the system row, and their debts sum`() {
        val srcs = listOf(
            Source(PRE_ID, Kind.PRE_APP, "قبل التطبيق"),
            Source(MKT_ID, Kind.MARKET, "شراء من السوق"),
            Source("s1", Kind.MARKET, "محل أم علي", debt = 15_000),
            Source("s2", Kind.MARKET, "محل أبو خالد", debt = 5_000),
            Source("s3", Kind.BALE, "بالة", cost = 400),
        )
        assertEquals(listOf("محل أم علي", "محل أبو خالد"), marketShops(srcs).map { it.label })
        assertEquals(20_000, marketDebtTotal(srcs))
    }

    @Test
    fun `a shop's cost derives from its items buy price times shelved`() {
        val shop = Source("s1", Kind.MARKET, "محل أم علي", debt = 15_000)
        val items = listOf(
            Shelf("h1", "بلوزة صوف", tasira = 5_000, shelved = 3, sold = 1, sourceId = "s1", buy = 3_000),
            Shelf("h2", "حقيبة", tasira = 9_000, shelved = 2, sold = 0, sourceId = "s1", buy = 4_000),
        )
        val view = sourceViews(listOf(shop), items, usdRate = 1_500).single()
        assertEquals(3 * 3_000L + 2 * 4_000L, view.costLocal)
        assertEquals(1 * 5_000L, view.revenue) // sold × tasira attribution
        assertEquals(15_000, view.debt)
        assertEquals(Kind.MARKET, view.kind)
    }

    @Test
    fun `a payment within the balance needs no paper-debt prompt`() {
        assertNull(paperDebtShortfall(balanceBefore = 10_000, payment = 6_000))
        assertNull(paperDebtShortfall(balanceBefore = 10_000, payment = 10_000)) // exact = fits
    }

    @Test
    fun `an overshooting payment surfaces the unrecorded paper debt as its shortfall`() {
        // she pays 15,000 but only 6,000 is recorded → 9,000 was old paper debt
        assertEquals(9_000L, paperDebtShortfall(balanceBefore = 6_000, payment = 15_000))
        assertEquals(15_000L, paperDebtShortfall(balanceBefore = 0, payment = 15_000)) // nothing recorded yet
    }

    @Test
    fun `recording the shortfall as opening debt lands the payment exactly at zero`() {
        val c0 = Customer("c1", "أم محمد", openingDebt = 6_000)
        val pay = DayEntry("e1", "دفعة — أم محمد", "الآن", "+ 15,000", "pos", "c1", debtDelta = -15_000, day = 1)
        val short = paperDebtShortfall(customerBalance(c0, listOf()), 15_000)!!
        // «نعم» path: opening debt bumped by the shortfall, then the payment applied
        val c1 = c0.copy(openingDebt = c0.openingDebt + short)
        assertEquals(0L, customerBalance(c1, listOf(pay)))
        // «لا، هي لها» path: payment applied as-is → negative (shop owes her)
        assertEquals(-9_000L, customerBalance(c0, listOf(pay)))
    }

    @Test
    fun `a supplier payment reduces the shop's debt and voiding it restores it`() {
        val shop = Source("s1", Kind.MARKET, "محل أم علي", debt = 15_000)
        val pay = DayEntry("e1", "دفعة للمحل — محل أم علي", "الآن", "− 5,000", "neg", day = 1, sourceId = "s1", moneyOut = 5_000)
        assertEquals(10_000L, shopDebtNow(shop, listOf(pay)))
        // voiding removes the entry — the debt restores by construction, nothing else to undo
        assertEquals(15_000L, shopDebtNow(shop, emptyList()))
        // another shop's payments never touch this one
        val other = DayEntry("e2", "دفعة للمحل — آخر", "الآن", "− 2,000", "neg", day = 1, sourceId = "s9", moneyOut = 2_000)
        assertEquals(15_000L, shopDebtNow(shop, listOf(other)))
    }

    @Test
    fun `supplier payments are money-out and never count in the day's cash or sales`() {
        val pay = DayEntry("e1", "دفعة للمحل — محل أم علي", "الآن", "− 5,000", "neg", day = 7, sourceId = "s1", moneyOut = 5_000)
        assertEquals(0L, cashForDay(listOf(pay), 7))
        assertEquals(0L, salesForDay(listOf(pay), 7))
    }

    @Test
    fun `item stats sum actual sale prices, average them, and track the last-sold day`() {
        val item = Shelf("h1", "فستان", tasira = 10_000, shelved = 20, sold = 3, sourceId = "s_mkt", buy = 6_000)
        val entries = listOf(
            // sold at 9,000 (haggled below the 10,000 tasira) then 8,000 — 3 pieces total
            DayEntry("e1", "بيع", "", "", "pos", day = 5, lines = "h1|فستان|9000|1;hX|قميص|5000|2"),
            DayEntry("e2", "بيع", "", "", "pos", day = 8, lines = "h1|فستان|8000|2"),
        )
        val s = itemStats(item, sources = listOf(Source("s_mkt", Kind.MARKET, "محل", null)), shelf = listOf(item), entries = entries, usdRate = 1_500)
        assertEquals(3, s.soldPieces)
        assertEquals(9_000L + 8_000L * 2, s.revenue) // 25,000 actual, not tasira × sold
        assertEquals(25_000L / 3, s.avgSellPrice) // 8,333 — below tasira, shows the haggling
        assertEquals(8L, s.lastSoldDay)
        assertEquals(25_000L - 6_000L * 3, s.profit) // buy cost basis → 7,000
    }

    @Test
    fun `item stats leave profit absent for an untracked item and nulls when never sold`() {
        val pre = Shelf("h2", "بنطال", tasira = 5_000, shelved = 10, sold = 0, sourceId = PRE_ID)
        val s = itemStats(pre, emptyList(), listOf(pre), emptyList(), 1_500)
        assertEquals(0, s.soldPieces)
        assertNull(s.avgSellPrice)
        assertNull(s.lastSoldDay)
        assertNull(s.profit) // قبل التطبيق has no cost basis
    }

    @Test
    fun `estimated untracked profit is absent when nothing has a cost basis to learn from`() {
        val untrackedOnly = listOf(
            Shelf("a", "فستان", 10_000, shelved = 5, sold = 2, sourceId = PRE_ID),
            Shelf("b", "بنطال", 6_000, shelved = 4, sold = 1, sourceId = null), // غير محدد
        )
        assertNull(estimatedUntrackedProfit(untrackedOnly)) // no tracked item → «—», never a guess
    }

    @Test
    fun `estimated untracked profit prefers the same item name's margin over the shop-wide one`() {
        val shelf = listOf(
            Shelf("t1", "فستان", 10_000, shelved = 5, sold = 1, sourceId = "s_mkt", buy = 6_000), // فستان margin 0.4
            Shelf("t2", "حقيبة", 10_000, shelved = 5, sold = 1, sourceId = "s_mkt", buy = 9_000), // حقيبة margin 0.1
            Shelf("u", "فستان", 10_000, shelved = 10, sold = 3, sourceId = PRE_ID), // untracked فستان
        )
        // same-name margin 0.4 → 3×10,000×0.4 = 12,000 (shop-wide 0.25 would wrongly give 7,500)
        assertEquals(12_000L, estimatedUntrackedProfit(shelf))
    }

    @Test
    fun `estimated untracked profit falls back to the shop-wide margin for an unseen name`() {
        val shelf = listOf(
            Shelf("t", "حقيبة", 10_000, shelved = 5, sold = 1, sourceId = "s_mkt", buy = 6_000), // margin 0.4
            Shelf("u", "فستان", 5_000, shelved = 10, sold = 2, sourceId = null), // untracked, name unseen
        )
        assertEquals(4_000L, estimatedUntrackedProfit(shelf)) // 2×5,000×0.4
    }

    @Test
    fun `estimated untracked profit is zero when we can estimate but nothing untracked has sold`() {
        val shelf = listOf(Shelf("t", "حقيبة", 10_000, shelved = 5, sold = 1, sourceId = "s_mkt", buy = 6_000))
        assertEquals(0L, estimatedUntrackedProfit(shelf))
    }

    @Test
    fun `bale cost recovery is revenue as a percent of cost at today's rate`() {
        val dr = sourceViews(sources, shelf, 1500).first { it.id == "s_dr" }
        assertEquals(20, recoveryPct(dr.revenue, dr.costLocal)) // 120,000 of 600,000
        assertEquals(150, recoveryPct(revenue = 900_000, costLocal = 600_000)) // paid for itself — passes 100
        assertNull(recoveryPct(revenue = 120_000, costLocal = null)) // قبل التطبيق: nothing to recover
        assertNull(recoveryPct(revenue = 120_000, costLocal = 0)) // zero-cost guard
    }

    @Test
    fun `average sold price is revenue over pieces sold, absent until something sells`() {
        val dr = sourceViews(sources, shelf, 1500).first { it.id == "s_dr" }
        assertEquals(12, dr.sold) // فستان 12 pieces
        assertEquals(10_000L, avgSoldPrice(dr.revenue, dr.sold))
        assertNull(avgSoldPrice(revenue = 0, sold = 0))
    }

    @Test
    fun `a bale's piece ledger sums sold, on shelf and in bale across its items`() {
        val pc = sourceViews(sources, shelf, 1500).first { it.id == "s_pc" }
        assertEquals(28 + 9, pc.sold)
        assertEquals(33, pc.remain) // (40−28) + (30−9)
        assertEquals(8, pc.inPkg) // قميص: 30 counted − 22 shelved
    }

    @Test
    fun `day book rolls to the new today on resume only when it was viewing the old today`() {
        assertEquals(101L, rollViewedDay(today = 100, viewedDay = 100, newToday = 101)) // on today → follows
        assertEquals(98L, rollViewedDay(today = 100, viewedDay = 98, newToday = 101)) // flipped back → stays
    }

    // ── edge-case catalog (F8b): these read as documentation of the ledger's invariants ──

    @Test
    fun `day totals count only that day's entries, so a void simply drops its contribution`() {
        val d = 100L
        val sale = DayEntry("e1", "بيع", "", "", "pos", day = d, saleAmount = 6_000, cashAmount = 6_000)
        val other = DayEntry("e2", "بيع", "", "", "pos", day = d + 1, saleAmount = 9_000, cashAmount = 9_000)
        assertEquals(6_000L, salesForDay(listOf(sale, other), d)) // other day excluded
        assertEquals(6_000L, cashForDay(listOf(sale, other), d))
        // voiding removes the row entirely (that's how void works) → the day nets to zero
        assertEquals(0L, salesForDay(listOf(other), d))
    }

    @Test
    fun `a voided qayd counts for nothing but is not removed`() {
        val c = Customer("c1", "أم محمد", openingDebt = 10_000)
        val pay = DayEntry("e1", "دفعة — أم محمد", "", "+ 6,000", "pos", "c1", debtDelta = -6_000, day = 5, cashAmount = 6_000)
        assertEquals(4_000L, customerBalance(c, listOf(pay)))
        assertEquals(6_000L, cashForDay(listOf(pay), 5))
        // void it → excluded from balance and the day's cash, but the row still exists
        val voided = pay.copy(voided = true)
        assertEquals(10_000L, customerBalance(c, listOf(voided))) // back to opening debt
        assertEquals(0L, cashForDay(listOf(voided), 5))
        assertTrue(listOf(voided).any { it.id == "e1" }) // still in the book
    }

    @Test
    fun `a voided supplier payment stops reducing the shop's debt`() {
        val shop = Source("s1", Kind.MARKET, "محل", debt = 15_000)
        val pay = DayEntry("e1", "دفعة للمحل", "", "− 5,000", "neg", day = 1, sourceId = "s1", moneyOut = 5_000)
        assertEquals(10_000L, shopDebtNow(shop, listOf(pay)))
        assertEquals(15_000L, shopDebtNow(shop, listOf(pay.copy(voided = true)))) // void → debt restored
    }

    @Test
    fun `a supplier payment never moves any customer balance`() {
        val c = Customer("c1", "أم محمد", openingDebt = 10_000)
        val supplierPay = DayEntry("e1", "دفعة للمحل", "", "", "neg", customerId = null, day = 1, sourceId = "s1", moneyOut = 5_000)
        assertEquals(10_000L, customerBalance(c, listOf(supplierPay))) // untouched — it isn't hers
    }

    @Test
    fun `an amana trial is tracked apart from hard debt`() {
        val c = Customer("c1", "سميرة")
        val trial = DayEntry("e1", "أمانة", "", "", "amber", customerId = "c1", day = 1, trialAmount = 8_000)
        assertEquals(0L, customerBalance(c, listOf(trial))) // not debt
        assertEquals(8_000L, customerTrial(c, listOf(trial))) // tracked separately
    }

    @Test
    fun `estimated profit tolerates zero and negative learned margins`() {
        val zero = listOf(
            Shelf("t", "حقيبة", 10_000, shelved = 5, sold = 1, sourceId = "s", buy = 10_000), // margin 0
            Shelf("u", "فستان", 5_000, shelved = 4, sold = 2, sourceId = PRE_ID),
        )
        assertEquals(0L, estimatedUntrackedProfit(zero)) // 0 margin → 0 estimate, no crash
        val loss = listOf(
            Shelf("t", "حقيبة", 10_000, shelved = 5, sold = 1, sourceId = "s", buy = 12_000), // margin −0.2
            Shelf("u", "فستان", 5_000, shelved = 4, sold = 2, sourceId = PRE_ID), // 2×5,000×−0.2
        )
        assertEquals(-2_000L, estimatedUntrackedProfit(loss)) // honestly negative
    }
}
