package com.daftar.app.kernel.format

import org.junit.Assert.assertEquals
import org.junit.Test

class ArabicNumbersTest {

    @Test
    fun `zero renders as the eastern arabic zero`() {
        assertEquals("٠", ArabicNumbers.format(0))
    }

    @Test
    fun `thousands are grouped with the arabic separator`() {
        assertEquals("٧٬٥٠٠", ArabicNumbers.format(7_500))
    }

    @Test
    fun `large numbers group every three digits`() {
        assertEquals("١٬٢٣٤٬٥٦٧", ArabicNumbers.format(1_234_567))
    }

    @Test
    fun `numbers below one thousand have no separator`() {
        assertEquals("٩٩٩", ArabicNumbers.format(999))
    }

    @Test
    fun `negative amounts keep the sign`() {
        assertEquals("-٣٬٥٠٠", ArabicNumbers.format(-3_500))
    }

    @Test
    fun `eastern digits normalize to western`() {
        assertEquals("01", ArabicNumbers.toWesternDigits("٠١"))
        assertEquals("5000", ArabicNumbers.toWesternDigits("٥٠٠٠"))
    }

    @Test
    fun `mixed input normalizes - both keyboards supported`() {
        assertEquals("50", ArabicNumbers.toWesternDigits("٥0"))
        assertEquals("0912345678", ArabicNumbers.toWesternDigits("٠٩١٢345678"))
    }

    @Test
    fun `western digits and other characters pass through`() {
        assertEquals("0912 345", ArabicNumbers.toWesternDigits("0912 345"))
    }

    @Test
    fun `amount parsing accepts either digit system`() {
        assertEquals(7500L, ArabicNumbers.parseAmount("٧٥٠٠"))
        assertEquals(7500L, ArabicNumbers.parseAmount("7500"))
        assertEquals(0L, ArabicNumbers.parseAmount(""))
        assertEquals(0L, ArabicNumbers.parseAmount("abc"))
    }
}
