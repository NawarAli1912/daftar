package com.daftar.app.store

import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daftar.app.kernel.theme.Amiri
import com.daftar.app.kernel.theme.Plex

@Composable
internal fun StoreSheets(st: StoreState, vm: StoreViewModel) {
    // button-opened editors — centered pop-up cards (scale + fade), via OverlaySlot
    OverlaySlot(st.screen.takeIf { it == "pay" }) { PaySheet(st, vm) }
    OverlaySlot(st.screen.takeIf { it == "sale" }) { SaleSheet(st, vm) }
    OverlaySlot(st.screen.takeIf { it == "return" }) { ReturnSheet(st, vm) }
    OverlaySlot(st.screen.takeIf { it == "withdraw" }) { WithdrawSheet(st, vm) }
    OverlaySlot(st.screen.takeIf { it == "additem" }) { AddItemSheet(st, vm) }
    // إدارة بالة / shop are shared-element morphs (open from a source card) — PackageDetailShared
    // and ShopDetailShared in StoreApp.
    // bottom sheets — expand from the tapped element and collapse back into it (OverlaySlot)
    OverlaySlot(st.screen.takeIf { it == "chooser" }) { Chooser(vm) }
    OverlaySlot(st.screen.takeIf { it == "addsrc" }) { AddSourceSheet(st, vm) }
    OverlaySlot(st.custPickerOpen.takeIf { it }) { CustPicker(st, vm) }
    OverlaySlot(st.custAddOpen.takeIf { it }) { AddCustomerSheet(st, vm) }
    // صنف / قيد / زبونة details are shared-element container transforms — rendered by
    // *DetailShared in StoreApp (they need the SharedTransitionLayout scope), not here.
    OverlaySlot(st.specifyId) { SpecifySheet(st, vm) } // layers on top of the sale detail
    OverlaySlot(st.maintOpen.takeIf { it }) { MaintSheet(vm) }
    OverlaySlot(st.paperDebtPrompt.takeIf { it }) { PaperDebtSheet(st, vm) }
    OverlaySlot(st.confirm) { ConfirmSheet(st, vm) }
    // NOTE: the undo toast is rendered inline in StoreApp's Column (F2) — not a full-screen
    // overlay — so it never covers «+ قيد جديد» or the tab bar, and it can be swiped away.
}

// ── F3 paper-debt catch — a دفعة overshoots her balance; record her old paper debt first ──
@Composable
private fun PaperDebtSheet(st: StoreState, vm: StoreViewModel) {
    val cust = st.saleCustomerId?.let { id -> st.customers.find { it.id == id } }
    BottomSheet(onDismiss = vm::closePaperDebt) {
        Text("عليها دين قديم؟", fontSize = fHead, fontWeight = FontWeight.Bold, color = cInk, modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 8.dp))
        Text(
            "الدفعة أكبر من رصيد ${cust?.name ?: "الزبونة"} المسجّل. غالباً لأن دينها القديم من الدفتر الورقي لم يُسجَّل بعد.",
            fontSize = fBody, color = cDim, lineHeight = 20.sp, modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 14.dp),
        )
        Row(
            Modifier.fillMaxWidth().card(rMd).padding(horizontal = 13.dp, vertical = 11.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("دين قديم من الدفتر الورقي", fontSize = fBody, fontWeight = FontWeight.SemiBold, color = cDebt, modifier = Modifier.weight(1f, fill = false).padding(end = 8.dp))
            LabeledStepper("", fmt(st.paperDebtAmount), { vm.paperDebtStep(-1) }, { vm.paperDebtStep(1) }, valueMin = 64.dp, valueSize = 18.sp, borderColor = cUnspecBorder, btnColor = cDebt, raw = st.paperDebtAmount, onType = vm::setPaperDebtAmount)
        }
        Spacer(Modifier.height(12.dp))
        PrimaryButton("سجّلي الدين القديم ثم الدفعة ✓", fontSize = fBodyL, radius = rMd, vertical = 13.dp) { vm.confirmPaperDebt() }
        Box(Modifier.fillMaxWidth().padding(top = 12.dp).tap { vm.declinePaperDebt() }, contentAlignment = Alignment.Center) {
            Text("لا، هي لها — الرصيد يصبح لها", fontSize = fBody, fontWeight = FontWeight.Bold, color = cDim)
        }
    }
}

// ── maintainer tools — reachable only via the hidden long-press on the دفتر wordmark ──
@Composable
private fun MaintSheet(vm: StoreViewModel) {
    BottomSheet(onDismiss = vm::closeMaint) {
        Text("أدوات نوّار — للصيانة", fontSize = fTitle, fontWeight = FontWeight.Bold, color = cInk, modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 12.dp))
        MaintContent(vm)
    }
}

// A confirm-before-you-wipe sheet for the two destructive actions behind the maintainer sheet.
@Composable
private fun ConfirmSheet(st: StoreState, vm: StoreViewModel) {
    val sample = st.confirm == "sample"
    val title = if (sample) "تحميل بيانات تجريبية؟" else "مسح كل شيء؟"
    val body = if (sample) "سيُستبدَل كل ما سجّلتِه ببيانات للتجربة فقط. لا يمكن التراجع."
    else "سيُمحى الدفتر والبضاعة والزبائن نهائياً وتعودين للبداية. لا يمكن التراجع."
    val cta = if (sample) "استبدال بالتجريبية" else "نعم، امسحي الكل"
    BottomSheet(onDismiss = vm::dismissConfirm) {
        Text(title, fontSize = fHead, fontWeight = FontWeight.Bold, color = cInk, modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 8.dp))
        Text(body, fontSize = fBody, color = cDim, lineHeight = 19.sp, modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 16.dp))
        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(rMd)).background(cDebt).tap { if (sample) vm.loadSample() else vm.resetApp() }.padding(vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) { Text(cta, fontSize = fTitle, fontWeight = FontWeight.Bold, color = cAink) }
        Spacer(Modifier.height(9.dp))
        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(rMd)).background(cCard).border(1.5.dp, cAccent, RoundedCornerShape(rMd)).tap { vm.dismissConfirm() }.padding(vertical = 13.dp),
            contentAlignment = Alignment.Center,
        ) { Text("تراجع", fontSize = fTitle, fontWeight = FontWeight.Bold, color = cAccent) }
    }
}

// ── overlay presence: expand-from-item, collapse-back-to-it ──
// The shared transition state that drives a sheet's enter AND exit. `OverlaySlot` provides it
// and keeps the sheet composed through the collapse, so dismissing returns the sheet to its
// place instead of making it vanish.
internal val LocalSheetTransition = androidx.compose.runtime.compositionLocalOf<MutableTransitionState<Boolean>?> { null }

@Composable
private fun OverlaySlot(active: Any?, content: @Composable () -> Unit) {
    val state = remember { MutableTransitionState(false) }
    state.targetState = active != null
    // stay composed while entering, shown, or collapsing — leave only once fully hidden
    if (state.currentState || state.targetState) {
        androidx.compose.runtime.CompositionLocalProvider(LocalSheetTransition provides state) { content() }
    }
}

// Retains the last non-null value so a sheet keeps its content during the collapse, after the
// triggering state has already been nulled.
@Composable
private fun <T> rememberLast(value: T?): T? {
    val holder = remember { mutableStateOf(value) }
    if (value != null) holder.value = value
    return holder.value
}

// ── shared sheet chrome ──
// Button-opened sheets have no row to grow from, so they present as a centered pop-up card
// that scales + fades in and reverses out — the same visual language as the shared-element
// morph cards (rounded rLg card, scrim), just from the centre. Symmetric enter/exit driven by
// LocalSheetTransition (OverlaySlot keeps it composed through the collapse).
@Composable
private fun BottomSheet(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val state = LocalSheetTransition.current
    val p = if (state != null) {
        val transition = androidx.compose.animation.core.rememberTransition(state, label = "popup")
        val v by transition.animateFloat(
            transitionSpec = { spring(dampingRatio = 0.8f, stiffness = 320f) }, label = "p",
        ) { if (it) 1f else 0f }
        v
    } else 1f
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier.fillMaxSize().graphicsLayer { alpha = p.coerceIn(0f, 1f) }.background(cScrim)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
        )
        BoxWithConstraints(Modifier.fillMaxSize().imePadding().padding(12.dp), contentAlignment = Alignment.Center) {
            val maxH = maxHeight * 0.86f
            Column(
                Modifier.fillMaxWidth().heightIn(max = maxH)
                    .graphicsLayer {
                        val s = 0.9f + 0.1f * p
                        scaleX = s; scaleY = s
                        alpha = (p * 1.4f).coerceIn(0f, 1f)
                    }
                    .clip(RoundedCornerShape(rLg)).background(cBg)
                    .navigationBarsPadding().verticalScroll(rememberScrollState()).padding(20.dp),
                content = content,
            )
        }
    }
}

// A form pop-up: same centered scale+fade card, but with a fixed header (title + ✕) and a
// pinned footer save button, and a scrollable middle. For the button-opened editors
// (بيع / دفعة / إرجاع / إضافة صنف) — one language with the simple pop-ups above.
@Composable
private fun PopupEditor(
    title: String,
    onClose: () -> Unit,
    footerLabel: String,
    onSave: () -> Unit,
    back: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val state = LocalSheetTransition.current
    val p = if (state != null) {
        val transition = androidx.compose.animation.core.rememberTransition(state, label = "popup")
        val v by transition.animateFloat(
            transitionSpec = { spring(dampingRatio = 0.8f, stiffness = 320f) }, label = "p",
        ) { if (it) 1f else 0f }
        v
    } else 1f
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier.fillMaxSize().graphicsLayer { alpha = p.coerceIn(0f, 1f) }.background(cScrim)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClose),
        )
        BoxWithConstraints(Modifier.fillMaxSize().imePadding().padding(12.dp), contentAlignment = Alignment.Center) {
            val maxH = maxHeight * 0.9f
            Column(
                Modifier.fillMaxWidth().heightIn(max = maxH)
                    .graphicsLayer { val s = 0.9f + 0.1f * p; scaleX = s; scaleY = s; alpha = (p * 1.4f).coerceIn(0f, 1f) }
                    .clip(RoundedCornerShape(rLg)).background(cBg).navigationBarsPadding(),
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(start = 18.dp, end = 16.dp, top = 16.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (back != null) Text("‹", fontSize = 24.sp, color = cDim, modifier = Modifier.tap(back).padding(horizontal = 10.dp, vertical = 4.dp)) else Spacer(Modifier.width(1.dp))
                    Text(title, fontSize = fHead, fontWeight = FontWeight.Bold, color = cInk)
                    Text("✕", fontSize = 22.sp, color = cDim, modifier = Modifier.tap(onClose).padding(horizontal = 10.dp, vertical = 4.dp))
                }
                HorizontalDivider(color = cLine, thickness = 1.dp)
                Column(
                    Modifier.weight(1f, fill = false).fillMaxWidth().verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 15.dp),
                    content = content,
                )
                HorizontalDivider(color = cLine, thickness = 1.dp)
                Box(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 14.dp)) {
                    PrimaryButton(footerLabel, fontSize = fHead) { onSave() }
                }
            }
        }
    }
}

