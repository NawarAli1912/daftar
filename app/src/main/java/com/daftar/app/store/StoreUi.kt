package com.daftar.app.store

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daftar.app.kernel.theme.DaftarColors

// ── the prototype's CSS variables, as Compose colors ──
internal val cBg = DaftarColors.Surface0      // --bg  #E9E5DC
internal val cCard = DaftarColors.Surface1    // --card #FBFAF7
internal val cInk = DaftarColors.TextPrimary  // --ink #211E1A
internal val cDim = DaftarColors.TextSecondary// --dim #8C857A
internal val cLine = DaftarColors.Hairline    // --line #E0DACE
internal val cAccent = DaftarColors.Teal      // --accent #211E1A
internal val cAink = DaftarColors.OnTeal      // --aink #F7F4EC
internal val cDebt = DaftarColors.Oxblood     // --debt #B23124
internal val cPaid = DaftarColors.Green       // --paid #2F6B3D
internal val cAmber = DaftarColors.Amber      // --amber #996410

// literal accents used in a few places by the prototype
internal val cUnspecBorder = Color(0xFFF0CFCA)
internal val cAmberBg = Color(0xFFFFF8EA)
internal val cAmberBorder = Color(0xFFEEDCAE)
internal val cGreenBg = Color(0xFFEEF4EF)
internal val cGreenBorder = Color(0xFFCFE0D3)
internal val cScrim = Color(0x57140F0D)       // rgba(20,17,13,.34)
internal val cUndoAccent = Color(0xFFF4C84A)

// A tap target with a satisfying, iOS-style press: the element springs down to 0.96 while
// held and settles back with a gentle bounce on release. No ripple.
internal fun Modifier.tap(onClick: () -> Unit): Modifier = this.composed {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 650f),
        label = "press",
    )
    graphicsLayer { scaleX = scale; scaleY = scale }
        .clickable(interactionSource = src, indication = null, onClick = onClick)
}

// Rounded ± stepper button.
@Composable
internal fun StepBtn(
    sym: String,
    size: Dp,
    radius: Dp,
    borderW: Dp,
    borderColor: Color,
    textColor: Color,
    fontSize: TextUnit,
    onClick: () -> Unit,
) {
    Box(
        Modifier
            .size(size)
            .clip(RoundedCornerShape(radius))
            .border(borderW, borderColor, RoundedCornerShape(radius))
            .tap(onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(sym, fontSize = fontSize, fontWeight = FontWeight.Bold, color = textColor)
    }
}

// card surface: cream fill + hairline border + rounded corner
internal fun Modifier.card(radius: Dp = 14.dp): Modifier =
    this.clip(RoundedCornerShape(radius))
        .background(cCard)
        .border(1.dp, cLine, RoundedCornerShape(radius))

internal fun Modifier.fill(color: Color, radius: Dp): Modifier =
    this.clip(RoundedCornerShape(radius)).background(color)

// dashed hairline border (empty-day card, "+ صنف جديد" chip)
internal fun Modifier.dashedBorder(color: Color, radius: Dp, width: Dp = 1.dp): Modifier =
    this.drawBehind {
        drawRoundRect(
            color = color,
            style = Stroke(width = width.toPx(), pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(11f, 8f))),
            cornerRadius = CornerRadius(radius.toPx()),
        )
    }

// ── entrance motion — springs, for an Apple-like settle (replaces the prototype's linear eases) ──
// A 0→1 progress that runs once on first composition; re-runs if `key` changes.
internal val SheetSpring: AnimationSpec<Float> = spring(dampingRatio = 0.82f, stiffness = 320f)
internal val BounceSpring: AnimationSpec<Float> = spring(dampingRatio = 0.6f, stiffness = 340f)

@Composable
internal fun appearProgress(key: Any? = Unit, spec: AnimationSpec<Float> = SheetSpring): Float {
    var shown by remember(key) { mutableStateOf(false) }
    LaunchedEffect(key) { shown = true }
    val p by animateFloatAsState(targetValue = if (shown) 1f else 0f, animationSpec = spec, label = "appear")
    return p
}

// Apply a rise + fade (+ optional scale-from) driven by an appear progress.
internal fun Modifier.riseFade(p: Float, riseDp: Dp = 10.dp, fromScale: Float = 1f): Modifier =
    this.graphicsLayer {
        alpha = p
        translationY = (1f - p) * riseDp.toPx()
        val s = fromScale + (1f - fromScale) * p
        scaleX = s
        scaleY = s
    }

@Composable
internal fun Scrim(onClick: () -> Unit) {
    val p = appearProgress(spec = tween(200)) // plain fade for the dimming
    Box(Modifier.fillMaxSize().graphicsLayer { alpha = p }.background(cScrim).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick))
}

internal val sp11 = 11.sp
internal val sp12 = 12.sp
internal val sp13 = 13.sp
internal val sp14 = 14.sp
internal val sp15 = 15.sp
