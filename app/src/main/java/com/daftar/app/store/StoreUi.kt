package com.daftar.app.store

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
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

// The tapped element's on-screen rect (window coords), so a sheet can grow FROM it and shrink
// back INTO it (Dynamic-Island style). `tapExpand` sets it; a plain `tap` clears it so a
// button-opened sheet just slides up from the bottom instead of morphing from a stale rect.
internal val LocalSheetOrigin = androidx.compose.runtime.compositionLocalOf { androidx.compose.runtime.mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }

// A tap target with a satisfying, iOS-style press: the element springs down to 0.96 while
// held and settles back with a gentle bounce on release. No ripple.
internal fun Modifier.tap(onClick: () -> Unit): Modifier = this.composed {
    val src = remember { MutableInteractionSource() }
    val origin = LocalSheetOrigin.current
    val pressed by src.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 650f),
        label = "press",
    )
    graphicsLayer { scaleX = scale; scaleY = scale }
        .clickable(interactionSource = src, indication = null) { origin.value = null; onClick() }
}

// Like `tap`, but records this element's window rect first — the sheet it opens will expand
// out of this exact spot and collapse back into it.
internal fun Modifier.tapExpand(onClick: () -> Unit): Modifier = this.composed {
    val src = remember { MutableInteractionSource() }
    val origin = LocalSheetOrigin.current
    var bounds by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
    val pressed by src.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 650f),
        label = "press",
    )
    Modifier.onGloballyPositioned { bounds = it.boundsInWindow() }
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .clickable(interactionSource = src, indication = null) { origin.value = bounds; onClick() }
}

// Grouped-digits display for a money field being typed: "12345" renders "12,345" with a
// correct cursor mapping, so the value reads like everywhere else in the ledger.
private object ThousandsTransformation : androidx.compose.ui.text.input.VisualTransformation {
    override fun filter(text: androidx.compose.ui.text.AnnotatedString): androidx.compose.ui.text.input.TransformedText {
        val raw = text.text
        val grouped = raw.reversed().chunked(3).joinToString(",").reversed()
        val o2t = IntArray(raw.length + 1)
        var ri = 0
        grouped.forEachIndexed { ti, ch -> if (ch != ',') { o2t[ri] = ti; ri++ } }
        o2t[raw.length] = grouped.length
        val mapping = object : androidx.compose.ui.text.input.OffsetMapping {
            override fun originalToTransformed(offset: Int) = o2t[offset.coerceIn(0, raw.length)]
            override fun transformedToOriginal(offset: Int) = grouped.take(offset.coerceIn(0, grouped.length)).count { it != ',' }
        }
        return androidx.compose.ui.text.input.TransformedText(androidx.compose.ui.text.AnnotatedString(grouped), mapping)
    }
}

// A money amount she can EDIT DIRECTLY: looks like the bold ledger number, but a tap puts the
// cursor in it and opens the numeric keyboard (owner 2026-07-18: steppers alone — ±500 — were
// the only way to change an amount). Steppers stay beside it for quick nudges.
@Composable
internal fun MoneyValue(
    value: Long,
    onValue: (Long) -> Unit,
    fontSize: TextUnit,
    minWidth: Dp = 60.dp,
    color: Color = cInk,
) {
    // 0 renders as an EMPTY field (dim placeholder) — a literal "0" merges with typed digits
    // (clearing then typing 175 gave «1,750») and forces cursor gymnastics she can't do.
    // Width is computed from the grouped digit count (× the user's font scale): an unbounded
    // BasicTextField in a Row starves its siblings (labels wrapped letter-by-letter on ~360dp
    // phones), and IntrinsicSize.Min CLIPS — text fields don't report text width as intrinsic.
    val grouped = if (value == 0L) "0" else fmt(value)
    val fontScale = androidx.compose.ui.platform.LocalDensity.current.fontScale
    val fitWidth = (grouped.length * fontSize.value * 0.62f * fontScale + 12f).dp
    androidx.compose.foundation.text.BasicTextField(
        value = if (value == 0L) "" else value.toString(),
        onValueChange = { s -> onValue(s.filter { it.isDigit() }.take(9).toLongOrNull() ?: 0L) },
        modifier = Modifier.width(maxOf(minWidth, fitWidth)),
        textStyle = androidx.compose.ui.text.TextStyle(
            fontFamily = com.daftar.app.kernel.theme.Plex, fontSize = fontSize,
            fontWeight = FontWeight.Bold, color = color,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        ),
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
        singleLine = true,
        cursorBrush = androidx.compose.ui.graphics.SolidColor(cInk),
        visualTransformation = ThousandsTransformation,
        decorationBox = { inner ->
            Box(contentAlignment = Alignment.Center) {
                if (value == 0L) Text("0", fontSize = fontSize, fontWeight = FontWeight.Bold, color = cDim)
                inner()
            }
        },
    )
}

// One search box for the list screens (الزبائن، البضاعة) — replaces two copy-pasted fields.
@Composable
internal fun SearchField(value: String, onValueChange: (String) -> Unit, placeholder: String) {
    androidx.compose.foundation.text.BasicTextField(
        value = value, onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(rMd)).background(cBg).border(1.dp, cLine, RoundedCornerShape(rMd)).padding(horizontal = 13.dp, vertical = 13.dp),
        textStyle = androidx.compose.ui.text.TextStyle(fontFamily = com.daftar.app.kernel.theme.Plex, fontSize = fBodyL, color = cInk),
        cursorBrush = androidx.compose.ui.graphics.SolidColor(cInk), singleLine = true,
        decorationBox = { inner -> Box { if (value.isEmpty()) Text("🔍 $placeholder", fontSize = fBodyL, color = cDim); inner() } },
    )
}

