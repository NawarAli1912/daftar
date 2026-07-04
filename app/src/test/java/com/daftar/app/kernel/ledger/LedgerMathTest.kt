package com.daftar.app.kernel.ledger

import org.junit.Assert.assertEquals
import org.junit.Test

class LedgerMathTest {

    private fun opening(amount: Long, voided: Boolean = false) =
        LedgerLine(EntryKind.OPENING_BALANCE, amount, voided)

    private fun payment(amount: Long, voided: Boolean = false) =
        LedgerLine(EntryKind.PAYMENT, amount, voided)

    @Test
    fun `empty ledger means zero balance`() {
        assertEquals(0L, LedgerMath.balance(emptyList()))
    }

    @Test
    fun `opening balance increases what the customer owes`() {
        assertEquals(15_000L, LedgerMath.balance(listOf(opening(15_000))))
    }

    @Test
    fun `payment reduces the balance`() {
        assertEquals(10_000L, LedgerMath.balance(listOf(opening(15_000), payment(5_000))))
    }

    @Test
    fun `overpaying makes the balance negative - the shop owes her (D22)`() {
        assertEquals(-2_000L, LedgerMath.balance(listOf(opening(3_000), payment(5_000))))
    }

    @Test
    fun `a payment with no prior debt goes straight negative (D22)`() {
        assertEquals(-5_000L, LedgerMath.balance(listOf(payment(5_000))))
    }

    @Test
    fun `voided rows are excluded everywhere (D21)`() {
        val lines = listOf(opening(15_000), payment(5_000, voided = true))
        assertEquals(15_000L, LedgerMath.balance(lines))
    }

    @Test
    fun `balance side labels - positive is on her, negative is for her, zero is neither (D22)`() {
        assertEquals(BalanceSide.OWES_SHOP, LedgerMath.side(1L))
        assertEquals(BalanceSide.SHOP_OWES, LedgerMath.side(-1L))
        assertEquals(BalanceSide.SETTLED, LedgerMath.side(0L))
    }

    private fun sale(amount: Long, voided: Boolean = false) =
        LedgerLine(EntryKind.SALE, amount, voided)

    @Test
    fun `day total counts non-voided payments only`() {
        val lines = listOf(
            payment(5_000),
            payment(3_000),
            payment(9_000, voided = true),
            opening(20_000),
            sale(15_000),
        )
        assertEquals(8_000L, LedgerMath.paymentsTotal(lines))
    }

    @Test
    fun `a sale increases the balance and its payment reduces it (D13, D20)`() {
        val lines = listOf(sale(15_000), payment(5_000))
        assertEquals(10_000L, LedgerMath.balance(lines))
    }

    @Test
    fun `voided sale rows leave the balance untouched (undo cascade, D21)`() {
        val lines = listOf(sale(15_000, voided = true), payment(5_000, voided = true))
        assertEquals(0L, LedgerMath.balance(lines))
    }

    @Test
    fun `line total is quantity times per-unit price (D29)`() {
        assertEquals(130_000L, LedgerMath.lineTotal(qty = 2, unitPrice = 65_000))
        assertEquals(65_000L, LedgerMath.lineTotal(qty = 1, unitPrice = 65_000))
    }

    @Test
    fun `sale total sums non-voided lines`() {
        val lines = listOf(
            SaleLineAmounts(qty = 2, agreedUnit = 65_000, voided = false),
            SaleLineAmounts(qty = 1, agreedUnit = 10_000, voided = false),
            SaleLineAmounts(qty = 5, agreedUnit = 9_999, voided = true),
        )
        assertEquals(140_000L, LedgerMath.saleTotal(lines))
    }

    @Test
    fun `giveaway lines are valid at zero (D24)`() {
        assertEquals(0L, LedgerMath.lineTotal(qty = 3, unitPrice = 0))
    }
}
