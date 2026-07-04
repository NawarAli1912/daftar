package com.daftar.app.kernel.ledger

data class PricePointStock(
    val typeId: String,
    val price: Long,
    val estimatedRemaining: Int,
)

data class SourceSnapshot(
    val id: String,
    val arrivedAt: Long,
    val points: List<PricePointStock>,
)

sealed interface Attribution {
    data class ToSource(val sourceId: String) : Attribution
    data object Unmatched : Attribution
}

// The attribution algorithm (D35/D36) — the full path spec lives in docs/ATTRIBUTION.md.
// Match key is the chip she tapped: (typeId, asking price). The haggled price plays no
// role, the line is never split, and the result is stored once at save time.
object Attributor {

    fun attribute(typeId: String, askedUnit: Long, sources: List<SourceSnapshot>): Attribution {
        fun SourceSnapshot.point(): PricePointStock? =
            points.firstOrNull { it.typeId == typeId && it.price == askedUnit }

        val carrying = sources.filter { it.point() != null }
        if (carrying.isEmpty()) return Attribution.Unmatched

        val withStock = carrying.filter { it.point()!!.estimatedRemaining > 0 }
        val chosen = (withStock.ifEmpty { carrying }).maxBy { it.arrivedAt }
        return Attribution.ToSource(chosen.id)
    }
}
