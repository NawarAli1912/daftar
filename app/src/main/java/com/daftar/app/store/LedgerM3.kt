package com.daftar.app.store

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daftar.app.kernel.theme.Plex

// ── ledger-M3 vocabulary ──────────────────────────────────────────────────────────────────
// Material 3 components, dressed in the ledger-book identity (warm paper surfaces, ink text,
// leather-brown primary, oxblood debt) and sized for an elderly user reading at arm's length.
// This file is the single home for the redesign's shared controls; screens consume these
// instead of hand-rolling scrims, buttons and steppers. See DECISIONS (ledger-M3 supersedes
// the V2 prototype as the spec).

// ── the focus card ──────────────────────────────────────────────────────────────────────────
// A centered card that scales+fades up as the point of focus (owner 2026-07-18: "expand from its
// place and centralize itself as the point of focus, just like list items" — replaces the
// bottom-anchored ModalBottomSheet). In-tree (NOT a separate window) so cards stack by z-order and
// a nested picker/popup always sits on top — the old ModalBottomSheet layering bug is gone.
// Driven by LocalSheetTransition (provided by OverlaySlot) for a symmetric enter/exit.

@Composable
private fun sheetProgress(): Float {
    val state = LocalSheetTransition.current
    return if (state != null) {
        val transition = rememberTransition(state, label = "sheet")
        val v by transition.animateFloat(
            transitionSpec = { spring(dampingRatio = 0.82f, stiffness = 320f) }, label = "p",
        ) { if (it) 1f else 0f }
        v
    } else 1f
}

@Composable
private fun FocusScrim(p: Float, onDismiss: () -> Unit) {
    Box(
        Modifier.fillMaxSize().graphicsLayer { alpha = p.coerceIn(0f, 1f) }.background(cScrim)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
    )
}

private fun Modifier.focusCardMorph(p: Float): Modifier = this.graphicsLayer {
    val s = 0.9f + 0.1f * p
    scaleX = s; scaleY = s
    alpha = (p * 1.4f).coerceIn(0f, 1f)
}

// A short, simple focus card (confirm / picker) — wraps its content on the card surface, scrollable.
@Composable
internal fun LedgerSheet(
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val p = sheetProgress()
    Box(Modifier.fillMaxSize()) {
        FocusScrim(p, onDismiss)
        BoxWithConstraints(Modifier.fillMaxSize().imePadding().padding(12.dp), contentAlignment = Alignment.Center) {
            val maxH = maxHeight * 0.88f
            Column(
                Modifier.fillMaxWidth().heightIn(max = maxH).focusCardMorph(p)
                    .clip(RoundedCornerShape(rLg)).background(cCard).navigationBarsPadding()
                    .verticalScroll(rememberScrollState()).padding(20.dp),
                content = content,
            )
        }
    }
}

// The EDITOR focus card for a committing form (قيد / return / add item): title header → scrollable
// body → a primary action PINNED at the bottom, keyboard-aware. `back` shows a ‹ for sub-steps; the
// ✕ (and a scrim tap) dismiss.
@Composable
internal fun LedgerFormSheet(
    title: String,
    onDismiss: () -> Unit,
    saveLabel: String,
    onSave: () -> Unit,
    back: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val p = sheetProgress()
    Box(Modifier.fillMaxSize()) {
        FocusScrim(p, onDismiss)
        BoxWithConstraints(Modifier.fillMaxSize().imePadding().padding(12.dp), contentAlignment = Alignment.Center) {
            val maxH = maxHeight * 0.92f
            Column(
                Modifier.fillMaxWidth().heightIn(max = maxH).focusCardMorph(p)
                    .clip(RoundedCornerShape(rLg)).background(cBg).navigationBarsPadding(),
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(start = 18.dp, end = 16.dp, top = 16.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (back != null) Text("‹", fontSize = 26.sp, color = cDim, modifier = Modifier.tap(back).padding(horizontal = 10.dp, vertical = 4.dp))
                    else Spacer(Modifier.width(1.dp))
                    Text(title, fontSize = fHead, fontWeight = FontWeight.Bold, color = cInk)
                    Text("✕", fontSize = 22.sp, color = cDim, modifier = Modifier.tap(onDismiss).padding(horizontal = 10.dp, vertical = 4.dp))
                }
                HorizontalDivider(color = cLine, thickness = 1.dp)
                Column(
                    Modifier.weight(1f, fill = false).fillMaxWidth().verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 15.dp),
                    content = content,
                )
                HorizontalDivider(color = cLine, thickness = 1.dp)
                Box(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 14.dp)) {
                    LedgerButton(saveLabel, onClick = onSave)
                }
            }
        }
    }
}

