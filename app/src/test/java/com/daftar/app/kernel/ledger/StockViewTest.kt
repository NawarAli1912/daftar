package com.daftar.app.kernel.ledger

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StockViewTest {

    private val bale = "bale"
    private val shirt = "shirt"
    private val pants = "pants"

    private fun intake(source: String, type: String, price: Long, qty: Int, voided: Boolean = false) =
        IntakeCount(source, type, price, qty, voided)

    private fun sold(source: String, type: String, price: Long, qty: Int, voided: Boolean = false) =
        AttributedSale(source, type, price, qty, voided)

    @Test
    fun `counted minus sold gives remaining`() {
        val line = StockView.build(
            sourceIds = setOf(bale),
            intake = listOf(intake(bale, pants, 7_500, 40)),
            sold = listOf(sold(bale, pants, 7_500, 3)),
        ).single()
        assertEquals(40, line.counted)
        assertEquals(3, line.sold)
        assertEquals(37, line.remaining)
        assertFalse(line.hasGap)
    }

    @Test
    fun `sold with no intake is a phantom gap line (the 2-shirts case, D48)`() {
        val line = StockView.build(
            sourceIds = setOf(bale),
            intake = emptyList(),
            sold = listOf(sold(bale, shirt, 6_000, 2)),
        ).single()
        assertEquals(0, line.counted)
        assertEquals(2, line.sold)
        assertEquals(-2, line.remaining)
        assertTrue(line.hasGap)
    }

    @Test
    fun `over-selling a counted line is also a gap`() {
        val line = StockView.build(
            sourceIds = setOf(bale),
            intake = listOf(intake(bale, pants, 7_500, 2)),
            sold = listOf(sold(bale, pants, 7_500, 5)),
        ).single()
        assertEquals(-3, line.remaining)
        assertTrue(line.hasGap)
    }

    @Test
    fun `voided intake and sales are excluded, lines outside known sources dropped`() {
        val lines = StockView.build(
            sourceIds = setOf(bale),
            intake = listOf(
                intake(bale, pants, 7_500, 40),
                intake(bale, pants, 7_500, 99, voided = true),
                intake("ghost", shirt, 6_000, 5),
            ),
            sold = listOf(sold(bale, pants, 7_500, 9, voided = true)),
        )
        assertEquals(1, lines.size)
        assertEquals(40, lines.single().counted)
        assertEquals(0, lines.single().sold)
    }

    @Test
    fun `distinct price points on the same type are distinct lines`() {
        val lines = StockView.build(
            sourceIds = setOf(bale),
            intake = listOf(intake(bale, pants, 7_500, 40), intake(bale, pants, 6_500, 10)),
            sold = emptyList(),
        ).sortedBy { it.price }
        assertEquals(2, lines.size)
        assertEquals(6_500, lines[0].price)
        assertEquals(7_500, lines[1].price)
    }
}
