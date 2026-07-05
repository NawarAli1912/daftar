package com.daftar.app.kernel.ledger

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StoreInventoryTest {

    private val bale = "bale"
    private val pants = "pants"
    private val shirt = "shirt"

    @Test
    fun `shelf on-hand is shelved minus sold`() {
        val shelf = StoreInventory.shelf(
            shelved = listOf(StoreInventory.Shelved(bale, pants, 7_500, 20)),
            sold = listOf(StoreInventory.Sold(bale, pants, 7_500, 3)),
        ).single()
        assertEquals(bale, shelf.sourceId)
        assertEquals(17, shelf.onHand)
        assertFalse(shelf.unspecified)
    }

    @Test
    fun `unspecified provenance is its own shelf line and raises the red dot`() {
        val shelf = StoreInventory.shelf(
            shelved = listOf(StoreInventory.Shelved(null, shirt, 6_000, 2)),
            sold = emptyList(),
        )
        assertTrue(shelf.single().unspecified)
        assertTrue(StoreInventory.hasUnspecified(shelf))
    }

    @Test
    fun `same type-price from two sources are separate shelf lines`() {
        val shelf = StoreInventory.shelf(
            shelved = listOf(
                StoreInventory.Shelved("baleA", pants, 7_500, 10),
                StoreInventory.Shelved("baleB", pants, 7_500, 5),
            ),
            sold = listOf(StoreInventory.Sold("baleA", pants, 7_500, 4)),
        ).sortedBy { it.sourceId }
        assertEquals(2, shelf.size)
        assertEquals(6, shelf.first { it.sourceId == "baleA" }.onHand)
        assertEquals(5, shelf.first { it.sourceId == "baleB" }.onHand)
    }

    @Test
    fun `no red dot when every shelf item has a known source`() {
        val shelf = StoreInventory.shelf(
            shelved = listOf(StoreInventory.Shelved(bale, pants, 7_500, 10)),
            sold = emptyList(),
        )
        assertFalse(StoreInventory.hasUnspecified(shelf))
    }

    @Test
    fun `unshelved is counted minus shelved (two-tier), rest stays in the package`() {
        val unshelved = StoreInventory.unshelved(
            counted = listOf(StoreInventory.Counted(bale, pants, 7_500, 40)),
            shelved = listOf(StoreInventory.Shelved(bale, pants, 7_500, 15)),
        )
        assertEquals(25, unshelved[Triple(bale, pants, 7_500L)])
    }

    @Test
    fun `voided shelvings and sales are excluded`() {
        val shelf = StoreInventory.shelf(
            shelved = listOf(
                StoreInventory.Shelved(bale, pants, 7_500, 20),
                StoreInventory.Shelved(bale, pants, 7_500, 99, voided = true),
            ),
            sold = listOf(StoreInventory.Sold(bale, pants, 7_500, 5, voided = true)),
        ).single()
        assertEquals(20, shelf.onHand)
    }
}
