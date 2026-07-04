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

    @Test
    fun `day total counts non-voided payments only`() {
        val lines = listOf(
            payment(5_000),
            payment(3_000),
            payment(9_000, voided = true),
            opening(20_000),
        )
        assertEquals(8_000L, LedgerMath.paymentsTotal(lines))
    }
}