@Composable
private fun TextInput(value: String, onValueChange: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier, bg: Color = cBg, radius: androidx.compose.ui.unit.Dp = rSm, fontSize: androidx.compose.ui.unit.TextUnit = fBodyL) {
    BasicTextField(
        value = value, onValueChange = onValueChange,
        modifier = modifier.clip(RoundedCornerShape(radius)).background(bg).border(1.dp, cLine, RoundedCornerShape(radius)).padding(horizontal = 13.dp, vertical = 13.dp),
        textStyle = TextStyle(fontFamily = Plex, fontSize = fontSize, color = cInk),
        cursorBrush = SolidColor(cInk), singleLine = true,
        decorationBox = { inner ->
            Box {
                if (value.isEmpty()) Text(placeholder, fontSize = fontSize, color = cDim)
                inner()
            }
        },
    )
}

// A stepper block: label on the start, [− value +] on the end (used in many sheets).
// Pass raw + onType for MONEY values: the number itself becomes typeable (numeric keyboard),
// with the steppers kept for quick ±500 nudges. Counts stay display-only.
@Composable
private fun LabeledStepper(
    label: String, value: String, onMinus: () -> Unit, onPlus: () -> Unit,
    btnSize: androidx.compose.ui.unit.Dp = tapMd, valueMin: androidx.compose.ui.unit.Dp = 60.dp,
    valueSize: androidx.compose.ui.unit.TextUnit = 19.sp, btnFont: androidx.compose.ui.unit.TextUnit = 20.sp,
    borderColor: Color = cLine, btnColor: Color = cAccent,
    raw: Long? = null, onType: ((Long) -> Unit)? = null,
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
        StepBtn("−", btnSize, 10.dp, 1.5.dp, borderColor, btnColor, btnFont, onMinus)
        if (raw != null && onType != null) MoneyValue(raw, onType, valueSize, valueMin)
        else Text(value, fontSize = valueSize, fontWeight = FontWeight.Bold, color = cInk, textAlign = TextAlign.Center, modifier = Modifier.widthIn(min = valueMin))
        StepBtn("+", btnSize, 10.dp, 1.5.dp, borderColor, btnColor, btnFont, onPlus)
    }
    label.let {}
}

@Composable
private fun CardStepperRow(label: String, value: String, onMinus: () -> Unit, onPlus: () -> Unit, borderColor: Color = cLine, btnColor: Color = cAccent, labelColor: Color = cDim, bg: Color = cCard, raw: Long? = null, onType: ((Long) -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(rMd)).background(bg).border(1.dp, if (bg == cCard) cLine else borderColor, RoundedCornerShape(rMd)).padding(horizontal = 13.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
    ) {
        // the label yields (wraps) so the stepper cluster always keeps its full size — a long
        // label was silently squeezing the last StepBtn («+» in RTL) smaller than its twin
        Text(label, fontSize = fBody, fontWeight = FontWeight.SemiBold, color = labelColor, modifier = Modifier.weight(1f, fill = false).padding(end = 8.dp))
        LabeledStepper("", value, onMinus, onPlus, borderColor = borderColor, btnColor = btnColor, raw = raw, onType = onType)
    }
}

@Composable
private fun SnoozeChip(label: String, onClick: () -> Unit) {
    Box(
        Modifier.clip(RoundedCornerShape(rXs)).background(cCard).border(1.dp, cLine, RoundedCornerShape(rXs)).tap(onClick).padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(label, fontSize = fSmall, fontWeight = FontWeight.Bold, color = cAccent)
    }
}

@Composable
private fun CustomerRow(st: StoreState, vm: StoreViewModel) {
    val cust = st.saleCustomerId?.let { id -> st.customers.find { it.id == id } }
    Row(
        Modifier.fillMaxWidth().card(rMd).tap { vm.openCustPicker() }.padding(horizontal = 13.dp, vertical = 11.dp),
        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("الزبونة", fontSize = fSmall, fontWeight = FontWeight.SemiBold, color = cDim)
        Text(
            (cust?.name ?: "بدون اسم — نقدي") + " ›",
            fontSize = fBodyL, fontWeight = FontWeight.Bold, color = if (cust != null) cDebt else cInk,
        )
    }
}

@Composable
private fun CustPicker(st: StoreState, vm: StoreViewModel) {
    BottomSheet(onDismiss = vm::closeCustPicker) {
        Text("لِمَن هذه العملية؟", fontSize = fTitle, fontWeight = FontWeight.Bold, color = cInk, modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 12.dp))
        // نقدي — no customer
        Row(
            Modifier.fillMaxWidth().padding(bottom = 8.dp).card(rMd).tap { vm.pickCustomer(null) }.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("نقدي — بدون اسم", fontSize = fTitle, fontWeight = FontWeight.Bold, color = cInk)
            if (st.saleCustomerId == null) Text("✓", fontSize = fTitle, fontWeight = FontWeight.Bold, color = cPaid)
        }
        // debtors first — a دفعة is almost always one of them paying her tab (v2)
        st.customers.sortedByDescending { customerBalance(it, st.entries) }.forEach { c ->
            val bal = customerBalance(c, st.entries)
            Row(
                Modifier.fillMaxWidth().padding(bottom = 8.dp).card(rMd).tap { vm.pickCustomer(c.id) }.padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(c.name, fontSize = fTitle, fontWeight = FontWeight.Bold, color = cInk)
                Text(
                    if (bal > 0) "عليها ${fmt(bal)}" else "لا دين",
                    fontSize = fSmall, fontWeight = FontWeight.SemiBold, color = if (bal > 0) cDebt else cPaid,
                )
            }
        }
        if (!st.custNewOpen) {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(rMd)).background(cCard).dashedBorder(cLine, rMd).tap { vm.toggleCustNew() }.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("+ زبونة جديدة", fontSize = fBodyL, fontWeight = FontWeight.Bold, color = cAccent)
            }
        } else {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(rMd)).background(cCard).border(1.5.dp, cAccent, RoundedCornerShape(rMd)).padding(horizontal = 13.dp, vertical = 12.dp)) {
                TextInput(st.custNewName, vm::setCustNewName, "الاسم", modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                TextInput(st.custNewPhone, vm::setCustNewPhone, "الهاتف (اختياري)", modifier = Modifier.fillMaxWidth())
                Row(Modifier.fillMaxWidth().padding(top = 9.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("دين قديم (اختياري)", fontSize = fSmall, fontWeight = FontWeight.SemiBold, color = cDim, modifier = Modifier.weight(1f, fill = false).padding(end = 8.dp))
                    LabeledStepper("", fmt(st.custNewDebt), { vm.custNewDebtStep(-1) }, { vm.custNewDebtStep(1) }, valueMin = 60.dp, raw = st.custNewDebt, onType = vm::setCustNewDebt)
                }
                Spacer(Modifier.height(11.dp))
                PrimaryButton("أضيفي الزبونة ✓", fontSize = fBodyL, radius = rSm, vertical = 11.dp) { vm.addCustomer() }
            }
        }
    }
}

// ── add a customer to the directory (الزبائن) — not a sale picker ──
@Composable
private fun AddCustomerSheet(st: StoreState, vm: StoreViewModel) {
    BottomSheet(onDismiss = vm::closeAddCustomer) {
        Text("زبونة جديدة", fontSize = fHead, fontWeight = FontWeight.Bold, color = cInk, modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 4.dp))
        Text("تُضاف إلى دفتر الزبائن — سجّلي ديونها ودفعاتها لاحقاً.", fontSize = fSmall, color = cDim, lineHeight = 18.sp, modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 14.dp))
        TextInput(st.custNewName, vm::setCustNewName, "الاسم", modifier = Modifier.fillMaxWidth(), bg = cCard, radius = rMd, fontSize = fTitle)
        Spacer(Modifier.height(9.dp))
        TextInput(st.custNewPhone, vm::setCustNewPhone, "الهاتف (اختياري)", modifier = Modifier.fillMaxWidth(), bg = cCard, radius = rMd, fontSize = fTitle)
        Row(
            Modifier.fillMaxWidth().padding(top = 11.dp).card(rMd).padding(horizontal = 13.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f, fill = false).padding(end = 8.dp)) {
                Text("دين قديم (اختياري)", fontSize = fBody, fontWeight = FontWeight.SemiBold, color = cInk)
                Text("رصيد سابق من الدفتر الورقي", fontSize = fCaption, color = cDim)
            }
            LabeledStepper("", fmt(st.custNewDebt), { vm.custNewDebtStep(-1) }, { vm.custNewDebtStep(1) }, valueMin = 64.dp, raw = st.custNewDebt, onType = vm::setCustNewDebt)
        }
        Spacer(Modifier.height(14.dp))
        PrimaryButton("أضيفي الزبونة ✓", fontSize = fTitle, radius = rMd, vertical = 14.dp) { vm.saveNewCustomer() }
    }
}

