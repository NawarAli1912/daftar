package com.daftar.app.kernel.ledger

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfitMathTest {

    private val now = 1_000L * ProfitMath.DAY_MS // day 1000, so arrivedAt in days is easy
    private val bale = "bale-feb"

    private fun source(
        costUsd: Long? = 400,
        costLocal: Long? = 5_200_000,
        arrivedDaysAgo: Int = 0,
        voided: Boolean = false,
    ) = SourceCostInput(bale, costUsd, costLocal, now - arrivedDaysAgo * ProfitMath.DAY_MS, voided)

    private fun build(
        sources: List<SourceCostInput>,
        intake: List<IntakeQty> = emptyList(),
        revenue: List<RevenueLine> = emptyList(),
    ) = ProfitMath.build(sources, intake, revenue, now)

    @Test
    fun `sold value above local cost is profit and paid back`() {
        val result = build(
            sources = listOf(source(costLocal = 5_000_000)),
            revenue = listOf(RevenueLine(bale, value = 6_000_000, qty = 30)),
        ).single()

        assertEquals(6_000_000, result.soldValue)
        assertEquals(1_000_000L, result.profitLocal)
        assertEquals(1.2, result.recoveredRatio!!, 0.0001)
        assertTrue(result.paidBack)
    }

    @Test
    fun `sold value below local cost is a loss and not yet paid back`() {
        val result = build(
            sources = listOf(source(costLocal = 5_000_000)),
            revenue = listOf(RevenueLine(bale, value = 3_000_000, qty = 15)),
        ).single()

        assertEquals(-2_000_000L, result.profitLocal)
        assertEquals(0.6, result.recoveredRatio!!, 0.0001)
        assertFalse(result.paidBack)
    }

    @Test
    fun `unknown local cost leaves profit honest-empty (D10)`() {
        val result = build(
            sources = listOf(source(costUsd = 400, costLocal = null)),
            revenue = listOf(RevenueLine(bale, value = 6_000_000, qty = 30)),
        ).single()

        assertNull(result.profitLocal)
        assertNull(result.recoveredRatio)
        assertFalse(result.paidBack)
    }

    @Test
    fun `remaining is intake minus sold, clamped at zero`() {
        val result = build(
            sources = listOf(source()),
            intake = listOf(IntakeQty(bale, 40), IntakeQty(bale, 25)),
            revenue = listOf(RevenueLine(bale, value = 100, qty = 10)),
        ).single()

        assertEquals(65, result.intakeQty)
        assertEquals(10, result.soldQty)
        assertEquals(55, result.remainingQty)
    }

    @Test
    fun `oversold never shows negative remaining`() {
        val result = build(
            sources = listOf(source()),
            intake = listOf(IntakeQty(bale, 2)),
            revenue = listOf(RevenueLine(bale, value = 100, qty = 5)),
        ).single()

        assertEquals(0, result.remainingQty)
    }

    @Test
    fun `voided intake, revenue and sources are excluded (D21)`() {
        val result = build(
            sources = listOf(
                source(),
                SourceCostInput("ghost", 1, 1, now, voided = true),
            ),
            intake = listOf(IntakeQty(bale, 40), IntakeQty(bale, 99, voided = true)),
            revenue = listOf(
                RevenueLine(bale, value = 500, qty = 5),
                RevenueLine(bale, value = 9_999, qty = 9, voided = true),
            ),
        )

        assertEquals(1, result.size)
        assertEquals(500, result.single().soldValue)
        assertEquals(5, result.single().soldQty)
        assertEquals(35, result.single().remainingQty)
    }

    @Test
    fun `age is whole days since arrival, never negative`() {
        assertEquals(90, ProfitMath.ageDays(now - 90 * ProfitMath.DAY_MS, now))
        assertEquals(0, ProfitMath.ageDays(now + 5 * ProfitMath.DAY_MS, now))
    }

    @Test
    fun `aging stock with remaining that has not paid back needs a markdown`() {
        val result = build(
            sources = listOf(source(costLocal = 5_000_000, arrivedDaysAgo = 90)),
            intake = listOf(IntakeQty(bale, 40)),
            revenue = listOf(RevenueLine(bale, value = 1_000_000, qty = 5)),
        ).single()

        assertTrue(result.needsMarkdown())
    }

    @Test
    fun `paid-back stock never nags for a markdown`() {
        val result = build(
            sources = listOf(source(costLocal = 1_000_000, arrivedDaysAgo = 90)),
            intake = listOf(IntakeQty(bale, 40)),
            revenue = listOf(RevenueLine(bale, value = 2_000_000, qty = 20)),
        ).single()

        assertFalse(result.needsMarkdown())
    }

    @Test
    fun `sold-out stock never nags for a markdown even if old`() {
        val result = build(
            sources = listOf(source(costLocal = 5_000_000, arrivedDaysAgo = 200)),
            intake = listOf(IntakeQty(bale, 5)),
            revenue = listOf(RevenueLine(bale, value = 100, qty = 5)),
        ).single()

        assertFalse(result.needsMarkdown())
    }

    @Test
    fun `fresh aging-eligible stock is not yet nagged`() {
        val result = build(
            sources = listOf(source(costLocal = 5_000_000, arrivedDaysAgo = 10)),
            intake = listOf(IntakeQty(bale, 40)),
            revenue = listOf(RevenueLine(bale, value = 100, qty = 1)),
        ).single()

        assertFalse(result.needsMarkdown())
    }
}
