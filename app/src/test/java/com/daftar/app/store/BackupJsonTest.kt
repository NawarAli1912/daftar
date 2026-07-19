package com.daftar.app.store

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// Backup is the only durability guarantee (no cloud), so a JSON round-trip must be lossless —
// including the fields added over time (supplier sourceId/moneyOut at v16, opening debt).
class BackupJsonTest {

    @Test
    fun `a full snapshot survives a JSON round-trip unchanged`() {
        val snap = StoreSnapshot(
            seeded = true,
            usdRate = 1_600,
            sources = listOf(
                Source("s_dr", Kind.BALE, "بالة شتوية", cost = 400),
                Source("s1", Kind.MARKET, "محل أم علي", cost = null, debt = 15_000),
                Source(PRE_ID, Kind.PRE_APP, "قبل التطبيق", cost = null),
            ),
            shelf = listOf(
                Shelf("h1", "فستان", tasira = 10_000, shelved = 34, sold = 12, counted = 50, sourceId = "s_dr"),
                Shelf("h9", "حقيبة", tasira = 9_000, shelved = 5, sold = 2, sourceId = "s1", buy = 4_000),
                Shelf("h3", "بنطال", tasira = 5_000, shelved = 22, sold = 25, sourceId = null),
            ),
            entries = listOf(
                DayEntry("e1", "بيع — أم محمد", "الآن", "+ 16,500", "pos", "c_sam", debtDelta = 6_000, day = 100, saleAmount = 16_500, cashAmount = 10_000, lines = "h1|فستان|10000|1;h3|بنطال|5000|1"),
                DayEntry("e2", "دفعة للمحل — محل أم علي", "الآن", "− 5,000", "neg", customerId = null, day = 100, sourceId = "s1", moneyOut = 5_000),
                DayEntry("e3", "أمانة — سميرة", "الآن", "أمانة", "amber", "c_sam", day = 100, trialAmount = 8_000),
            ),
            customers = listOf(
                Customer("c_sam", "سميرة", phone = "0999", openingDebt = 3_000),
            ),
        )

        val restored = snapshotFromJson(snapshotToJson(snap))

        assertEquals(snap.usdRate, restored.usdRate)
        assertEquals(snap.sources, restored.sources)
        assertEquals(snap.shelf, restored.shelf)
        assertEquals(snap.customers, restored.customers)
        assertEquals(snap.entries, restored.entries) // includes sourceId/moneyOut/lines/trialAmount
        // the supplier payment specifically keeps its money-out identity across the round-trip
        val pay = restored.entries.single { it.moneyOut > 0 }
        assertEquals("s1", pay.sourceId)
        assertEquals(5_000L, pay.moneyOut)
    }

    @Test
    fun `a pre-v16 backup without supplier fields still loads (defaults applied)`() {
        // an entry JSON from before D68 — no sourceId / moneyOut keys
        val legacy = """
            {"seeded":true,"usdRate":1500,"sources":[],"shelf":[],"customers":[],
             "entries":[{"id":"e1","t":"بيع","d":"الآن","amt":"+ 4,000","cls":"pos",
                         "customerId":null,"debtDelta":0,"day":100,"saleAmount":4000,
                         "cashAmount":4000,"stockDelta":"","trialAmount":0,"lines":""}]}
        """.trimIndent()
        val restored = snapshotFromJson(legacy)
        val e = restored.entries.single()
        assertEquals(null, e.sourceId)
        assertEquals(0L, e.moneyOut)
    }

    @Test
    fun `a bale's frozen rate and count plus its expenses survive a JSON round-trip`() {
        val snap = StoreSnapshot(
            seeded = true, usdRate = 1_500,
            sources = listOf(Source("s_b", Kind.BALE, "بالة شتوية", cost = 275, ratePurchase = 13_300, countTotal = 175)),
            shelf = emptyList(),
            entries = emptyList(),
            customers = emptyList(),
            expenses = listOf(
                BaleExpense("x1", "s_b", "كوي", 30_000),
                BaleExpense("x2", "s_b", "نقل", 20_000),
            ),
        )
        val restored = snapshotFromJson(snapshotToJson(snap))
        assertEquals(snap.sources, restored.sources)   // countTotal + ratePurchase preserved
        assertEquals(snap.expenses, restored.expenses) // the bale-owned expense list preserved
        val b = restored.sources.single()
        assertEquals(13_300L, b.ratePurchase)
        assertEquals(175, b.countTotal)
    }

    @Test
    fun `a pre-v20 backup without bale count, rate, or expenses still loads with defaults`() {
        val legacy = """
            {"seeded":true,"usdRate":1500,
             "sources":[{"id":"s_b","kind":"BALE","label":"بالة","cost":400,"debt":0}],
             "shelf":[],"customers":[],"entries":[]}
        """.trimIndent()
        val restored = snapshotFromJson(legacy)
        val b = restored.sources.single()
        assertEquals(null, b.countTotal)   // legacy bale → live-rate fallback semantics
        assertEquals(null, b.ratePurchase)
        assertTrue(restored.expenses.isEmpty())
    }
}