// The one-line page intro under a screen's top — tells her what the page is for, in her words.
@Composable
internal fun PageIntro(text: String) {
    Text(text, fontSize = fSmall, color = cDim, lineHeight = 20.sp, modifier = Modifier.padding(bottom = 10.dp))
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
internal fun Modifier.card(radius: Dp = rLg): Modifier =
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

// F6 «real daftar» marker highlight — a translucent sweep drawn BEHIND a key token, with
// slightly uneven top/bottom edges so it reads like a hand-drawn highlighter on paper, not a
// solid chip. Use sparingly (≤ one token per row): oxblood for debtor names, amber for
// أمانة/sources, green for paid. `seed` (e.g. the token's hashCode) varies the waver so no two
// strokes look identical.
internal fun Modifier.marker(color: Color, seed: Int = 0): Modifier =
    this.drawBehind {
        val w = size.width
        val h = size.height
        val pad = 3.dp.toPx()
        // small deterministic jitter from the seed → each stroke sits a touch differently
        fun j(n: Int) = ((seed / (n + 1)) % 5 - 2) * 0.6.dp.toPx()
        val top = h * 0.16f
        val bot = h * 0.94f
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(-pad, top + j(1))
            lineTo(w + pad, top + j(2))
            lineTo(w + pad, bot + j(3))
            lineTo(-pad, bot + j(4))
            close()
        }
        drawPath(path, color.copy(alpha = 0.20f))
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

// Apply a rise (+ optional fade + scale-from) driven by an appear progress.
internal fun Modifier.riseFade(p: Float, riseDp: Dp = 10.dp, fromScale: Float = 1f, fade: Boolean = true): Modifier =
    this.graphicsLayer {
        alpha = if (fade) p else 1f
        translationY = (1f - p) * riseDp.toPx()
        val s = fromScale + (1f - fromScale) * p
        scaleX = s
        scaleY = s
    }

// ── screen-level transitions — one shared vocabulary so tabs, segments and day-flips
// all move the same way (replaces the instant, un-animated content swaps) ──

// A gentle fade + small rise when the keyed content changes. For tab and segment switches.
@Composable
internal fun <T> Swap(target: T, modifier: Modifier = Modifier, content: @Composable (T) -> Unit) {
    AnimatedContent(
        targetState = target,
        modifier = modifier,
        transitionSpec = {
            (fadeIn(tween(220)) + slideInVertically(spring(dampingRatio = 0.9f, stiffness = 380f)) { h -> h / 22 }) togetherWith
                fadeOut(tween(110))
        },
        label = "swap",
    ) { content(it) }
}

// A directional horizontal slide — the day book's page-turn. `forward` = moving to a newer day.
@Composable
internal fun <T> PageFlip(target: T, forward: Boolean, modifier: Modifier = Modifier, content: @Composable (T) -> Unit) {
    AnimatedContent(
        targetState = target,
        modifier = modifier,
        transitionSpec = {
            val dir = if (forward) 1 else -1
            (slideInHorizontally(spring(dampingRatio = 0.9f, stiffness = 320f)) { w -> dir * w / 5 } + fadeIn(tween(200))) togetherWith
                (slideOutHorizontally(tween(160)) { w -> -dir * w / 5 } + fadeOut(tween(140)))
        },
        label = "pageflip",
    ) { content(it) }
}

// ── design tokens ──
// One type scale and one radius scale, so the whole app speaks in named steps instead of
// ad-hoc per-element numbers.
// V3 (elderly-first): the whole scale sits 2–3sp above the V2 prototype — the owner reads at
// arm's length without glasses, so 16sp is the *floor* for anything she must read, and
// captions never drop below 13sp. Display sizes (amounts/totals: 24sp+) stay as literals.

// type scale (sp)
internal val fCaption = 13.sp // sub-labels, hints, meta — the smallest text in the app
internal val fSmall = 14.sp   // secondary text, chips
internal val fBody = 15.sp    // body, list sub-rows
internal val fBodyL = 16.sp   // list titles, inputs
internal val fTitle = 18.sp   // row/card titles, CTAs
internal val fHead = 20.sp    // sheet titles, primary buttons
internal val fGlyph = 22.sp   // tab/nav glyphs

// touch-target scale (dp) — nothing she must hit is smaller than tapSm; the money steppers
// in the sale flow (the 15-second path) use tapMd/tapLg.
internal val tapSm = 36.dp // subordinate steppers (quantity)
internal val tapMd = 42.dp // standard steppers (price, counts)
internal val tapLg = 48.dp // the big money steppers (pay, return)

// radius scale (dp) — Apple-soft: generously rounded, continuous-feeling corners. Bumped
// from the old tight 8/10/12/14 scale (owner: "more rounded corners like apple design").
internal val rXs = 11.dp  // badges, chips, small controls
internal val rSm = 14.dp  // inputs, small buttons
internal val rMd = 18.dp  // secondary cards, banners, mid buttons
internal val rLg = 24.dp  // cards, primary buttons, chooser rows
internal val rPill = 99.dp // fully-round pills / handles
