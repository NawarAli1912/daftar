package com.daftar.app.kernel.ledger

enum class SourceKind { BALE, MARKET, PRE_APP }

data class SourceMeta(val id: String, val arrivedAt: Long, val voided: Boolean)

data class IntakeCount(
    val sourceId: String,
    val typeId: String,
    val price: Long,
    val qty: Int,
    val voided: Boolean,
)

data class AttributedSale(
    val sourceId: String,
    val typeId: String,
    val price: Long,
    val qty: Int,
    val voided: Boolean,
)

// Builds the SourceSnapshots the Attributor consumes (D36/D38):
// remaining per (source, type, price) = intake counts − attributed sales.
object SnapshotBuilder {

    fun build(
        sources: List<SourceMeta>,
        intake: List<IntakeCount>,
        sold: List<AttributedSale>,
    ): List<SourceSnapshot> {
        val soldByKey = sold.filterNot { it.voided }
            .groupBy { Triple(it.sourceId, it.typeId, it.price) }
            .mapValues { (_, sales) -> sales.sumOf { it.qty } }

        return sources.filterNot { it.voided }.map { source ->
            val points = intake.filterNot { it.voided }
                .filter { it.sourceId == source.id }
                .groupBy { it.typeId to it.price }
                .map { (key, lines) ->
                    val (typeId, price) = key
                    PricePointStock(
                        typeId = typeId,
                        price = price,
                        estimatedRemaining = lines.sumOf { it.qty } -
                            (soldByKey[Triple(source.id, typeId, price)] ?: 0),
                    )
                }
            SourceSnapshot(source.id, source.arrivedAt, points)
        }
    }
}
