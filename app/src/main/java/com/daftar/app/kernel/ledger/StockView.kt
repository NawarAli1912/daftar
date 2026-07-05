package com.daftar.app.kernel.ledger

// D48: the honest item-level stock view. Every (source, type, price) that was either
// counted (intake) OR sold (attributed) becomes one line. `sold > counted` is not an
// error — it's a counting gap ("sold more than I counted"), surfaced, never blocking.
// Profit lives elsewhere (revenue − cost); this layer is only the optional stock estimate.
object StockView {

    data class StockLine(
        val sourceId: String,
        val typeId: String,
        val price: Long,
        val counted: Int,
        val sold: Int,
    ) {
        val remaining: Int get() = counted - sold
        val hasGap: Boolean get() = sold > counted
    }

    fun build(
        sourceIds: Set<String>,
        intake: List<IntakeCount>,
        sold: List<AttributedSale>,
    ): List<StockLine> {
        val countedByKey = intake.filterNot { it.voided }
            .filter { it.sourceId in sourceIds }
            .groupBy { Triple(it.sourceId, it.typeId, it.price) }
            .mapValues { (_, v) -> v.sumOf { it.qty } }
        val soldByKey = sold.filterNot { it.voided }
            .filter { it.sourceId in sourceIds }
            .groupBy { Triple(it.sourceId, it.typeId, it.price) }
            .mapValues { (_, v) -> v.sumOf { it.qty } }

        return (countedByKey.keys + soldByKey.keys).map { key ->
            StockLine(
                sourceId = key.first,
                typeId = key.second,
                price = key.third,
                counted = countedByKey[key] ?: 0,
                sold = soldByKey[key] ?: 0,
            )
        }
    }
}
