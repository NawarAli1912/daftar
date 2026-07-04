package com.daftar.app.kernel.ledger

enum class EntryKind { PAYMENT, OPENING_BALANCE }

enum class BalanceSide { OWES_SHOP, SHOP_OWES, SETTLED }

data class LedgerLine(
    val kind: EntryKind,
    val amount: Long,
    val voided: Boolean,
)

object LedgerMath {

    fun balance(lines: List<LedgerLine>): Long =
        lines.filterNot { it.voided }.sumOf { line ->
            when (line.kind) {
                EntryKind.OPENING_BALANCE -> line.amount
                EntryKind.PAYMENT -> -line.amount
            }
        }

    fun side(balance: Long): BalanceSide = when {
        balance > 0 -> BalanceSide.OWES_SHOP
        balance < 0 -> BalanceSide.SHOP_OWES
        else -> BalanceSide.SETTLED
    }

    fun paymentsTotal(lines: List<LedgerLine>): Long =
        lines.filterNot { it.voided }
            .filter { it.kind == EntryKind.PAYMENT }
            .sumOf { it.amount }
}