// ── one-sheet item editing (v2): tap an item, control everything incl. its source ──
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ColumnScope.ItemEditBody(st: StoreState, vm: StoreViewModel) {
    run {
        Text("صفحة الصنف", fontSize = fHead, fontWeight = FontWeight.Bold, color = cInk, modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 12.dp))
        // F1: the item's own record — what it sold, for how much vs its تسعيرة, and (when a
        // cost basis exists) its profit. Derived from the ledger; the edit controls follow.
        val item = st.shelf.find { it.id == rememberLast(st.editItemId) }
        if (item != null) {
            val stats = itemStats(item, st.sources, st.shelf, st.entries, st.usdRate, st.expenses)
            Column(Modifier.fillMaxWidth().card().padding(horizontal = 14.dp, vertical = 13.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    BaleStat("بيعت", "${stats.soldPieces}", cPaid)
                    BaleStat("الإيراد", fmt(stats.revenue), cInk)
                    BaleStat("متوسط البيع", stats.avgSellPrice?.let { fmt(it) } ?: "—", cInk)
                }
                stats.avgSellPrice?.let { avg ->
                    val diff = avg - item.tasira
                    val txt = when {
                        diff == 0L -> "بالتسعيرة تماماً (${fmt(item.tasira)})"
                        diff > 0 -> "أعلى من التسعيرة بـ${fmt(diff)}"
                        else -> "أقل من التسعيرة بـ${fmt(-diff)} (مساومة)"
                    }
                    Text(txt, fontSize = fCaption, color = if (diff < 0) cAmber else cPaid, modifier = Modifier.padding(top = 9.dp))
                }
                Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    BaleStat("آخر بيع", stats.lastSoldDay?.let { dayLabel(it, st.today) } ?: "—", cDim)
                    if (stats.profit != null) {
                        BaleStat(
                            "ربح تقريباً",
                            (if (stats.profit >= 0) "+ " else "− ") + fmt(kotlin.math.abs(stats.profit)),
                            if (stats.profit >= 0) cPaid else cDebt, bold = true,
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
        TextInput(st.eiName, vm::setEiName, "اسم الصنف", modifier = Modifier.fillMaxWidth(), bg = cCard, radius = rMd, fontSize = fTitle)
        Spacer(Modifier.height(9.dp))
        CardStepperRow("التسعيرة", fmt(st.eiTasira), { vm.eiTasiraStep(-1) }, { vm.eiTasiraStep(1) }, raw = st.eiTasira, onType = vm::setEiTasira)
        Spacer(Modifier.height(8.dp))
        CardStepperRow("في المحل الآن", "${st.eiOnHand}", { vm.eiOnHandStep(-1) }, { vm.eiOnHandStep(1) }, raw = st.eiOnHand.toLong(), onType = vm::setEiOnHand)
        // ITEM 6: a bale-sourced item shows its share of the bale's inclusive cost (USD×frozen
        // rate + expenses ÷ counted pieces), read-only — «—» when incomputable. The old editable
        // «سعر الشراء للقطعة» stepper was market-only and is gone with شراء من السوق.
        if (item != null && st.sources.find { it.id == item.sourceId }?.kind == Kind.BALE) {
            val perPiece = itemStats(item, st.sources, st.shelf, st.entries, st.usdRate, st.expenses).perPieceCost
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(rMd)).background(cBg).border(1.dp, cLine, RoundedCornerShape(rMd)).padding(horizontal = 13.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("سعر الشراء للقطعة تقريباً", fontSize = fBody, fontWeight = FontWeight.SemiBold, color = cDim, modifier = Modifier.weight(1f, fill = false).padding(end = 8.dp))
                Text(perPiece?.let { fmt(it) } ?: "—", fontSize = fBodyL, fontWeight = FontWeight.Bold, color = cInk)
            }
        }
        // V3 merge: one «بضاعة قديمة» chip (maps to PRE_ID) + بالات only — شراء من السوق hidden.
        Text("من أين أتى؟ — وجّهيه لبالته الصحيحة إن عرفتِها", fontSize = fSmall, fontWeight = FontWeight.SemiBold, color = cDim, modifier = Modifier.padding(start = 4.dp, top = 14.dp, bottom = 8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val oldStockSel = st.eiSource == null || st.eiSource == PRE_ID
            SourcePickChip("بضاعة قديمة", oldStockSel, false) { vm.eiPickSource(PRE_ID) }
            st.sources.filter { it.kind == Kind.BALE }.forEach { srcOpt ->
                SourcePickChip(srcOpt.label, st.eiSource == srcOpt.id, false) { vm.eiPickSource(srcOpt.id) }
            }
        }
        Spacer(Modifier.height(14.dp))
        PrimaryButton("حفظ التعديل ✓", fontSize = fTitle, radius = rMd, vertical = 14.dp) { vm.saveEditItem() }
    }
}

// ── chooser ──
@Composable
private fun Chooser(vm: StoreViewModel) {
    BottomSheet(onDismiss = vm::closeSheet) {
        Text("ماذا تسجّلين؟", fontSize = fHead, fontWeight = FontWeight.Bold, color = cInk, modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 12.dp))
        ChooserOption("🧾", "بيع", "أصناف من المحل بأسعار", outlined = true) { vm.openSale() }
        Spacer(Modifier.height(10.dp))
        ChooserOption("💵", "دفعة", "مبلغ على الرصيد") { vm.openPay() }
        Spacer(Modifier.height(10.dp))
        ChooserOption("↩️", "إرجاع", "قيمة تُعاد للرصيد") { vm.openReturn() }
        Spacer(Modifier.height(10.dp))
        ChooserOption("🎁", "أخذت لنفسي / هدية", "قطع تخرج من المحل بلا مبلغ") { vm.openWithdraw() }
    }
}

// ── return (إرجاع) ──
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReturnSheet(st: StoreState, vm: StoreViewModel) {
    PopupEditor("إرجاع جديد", onClose = vm::closeSheet, footerLabel = "حفظ الإرجاع ✓", onSave = vm::saveReturn) {
        run {
            CustomerRow(st, vm)
            Spacer(Modifier.height(14.dp))
            Text("قيمة الإرجاع", fontSize = fSmall, fontWeight = FontWeight.SemiBold, color = cDim, modifier = Modifier.padding(start = 2.dp, bottom = 8.dp))
            Row(
                Modifier.fillMaxWidth().card(rLg).padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically,
            ) {
                StepBtn("−", tapLg, 14.dp, 1.5.dp, cLine, cAccent, 24.sp) { vm.returnAmountStep(-1) }
                MoneyValue(st.returnAmount, vm::setReturnAmount, 30.sp, 120.dp)
                StepBtn("+", tapLg, 14.dp, 1.5.dp, cLine, cAccent, 24.sp) { vm.returnAmountStep(1) }
            }
            // suggest what SHE actually took (from her own قيود) when a customer is picked; fall
            // back to الرف for a nameless إرجاع. Picking pre-fills the price she was recorded at.
            val taken = customerTakenLines(st.saleCustomerId, st.entries)
            Text(
                if (taken.isNotEmpty()) "الصنف المُعاد (اختياري) — ممّا أخذته، يعود إلى الرف" else "الصنف المُعاد (اختياري) — يعود إلى الرف",
                fontSize = fSmall, fontWeight = FontWeight.SemiBold, color = cDim, modifier = Modifier.padding(start = 2.dp, top = 16.dp, bottom = 8.dp),
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (taken.isNotEmpty()) {
                    taken.forEach { l ->
                        val sel = st.returnItemId == l.shelfId
                        Column(
                            Modifier.clip(RoundedCornerShape(rSm)).background(if (sel) cAccent else cCard).border(1.5.dp, if (sel) cAccent else cLine, RoundedCornerShape(rSm)).tap { vm.returnPickTaken(l.shelfId, l.price) }.padding(horizontal = 13.dp, vertical = 9.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(l.name, fontSize = fBodyL, fontWeight = FontWeight.Bold, color = if (sel) cAink else cInk)
                            Text(fmt(l.price), fontSize = fCaption, color = if (sel) cAink else cDim)
                        }
                    }
                } else {
                    st.shelf.take(10).forEach { x ->
                        val sel = st.returnItemId == x.id
                        Column(
                            Modifier.clip(RoundedCornerShape(rSm)).background(if (sel) cAccent else cCard).border(1.5.dp, if (sel) cAccent else cLine, RoundedCornerShape(rSm)).tap { vm.returnPickItem(x.id) }.padding(horizontal = 13.dp, vertical = 9.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(x.name, fontSize = fBodyL, fontWeight = FontWeight.Bold, color = if (sel) cAink else cInk)
                            Text(fmt(x.tasira), fontSize = fCaption, color = if (sel) cAink else cDim)
                        }
                    }
                }
            }
            Box(Modifier.fillMaxWidth().padding(top = 12.dp).clip(RoundedCornerShape(rMd)).background(cGreenBg).border(1.dp, cGreenBorder, RoundedCornerShape(rMd)).padding(horizontal = 12.dp, vertical = 10.dp)) {
                Text("تُخصم القيمة من دين الزبونة" + (if (st.returnItemId != null) " ويعود الصنف إلى الرف." else "."), fontSize = fSmall, color = cPaid, lineHeight = 18.sp)
            }
        }
    }
}

// ── entry detail: view & void a past قيد ──
// The قيد detail content — rendered inside the shared-element container transform (the
// day-book row morphs into this card). `e` is resolved by the caller so it survives the
// collapse animation. Kept as a ColumnScope body so the shared-bounds card supplies the Column.
@Composable
internal fun ColumnScope.EntryDetailBody(st: StoreState, vm: StoreViewModel, e: DayEntry) {
    val amtColor = when (e.cls) { "pos" -> cPaid; "amber" -> cAmber; "neg" -> cDebt; "withdraw" -> cDim; else -> cInk }
    run {
        Text("القيد", fontSize = fTitle, fontWeight = FontWeight.Bold, color = cInk, modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 12.dp))
        Column(Modifier.fillMaxWidth().card(rMd).padding(horizontal = 14.dp, vertical = 12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Text(e.t, fontSize = fTitle, fontWeight = FontWeight.SemiBold, color = cInk, modifier = Modifier.weight(1f, fill = false))
                Spacer(Modifier.width(10.dp))
                Text(e.amt, fontSize = fHead, fontWeight = FontWeight.Bold, color = amtColor)
            }
            Text(e.d, fontSize = fSmall, color = cDim, modifier = Modifier.padding(top = 4.dp))
        }

        // the sold items, each with a source chip to attribute where it came from
        val soldLines = decodeLines(e.lines)
        if (soldLines.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            Text("الأصناف المُباعة — اضغطي المصدر لتحديد من أين أتت", fontSize = fSmall, fontWeight = FontWeight.SemiBold, color = cDim, modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 8.dp))
            Column(Modifier.fillMaxWidth().card().padding(horizontal = 14.dp)) {
                soldLines.forEach { l ->
                    val item = st.shelf.find { it.id == l.shelfId }
                    val unspec = item == null || item.unspecified
                    val srcLabel = if (item != null && !item.unspecified) vm.sourceLabelFor(item.sourceId) else "لا أعلم"
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 12.dp).drawBottomLine(),
                        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f, fill = false)) {
                            Text(l.name, fontSize = fBodyL, fontWeight = FontWeight.Bold, color = cInk)
                            Text("${fmt(l.price)} · ×${l.qty}", fontSize = fCaption, color = cDim)
                        }
                        Spacer(Modifier.width(8.dp))
                        Row(
                            Modifier.clip(RoundedCornerShape(rXs)).background(cBg).border(1.dp, if (unspec) cUnspecBorder else cLine, RoundedCornerShape(rXs))
                                .then(if (item != null) Modifier.tap { vm.openSpecify(l.shelfId) } else Modifier)
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            if (unspec) Box(Modifier.size(7.dp).clip(RoundedCornerShape(50)).background(cDebt))
                            Text(if (item != null) "$srcLabel ✎" else srcLabel, fontSize = fSmall, fontWeight = FontWeight.Bold, color = if (unspec) cDebt else cDim)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        if (e.voided) {
            // D71: a voided قيد is kept — offer to bring it back, nothing is destroyed.
            PrimaryButton("استرجاع القيد ↻", fontSize = fTitle, radius = rMd, vertical = 14.dp) { vm.restoreEntry(e.id) }
            Text("هذا القيد ملغى ولا يُحسب. «استرجاع» يعيده كما كان.", fontSize = fCaption, color = cDim, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 10.dp))
        } else if (e.trialAmount > 0 && openTrialEntries(e.customerId, st.entries).any { it.id == e.id }) {
            // F2: a تجريب ends one of two ways — she kept it (→ بيع بالدَّين) or returned it
            // (→ للمحل). Show exactly those, mirroring the زبونة card; no generic تعديل/حذف, whose
            // meaning was ambiguous for a trial and read as a duplicate delete.
            Text("تجريب — كيف انتهى؟", fontSize = fSmall, fontWeight = FontWeight.SemiBold, color = cAmber, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                Box(
                    Modifier.weight(1f).clip(RoundedCornerShape(rMd)).background(cCard).border(1.5.dp, cAmber, RoundedCornerShape(rMd)).tap { vm.convertTrialEntry(e.id) }.padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center,
                ) { Text("أبقتها — بيع", fontSize = fTitle, fontWeight = FontWeight.Bold, color = cAmber) }
                Box(
                    Modifier.weight(1f).clip(RoundedCornerShape(rMd)).background(cCard).border(1.5.dp, cAccent, RoundedCornerShape(rMd)).tap { vm.voidEntry(e.id) }.padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center,
                ) { Text("أعادتها — للمحل", fontSize = fTitle, fontWeight = FontWeight.Bold, color = cAccent) }
            }
            Text("«أبقتها» تحوّلها إلى بيع بالدَّين، و«أعادتها» تعيد القطع إلى المحل — وكلاهما قابل للاسترجاع.", fontSize = fCaption, color = cDim, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 10.dp))
        } else if (e.trialAmount > 0) {
            // already resolved by a keep-&-pay قيد (it nets this تجريب) — read-only, so it isn't
            // kept or returned twice. To undo, she deletes the «تحويل تجريب ودفعة» قيد.
            Text("هذا التجريب حُسم بدفعة وأصبح بيعاً. لإلغائه احذفي قيد «تحويل تجريب ودفعة».", fontSize = fBody, color = cDim, lineHeight = 20.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp, horizontal = 4.dp))
        } else if (e.cls == "withdraw") {
            // F6: a withdrawal is void-and-redo (there's no money to edit); «حذف» puts the pieces
            // back in the shop and is reversible.
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(rMd)).background(cCard).border(1.5.dp, cDebt, RoundedCornerShape(rMd)).tap { vm.voidEntry(e.id) }.padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) { Text("حذف — تُعاد للمحل ↺", fontSize = fTitle, fontWeight = FontWeight.Bold, color = cDebt) }
            Text("سحب للاستعمال الشخصي أو هدية. «حذف» يشطبه ويعيد القطع إلى المحل، ويمكن استرجاعه لاحقاً.", fontSize = fCaption, color = cDim, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 10.dp))
        } else {
            // supplier payments (D68) are void-and-redo, not editable — hide تعديل for them
            val editable = e.moneyOut == 0L && (soldLines.isNotEmpty() || e.t.startsWith("دفعة") || e.t.startsWith("إرجاع"))
            if (editable) {
                PrimaryButton("تعديل القيد ✎", fontSize = fTitle, radius = rMd, vertical = 14.dp) { vm.editEntry(e.id) }
                Spacer(Modifier.height(9.dp))
            }
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(rMd)).background(cCard).border(1.5.dp, cDebt, RoundedCornerShape(rMd)).tap { vm.voidEntry(e.id) }.padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("حذف هذا القيد ↺", fontSize = fTitle, fontWeight = FontWeight.Bold, color = cDebt)
            }
            Text("«تعديل» يفتح القيد لتصحيحه، و«حذف» يشطبه ويوقف حسابه — ويمكن استرجاعه لاحقاً.", fontSize = fCaption, color = cDim, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 10.dp))
        }
    }
}

