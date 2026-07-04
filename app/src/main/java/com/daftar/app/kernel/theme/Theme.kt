package com.daftar.app.kernel.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
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

// D44: clean light theme — iOS-grouped surfaces, paper-honest for a daftar.
object DaftarColors {
    val Surface0 = Color(0xFFF3F2F7) // app background — soft neutral, not stark white
    val Surface1 = Color(0xFFFFFFFF) // cards
    val Surface2 = Color(0xFFECEBF1) // subtle fills, selected chips
    val Hairline = Color(0xFFE4E3EA) // dividers, ledger rules
    val TextPrimary = Color(0xFF1C1C1E)
    val TextSecondary = Color(0xFF8A8A8E)
    val Teal = Color(0xFF10766A) // refined accent, legible on light
    val OnTeal = Color(0xFFFFFFFF)
    val Red = Color(0xFFD6453F) // debt
    val Green = Color(0xFF2E9E5B) // paid / shop-owes
    val Amber = Color(0xFFB4791F) // old debt
}

val Cairo = FontFamily(Font(R.font.cairo))

private val LightScheme = lightColorScheme(
    primary = DaftarColors.Teal,
    onPrimary = DaftarColors.OnTeal,
    primaryContainer = DaftarColors.Teal,
    onPrimaryContainer = DaftarColors.OnTeal,
    secondaryContainer = DaftarColors.Surface2,
    onSecondaryContainer = DaftarColors.TextPrimary,
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
            colorScheme = LightScheme,
            typography = DaftarTypography,
            shapes = Shapes(
                small = RoundedCornerShape(14.dp),
                medium = RoundedCornerShape(20.dp),
                large = RoundedCornerShape(26.dp),
            ),
            content = content,
        )
    }
}
