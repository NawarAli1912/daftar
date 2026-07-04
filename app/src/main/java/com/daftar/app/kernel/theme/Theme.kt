package com.daftar.app.kernel.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daftar.app.R

object DaftarColors {
    val Surface0 = Color(0xFF0E0E10)
    val Surface1 = Color(0xFF1B1B20)
    val Surface2 = Color(0xFF232329)
    val Hairline = Color(0xFF2A2A31)
    val TextPrimary = Color(0xFFF4F4F5)
    val TextSecondary = Color(0xFFA1A1AA)
    val Teal = Color(0xFF2DD4BF)
    val OnTeal = Color(0xFF04342C)
    val Red = Color(0xFFF87171)
    val Green = Color(0xFF4ADE80)
    val Amber = Color(0xFFFBBF24)
}

val Cairo = FontFamily(Font(R.font.cairo))

private val DarkScheme = darkColorScheme(
    primary = DaftarColors.Teal,
    onPrimary = DaftarColors.OnTeal,
    primaryContainer = DaftarColors.Teal,
    onPrimaryContainer = DaftarColors.OnTeal,
    secondaryContainer = DaftarColors.Teal,
    onSecondaryContainer = DaftarColors.OnTeal,
    background = DaftarColors.Surface0,
    onBackground = DaftarColors.TextPrimary,
    surface = DaftarColors.Surface1,
    onSurface = DaftarColors.TextPrimary,
    surfaceVariant = DaftarColors.Surface2,
    onSurfaceVariant = DaftarColors.TextSecondary,
    outline = DaftarColors.Hairline,
    surfaceContainer = DaftarColors.Surface1,
    surfaceContainerHigh = DaftarColors.Surface2,
    error = DaftarColors.Red,
)

private fun cairo(size: Int, weight: FontWeight = FontWeight.Normal) =
    TextStyle(fontFamily = Cairo, fontSize = size.sp, fontWeight = weight)

private val DaftarTypography = Typography(
    headlineSmall = cairo(24, FontWeight.Bold),
    titleLarge = cairo(20, FontWeight.Bold),
    titleMedium = cairo(17, FontWeight.SemiBold),
    bodyLarge = cairo(16),
    bodyMedium = cairo(14),
    labelLarge = cairo(15, FontWeight.SemiBold),
    labelMedium = cairo(13),
)

@Composable
fun DaftarTheme(content: @Composable () -> Unit) {
    val direction = if (com.daftar.app.kernel.i18n.Str.arabic) LayoutDirection.Rtl else LayoutDirection.Ltr
    CompositionLocalProvider(LocalLayoutDirection provides direction) {
        MaterialTheme(
            colorScheme = DarkScheme,
            typography = DaftarTypography,
            shapes = Shapes(
                small = RoundedCornerShape(10.dp),
                medium = RoundedCornerShape(14.dp),
                large = RoundedCornerShape(18.dp),
            ),
            content = content,
        )
    }
}
