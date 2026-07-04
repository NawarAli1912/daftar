package com.daftar.app.kernel.ledger

// Stock v2 (D10/D23) — the "did this bale pay for itself?" calculation.
// Pure and labelled-approximate: cost is what she paid, soldValue is the agreed
// value of goods attributed out of the source (D36). Cost is in USD; the local
// equivalent (costLocal) is the only figure comparable to local-currency revenue,
// so profit exists only when costLocal is known. Everything else stays honest-empty.

data class SourceCostInput(
    val sourceId: String,
    val costUsd: Long?,
    val costLocal: Long?,
    val arrivedAt: Long,
    val voided: Boolean = false,
)

data class IntakeQty(
    val sourceId: String,
    val qty: Int,
    val voided: Boolean = false,
)

// One attributed money event: a sold sale-line (value = agreedUnit × qty) or a
// typed payment attributed to a source (value = the amount received, qty 1).
data class RevenueLine(
    val sourceId: String,
    val value: Long,
    val qty: Int,
    val voided: Boolean = false,
)

data class SourceProfit(
    val sourceId: String,
    val costUsd: Long?,
    val costLocal: Long?,
    val intakeQty: Int,
    val soldQty: Int,
    val soldValue: Long,
    val remainingQty: Int,
    val ageDays: Int,
) {
    /** Money over (+) or under (−) the local cost. Null when the local cost is unknown. */
    val profitLocal: Long? get() = costLocal?.let { soldValue - it }

    /** soldValue ÷ costLocal — how much of the outlay has come back. Null when cost unknown. */
    val recoveredRatio: Double? get() = costLocal?.takeIf { it > 0 }?.let { soldValue.toDouble() / it }

    /** True once the source's goods have sold for at least what it cost. */
    val paidBack: Boolean get() = costLocal != null && costLocal > 0 && soldValue >= costLocal

    /** Old stock still on the rack that hasn't paid for itself — a markdown is worth suggesting. */
    fun needsMarkdown(agingDays: Int = ProfitMath.AGING_DAYS): Boolean =
        remainingQty > 0 && ageDays >= agingDays && !paidBack
}

object ProfitMath {
    // Heuristic: stock older than this that hasn't recovered its cost is "aging".
    // TODO(owner): tune this threshold with Nawar — it's a genuine product decision.
    const val AGING_DAYS = 60
    const val DAY_MS = 24L * 60 * 60 * 1000

    fun ageDays(arrivedAt: Long, now: Long): Int =
        ((now - arrivedAt) / DAY_MS).coerceAtLeast(0).toInt()

    fun build(
        sources: List<SourceCostInput>,
        intake: List<IntakeQty>,
        revenue: List<RevenueLine>,
        now: Long,
    ): List<SourceProfit> {
        val intakeBySource = intake.filterNot { it.voided }.groupBy { it.sourceId }
        val revenueBySource = revenue.filterNot { it.voided }.groupBy { it.sourceId }
        return sources.filterNot { it.voided }.map { source ->
            val intakeQty = intakeBySource[source.sourceId].orEmpty().sumOf { it.qty }
            val lines = revenueBySource[source.sourceId].orEmpty()
            val soldQty = lines.sumOf { it.qty }
            val soldValue = lines.sumOf { it.value }
            SourceProfit(
                sourceId = source.sourceId,
                costUsd = source.costUsd,
                costLocal = source.costLocal,
                intakeQty = intakeQty,
                soldQty = soldQty,
                soldValue = soldValue,
                remainingQty = (intakeQty - soldQty).coerceAtLeast(0),
                ageDays = ageDays(source.arrivedAt, now),
            )
        }
    }
}
