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

// Ledger-paper (matches prototype daftar-app-v1, owner 2026-07-05, supersedes D44):
// warm cream paper, ink text, ink primary actions, oxblood brand + margin rule, green money.
object DaftarColors {
    val Surface0 = Color(0xFFE9E5DC) // warm paper background
    val Surface1 = Color(0xFFFBFAF7) // card — off-white paper
    val Surface2 = Color(0xFFEFE9DE) // subtle fills, selected chips
    val Hairline = Color(0xFFE0DACE) // ledger rules, dividers
    val TextPrimary = Color(0xFF211E1A) // ink
    val TextSecondary = Color(0xFF6E675C) // dim — V3: darkened from #8C857A (3.1:1, failed WCAG AA) to ≥4.5:1 on card
    val Teal = Color(0xFFA4161A) // primary action — ledger-M3 vivid red (owner 2026-07-18: brown→vivid red matching the oxblood icon, a deeper/more-saturated shade). Distinct from debt in practice: primary is always a FILL, debt is always text/number. green=paid, amber=trial untouched
    val OnTeal = Color(0xFFF7F4EC) // light ink on the dark action
    val Oxblood = Color(0xFFB23124) // brand: wordmark, the margin rule, debt
    val Red = Color(0xFFB23124) // debt / negative
    val Green = Color(0xFF2F6B3D) // paid / money-in
    val Amber = Color(0xFF996410) // old debt / aging
}

val Cairo = FontFamily(Font(R.font.cairo))

// IBM Plex Sans Arabic — the V2 prototype's body face (weights 400/500/600/700).
val Plex = FontFamily(
    Font(R.font.ibm_plex_sans_arabic_regular, FontWeight.Normal),
    Font(R.font.ibm_plex_sans_arabic_medium, FontWeight.Medium),
    Font(R.font.ibm_plex_sans_arabic_semibold, FontWeight.SemiBold),
    Font(R.font.ibm_plex_sans_arabic_bold, FontWeight.Bold),
)

// Amiri serif — the prototype's wordmark/brand face (oxblood دفتر).
val Amiri = FontFamily(
    Font(R.font.amiri_regular),
    Font(R.font.amiri_bold, FontWeight.Bold),
)

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

private fun plex(size: Int, weight: FontWeight = FontWeight.Normal) =
    TextStyle(fontFamily = Plex, fontSize = size.sp, fontWeight = weight)

private val DaftarTypography = Typography(
    headlineSmall = plex(24, FontWeight.Bold),
    titleLarge = plex(20, FontWeight.Bold),
    titleMedium = plex(17, FontWeight.SemiBold),
    bodyLarge = plex(16),
    bodyMedium = plex(14),
    labelLarge = plex(15, FontWeight.SemiBold),
    labelMedium = plex(13),
)

@Composable
fun DaftarTheme(content: @Composable () -> Unit) {
    val direction = if (com.daftar.app.kernel.i18n.Str.arabic) LayoutDirection.Rtl else LayoutDirection.Ltr
    CompositionLocalProvider(LocalLayoutDirection provides direction) {
        MaterialTheme(
            colorScheme = LightScheme,
            typography = DaftarTypography,
            // Aligned to the store slice's ledger radius scale (rSm/rMd/rLg = 14/18/24) so M3
            // components (Card, Menu, TextField containers…) round like the rest of the app.
            // Literals, not the rXs… tokens, because kernel must not depend on the store slice.
            shapes = Shapes(
                small = RoundedCornerShape(14.dp),
                medium = RoundedCornerShape(18.dp),
                large = RoundedCornerShape(24.dp),
            ),
            content = content,
        )
    }
}