// Primary action — filled leather-brown, generously rounded, ≥56dp tall for her thumb.
@Composable
internal fun LedgerButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    container: Color = cAccent,
    content: Color = cAink,
    fontSize: TextUnit = fHead,
    minHeight: Dp = 56.dp,
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().heightIn(min = minHeight),
        shape = RoundedCornerShape(rLg),
        colors = ButtonDefaults.buttonColors(containerColor = container, contentColor = content),
        contentPadding = PaddingValues(vertical = 14.dp, horizontal = 20.dp),
    ) {
        Text(text, fontFamily = Plex, fontSize = fontSize, fontWeight = FontWeight.Bold)
    }
}

// Destructive action — same shape/size, oxblood fill (wipe / reset).
@Composable
internal fun LedgerDangerButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) =
    LedgerButton(text, onClick, modifier, container = cDebt, content = cAink)

// Secondary / cancel — outlined in the primary tone on paper.
@Composable
internal fun LedgerOutlineButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tone: Color = cAccent,
    fontSize: TextUnit = fTitle,
    minHeight: Dp = 52.dp,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().heightIn(min = minHeight),
        shape = RoundedCornerShape(rLg),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, tone),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = tone),
        contentPadding = PaddingValues(vertical = 13.dp, horizontal = 20.dp),
    ) {
        Text(text, fontFamily = Plex, fontSize = fontSize, fontWeight = FontWeight.Bold)
    }
}

// Single-choice segment (كيف دفعت؟ …) — a custom pill-in-a-track control, NOT M3's connected
// SegmentedButton (whose selected-segment border renders square until an interaction, and which
// clashes with the app's other segmented controls). This matches the SegBtn family used by
// الأصناف/المصادر and the size selector: one bordered track, equal pills, active fills red.
@Composable
internal fun LedgerSegmented(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(rMd)).background(cCard)
            .border(1.dp, cLine, RoundedCornerShape(rMd)).padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        options.forEachIndexed { i, label ->
            val active = i == selectedIndex
            val bg by animateColorAsState(if (active) cAccent else Color.Transparent, spring(stiffness = 700f), label = "segbg")
            val fg by animateColorAsState(if (active) cAink else cInk, spring(stiffness = 700f), label = "segfg")
            Box(
                Modifier.weight(1f).clip(RoundedCornerShape(rSm)).background(bg).heightIn(min = 48.dp).tap { onSelect(i) },
                contentAlignment = Alignment.Center,
            ) {
                Text(label, fontFamily = Plex, fontSize = fBody, fontWeight = FontWeight.Bold, color = fg, maxLines = 1)
            }
        }
    }
}

// The create action for a list screen — an M3 Extended FAB, ledger-red, in the pinned slot
// above the tab bar (never scrolls away, never covers content). One consistent home for every
// "+ new thing" (قيد / صنف / بالة), resolving the old scroll-away vs pinned inconsistency.
@Composable
internal fun LedgerFab(label: String, onClick: () -> Unit) {
    ExtendedFloatingActionButton(
        onClick = onClick,
        containerColor = cAccent,
        contentColor = cAink,
        shape = RoundedCornerShape(rLg),
    ) {
        Text("＋ $label", fontFamily = Plex, fontSize = fTitle, fontWeight = FontWeight.Bold)
    }
}

// A single-line text input — M3 OutlinedTextField on the paper surface, ledger-toned.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LedgerTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        placeholder = { Text(placeholder, fontFamily = Plex, fontSize = fBodyL, color = cDim) },
        textStyle = TextStyle(fontFamily = Plex, fontSize = fBodyL, color = cInk),
        singleLine = true,
        shape = RoundedCornerShape(rSm),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = cAccent,
            unfocusedBorderColor = cLine,
            focusedContainerColor = cBg,
            unfocusedContainerColor = cBg,
            cursorColor = cInk,
        ),
    )
}