// ── customer detail: balance + history + record a payment (shared-element card body) ──
@Composable
internal fun ColumnScope.CustomerDetailBody(st: StoreState, vm: StoreViewModel, c: Customer) {
    val bal = customerBalance(c, st.entries)
    val trial = customerTrial(c, st.entries)
    val history = st.entries.filter { it.customerId == c.id }
    run {
        if (st.custEditId == c.id) {
            // v2: every record editable forever — name, phone, and the opening دين قديم.
            Text("تعديل ${c.name}", fontSize = fHead, fontWeight = FontWeight.Bold, color = cInk, modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 12.dp))
            TextInput(st.custNewName, vm::setCustNewName, "الاسم", modifier = Modifier.fillMaxWidth(), bg = cCard, radius = rMd, fontSize = fTitle)
            Spacer(Modifier.height(9.dp))
            TextInput(st.custNewPhone, vm::setCustNewPhone, "الهاتف (اختياري)", modifier = Modifier.fillMaxWidth(), bg = cCard, radius = rMd, fontSize = fTitle)
            Row(
                Modifier.fillMaxWidth().padding(top = 11.dp).card(rMd).padding(horizontal = 13.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f, fill = false).padding(end = 8.dp)) {
                    Text("دين قديم", fontSize = fBody, fontWeight = FontWeight.SemiBold, color = cInk)
                    Text("تعديله يعدّل رصيدها مباشرةً", fontSize = fCaption, color = cDim)
                }
                LabeledStepper("", fmt(st.custNewDebt), { vm.custNewDebtStep(-1) }, { vm.custNewDebtStep(1) }, valueMin = 64.dp, raw = st.custNewDebt, onType = vm::setCustNewDebt)
            }
            Spacer(Modifier.height(14.dp))
            PrimaryButton("حفظ التعديل ✓", fontSize = fTitle, radius = rMd, vertical = 14.dp) { vm.saveEditCustomer() }
            Spacer(Modifier.height(9.dp))
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(rMd)).background(cCard).border(1.5.dp, cAccent, RoundedCornerShape(rMd)).tap { vm.cancelEditCustomer() }.padding(vertical = 13.dp),
                contentAlignment = Alignment.Center,
            ) { Text("تراجع", fontSize = fTitle, fontWeight = FontWeight.Bold, color = cAccent) }
            return@run
        }
        Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, bottom = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(c.name, fontSize = fHead, fontWeight = FontWeight.Bold, color = cInk)
                Box(Modifier.clip(RoundedCornerShape(rXs)).background(cBg).border(1.dp, cLine, RoundedCornerShape(rXs)).tap { vm.startEditCustomer(c.id) }.padding(horizontal = 9.dp, vertical = 3.dp)) {
                    Text("✎ تعديل", fontSize = fCaption, fontWeight = FontWeight.Bold, color = cAccent)
                }
            }
            Text(
                if (bal > 0) "عليها ${fmt(bal)}" else if (bal == 0L) "لا دين" else "لها ${fmt(-bal)}",
                fontSize = fBodyL, fontWeight = FontWeight.Bold, color = if (bal > 0) cDebt else cPaid,
            )
        }
        // تجريب معها — each open trial قيد resolves on its own (v2): kept → sale, or back to the
        // shelf. A trial she already kept via a دفعة is netted out and no longer listed here.
        val trialEntries = openTrialEntries(c.id, st.entries)
        if (trialEntries.isNotEmpty()) {
            Text("تجريب معها (قد يُعاد): ${fmt(trial)}", fontSize = fSmall, fontWeight = FontWeight.SemiBold, color = cAmber, modifier = Modifier.padding(start = 4.dp, bottom = 6.dp))
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(rSm)).background(cCard).border(1.dp, cAmber, RoundedCornerShape(rSm)).padding(horizontal = 13.dp, vertical = 4.dp)) {
                trialEntries.forEach { e ->
                    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                decodeLines(e.lines).joinToString(" + ") { "${it.name} ×${it.qty}" }.ifEmpty { "تجريب" },
                                fontSize = fBody, fontWeight = FontWeight.SemiBold, color = cInk, modifier = Modifier.weight(1f, fill = false),
                            )
                            Text(fmt(e.trialAmount), fontSize = fBody, fontWeight = FontWeight.Bold, color = cAmber)
                        }
                        Row(Modifier.fillMaxWidth().padding(top = 7.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            Box(
                                Modifier.weight(1f).clip(RoundedCornerShape(rXs)).background(cCard).border(1.dp, cAmber, RoundedCornerShape(rXs)).tap { vm.convertTrialEntry(e.id) }.padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center,
                            ) { Text("أبقتها — بيع", fontSize = fSmall, fontWeight = FontWeight.Bold, color = cAmber) }
                            Box(
                                Modifier.weight(1f).clip(RoundedCornerShape(rXs)).background(cCard).border(1.dp, cLine, RoundedCornerShape(rXs)).tap { vm.voidEntry(e.id) }.padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center,
                            ) { Text("أعادتها — للمحل", fontSize = fSmall, fontWeight = FontWeight.Bold, color = cAccent) }
                        }
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
        }
        c.phone?.let { Text("☎ $it", fontSize = fSmall, color = cDim, modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)) }
        // reminder: due date + one-tap snooze (FR-3.2), only while she owes
        if (bal > 0) {
            val overdue = c.dueEpochDay != null && c.dueEpochDay < st.today
            Text("التذكير: ${dueStatus(c.dueEpochDay, st.today)}", fontSize = fSmall, fontWeight = FontWeight.Bold, color = if (overdue) cDebt else cInk, modifier = Modifier.padding(start = 4.dp, top = 6.dp, bottom = 6.dp))
            Text("ذكّريني بعد:", fontSize = fCaption, color = cDim, modifier = Modifier.padding(start = 4.dp, bottom = 6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                SnoozeChip("أسبوع") { vm.snooze(c.id, 7) }
                SnoozeChip("أسبوعان") { vm.snooze(c.id, 14) }
                SnoozeChip("شهر") { vm.snooze(c.id, 30) }
            }
        }
        Spacer(Modifier.height(12.dp))
        if (history.isEmpty()) {
            Text("لا حركات مسجّلة بعد", fontSize = fBody, color = cDim, modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp))
        } else {
            Column(Modifier.fillMaxWidth().card().padding(horizontal = 14.dp)) {
                history.take(8).forEach { e ->
                    val amtColor = when (e.cls) { "pos" -> cPaid; "amber" -> cAmber; else -> cInk }
                    Row(Modifier.fillMaxWidth().padding(vertical = 11.dp).drawBottomLine(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f, fill = false)) {
                            Text(e.t, fontSize = fBody, fontWeight = FontWeight.SemiBold, color = cInk)
                            Text(e.d, fontSize = fCaption, color = cDim)
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(e.amt, fontSize = fBody, fontWeight = FontWeight.Bold, color = amtColor)
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        PrimaryButton("+ دفعة من ${c.name}", fontSize = fTitle, radius = rMd, vertical = 14.dp) { vm.payThisCustomer(c.id) }
    }
}

@Composable
private fun ChooserOption(icon: String, title: String, sub: String, outlined: Boolean = false, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(rLg)).background(cCard)
            .border(if (outlined) 1.5.dp else 1.dp, if (outlined) cAccent else cLine, RoundedCornerShape(rLg))
            .tap(onClick).padding(15.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        Text(icon, fontSize = 23.sp)
        Column {
            Text(title, fontSize = fHead, fontWeight = FontWeight.Bold, color = cInk)
            Text(sub, fontSize = fSmall, color = cDim)
        }
    }
}

// ── payment (D37) ──
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PaySheet(st: StoreState, vm: StoreViewModel) {
    PopupEditor("دفعة جديدة", onClose = vm::closeSheet, footerLabel = "حفظ ✓", onSave = vm::savePay) {
        run {
            CustomerRow(st, vm)
            Spacer(Modifier.height(14.dp))
            Text("كم المبلغ؟", fontSize = fSmall, fontWeight = FontWeight.SemiBold, color = cDim, modifier = Modifier.padding(start = 2.dp, bottom = 8.dp))
            Row(
                Modifier.fillMaxWidth().card(rLg).padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically,
            ) {
                StepBtn("−", tapLg, 14.dp, 1.5.dp, cLine, cAccent, 24.sp) { vm.payAmountStep(-1) }
                MoneyValue(st.payAmount, vm::setPayAmount, 30.sp, 120.dp)
                StepBtn("+", tapLg, 14.dp, 1.5.dp, cLine, cAccent, 24.sp) { vm.payAmountStep(1) }
            }
            val cust = st.saleCustomerId?.let { id -> st.customers.find { it.id == id } }
            if (cust != null) {
                // her live balance INCLUDING the تجريب she just decided to keep — the same number
                // «سداد كامل» fills and the context line reads.
                val owed = payOwedWithKept(cust, st.entries, st.payTrialId)
                Row(
                    Modifier.fillMaxWidth().padding(top = 13.dp).clip(RoundedCornerShape(rMd)).background(cBg).border(1.dp, cLine, RoundedCornerShape(rMd)).padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        when {
                            owed > 0 -> "عليها الآن: ${fmt(owed)}"
                            owed == 0L -> "لا دين عليها"
                            else -> "لها ${fmt(-owed)}"
                        },
                        fontSize = fBody, fontWeight = FontWeight.Bold, color = if (owed > 0) cDebt else cPaid,
                    )
                    if (owed > 0) {
                        Box(
                            Modifier.clip(RoundedCornerShape(rXs)).background(cCard).border(1.5.dp, cAccent, RoundedCornerShape(rXs)).tap { vm.payFillOwed() }.padding(horizontal = 12.dp, vertical = 7.dp),
                        ) { Text("سداد كامل", fontSize = fSmall, fontWeight = FontWeight.Bold, color = cAccent) }
                    }
                }
                // her outstanding تجريب — «قرّرت تُبقيها»: selecting one folds its conversion to
                // debt into this payment. Only HER open trials; no الرف here (a دفعة never touches stock).
                val openTrials = openTrialEntries(st.saleCustomerId, st.entries)
                if (openTrials.isNotEmpty()) {
                    Text("قرّرت تُبقي تجريباً؟ اضغطيه — يصبح بيعاً ثم تُخصم منه الدفعة", fontSize = fSmall, fontWeight = FontWeight.SemiBold, color = cDim, modifier = Modifier.padding(start = 2.dp, top = 16.dp, bottom = 8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        openTrials.forEach { e ->
                            val sel = st.payTrialId == e.id
                            val name = decodeLines(e.lines).joinToString(" + ") { it.name }.ifEmpty { "تجريب" }
                            Column(
                                Modifier.clip(RoundedCornerShape(rSm)).background(if (sel) cAmber else cCard).border(1.5.dp, if (sel) cAmber else cAmberBorder, RoundedCornerShape(rSm)).tap { vm.payPickTrial(e.id) }.padding(horizontal = 13.dp, vertical = 9.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(name, fontSize = fBodyL, fontWeight = FontWeight.Bold, color = if (sel) cAink else cInk)
                                Text(fmt(e.trialAmount), fontSize = fCaption, color = if (sel) cAink else cAmber)
                            }
                        }
                    }
                    if (st.payTrialId != null) {
                        Box(Modifier.fillMaxWidth().padding(top = 12.dp).clip(RoundedCornerShape(rMd)).background(cGreenBg).border(1.dp, cGreenBorder, RoundedCornerShape(rMd)).padding(horizontal = 12.dp, vertical = 10.dp)) {
                            Text("يتحوّل التجريب إلى بيع على حسابها، ثم تُخصم الدفعة من رصيدها. لا يُنقص من المحل شيء.", fontSize = fSmall, color = cPaid, lineHeight = 18.sp)
                        }
                    }
                }
            } else {
                // نقدي: no balance will change — if this money is for an item, it's a sale.
                Column(
                    Modifier.fillMaxWidth().padding(top = 16.dp).clip(RoundedCornerShape(rMd)).background(cAmberBg).border(1.dp, cAmberBorder, RoundedCornerShape(rMd)).padding(horizontal = 12.dp, vertical = 10.dp),
                ) {
                    Text("دفعة نقدي لا تُنقص دين أحد.", fontSize = fSmall, fontWeight = FontWeight.Bold, color = cAmber)
                    Text("مبلغ عن صنف؟ سجّليه بيعاً ←", fontSize = fBody, fontWeight = FontWeight.Bold, color = cAccent, modifier = Modifier.padding(top = 7.dp).tap { vm.openSale() })
                }
            }
        }
    }
}

// ── sale ──
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SaleSheet(st: StoreState, vm: StoreViewModel) {
    PopupEditor("بيع جديد", onClose = vm::closeSheet, footerLabel = "حفظ ✓", onSave = vm::saveSale) {
        run {
            CustomerRow(st, vm)
            Spacer(Modifier.height(13.dp))
            Text("مقترحة من المحل — اضغطي لإضافتها", fontSize = fSmall, fontWeight = FontWeight.SemiBold, color = cDim, modifier = Modifier.padding(start = 2.dp, bottom = 9.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                st.shelf.filter { it.onHand > 0 }.sortedByDescending { it.onHand }.forEach { g ->
                    Column(
                        Modifier.widthIn(min = 64.dp).clip(RoundedCornerShape(rSm)).background(cCard).border(1.5.dp, cLine, RoundedCornerShape(rSm)).tap { vm.addLine(g.id) }.padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(g.name, fontSize = fBodyL, fontWeight = FontWeight.Bold, color = cInk)
                        Text("${fmt(g.tasira)} · ${g.onHand}", fontSize = fCaption, color = cDim, modifier = Modifier.padding(top = 1.dp))
                    }
                }
                Box(
                    Modifier.clip(RoundedCornerShape(rSm)).dashedBorder(cLine, rSm, 1.5.dp).tap { vm.toggleAddNew() }.padding(horizontal = 13.dp, vertical = 9.dp),
                    contentAlignment = Alignment.Center,
                ) { Text("+ صنف جديد", fontSize = fBody, fontWeight = FontWeight.Bold, color = cDim) }
            }
            if (st.addNewOpen) {
                Column(Modifier.fillMaxWidth().padding(top = 11.dp).clip(RoundedCornerShape(rMd)).background(cCard).border(1.5.dp, cAccent, RoundedCornerShape(rMd)).padding(horizontal = 13.dp, vertical = 12.dp)) {
                    TextInput(st.newName, vm::setNewName, "اسم الصنف", modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(9.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("السعر", fontSize = fSmall, fontWeight = FontWeight.SemiBold, color = cDim)
                        LabeledStepper("", fmt(st.newPrice), { vm.newPriceStep(-1) }, { vm.newPriceStep(1) }, valueMin = 58.dp, raw = st.newPrice, onType = vm::setNewPrice)
                    }
                    Text("سيُضاف للمحل كـ«لا أعلم» (نقطة حمراء) تحلّينه لاحقاً", fontSize = fCaption, color = cAmber, modifier = Modifier.padding(top = 9.dp))
                    Spacer(Modifier.height(11.dp))
                    PrimaryButton("أضيفي للسلة", fontSize = fBodyL, radius = rSm, vertical = 11.dp) { vm.addNewItem() }
                }
            }
            if (st.lines.isNotEmpty()) {
                Column(Modifier.fillMaxWidth().padding(top = 15.dp).card().padding(horizontal = 14.dp, vertical = 6.dp)) {
                    st.lines.forEachIndexed { i, l -> SaleLineRow(i, l, vm) }
                    // total — the ledger double rule
                    Spacer(Modifier.height(10.dp))
                    HorizontalDivider(color = cInk, thickness = 1.5.dp)
                    Spacer(Modifier.height(2.dp))
                    HorizontalDivider(color = cInk, thickness = 1.dp)
                    Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                        Text("المجموع", fontSize = fTitle, fontWeight = FontWeight.Bold, color = cInk)
                        Text(fmt(st.lines.sumOf { it.price * it.qty }), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = cInk)
                    }
                }
            }
            Text("كيف دفعت؟", fontSize = fSmall, fontWeight = FontWeight.SemiBold, color = cDim, modifier = Modifier.padding(start = 2.dp, top = 16.dp, bottom = 8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                PayModeBtn("دفعت الكل", st.pay == "full", Modifier.weight(1f)) { vm.setPay("full") }
                PayModeBtn("دفعت جزءاً", st.pay == "partial", Modifier.weight(1f)) { vm.setPay("partial") }
                PayModeBtn("تجريب", st.pay == "trial", Modifier.weight(1f)) { vm.setPay("trial") }
            }
            if (st.pay == "partial") {
                val total = st.lines.sumOf { it.price * it.qty }
                val paid = minOf(st.partialPaid, total)
                Spacer(Modifier.height(10.dp))
                Column(Modifier.fillMaxWidth().card(rMd).padding(horizontal = 13.dp, vertical = 12.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("كم دفعت الآن؟", fontSize = fSmall, fontWeight = FontWeight.SemiBold, color = cDim)
                        LabeledStepper("", fmt(st.partialPaid), { vm.partialStep(-1) }, { vm.partialStep(1) }, valueMin = 70.dp, raw = st.partialPaid, onType = vm::setPartialPaid)
                    }
                    Text("الباقي ديناً: ${fmt(total - paid)}", fontSize = fSmall, fontWeight = FontWeight.Bold, color = cDebt, modifier = Modifier.padding(top = 8.dp))
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SaleLineRow(i: Int, l: SaleLine, vm: StoreViewModel) {
    // Each line stacks vertically: the item NAME gets its own full-width row (so a long name —
    // «بنطال قماش نسواني» — never competes with the price stepper and stacks letter-by-letter),
    // then السعر on its own row, then الكمية. Robust at any width / font scale.
    Column(Modifier.fillMaxWidth().padding(vertical = 12.dp).drawBottomLine()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(l.name, fontSize = fTitle, fontWeight = FontWeight.SemiBold, color = cInk, modifier = Modifier.weight(1f))
            if (l.haggled) Text(fmt(l.tasira), fontSize = fCaption, color = cDim, textDecoration = TextDecoration.LineThrough, maxLines = 1, softWrap = false, modifier = Modifier.padding(start = 8.dp))
        }
        Row(Modifier.fillMaxWidth().padding(top = 9.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("السعر", fontSize = fCaption, fontWeight = FontWeight.SemiBold, color = cDim)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StepBtn("−", tapMd, 10.dp, 1.5.dp, cLine, cAccent, 20.sp) { vm.priceStep(i, -1) }
                MoneyValue(l.price, { v -> vm.setLinePrice(i, v) }, fTitle, 60.dp)
                StepBtn("+", tapMd, 10.dp, 1.5.dp, cLine, cAccent, 20.sp) { vm.priceStep(i, 1) }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("الكمية", fontSize = fCaption, fontWeight = FontWeight.SemiBold, color = cDim)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StepBtn("−", tapSm, 9.dp, 1.dp, cLine, cDim, 17.sp) { vm.qtyStep(i, -1) }
                Text("×${l.qty}", fontSize = fBodyL, fontWeight = FontWeight.Bold, color = cInk, textAlign = TextAlign.Center, modifier = Modifier.widthIn(min = 26.dp))
                StepBtn("+", tapSm, 9.dp, 1.dp, cLine, cDim, 17.sp) { vm.qtyStep(i, 1) }
            }
        }
    }
}

// F6/D73 — أخذت لنفسي / هدية: reuses the shelf-chip picker, but no price and no pay modes;
// pieces leave the محل for zero money and land in their own الحساب bucket.
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WithdrawSheet(st: StoreState, vm: StoreViewModel) {
    PopupEditor("أخذت لنفسي / هدية", onClose = vm::closeSheet, footerLabel = "حفظ ✓", onSave = vm::saveWithdrawal) {
        run {
            Text("قطع تخرج من المحل بلا مبلغ — لا تُحسب بيعاً ولا ربحاً.", fontSize = fSmall, color = cDim, lineHeight = 17.sp, modifier = Modifier.padding(start = 2.dp, bottom = 11.dp))
            Text("اضغطي الصنف لإضافته", fontSize = fSmall, fontWeight = FontWeight.SemiBold, color = cDim, modifier = Modifier.padding(start = 2.dp, bottom = 9.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                st.shelf.filter { it.onHand > 0 }.sortedByDescending { it.onHand }.forEach { g ->
                    Column(
                        Modifier.widthIn(min = 64.dp).clip(RoundedCornerShape(rSm)).background(cCard).border(1.5.dp, cLine, RoundedCornerShape(rSm)).tap { vm.addLine(g.id) }.padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(g.name, fontSize = fBodyL, fontWeight = FontWeight.Bold, color = cInk)
                        Text("في المحل ${g.onHand}", fontSize = fCaption, color = cDim, modifier = Modifier.padding(top = 1.dp))
                    }
                }
            }
            if (st.lines.isNotEmpty()) {
                Column(Modifier.fillMaxWidth().padding(top = 15.dp).card().padding(horizontal = 14.dp, vertical = 6.dp)) {
                    st.lines.forEachIndexed { i, l -> WithdrawLineRow(i, l, vm) }
                    Spacer(Modifier.height(10.dp))
                    HorizontalDivider(color = cLine, thickness = 1.dp)
                    Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                        Text("قيمة تقريبية", fontSize = fBody, fontWeight = FontWeight.SemiBold, color = cDim)
                        Text("~ ${fmt(st.lines.sumOf { it.price * it.qty })}", fontSize = fBodyL, fontWeight = FontWeight.Bold, color = cAmber)
                    }
                    Text("بأسعار العرض — للمتابعة فقط، لا تدخل في المال أو الربح.", fontSize = fCaption, color = cDim, modifier = Modifier.padding(top = 6.dp))
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun WithdrawLineRow(i: Int, l: SaleLine, vm: StoreViewModel) {
    Row(Modifier.fillMaxWidth().padding(vertical = 11.dp).drawBottomLine(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(l.name, fontSize = fTitle, fontWeight = FontWeight.SemiBold, color = cInk, modifier = Modifier.weight(1f, fill = false))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            Text("الكمية", fontSize = fCaption, fontWeight = FontWeight.SemiBold, color = cDim)
            StepBtn("−", tapMd, 10.dp, 1.5.dp, cLine, cAccent, 20.sp) { vm.withdrawQtyStep(i, -1) }
            Text("×${l.qty}", fontSize = fBodyL, fontWeight = FontWeight.Bold, color = cInk, textAlign = TextAlign.Center, modifier = Modifier.widthIn(min = 24.dp))
            StepBtn("+", tapMd, 10.dp, 1.5.dp, cLine, cAccent, 20.sp) { vm.withdrawQtyStep(i, 1) }
        }
    }
}

@Composable
private fun PayModeBtn(label: String, active: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier.clip(RoundedCornerShape(rMd)).background(if (active) cAccent else cCard).border(1.5.dp, if (active) cAccent else cLine, RoundedCornerShape(rMd)).tap(onClick).padding(vertical = 13.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center,
    ) { Text(label, fontSize = fBody, fontWeight = FontWeight.Bold, color = if (active) cAink else cDim) }
}

// ── specify source ──
@Composable
private fun SpecifySheet(st: StoreState, vm: StoreViewModel) {
    BottomSheet(onDismiss = vm::closeSpecify) {
        Text("من أي مصدر؟", fontSize = fTitle, fontWeight = FontWeight.Bold, color = cInk, modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 4.dp))
        Text("وجّهي القطع إلى بالتها لتزيد عدّاداتها، أو اتركيها «بضاعة قديمة».", fontSize = fSmall, color = cDim, modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 12.dp))
        // V3 merge: بالات only + one «بضاعة قديمة» bucket (PRE_ID) — شراء من السوق hidden.
        st.sources.filter { it.kind == Kind.BALE }.forEach { srcOpt ->
            Row(
                Modifier.fillMaxWidth().padding(bottom = 8.dp).card(rMd).tap { vm.pickSource(srcOpt.id) }.padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("📦 ${srcOpt.label}", fontSize = fTitle, fontWeight = FontWeight.Bold, color = cInk)
                Text(srcOpt.kind.label, fontSize = fCaption, color = cDim)
            }
        }
        Row(
            Modifier.fillMaxWidth().card(rMd).tap { vm.pickSource(PRE_ID) }.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("بضاعة قديمة — بلا مصدر", fontSize = fBodyL, fontWeight = FontWeight.Bold, color = cInk)
            Text("بلا كلفة", fontSize = fCaption, color = cDim)
        }
    }
}

// ── the bale screen (F1) — one place to rename, read stats, count, shelve and edit ──
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ColumnScope.PackageBody(st: StoreState, vm: StoreViewModel) {
    val sv = sourceViews(st.sources, st.shelf, st.usdRate, st.expenses).find { it.id == rememberLast(st.pkgId) } ?: return
    val items = st.shelf.filter { it.sourceId == sv.id }
    run {
        Text("📦 ${sv.label}", fontSize = fHead, fontWeight = FontWeight.Bold, color = cInk, modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 14.dp))
            // rename — the same in-place pattern the shops use (source-generic handlers)
            Row(Modifier.fillMaxWidth().padding(bottom = 11.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                if (st.shopRenameId == sv.id) {
                    TextInput(st.shopName, vm::setShopName, "اسم البالة", modifier = Modifier.weight(1f), bg = cCard)
                    Text("حفظ", fontSize = fBody, fontWeight = FontWeight.Bold, color = cAccent, modifier = Modifier.padding(start = 10.dp).tap { vm.saveRenameShop() })
                } else {
                    Text("${sv.label} ✎", fontSize = fTitle, fontWeight = FontWeight.Bold, color = cInk, modifier = Modifier.tap { vm.startRenameShop(sv.id) })
                    Text(sv.kindLabel, fontSize = fCaption, color = cDim)
                }
            }
            // slice 4: how the counted total splits into named types. The remainder may go negative
            // (she named more than she counted) — an amber warning, never a block.
            sv.countTotal?.let { total ->
                val allocated = baleAllocated(sv.id, st.shelf)
                val remainder = baleUnallocated(total, allocated)
                if (remainder < 0) {
                    Row(
                        Modifier.fillMaxWidth().padding(bottom = 11.dp).clip(RoundedCornerShape(rSm)).background(cAmberBg).border(1.dp, cAmberBorder, RoundedCornerShape(rSm)).padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("مصنّف أكثر من العدّ بـ ${-remainder} — أعيدي العدّ أو صحّحي الأصناف", fontSize = fSmall, fontWeight = FontWeight.SemiBold, color = cAmber, lineHeight = 18.sp)
                    }
                } else {
                    Row(
                        Modifier.fillMaxWidth().padding(bottom = 11.dp).clip(RoundedCornerShape(rSm)).background(cBg).border(1.dp, cLine, RoundedCornerShape(rSm)).padding(horizontal = 12.dp, vertical = 9.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("معدود $total", fontSize = fSmall, fontWeight = FontWeight.Bold, color = cInk)
                        Text("·", fontSize = fSmall, color = cDim)
                        Text("مصنّف $allocated", fontSize = fSmall, fontWeight = FontWeight.SemiBold, color = cDim)
                        Text("·", fontSize = fSmall, color = cDim)
                        Text("متبقي غير مصنّف $remainder", fontSize = fSmall, fontWeight = FontWeight.Bold, color = if (remainder > 0) cPaid else cDim)
                    }
                }
            }
            // stats — the bale's ledger at a glance. V3 framing (ITEM 6): never a red loss —
            // pre-recovery shows «بقي لاسترداد رأس المال» (positive), post shows the profit.
            val fr = baleFraming(sv.revenue, sv.costLocal, sv.sold)
            Column(Modifier.fillMaxWidth().card().padding(horizontal = 14.dp, vertical = 13.dp)) {
                FlowRow(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    // slice 3: expenses join the cost as «$275 + 50,000» (USD cost + SYP expenses)
                    if (sv.expensesTotal > 0)
                        BaleStat("التكلفة", "${sv.costFmt} + ${fmt(sv.expensesTotal)}", cInk, valueSize = fBodyL)
                    else BaleStat("التكلفة", sv.costFmt, cInk)
                    BaleStat("الإيراد المنسوب", sv.revFmt, cInk)
                    when {
                        fr == null -> BaleStat("الربح تقريباً", "—", cDim, bold = true)
                        fr.recovered -> BaleStat("الربح تقريباً", "+ " + fmt(fr.profit!!), cPaid, bold = true)
                        else -> BaleStat("بقي لاسترداد رأس المال", fmt(fr.remainingToRecover!!), cAmber, bold = true, valueSize = fBodyL)
                    }
                }
                fr?.let {
                    Text(it.statusLine, fontSize = fCaption, fontWeight = FontWeight.SemiBold, color = if (it.recovered) cPaid else cAmber, modifier = Modifier.padding(top = 9.dp))
                }
                val pct = recoveryPct(sv.revenue, sv.costLocal)
                if (pct != null) {
                    val done = pct >= 100
                    Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("استرداد رأس المال", fontSize = fCaption, fontWeight = FontWeight.SemiBold, color = cDim)
                        Text(if (done) "$pct% ✓" else "$pct%", fontSize = fBody, fontWeight = FontWeight.ExtraBold, color = if (done) cPaid else cAmber)
                    }
                    Box(Modifier.fillMaxWidth().padding(top = 6.dp).height(8.dp).clip(RoundedCornerShape(rXs)).background(cBg).border(1.dp, cLine, RoundedCornerShape(rXs))) {
                        Box(Modifier.fillMaxWidth(minOf(pct, 100) / 100f).fillMaxHeight().background(if (done) cPaid else cAmber))
                    }
                    // frozen-rate bales don't move with today's rate — only legacy bales do
                    if (sv.ratePurchase == null) Text("يتغيّر مع سعر صرف اليوم", fontSize = fCaption, color = cDim, modifier = Modifier.padding(top = 5.dp))
                }
                Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    BaleStat("بيعت", "${sv.sold}", cPaid)
                    BaleStat("في المحل", "${sv.remain}", cInk)
                    BaleStat("في البالة", "${sv.inPkg}", cAmber)
                    BaleStat("متوسط سعر البيع", avgSoldPrice(sv.revenue, sv.sold)?.let { fmt(it) } ?: "—", cInk)
                }
                // slice 2: the rate frozen into this bale (legacy bales have none → live rate, no line)
                sv.ratePurchase?.let { rate ->
                    Text("سعر الصرف المُثبّت: ${fmt(rate)}", fontSize = fCaption, fontWeight = FontWeight.SemiBold, color = cDim, modifier = Modifier.padding(top = 10.dp))
                }
                // what each piece cost her: the bale's inclusive cost (USD×frozen rate + expenses)
                // ÷ the counted total — the headline she looks for right after counting + pricing.
                val cnt = sv.countTotal ?: 0
                if (cnt > 0 && sv.costLocal != null) {
                    Text("سعر القطعة تقريباً: ${fmt(sv.costLocal!! / cnt)}", fontSize = fBody, fontWeight = FontWeight.Bold, color = cInk, modifier = Modifier.padding(top = 8.dp))
                }
                // ITEM 6: the USD line mirrors the SYP framing — pre-recovery «بقي تقريباً: $N»
                // (positive), post «الربح بالدولار تقريباً: + $N». Never a negative dollar sign.
                val usdRate = sv.ratePurchase ?: st.usdRate
                fr?.let { f ->
                    if (f.recovered) {
                        baleUsdProfit(f.profit, usdRate)?.let { usd ->
                            Text("الربح بالدولار تقريباً: + \$${fmt(usd)}", fontSize = fCaption, fontWeight = FontWeight.Bold, color = cPaid, modifier = Modifier.padding(top = 4.dp))
                        }
                    } else {
                        baleUsdProfit(f.remainingToRecover, usdRate)?.let { usd ->
                            Text("بقي تقريباً: \$${fmt(usd)}", fontSize = fCaption, fontWeight = FontWeight.Bold, color = cAmber, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Text("عُدّي أصناف البالة وانقليها إلى المحل — كلها أو جزءاً. ما يبقى «في البالة» تنقلينه لاحقاً. اضغطي اسم الصنف لتعديله.", fontSize = fSmall, color = cDim, lineHeight = 18.sp, modifier = Modifier.padding(start = 2.dp, end = 2.dp, bottom = 12.dp))
            Column(Modifier.fillMaxWidth().card().padding(horizontal = 14.dp)) {
                items.forEach { p -> PackageItemRow(p, vm) }
                Box(Modifier.fillMaxWidth().tap { vm.togglePkgAdd() }.padding(top = 11.dp, bottom = 7.dp), contentAlignment = Alignment.Center) {
                    Text("+ عدّ صنف في البالة", fontSize = fBody, fontWeight = FontWeight.Bold, color = cAccent)
                }
            }
            if (st.pkgAddOpen) {
                Column(Modifier.fillMaxWidth().padding(top = 11.dp).clip(RoundedCornerShape(rMd)).background(cCard).border(1.5.dp, cAccent, RoundedCornerShape(rMd)).padding(horizontal = 13.dp, vertical = 12.dp)) {
                    TextInput(st.aiName, vm::setAiName, "اسم الصنف", modifier = Modifier.fillMaxWidth())
                    Row(Modifier.fillMaxWidth().padding(top = 9.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("التسعيرة", fontSize = fSmall, fontWeight = FontWeight.SemiBold, color = cDim)
                        LabeledStepper("", fmt(st.aiTasira), { vm.aiTasiraStep(-1) }, { vm.aiTasiraStep(1) }, btnSize = tapSm, valueMin = 56.dp, valueSize = 16.sp, btnFont = 18.sp, raw = st.aiTasira, onType = vm::setAiTasira)
                    }
                    Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("العدد في البالة", fontSize = fSmall, fontWeight = FontWeight.SemiBold, color = cDim)
                        LabeledStepper("", "${st.aiCount}", { vm.aiCountStep(-1) }, { vm.aiCountStep(1) }, btnSize = tapSm, valueMin = 32.dp, valueSize = 16.sp, btnFont = 18.sp, raw = st.aiCount.toLong(), onType = vm::setAiCount)
                    }
                    Spacer(Modifier.height(11.dp))
                    PrimaryButton("عدّ (يبقى في البالة) ✓", fontSize = fBodyL, radius = rSm, vertical = 11.dp) { vm.savePkgCount() }
                }
            }
            // ── slice 3: bale expenses — a typed list deducted IN FULL from this bale's profit ──
            Spacer(Modifier.height(16.dp))
            Text("مصاريف البالة", fontSize = fTitle, fontWeight = FontWeight.Bold, color = cInk, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
            Text("مثل الكوي — تُخصم كاملةً من ربح هذه البالة.", fontSize = fSmall, color = cDim, modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 8.dp))
            val myExpenses = st.expenses.filter { it.sourceId == sv.id }
            Column(Modifier.fillMaxWidth().card().padding(horizontal = 14.dp, vertical = 4.dp)) {
                myExpenses.forEach { ex ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 11.dp).drawBottomLine(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(ex.label, fontSize = fBodyL, fontWeight = FontWeight.Bold, color = cInk)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(fmt(ex.amount), fontSize = fBodyL, fontWeight = FontWeight.Bold, color = cDebt)
                            Text("✕", fontSize = fBodyL, fontWeight = FontWeight.Bold, color = cDim, modifier = Modifier.tap { vm.removeExpense(ex.id) })
                        }
                    }
                }
                if (myExpenses.isEmpty()) Text("لا مصاريف بعد", fontSize = fSmall, color = cDim, modifier = Modifier.padding(vertical = 11.dp))
                // add row — quick-pick her used labels («كوي» always), or type a new one
                Column(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 8.dp)) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        expenseLabelChips(st.expenses).forEach { chip ->
                            val sel = st.expenseLabel.trim() == chip
                            Box(
                                Modifier.clip(RoundedCornerShape(rSm)).background(if (sel) cAccent else cCard).border(1.5.dp, if (sel) cAccent else cLine, RoundedCornerShape(rSm)).tap { vm.setExpenseLabel(chip) }.padding(horizontal = 13.dp, vertical = 8.dp),
                            ) {
                                Text(chip, fontSize = fBody, fontWeight = FontWeight.Bold, color = if (sel) cAink else cInk)
                            }
                        }
                    }
                    Spacer(Modifier.height(9.dp))
                    TextInput(st.expenseLabel, vm::setExpenseLabel, "اسم المصروف — مثال: نقل", modifier = Modifier.fillMaxWidth(), bg = cBg)
                    Row(Modifier.fillMaxWidth().padding(top = 9.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("المبلغ", fontSize = fSmall, fontWeight = FontWeight.SemiBold, color = cDim)
                        LabeledStepper("", fmt(st.expenseAmount), { vm.expenseAmountStep(-1) }, { vm.expenseAmountStep(1) }, valueMin = 70.dp, borderColor = cAmberBorder, btnColor = cAmber, raw = st.expenseAmount, onType = vm::setExpenseAmount)
                    }
                    Spacer(Modifier.height(11.dp))
                    PrimaryButton("أضيفي المصروف ✓", fontSize = fBodyL, radius = rSm, vertical = 11.dp) { vm.addExpense() }
                }
            }
        }
    }


@Composable
private fun BaleStat(label: String, value: String, color: Color, bold: Boolean = false, valueSize: androidx.compose.ui.unit.TextUnit = fTitle) {
    Column {
        Text(label, fontSize = fCaption, fontWeight = FontWeight.SemiBold, color = cDim)
        Text(value, fontSize = valueSize, fontWeight = if (bold) FontWeight.ExtraBold else FontWeight.Bold, color = color, modifier = Modifier.padding(top = 1.dp))
    }
}

@Composable
private fun PackageItemRow(p: Shelf, vm: StoreViewModel) {
    Column(Modifier.fillMaxWidth().padding(vertical = 12.dp).drawBottomLine()) {
        // the name opens the one-sheet item editor (F1) — tasira, counts, source, everything
        Row(Modifier.fillMaxWidth().tap { vm.openEditItem(p.id) }, horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("${p.name} ✎", fontSize = fTitle, fontWeight = FontWeight.Bold, color = cInk)
            Text(fmt(p.tasira), fontSize = fSmall, color = cDim)
        }
        Row(Modifier.fillMaxWidth().padding(top = 9.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("في البالة: ${p.inPkg}", fontSize = fSmall, fontWeight = FontWeight.Bold, color = cAmber)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("في المحل", fontSize = fCaption, color = cDim)
                StepBtn("−", tapMd, 10.dp, 1.5.dp, cLine, cAccent, 20.sp) { vm.shelveStep(p.id, -1) }
                Text("${p.shelved}", fontSize = fTitle, fontWeight = FontWeight.ExtraBold, color = cInk, textAlign = TextAlign.Center, modifier = Modifier.widthIn(min = 28.dp))
                StepBtn("+", tapMd, 10.dp, 1.5.dp, cLine, cAccent, 20.sp) { vm.shelveStep(p.id, 1) }
            }
        }
        if (p.inPkg > 0) {
            Text("انقلي الكل (${p.inPkg}) ←", fontSize = fCaption, fontWeight = FontWeight.Bold, color = cAccent, modifier = Modifier.padding(top = 7.dp).tap { vm.shelveAll(p.id) })
        } else {
            Text("الكل في المحل ✓", fontSize = fCaption, fontWeight = FontWeight.Bold, color = cPaid, modifier = Modifier.padding(top = 7.dp))
        }
    }
}

// ── the shop screen (F2) — rename, stats, items and the shop's debt with «دفعتُ للمحل» ──
@Composable
internal fun ColumnScope.ShopBody(st: StoreState, vm: StoreViewModel) {
    val sv = sourceViews(st.sources, st.shelf, st.usdRate, st.expenses).find { it.id == rememberLast(st.shopId) } ?: return
    val items = st.shelf.filter { it.sourceId == sv.id }
    val debtNow = maxOf(0, shopDebtNow(st.sources.first { it.id == sv.id }, st.entries))
    run {
        Text("🏪 ${sv.label}", fontSize = fHead, fontWeight = FontWeight.Bold, color = cInk, modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 14.dp))
            // rename — same in-place pattern as the bale screen
            Row(Modifier.fillMaxWidth().padding(bottom = 11.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                if (st.shopRenameId == sv.id) {
                    TextInput(st.shopName, vm::setShopName, "اسم المحل", modifier = Modifier.weight(1f), bg = cCard)
                    Text("حفظ", fontSize = fBody, fontWeight = FontWeight.Bold, color = cAccent, modifier = Modifier.padding(start = 10.dp).tap { vm.saveRenameShop() })
                } else {
                    Text("${sv.label} ✎", fontSize = fTitle, fontWeight = FontWeight.Bold, color = cInk, modifier = Modifier.tap { vm.startRenameShop(sv.id) })
                    Text("محل من السوق", fontSize = fCaption, color = cDim)
                }
            }
            // stats
            Column(Modifier.fillMaxWidth().card().padding(horizontal = 14.dp, vertical = 13.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    BaleStat("كلفة بضاعته", fmt(sv.costLocal ?: 0), cInk)
                    BaleStat("الإيراد المنسوب", sv.revFmt, cInk)
                    BaleStat("الربح تقريباً", sv.profitFmt, if (sv.profit == null) cDim else if (sv.profit >= 0) cPaid else cDebt, bold = true)
                }
            }
            Spacer(Modifier.height(12.dp))
            // the shop's debt + the real pay action (D68) — adjust steppers stay for
            // entering new credit or correcting; paying is a قيد in the day book
            Column(Modifier.fillMaxWidth().card().padding(horizontal = 14.dp, vertical = 13.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(if (debtNow > 0) "دينه علينا" else "لا دين له", fontSize = fBody, fontWeight = FontWeight.Bold, color = if (debtNow > 0) cDebt else cPaid)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        StepBtn("−", tapMd, 10.dp, 1.5.dp, cLine, cDim, 20.sp) { vm.shopOwedStep(sv.id, -1) }
                        Text(fmt(debtNow), fontSize = fTitle, fontWeight = FontWeight.ExtraBold, color = if (debtNow > 0) cDebt else cDim, textAlign = TextAlign.Center, modifier = Modifier.widthIn(min = 64.dp))
                        StepBtn("+", tapMd, 10.dp, 1.5.dp, cLine, cDim, 20.sp) { vm.shopOwedStep(sv.id, 1) }
                    }
                }
                if (debtNow > 0) {
                    Spacer(Modifier.height(11.dp))
                    if (st.shopPayOpen) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("كم دفعتِ الآن؟", fontSize = fSmall, fontWeight = FontWeight.SemiBold, color = cDim)
                            LabeledStepper("", fmt(st.shopPayAmount), { vm.shopPayStep(-1) }, { vm.shopPayStep(1) }, valueMin = 64.dp, valueSize = 17.sp, raw = st.shopPayAmount, onType = vm::setShopPayAmount)
                        }
                        Spacer(Modifier.height(10.dp))
                        PrimaryButton("سجّلي الدفعة للمحل ✓", fontSize = fBodyL, radius = rSm, vertical = 11.dp) { vm.saveShopPay() }
                        Text("تُسجَّل قيداً في دفتر اليوم ويمكن إلغاؤها — ولا تُحسب ضمن «قبضنا اليوم».", fontSize = fCaption, color = cDim, lineHeight = 17.sp, modifier = Modifier.padding(top = 7.dp))
                    } else {
                        OutlineButton("دفعتُ للمحل ↧", fontSize = fBodyL, radius = rSm, vertical = 11.dp, filledCard = false) { vm.toggleShopPay() }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Text("مشترياتك منه — اضغطي اسم الصنف لتعديله.", fontSize = fSmall, color = cDim, modifier = Modifier.padding(start = 2.dp, end = 2.dp, bottom = 8.dp))
            Column(Modifier.fillMaxWidth().card().padding(horizontal = 14.dp)) {
                items.forEach { p ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp).drawBottomLine().tap { vm.openEditItem(p.id) }, horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("${p.name} ✎", fontSize = fBodyL, fontWeight = FontWeight.Bold, color = cInk)
                        Text("×${p.shelved}" + (p.buy?.let { b -> " · شراء @${fmt(b)}" } ?: "") + " · تسعيرة ${fmt(p.tasira)}", fontSize = fCaption, color = cDim)
                    }
                }
                if (items.isEmpty()) Text("لا مشتريات بعد", fontSize = fSmall, color = cDim, modifier = Modifier.padding(vertical = 12.dp))
                Box(Modifier.fillMaxWidth().tap { vm.openAddItemFor(sv.id) }.padding(top = 11.dp, bottom = 7.dp), contentAlignment = Alignment.Center) {
                    Text("+ صنف من هذا المحل", fontSize = fBody, fontWeight = FontWeight.Bold, color = cAccent)
                }
            }
        }
    }

// ── add item to shelf ──
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddItemSheet(st: StoreState, vm: StoreViewModel) {
    PopupEditor("إضافة صنف للمحل", onClose = vm::closeAddItem, footerLabel = "أضيفي للمحل ✓", onSave = vm::saveAiItem) {
        run {
            Text("الصنف", fontSize = fSmall, fontWeight = FontWeight.SemiBold, color = cDim, modifier = Modifier.padding(start = 2.dp, bottom = 6.dp))
            TextInput(st.aiName, vm::setAiName, "مثال: فستان", modifier = Modifier.fillMaxWidth(), bg = cCard, radius = rMd, fontSize = fTitle)
            Spacer(Modifier.height(11.dp))
            CardStepperRow("التسعيرة", fmt(st.aiTasira), { vm.aiTasiraStep(-1) }, { vm.aiTasiraStep(1) }, raw = st.aiTasira, onType = vm::setAiTasira)
            Spacer(Modifier.height(8.dp))
            CardStepperRow("العدد في المحل", "${st.aiCount}", { vm.aiCountStep(-1) }, { vm.aiCountStep(1) }, raw = st.aiCount.toLong(), onType = vm::setAiCount)
            // V3 merge: one «بضاعة قديمة» chip (maps to PRE_ID) + بالات only — شراء من السوق hidden.
            Text("من أين؟ («بضاعة قديمة» الافتراضي — وجّهيه لبالته إن عرفتِها)", fontSize = fSmall, fontWeight = FontWeight.SemiBold, color = cDim, modifier = Modifier.padding(start = 2.dp, top = 16.dp, bottom = 8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val oldStockSel = st.aiSource == "none" || st.aiSource == PRE_ID
                SourcePickChip("بضاعة قديمة", oldStockSel, false) { vm.aiPickSource(PRE_ID) }
                st.sources.filter { it.kind == Kind.BALE }.forEach { srcOpt ->
                    SourcePickChip(srcOpt.label, st.aiSource == srcOpt.id, false) { vm.aiPickSource(srcOpt.id) }
                }
            }
            Text("بضاعتك القديمة أو ما لا تعرفين مصدره؟ اتركيها على «بضاعة قديمة» — بلا كلفة ولا ربح.", fontSize = fCaption, color = cDim, lineHeight = 18.sp, modifier = Modifier.padding(top = 12.dp))
        }
    }
}

@Composable
private fun SourcePickChip(label: String, selected: Boolean, unspec: Boolean, onClick: () -> Unit) {
    val bg = if (selected) (if (unspec) cDebt else cAccent) else cCard
    val col = if (selected) (if (unspec) Color.White else cAink) else (if (unspec) cDebt else cInk)
    val border = if (selected) (if (unspec) cDebt else cAccent) else (if (unspec) cUnspecBorder else cLine)
    Box(Modifier.clip(RoundedCornerShape(rSm)).background(bg).border(1.5.dp, border, RoundedCornerShape(rSm)).tap(onClick).padding(horizontal = 13.dp, vertical = 9.dp)) {
        Text(label, fontSize = fBody, fontWeight = FontWeight.Bold, color = col)
    }
}

// ── add source ──
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddSourceSheet(st: StoreState, vm: StoreViewModel) {
    BottomSheet(onDismiss = vm::closeAddSource) {
        Text("بالة جديدة", fontSize = fTitle, fontWeight = FontWeight.Bold, color = cInk, modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 12.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 14.dp)) {
            val sel = st.addSrcKind == Kind.BALE
            Box(Modifier.clip(RoundedCornerShape(rSm)).background(if (sel) cAccent else cCard).border(1.5.dp, if (sel) cAccent else cLine, RoundedCornerShape(rSm)).tap { vm.pickKind(Kind.BALE) }.padding(horizontal = 14.dp, vertical = 10.dp)) {
                Text(Kind.BALE.label, fontSize = fBodyL, fontWeight = FontWeight.Bold, color = if (sel) cAink else cInk)
            }
        }
        TextInput(st.newSrcName, vm::setNewSrcName, "اسم البالة — مثال: بالة شتوية", modifier = Modifier.fillMaxWidth(), bg = cCard, radius = rMd)
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier.fillMaxWidth().card(rMd).padding(horizontal = 13.dp, vertical = 11.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("التكلفة (USD)", fontSize = fBody, fontWeight = FontWeight.SemiBold, color = cDim, modifier = Modifier.weight(1f, fill = false).padding(end = 8.dp))
            LabeledStepper("", "$" + fmt(st.newCost), { vm.costStep(-1) }, { vm.costStep(1) }, valueMin = 64.dp, valueSize = 18.sp, raw = st.newCost, onType = vm::setNewCost)
        }
        Spacer(Modifier.height(8.dp))
        // slice 2: the pieces she counted at purchase — the remainder («متبقي غير مصنّف») is tracked
        // against this as she names item types. 0 is fine (she can count later).
        CardStepperRow("كم قطعة؟ (بعد العدّ)", "${st.newCount}", { vm.newCountStep(-1) }, { vm.newCountStep(1) }, raw = st.newCount.toLong(), onType = vm::setNewCount)
        Spacer(Modifier.height(8.dp))
        // slice 2: the rate freezes into THIS bale on save — today's global rate is only its default
        Row(
            Modifier.fillMaxWidth().card(rMd).padding(horizontal = 13.dp, vertical = 11.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("سعر الصرف لهذه البالة", fontSize = fBody, fontWeight = FontWeight.SemiBold, color = cDim)
            MoneyValue(st.newRate, vm::setNewRate, fBodyL, 64.dp)
        }
        Text("يُثبَّت سعر الصرف مع البالة — تغييرُ سعر اليوم لاحقاً لا يمسّها.", fontSize = fCaption, color = cDim, lineHeight = 17.sp, modifier = Modifier.padding(top = 7.dp, start = 2.dp, end = 2.dp))
        Spacer(Modifier.height(12.dp))
        PrimaryButton("أضيفي المصدر ✓", fontSize = fTitle, radius = rLg, vertical = 14.dp) { vm.saveSource() }
    }
}

