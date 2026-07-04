package com.daftar.app.kernel.ledger

import org.junit.Assert.assertEquals
import org.junit.Test

class AttributorTest {

    private val trousers = "type-trousers"
    private val dress = "type-dress"

    private fun source(
        id: String,
        arrivedAt: Long,
        vararg points: Triple<String, Long, Int>,
    ) = SourceSnapshot(
        id = id,
        arrivedAt = arrivedAt,
        points = points.map { (typeId, price, remaining) ->
            PricePointStock(typeId, price, remaining)
        },
    )

    @Test
    fun `path 1 - exactly one carrying source gets the line`() {
        val sources = listOf(source("bale-feb", 100, Triple(trousers, 7_500L, 40)))
        assertEquals(
            Attribution.ToSource("bale-feb"),
            Attributor.attribute(trousers, 7_500, sources),
        )
    }

    @Test
    fun `path 2 - several carrying sources - newest wins (D35)`() {
        val sources = listOf(
            source("bale-feb", 100, Triple(trousers, 7_500L, 40)),
            source("market-mar", 200, Triple(trousers, 7_500L, 5)),
        )
        assertEquals(
            Attribution.ToSource("market-mar"),
            Attributor.attribute(trousers, 7_500, sources),
        )
    }

    @Test
    fun `path 3 - no source carries the price point - unmatched (D11)`() {
        val sources = listOf(source("bale-feb", 100, Triple(trousers, 7_500L, 40)))
        assertEquals(
            Attribution.Unmatched,
            Attributor.attribute(dress, 8_000, sources),
        )
    }

    @Test
    fun `path 4 - same type at a different price is a different chip - unmatched`() {
        val sources = listOf(source("bale-feb", 100, Triple(trousers, 7_500L, 40)))
        assertEquals(
            Attribution.Unmatched,
            Attributor.attribute(trousers, 5_000, sources),
        )
    }

    @Test
    fun `path 5 - newest is estimated empty - falls to newest WITH stock (D36)`() {
        val sources = listOf(
            source("bale-feb", 100, Triple(trousers, 7_500L, 12)),
            source("market-mar", 200, Triple(trousers, 7_500L, 0)),
        )
        assertEquals(
            Attribution.ToSource("bale-feb"),
            Attributor.attribute(trousers, 7_500, sources),
        )
    }

    @Test
    fun `path 6 - everything estimated empty - newest overall (estimates are rough)`() {
        val sources = listOf(
            source("bale-feb", 100, Triple(trousers, 7_500L, 0)),
            source("market-mar", 200, Triple(trousers, 7_500L, 0)),
        )
        assertEquals(
            Attribution.ToSource("market-mar"),
            Attributor.attribute(trousers, 7_500, sources),
        )
    }

    @Test
    fun `path 7 - no sources at all - unmatched (pre-stock interim, D34)`() {
        assertEquals(
            Attribution.Unmatched,
            Attributor.attribute(trousers, 7_500, emptyList()),
        )
    }

    @Test
    fun `negative remaining counts as no stock, not as an error`() {
        val sources = listOf(
            source("bale-feb", 100, Triple(trousers, 7_500L, 3)),
            source("market-mar", 200, Triple(trousers, 7_500L, -2)),
        )
        assertEquals(
            Attribution.ToSource("bale-feb"),
            Attributor.attribute(trousers, 7_500, sources),
        )
    }
}
