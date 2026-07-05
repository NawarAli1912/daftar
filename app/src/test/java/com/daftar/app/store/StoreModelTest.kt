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
    fun `day labels read today, yesterday, then a dated weekday`() {
        val today = java.time.LocalDate.of(2026, 7, 5).toEpochDay() // a Sunday
        assertEquals("اليوم", dayLabel(today, today))
        assertEquals("أمس", dayLabel(today - 1, today))
        assertEquals("الجمعة 3/7", dayLabel(today - 2, today)) // Fri 3 July
    }
}
