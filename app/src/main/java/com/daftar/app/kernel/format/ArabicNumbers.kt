package com.daftar.app.kernel.format

object ArabicNumbers {

    private const val EASTERN_ZERO = '٠'
    private const val SEPARATOR = '٬'

    fun format(amount: Long): String {
        val digits = kotlin.math.abs(amount).toString()
        val grouped = StringBuilder()
        digits.forEachIndexed { index, char ->
            val remaining = digits.length - index
            grouped.append(EASTERN_ZERO + (char - '0'))
            if (remaining > 1 && (remaining - 1) % 3 == 0) grouped.append(SEPARATOR)
        }
        return if (amount < 0) "-$grouped" else grouped.toString()
    }
}
