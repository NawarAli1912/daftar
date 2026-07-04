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
}
