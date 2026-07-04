package com.daftar.app.kernel.ledger

import org.junit.Assert.assertEquals
import org.junit.Test

class SnapshotBuilderTest {

    private val trousers = "type-trousers"

    private fun snapshotFor(
        sources: List<SourceMeta>,
        intake: List<IntakeCount>,
        sold: List<AttributedSale> = emptyList(),
    ) = SnapshotBuilder.build(sources, intake, sold)

    @Test
    fun `counted stock becomes the remaining estimate`() {
        val result = snapshotFor(
            sources = listOf(SourceMeta("bale-feb", 100, voided = false)),
            intake = listOf(IntakeCount("bale-feb", trousers, 7_500, 40, voided = false)),
        )
        assertEquals(40, result.single().points.single().estimatedRemaining)
    }

    @Test
    fun `append-only sittings sum into one point (D10)`() {
        val result = snapshotFor(
            sources = listOf(SourceMeta("bale-feb", 100, voided = false)),
            intake = listOf(
                IntakeCount("bale-feb", trousers, 7_500, 25, voided = false),
                IntakeCount("bale-feb", trousers, 7_500, 15, voided = false),
            ),
        )
        assertEquals(40, result.single().points.single().estimatedRemaining)
    }

    @Test
    fun `attributed sales reduce the remaining`() {
        val result = snapshotFor(
            sources = listOf(SourceMeta("bale-feb", 100, voided = false)),
            intake = listOf(IntakeCount("bale-feb", trousers, 7_500, 40, voided = false)),
            sold = listOf(AttributedSale("bale-feb", trousers, 7_500, 3, voided = false)),
        )
        assertEquals(37, result.single().points.single().estimatedRemaining)
    }

    @Test
    fun `voided intake and voided sales are excluded (D21)`() {
        val result = snapshotFor(
            sources = listOf(SourceMeta("bale-feb", 100, voided = false)),
            intake = listOf(
                IntakeCount("bale-feb", trousers, 7_500, 40, voided = false),
                IntakeCount("bale-feb", trousers, 7_500, 99, voided = true),
            ),
            sold = listOf(AttributedSale("bale-feb", trousers, 7_500, 5, voided = true)),
        )
        assertEquals(40, result.single().points.single().estimatedRemaining)
    }

    @Test
    fun `remaining may go negative - honest undercount signal (D36)`() {
        val result = snapshotFor(
            sources = listOf(SourceMeta("bale-feb", 100, voided = false)),
            intake = listOf(IntakeCount("bale-feb", trousers, 7_500, 2, voided = false)),
            sold = listOf(AttributedSale("bale-feb", trousers, 7_500, 5, voided = false)),
        )
        assertEquals(-3, result.single().points.single().estimatedRemaining)
    }

    @Test
    fun `voided sources disappear entirely`() {
        val result = snapshotFor(
            sources = listOf(SourceMeta("bale-feb", 100, voided = true)),
            intake = listOf(IntakeCount("bale-feb", trousers, 7_500, 40, voided = false)),
        )
        assertEquals(0, result.size)
    }
}
