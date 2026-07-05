package com.daftar.app.kernel.ledger

// D51 — the two-tier store model. A package holds *counted* items; the owner *shelves*
// all or part onto the Store; the Store is the sellable shelf and the only source of sale
// suggestions. Every shelf item carries explicit provenance (a source) or is unspecified
// (sourceId == null → the red-dot to resolve). No newest-first guessing.
object StoreInventory {

    // What a package was counted to hold.
    data class Counted(
        val sourceId: String,
        val typeId: String,
        val price: Long,
        val qty: Int,
        val voided: Boolean = false,
    )

    // A shelving move: qty of a (type,price) put on the shelf, tagged with its provenance.
    // sourceId == null means unspecified (e.g. an item first seen mid-sale).
    data class Shelved(
        val sourceId: String?,
        val typeId: String,
        val price: Long,
        val qty: Int,
        val voided: Boolean = false,
    )

    // A sale off the shelf, attributed to the shelf item's provenance (null = unspecified).
    data class Sold(
        val sourceId: String?,
        val typeId: String,
        val price: Long,
        val qty: Int,
        val voided: Boolean = false,
    )

    data class ShelfItem(
        val sourceId: String?,
        val typeId: String,
        val price: Long,
        val onHand: Int,
    ) {
        val unspecified: Boolean get() = sourceId == null
    }

    private data class Key(val sourceId: String?, val typeId: String, val price: Long)

    // On-hand per (provenance, type, price) = shelved − sold. These are the sale suggestions
    // (those with onHand > 0). Negative on-hand means over-sold — kept honest, never clamped here.
    fun shelf(shelved: List<Shelved>, sold: List<Sold>): List<ShelfItem> {
        val shelvedByKey = shelved.filterNot { it.voided }
            .groupBy { Key(it.sourceId, it.typeId, it.price) }
            .mapValues { (_, v) -> v.sumOf { it.qty } }
        val soldByKey = sold.filterNot { it.voided }
            .groupBy { Key(it.sourceId, it.typeId, it.price) }
            .mapValues { (_, v) -> v.sumOf { it.qty } }
        return (shelvedByKey.keys + soldByKey.keys).map { key ->
            ShelfItem(key.sourceId, key.typeId, key.price, (shelvedByKey[key] ?: 0) - (soldByKey[key] ?: 0))
        }
    }

    // Still in the package, not yet shelved, per (source,type,price) = counted − shelved.
    fun unshelved(counted: List<Counted>, shelved: List<Shelved>): Map<Triple<String, String, Long>, Int> {
        val shelvedBySource = shelved.filterNot { it.voided && it.sourceId != null }
            .filter { it.sourceId != null }
            .groupBy { Triple(it.sourceId!!, it.typeId, it.price) }
            .mapValues { (_, v) -> v.sumOf { it.qty } }
        return counted.filterNot { it.voided }
            .groupBy { Triple(it.sourceId, it.typeId, it.price) }
            .mapValues { (key, v) -> v.sumOf { it.qty } - (shelvedBySource[key] ?: 0) }
    }

    // The red-dot condition: any on-shelf item whose provenance is unknown.
    fun hasUnspecified(shelf: List<ShelfItem>): Boolean =
        shelf.any { it.unspecified && it.onHand > 0 }
}
