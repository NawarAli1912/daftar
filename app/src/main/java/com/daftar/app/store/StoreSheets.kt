package com.daftar.app.store

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
    when (st.screen) {
        "chooser" -> Chooser(vm)
        "pay" -> PaySheet(st, vm)
        "sale" -> SaleSheet(st, vm)
        "return" -> ReturnSheet(st, vm)
        "additem" -> AddItemSheet(st, vm)
        "package" -> PackageSheet(st, vm)
        "addsrc" -> AddSourceSheet(st, vm)
    }
    if (st.custPickerOpen) CustPicker(st, vm)
    if (st.detailEntryId != null) EntryDetailSheet(st, vm)
    if (st.detailCustomerId != null) CustomerDetailSheet(st, vm)
    if (st.specifyId != null) SpecifySheet(st, vm) // layers on top of the sale detail
    if (st.undo != null && st.screen == "home") UndoToast(vm)
}

// ── shared sheet chrome ──
@Composable
private fun BottomSheet(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Box(Modifier.fillMaxSize()) {
        Scrim(onDismiss)
        SlideUp(Modifier.align(Alignment.BottomCenter)) { // real slide up from below
            Column(
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)).background(cBg)
                    .navigationBarsPadding().padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 22.dp),
            ) {
                Box(Modifier.align(Alignment.CenterHorizontally).width(38.dp).height(4.dp).clip(RoundedCornerShape(99.dp)).background(cLine))
                Spacer(Modifier.height(14.dp))
                content()
            }
        }
    }
}

@Composable
private fun SheetHeader(title: String, onClose: () -> Unit, back: (() -> Unit)? = null) {
    Column(Modifier.fillMaxWidth().background(cBg).statusBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
        ) {
            if (back != null) Text("‹", fontSize = 22.sp, color = cDim, modifier = Modifier.tap(back))
            Text(title, fontSize = 16.5.sp, fontWeight = FontWeight.Bold, color = cInk)
            Text("✕", fontSize = 20.sp, color = cDim, modifier = Modifier.tap(onClose))
        }
        HorizontalDivider(color = cLine, thickness = 1.dp)
    }
}

@Composable
private fun SheetFooter(label: String, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().background(cBg)) {
        HorizontalDivider(color = cLine, thickness = 1.dp)
        Box(Modifier.fillMaxWidth().navigationBarsPadding().padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 14.dp)) {
            PrimaryButton(label, fontSize = 17.sp) { onClick() }
        }
    }
}

@Composable
private fun TextInput(value: String, onValueChange: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier, bg: Color = cBg, radius: androidx.compose.ui.unit.Dp = 10.dp, fontSize: androidx.compose.ui.unit.TextUnit = 14.sp) {
    BasicTextField(
        value = value, onValueChange = onValueChange,
        modifier = modifier.clip(RoundedCornerShape(radius)).background(bg).border(1.dp, cLine, RoundedCornerShape(radius)).padding(horizontal = 12.dp, vertical = 11.dp),
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
@Composable
private fun LabeledStepper(
    label: String, value: String, onMinus: () -> Unit, onPlus: () -> Unit,
    btnSize: androidx.compose.ui.unit.Dp = 28.dp, valueMin: androidx.compose.ui.unit.Dp = 60.dp,
    valueSize: androidx.compose.ui.unit.TextUnit = 17.sp, btnFont: androidx.compose.ui.unit.TextUnit = 17.sp,
    borderColor: Color = cLine, btnColor: Color = cAccent,
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
        StepBtn("−", btnSize, 8.dp, 1.5.dp, borderColor, btnColor, btnFont, onMinus)
        Text(value, fontSize = valueSize, fontWeight = FontWeight.Bold, color = cInk, textAlign = TextAlign.Center, modifier = Modifier.widthIn(min = valueMin))
        StepBtn("+", btnSize, 8.dp, 1.5.dp, borderColor, btnColor, btnFont, onPlus)
    }
    label.let {}
}

@Composable
private fun CardStepperRow(label: String, value: String, onMinus: () -> Unit, onPlus: () -> Unit, borderColor: Color = cLine, btnColor: Color = cAccent, labelColor: Color = cDim, bg: Color = cCard) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(bg).border(1.dp, if (bg == cCard) cLine else borderColor, RoundedCornerShape(12.dp)).padding(horizontal = 13.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = labelColor)
        LabeledStepper("", value, onMinus, onPlus, borderColor = borderColor, btnColor = btnColor)
    }
}

@Composable
private fun SnoozeChip(label: String, onClick: () -> Unit) {
    Box(
        Modifier.clip(RoundedCornerShape(9.dp)).background(cCard).border(1.dp, cLine, RoundedCornerShape(9.dp)).tap(onClick).padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(label, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = cAccent)
    }
}

@Composable
private fun CustomerRow(st: StoreState, vm: StoreViewModel) {
    val cust = st.saleCustomerId?.let { id -> st.customers.find { it.id == id } }
    Row(
        Modifier.fillMaxWidth().card(12.dp).tap { vm.openCustPicker() }.padding(horizontal = 13.dp, vertical = 11.dp),
        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("الزبونة", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = cDim)
        Text(
            (cust?.name ?: "بدون اسم — نقدي") + " ›",
            fontSize = 14.5.sp, fontWeight = FontWeight.Bold, color = if (cust != null) cDebt else cInk,
        )
    }
}

@Composable
private fun CustPicker(st: StoreState, vm: StoreViewModel) {
    BottomSheet(onDismiss = vm::closeCustPicker) {
        Text("لِمَن هذه العملية؟", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = cInk, modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 12.dp))
        // نقدي — no customer
        Row(
            Modifier.fillMaxWidth().padding(bottom = 8.dp).card(12.dp).tap { vm.pickCustomer(null) }.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("نقدي — بدون اسم", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = cInk)
            if (st.saleCustomerId == null) Text("✓", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = cPaid)
        }
        st.customers.forEach { c ->
            val bal = customerBalance(c, st.entries)
            Row(
                Modifier.fillMaxWidth().padding(bottom = 8.dp).card(12.dp).tap { vm.pickCustomer(c.id) }.padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(c.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = cInk)
                Text(
                    if (bal > 0) "عليها ${fmt(bal)}" else "لا دين",
                    fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = if (bal > 0) cDebt else cPaid,
                )
            }
        }
        if (!st.custNewOpen) {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(cCard).dashedBorder(cLine, 12.dp).tap { vm.toggleCustNew() }.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("+ زبونة جديدة", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = cAccent)
            }
        } else {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).background(cCard).border(1.5.dp, cAccent, RoundedCornerShape(13.dp)).padding(horizontal = 13.dp, vertical = 12.dp)) {
                TextInput(st.custNewName, vm::setCustNewName, "الاسم", modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                TextInput(st.custNewPhone, vm::setCustNewPhone, "الهاتف (اختياري)", modifier = Modifier.fillMaxWidth())
                Row(Modifier.fillMaxWidth().padding(top = 9.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("دين قديم (اختياري)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = cDim)
                    LabeledStepper("", fmt(st.custNewDebt), { vm.custNewDebtStep(-1) }, { vm.custNewDebtStep(1) }, btnSize = 28.dp, valueMin = 60.dp)
                }
                Spacer(Modifier.height(11.dp))
                PrimaryButton("أضيفي الزبونة ✓", fontSize = 14.sp, radius = 11.dp, vertical = 11.dp) { vm.addCustomer() }
            }
        }
    }
}

// ── chooser ──
@Composable
private fun Chooser(vm: StoreViewModel) {
    BottomSheet(onDismiss = vm::closeSheet) {
        Text("ماذا تسجّلين؟", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = cInk, modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 12.dp))
        ChooserOption("🧾", "بيع", "أصناف من الرف بأسعار", outlined = true) { vm.openSale() }
        Spacer(Modifier.height(10.dp))
        ChooserOption("💵", "دفعة", "مبلغ على الرصيد") { vm.openPay() }
        Spacer(Modifier.height(10.dp))
        ChooserOption("↩️", "إرجاع", "قيمة تُعاد للرصيد") { vm.openReturn() }
    }
}

// ── return (إرجاع) ──
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReturnSheet(st: StoreState, vm: StoreViewModel) {
    Column(Modifier.fillMaxSize().riseFade(appearProgress(), riseDp = 460.dp, fade = false).background(cBg)) {
        SheetHeader("إرجاع جديد", onClose = vm::closeSheet)
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 15.dp)) {
            CustomerRow(st, vm)
            Spacer(Modifier.height(14.dp))
            Text("قيمة الإرجاع", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = cDim, modifier = Modifier.padding(start = 2.dp, bottom = 8.dp))
            Row(
                Modifier.fillMaxWidth().card(15.dp).padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically,
            ) {
                StepBtn("−", 42.dp, 12.dp, 1.5.dp, cLine, cAccent, 22.sp) { vm.returnAmountStep(-1) }
                Text(fmt(st.returnAmount), fontSize = 30.sp, fontWeight = FontWeight.Bold, color = cInk, textAlign = TextAlign.Center, modifier = Modifier.widthIn(min = 120.dp))
                StepBtn("+", 42.dp, 12.dp, 1.5.dp, cLine, cAccent, 22.sp) { vm.returnAmountStep(1) }
            }
            Text("الصنف المُعاد (اختياري) — يعود إلى الرف", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = cDim, modifier = Modifier.padding(start = 2.dp, top = 16.dp, bottom = 8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                st.shelf.take(10).forEach { x ->
                    val sel = st.returnItemId == x.id
                    Column(
                        Modifier.clip(RoundedCornerShape(11.dp)).background(if (sel) cAccent else cCard).border(1.5.dp, if (sel) cAccent else cLine, RoundedCornerShape(11.dp)).tap { vm.returnPickItem(x.id) }.padding(horizontal = 13.dp, vertical = 9.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(x.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (sel) cAink else cInk)
                        Text(fmt(x.tasira), fontSize = 11.sp, color = if (sel) cAink else cDim)
                    }
                }
            }
            Box(Modifier.fillMaxWidth().padding(top = 12.dp).clip(RoundedCornerShape(12.dp)).background(cGreenBg).border(1.dp, cGreenBorder, RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 10.dp)) {
                Text("تُخصم القيمة من دين الزبونة" + (if (st.returnItemId != null) " ويعود الصنف إلى الرف." else "."), fontSize = 12.sp, color = cPaid, lineHeight = 18.sp)
            }
        }
        SheetFooter("حفظ الإرجاع ✓", vm::saveReturn)
    }
}

// ── entry detail: view & void a past قيد ──
@Composable
private fun EntryDetailSheet(st: StoreState, vm: StoreViewModel) {
    val e = st.entries.find { it.id == st.detailEntryId } ?: return
    val amtColor = when (e.cls) { "pos" -> cPaid; "amber" -> cAmber; else -> cInk }
    BottomSheet(onDismiss = vm::closeEntry) {
        Text("القيد", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = cInk, modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 12.dp))
        Column(Modifier.fillMaxWidth().card(13.dp).padding(horizontal = 14.dp, vertical = 12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Text(e.t, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = cInk, modifier = Modifier.weight(1f, fill = false))
                Spacer(Modifier.width(10.dp))
                Text(e.amt, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = amtColor)
            }
            Text(e.d, fontSize = 12.sp, color = cDim, modifier = Modifier.padding(top = 4.dp))
        }

        // the sold items, each with a source chip to attribute where it came from
        val soldLines = decodeLines(e.lines)
        if (soldLines.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            Text("الأصناف المُباعة — اضغطي المصدر لتحديد من أين أتت", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = cDim, modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 8.dp))
            Column(Modifier.fillMaxWidth().card().padding(horizontal = 14.dp)) {
                soldLines.forEach { l ->
                    val item = st.shelf.find { it.id == l.shelfId }
                    val unspec = item == null || item.unspecified
                    val srcLabel = if (item != null && !item.unspecified) vm.sourceLabelFor(item.sourceId) else "غير محدد"
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 12.dp).drawBottomLine(),
                        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f, fill = false)) {
                            Text(l.name, fontSize = 14.5.sp, fontWeight = FontWeight.Bold, color = cInk)
                            Text("${fmt(l.price)} · ×${l.qty}", fontSize = 11.5.sp, color = cDim)
                        }
                        Spacer(Modifier.width(8.dp))
                        Row(
                            Modifier.clip(RoundedCornerShape(9.dp)).background(cBg).border(1.dp, if (unspec) cUnspecBorder else cLine, RoundedCornerShape(9.dp))
                                .then(if (item != null) Modifier.tap { vm.openSpecify(l.shelfId) } else Modifier)
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            if (unspec) Box(Modifier.size(7.dp).clip(RoundedCornerShape(50)).background(cDebt))
                            Text(if (item != null) "$srcLabel ✎" else srcLabel, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (unspec) cDebt else cDim)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).background(cCard).border(1.5.dp, cDebt, RoundedCornerShape(13.dp)).tap { vm.voidEntry(e.id) }.padding(vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text("إلغاء هذا القيد ↺", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = cDebt)
        }
        Text("يُعيد المبلغ والدين والبضاعة كما كانت.", fontSize = 11.5.sp, color = cDim, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 10.dp))
    }
}

// ── customer detail: balance + history + record a payment ──
@Composable
private fun CustomerDetailSheet(st: StoreState, vm: StoreViewModel) {
    val c = st.customers.find { it.id == st.detailCustomerId } ?: return
    val bal = customerBalance(c, st.entries)
    val trial = customerTrial(c, st.entries)
    val history = st.entries.filter { it.customerId == c.id }
    BottomSheet(onDismiss = vm::closeCustomer) {
        Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, bottom = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(c.name, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = cInk)
            Text(
                if (bal > 0) "عليها ${fmt(bal)}" else if (bal == 0L) "لا دين" else "لها ${fmt(-bal)}",
                fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (bal > 0) cDebt else cPaid,
            )
        }
        if (trial > 0) {
            Text("أمانة معها (قد تُعاد): ${fmt(trial)}", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = cAmber, modifier = Modifier.padding(start = 4.dp, bottom = 6.dp))
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(11.dp)).background(cCard).border(1.5.dp, cAmber, RoundedCornerShape(11.dp)).tap { vm.convertTrial(c.id) }.padding(vertical = 11.dp),
                contentAlignment = Alignment.Center,
            ) { Text("أبقتها — حوّلي الأمانة إلى بيع", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = cAmber) }
            Spacer(Modifier.height(4.dp))
        }
        c.phone?.let { Text("☎ $it", fontSize = 12.sp, color = cDim, modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)) }
        // reminder: due date + one-tap snooze (FR-3.2), only while she owes
        if (bal > 0) {
            val overdue = c.dueEpochDay != null && c.dueEpochDay < st.today
            Text("التذكير: ${dueStatus(c.dueEpochDay, st.today)}", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = if (overdue) cDebt else cInk, modifier = Modifier.padding(start = 4.dp, top = 6.dp, bottom = 6.dp))
            Text("ذكّريني بعد:", fontSize = 11.sp, color = cDim, modifier = Modifier.padding(start = 4.dp, bottom = 6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                SnoozeChip("أسبوع") { vm.snooze(c.id, 7) }
                SnoozeChip("أسبوعان") { vm.snooze(c.id, 14) }
                SnoozeChip("شهر") { vm.snooze(c.id, 30) }
            }
        }
        Spacer(Modifier.height(12.dp))
        if (history.isEmpty()) {
            Text("لا حركات مسجّلة بعد", fontSize = 13.sp, color = cDim, modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp))
        } else {
            Column(Modifier.fillMaxWidth().card().padding(horizontal = 14.dp)) {
                history.take(8).forEach { e ->
                    val amtColor = when (e.cls) { "pos" -> cPaid; "amber" -> cAmber; else -> cInk }
                    Row(Modifier.fillMaxWidth().padding(vertical = 11.dp).drawBottomLine(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f, fill = false)) {
                            Text(e.t, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = cInk)
                            Text(e.d, fontSize = 11.sp, color = cDim)
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(e.amt, fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = amtColor)
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        PrimaryButton("+ دفعة من ${c.name}", fontSize = 15.sp, radius = 13.dp, vertical = 14.dp) { vm.payThisCustomer(c.id) }
    }
}

@Composable
private fun ChooserOption(icon: String, title: String, sub: String, outlined: Boolean = false, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp)).background(cCard)
            .border(if (outlined) 1.5.dp else 1.dp, if (outlined) cAccent else cLine, RoundedCornerShape(15.dp))
            .tap(onClick).padding(15.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        Text(icon, fontSize = 23.sp)
        Column {
            Text(title, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = cInk)
            Text(sub, fontSize = 12.sp, color = cDim)
        }
    }
}

// ── payment (D37) ──
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PaySheet(st: StoreState, vm: StoreViewModel) {
    Column(Modifier.fillMaxSize().riseFade(appearProgress(), riseDp = 460.dp, fade = false).background(cBg)) {
        SheetHeader("دفعة جديدة", onClose = vm::closeSheet)
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 15.dp)) {
            CustomerRow(st, vm)
            Spacer(Modifier.height(14.dp))
            Text("كم المبلغ؟", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = cDim, modifier = Modifier.padding(start = 2.dp, bottom = 8.dp))
            Row(
                Modifier.fillMaxWidth().card(15.dp).padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically,
            ) {
                StepBtn("−", 42.dp, 12.dp, 1.5.dp, cLine, cAccent, 22.sp) { vm.payAmountStep(-1) }
                Text(fmt(st.payAmount), fontSize = 30.sp, fontWeight = FontWeight.Bold, color = cInk, textAlign = TextAlign.Center, modifier = Modifier.widthIn(min = 120.dp))
                StepBtn("+", 42.dp, 12.dp, 1.5.dp, cLine, cAccent, 22.sp) { vm.payAmountStep(1) }
            }
            Text("نوع (اختياري) — يُسند المبلغ لمصدره", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = cDim, modifier = Modifier.padding(start = 2.dp, top = 16.dp, bottom = 8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                st.shelf.filter { it.onHand > 0 }.take(8).forEach { x ->
                    val sel = st.payTypeId == x.id
                    Column(
                        Modifier.clip(RoundedCornerShape(11.dp)).background(if (sel) cAccent else cCard).border(1.5.dp, if (sel) cAccent else cLine, RoundedCornerShape(11.dp)).tap { vm.payPickType(x.id) }.padding(horizontal = 13.dp, vertical = 9.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(x.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (sel) cAink else cInk)
                        Text(fmt(x.tasira), fontSize = 11.sp, color = if (sel) cAink else cDim)
                    }
                }
            }
            if (st.payTypeId != null) {
                val name = st.shelf.find { it.id == st.payTypeId }?.name ?: ""
                Box(Modifier.fillMaxWidth().padding(top = 12.dp).clip(RoundedCornerShape(12.dp)).background(cGreenBg).border(1.dp, cGreenBorder, RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 10.dp)) {
                    Text("مبلغ $name يُحسب إيراداً لمصدر الصنف — ويُنقص قطعة من الرف.", fontSize = 12.sp, color = cPaid, lineHeight = 18.sp)
                }
            }
        }
        SheetFooter("حفظ ✓", vm::savePay)
    }
}

// ── sale ──
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SaleSheet(st: StoreState, vm: StoreViewModel) {
    Column(Modifier.fillMaxSize().riseFade(appearProgress(), riseDp = 460.dp, fade = false).background(cBg)) {
        SheetHeader("بيع جديد", onClose = vm::closeSheet)
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 13.dp, bottom = 8.dp)) {
            CustomerRow(st, vm)
            Spacer(Modifier.height(13.dp))
            Text("مقترحة من الرف — اضغطي لإضافتها", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = cDim, modifier = Modifier.padding(start = 2.dp, bottom = 9.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                st.shelf.filter { it.onHand > 0 }.sortedByDescending { it.onHand }.forEach { g ->
                    Column(
                        Modifier.widthIn(min = 64.dp).clip(RoundedCornerShape(11.dp)).background(cCard).border(1.5.dp, cLine, RoundedCornerShape(11.dp)).tap { vm.addLine(g.id) }.padding(horizontal = 13.dp, vertical = 9.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(g.name, fontSize = 14.5.sp, fontWeight = FontWeight.Bold, color = cInk)
                        Text("${fmt(g.tasira)} · ${g.onHand}", fontSize = 11.sp, color = cDim, modifier = Modifier.padding(top = 1.dp))
                    }
                }
                Box(
                    Modifier.clip(RoundedCornerShape(11.dp)).dashedBorder(cLine, 11.dp, 1.5.dp).tap { vm.toggleAddNew() }.padding(horizontal = 13.dp, vertical = 9.dp),
                    contentAlignment = Alignment.Center,
                ) { Text("+ صنف جديد", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = cDim) }
            }
            if (st.addNewOpen) {
                Column(Modifier.fillMaxWidth().padding(top = 11.dp).clip(RoundedCornerShape(13.dp)).background(cCard).border(1.5.dp, cAccent, RoundedCornerShape(13.dp)).padding(horizontal = 13.dp, vertical = 12.dp)) {
                    TextInput(st.newName, vm::setNewName, "اسم الصنف", modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(9.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("السعر", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = cDim)
                        LabeledStepper("", fmt(st.newPrice), { vm.newPriceStep(-1) }, { vm.newPriceStep(1) }, btnSize = 28.dp, valueMin = 58.dp)
                    }
                    Text("سيُضاف للرف كـ«غير محدد» (نقطة حمراء) تحلّينه لاحقاً", fontSize = 11.sp, color = cAmber, modifier = Modifier.padding(top = 9.dp))
                    Spacer(Modifier.height(11.dp))
                    PrimaryButton("أضيفي للسلة", fontSize = 14.sp, radius = 11.dp, vertical = 11.dp) { vm.addNewItem() }
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
                        Text("المجموع", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = cInk)
                        Text(fmt(st.lines.sumOf { it.price * it.qty }), fontSize = 21.sp, fontWeight = FontWeight.Bold, color = cInk)
                    }
                }
            }
            Text("كيف دفعت؟", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = cDim, modifier = Modifier.padding(start = 2.dp, top = 16.dp, bottom = 8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                PayModeBtn("دفعت الكل", st.pay == "full", Modifier.weight(1f)) { vm.setPay("full") }
                PayModeBtn("دفعت جزءاً", st.pay == "partial", Modifier.weight(1f)) { vm.setPay("partial") }
                PayModeBtn("أمانة", st.pay == "trial", Modifier.weight(1f)) { vm.setPay("trial") }
            }
            Spacer(Modifier.height(8.dp))
        }
        SheetFooter("حفظ ✓", vm::saveSale)
    }
}

@Composable
private fun SaleLineRow(i: Int, l: SaleLine, vm: StoreViewModel) {
    Column(Modifier.fillMaxWidth().padding(vertical = 11.dp).drawBottomLine()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f, fill = false)) {
                Text(l.name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = cInk)
                if (l.haggled) Text(fmt(l.tasira), fontSize = 11.sp, color = cDim, textDecoration = TextDecoration.LineThrough, modifier = Modifier.padding(horizontal = 6.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                StepBtn("−", 28.dp, 8.dp, 1.5.dp, cLine, cAccent, 17.sp) { vm.priceStep(i, -1) }
                Text(fmt(l.price), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = cInk, textAlign = TextAlign.Center, modifier = Modifier.widthIn(min = 52.dp))
                StepBtn("+", 28.dp, 8.dp, 1.5.dp, cLine, cAccent, 17.sp) { vm.priceStep(i, 1) }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 7.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            Text("الكمية", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = cDim)
            StepBtn("−", 23.dp, 7.dp, 1.dp, cLine, cDim, 14.sp) { vm.qtyStep(i, -1) }
            Text("×${l.qty}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = cInk, textAlign = TextAlign.Center, modifier = Modifier.widthIn(min = 20.dp))
            StepBtn("+", 23.dp, 7.dp, 1.dp, cLine, cDim, 14.sp) { vm.qtyStep(i, 1) }
        }
    }
}

@Composable
private fun PayModeBtn(label: String, active: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier.clip(RoundedCornerShape(12.dp)).background(if (active) cAccent else cCard).border(1.5.dp, if (active) cAccent else cLine, RoundedCornerShape(12.dp)).tap(onClick).padding(vertical = 11.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center,
    ) { Text(label, fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = if (active) cAink else cDim) }
}

// ── specify source ──
@Composable
private fun SpecifySheet(st: StoreState, vm: StoreViewModel) {
    BottomSheet(onDismiss = vm::closeSpecify) {
        Text("من أي مصدر؟", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = cInk, modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 4.dp))
        Text("توجيه القطع لأي مصدر — حتى المنفَذ — يزيد عدّاداته.", fontSize = 12.sp, color = cDim, modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 12.dp))
        st.sources.forEach { srcOpt ->
            Row(
                Modifier.fillMaxWidth().padding(bottom = 8.dp).card(12.dp).tap { vm.pickSource(srcOpt.id) }.padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("📦 ${srcOpt.label}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = cInk)
                Text(srcOpt.kind.label, fontSize = 11.sp, color = cDim)
            }
        }
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(cCard).dashedBorder(cLine, 12.dp).tap { vm.pickSource("none") }.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(Modifier.size(8.dp).clip(RoundedCornerShape(50)).background(cDebt))
            Text("غير محدد — لاحقاً", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = cDebt)
        }
    }
}

// ── package (count & shelve) ──
@Composable
private fun PackageSheet(st: StoreState, vm: StoreViewModel) {
    val pkgLabel = st.sources.find { it.id == st.pkgId }?.label ?: ""
    val items = st.shelf.filter { it.sourceId == st.pkgId }
    Column(Modifier.fillMaxSize().riseFade(appearProgress(), riseDp = 460.dp, fade = false).background(cBg)) {
        SheetHeader("📦 $pkgLabel", onClose = vm::closePackage, back = vm::closePackage)
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 14.dp)) {
            Text("عُدّي أصناف البالة وارفعيها على الرف — كلها أو جزءاً. ما يبقى «في البالة» تُنزلينه لاحقاً.", fontSize = 12.sp, color = cDim, lineHeight = 18.sp, modifier = Modifier.padding(start = 2.dp, end = 2.dp, bottom = 12.dp))
            Column(Modifier.fillMaxWidth().card().padding(horizontal = 14.dp)) {
                items.forEach { p -> PackageItemRow(p, vm) }
                Box(Modifier.fillMaxWidth().tap { vm.togglePkgAdd() }.padding(top = 11.dp, bottom = 7.dp), contentAlignment = Alignment.Center) {
                    Text("+ عدّ صنف في البالة", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = cAccent)
                }
            }
            if (st.pkgAddOpen) {
                Column(Modifier.fillMaxWidth().padding(top = 11.dp).clip(RoundedCornerShape(13.dp)).background(cCard).border(1.5.dp, cAccent, RoundedCornerShape(13.dp)).padding(horizontal = 13.dp, vertical = 12.dp)) {
                    TextInput(st.aiName, vm::setAiName, "اسم الصنف", modifier = Modifier.fillMaxWidth())
                    Row(Modifier.fillMaxWidth().padding(top = 9.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("التسعيرة", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = cDim)
                        LabeledStepper("", fmt(st.aiTasira), { vm.aiTasiraStep(-1) }, { vm.aiTasiraStep(1) }, btnSize = 26.dp, valueMin = 56.dp, valueSize = 15.sp, btnFont = 16.sp)
                    }
                    Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("العدد المعدود", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = cDim)
                        LabeledStepper("", "${st.aiCount}", { vm.aiCountStep(-1) }, { vm.aiCountStep(1) }, btnSize = 26.dp, valueMin = 32.dp, valueSize = 15.sp, btnFont = 16.sp)
                    }
                    Spacer(Modifier.height(11.dp))
                    PrimaryButton("عدّ (يبقى في البالة) ✓", fontSize = 14.sp, radius = 11.dp, vertical = 11.dp) { vm.savePkgCount() }
                }
            }
        }
    }
}

@Composable
private fun PackageItemRow(p: Shelf, vm: StoreViewModel) {
    Column(Modifier.fillMaxWidth().padding(vertical = 12.dp).drawBottomLine()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(p.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = cInk)
            Text(fmt(p.tasira), fontSize = 12.sp, color = cDim)
        }
        Row(Modifier.fillMaxWidth().padding(top = 9.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("في البالة: ${p.inPkg}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = cAmber)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("على الرف", fontSize = 11.5.sp, color = cDim)
                StepBtn("−", 26.dp, 8.dp, 1.5.dp, cLine, cAccent, 16.sp) { vm.shelveStep(p.id, -1) }
                Text("${p.shelved}", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = cInk, textAlign = TextAlign.Center, modifier = Modifier.widthIn(min = 28.dp))
                StepBtn("+", 26.dp, 8.dp, 1.5.dp, cLine, cAccent, 16.sp) { vm.shelveStep(p.id, 1) }
            }
        }
        if (p.inPkg > 0) {
            Text("رفّ الكل (${p.inPkg}) ←", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = cAccent, modifier = Modifier.padding(top = 7.dp).tap { vm.shelveAll(p.id) })
        } else {
            Text("الكل على الرف ✓", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = cPaid, modifier = Modifier.padding(top = 7.dp))
        }
    }
}

// ── add item to shelf ──
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddItemSheet(st: StoreState, vm: StoreViewModel) {
    Column(Modifier.fillMaxSize().riseFade(appearProgress(), riseDp = 460.dp, fade = false).background(cBg)) {
        SheetHeader("إضافة صنف للرف", onClose = vm::closeAddItem)
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 15.dp)) {
            Text("الصنف", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = cDim, modifier = Modifier.padding(start = 2.dp, bottom = 6.dp))
            TextInput(st.aiName, vm::setAiName, "مثال: فستان", modifier = Modifier.fillMaxWidth(), bg = cCard, radius = 12.dp, fontSize = 15.sp)
            Spacer(Modifier.height(11.dp))
            CardStepperRow("التسعيرة", fmt(st.aiTasira), { vm.aiTasiraStep(-1) }, { vm.aiTasiraStep(1) })
            Spacer(Modifier.height(8.dp))
            CardStepperRow("العدد على الرف", "${st.aiCount}", { vm.aiCountStep(-1) }, { vm.aiCountStep(1) })
            Text("من أين؟", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = cDim, modifier = Modifier.padding(start = 2.dp, top = 16.dp, bottom = 8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                st.sources.forEach { srcOpt ->
                    val sel = st.aiSource == srcOpt.id
                    SourcePickChip(srcOpt.label, sel, false) { vm.aiPickSource(srcOpt.id) }
                }
                SourcePickChip("غير محدد", st.aiSource == "none", true) { vm.aiPickSource("none") }
            }
            if (st.aiSource == MKT_ID) {
                Spacer(Modifier.height(11.dp))
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(cAmberBg).border(1.dp, cAmberBorder, RoundedCornerShape(12.dp)).padding(horizontal = 13.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("سعر الشراء للقطعة", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = cAmber)
                    LabeledStepper("", fmt(st.aiBuy), { vm.aiBuyStep(-1) }, { vm.aiBuyStep(1) }, borderColor = cAmberBorder, btnColor = cAmber)
                }
                Text("شراء من السوق يُسجَّل لكل قطعة — الكلفة تتجمّع في المصدر.", fontSize = 11.sp, color = cAmber, modifier = Modifier.padding(top = 7.dp))
            }
            Text("بضاعتك الموجودة الآن؟ اتركيها على «قبل التطبيق» — بلا كلفة ولا ربح. ما لا تعرفين مصدره اجعليه «غير محدد» تحلّينه لاحقاً.", fontSize = 11.5.sp, color = cDim, lineHeight = 18.sp, modifier = Modifier.padding(top = 12.dp))
        }
        SheetFooter("أضيفي للرف ✓", vm::saveAiItem)
    }
}

@Composable
private fun SourcePickChip(label: String, selected: Boolean, unspec: Boolean, onClick: () -> Unit) {
    val bg = if (selected) (if (unspec) cDebt else cAccent) else cCard
    val col = if (selected) (if (unspec) Color.White else cAink) else (if (unspec) cDebt else cInk)
    val border = if (selected) (if (unspec) cDebt else cAccent) else (if (unspec) cUnspecBorder else cLine)
    Box(Modifier.clip(RoundedCornerShape(11.dp)).background(bg).border(1.5.dp, border, RoundedCornerShape(11.dp)).tap(onClick).padding(horizontal = 13.dp, vertical = 9.dp)) {
        Text(label, fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = col)
    }
}

// ── add source ──
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddSourceSheet(st: StoreState, vm: StoreViewModel) {
    BottomSheet(onDismiss = vm::closeAddSource) {
        Text("مصدر جديد — نوعه؟", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = cInk, modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 12.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 14.dp)) {
            val sel = st.addSrcKind == Kind.BALE
            Box(Modifier.clip(RoundedCornerShape(11.dp)).background(if (sel) cAccent else cCard).border(1.5.dp, if (sel) cAccent else cLine, RoundedCornerShape(11.dp)).tap { vm.pickKind(Kind.BALE) }.padding(horizontal = 14.dp, vertical = 10.dp)) {
                Text(Kind.BALE.label, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (sel) cAink else cInk)
            }
        }
        Row(
            Modifier.fillMaxWidth().card(12.dp).padding(horizontal = 13.dp, vertical = 11.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("التكلفة (USD)", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = cDim)
            LabeledStepper("", "$" + fmt(st.newCost), { vm.costStep(-1) }, { vm.costStep(1) }, valueMin = 64.dp, valueSize = 18.sp)
        }
        Spacer(Modifier.height(12.dp))
        PrimaryButton("أضيفي المصدر ✓", fontSize = 15.sp, radius = 14.dp, vertical = 14.dp) { vm.saveSource() }
    }
}

// ── undo toast ──
@Composable
private fun UndoToast(vm: StoreViewModel) {
    Box(Modifier.fillMaxSize().navigationBarsPadding()) {
        SlideUp(Modifier.align(Alignment.BottomCenter), bouncy = true) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(bottom = 74.dp)
                .clip(RoundedCornerShape(13.dp)).background(cInk).padding(horizontal = 16.dp, vertical = 13.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("تم تسجيل البيع — أُنقص من الرف", fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = cCard)
            Text("↺ تراجع", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = cUndoAccent, modifier = Modifier.tap { vm.undoSale() })
        }
        }
    }
}

// ── onboarding (first run) ──
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun Onboarding(st: StoreState, vm: StoreViewModel) {
    val isSetup = st.onb >= 3
    Column(Modifier.fillMaxSize().background(cBg).statusBarsPadding().navigationBarsPadding().padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 22.dp)) {
        Row(Modifier.fillMaxWidth().padding(bottom = 6.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("دفتر", fontFamily = Amiri, fontWeight = FontWeight.Bold, fontSize = 24.sp, color = cDebt)
            Text("تخطّي", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = cDim, modifier = Modifier.tap { vm.skipOnb() })
        }
        if (!isSetup) {
            val card = ONB[minOf(st.onb, 2)]
            Column(Modifier.weight(1f).fillMaxWidth().riseFade(appearProgress(key = st.onb), riseDp = 16.dp, fromScale = 0.98f), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Text(card.icon, fontSize = 74.sp, modifier = Modifier.padding(bottom = 22.dp))
                Text(card.eyebrow, fontSize = 13.sp, letterSpacing = 0.5.sp, fontWeight = FontWeight.Bold, color = cDebt, modifier = Modifier.padding(bottom = 8.dp))
                Text(card.title, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = cInk, textAlign = TextAlign.Center, modifier = Modifier.padding(bottom = 14.dp))
                Text(card.body, fontSize = 15.sp, color = cDim, lineHeight = 26.sp, textAlign = TextAlign.Center, modifier = Modifier.widthIn(max = 280.dp))
            }
        } else {
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
                Text("🧺", fontSize = 52.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 12.dp))
                Text("جهّزي محلّك", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = cInk, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))
                Text("أضيفي البضاعة الموجودة عندك الآن — تدخل الرف تحت «قبل التطبيق». ما لا تعرفين مصدره يبقى «غير محدد» تحلّينه لاحقاً.", fontSize = 13.5.sp, color = cDim, lineHeight = 21.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().widthIn(max = 290.dp).padding(bottom = 16.dp))
                Text("اضغطي على الأصناف التي عندك:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = cDim, modifier = Modifier.padding(start = 2.dp, end = 2.dp, bottom = 8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 16.dp)) {
                    SETUP_CHIPS.forEach { c ->
                        val on = st.setupList.any { it.name == c.name }
                        Row(
                            Modifier.clip(RoundedCornerShape(11.dp)).background(if (on) cAccent else cCard).border(1.5.dp, if (on) cAccent else cLine, RoundedCornerShape(11.dp)).tap { vm.setupAdd(c.name, c.price) }.padding(horizontal = 13.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            Text(c.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (on) cAink else cInk)
                            Text(fmt(c.price), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = if (on) cAink else cInk)
                        }
                    }
                }
                if (st.setupList.isNotEmpty()) {
                    Column(Modifier.fillMaxWidth().card(13.dp).padding(horizontal = 14.dp, vertical = 6.dp)) {
                        Text("على الرف الآن (${st.setupList.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = cDim, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
                        st.setupList.forEach { sPick ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 9.dp).drawTopLine(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(sPick.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = cInk)
                                Text("${fmt(sPick.price)} · ×${sPick.qty}", fontSize = 13.sp, color = cDim)
                            }
                        }
                    }
                }
            }
        }
        // footer: dots + primary + sample
        Column(Modifier.fillMaxWidth().padding(top = 14.dp)) {
            Row(Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(7.dp, Alignment.CenterHorizontally)) {
                val active = minOf(st.onb, 3)
                (0..3).forEach { i -> Box(Modifier.size(7.dp).clip(RoundedCornerShape(50)).background(if (i == active) cDebt else cLine)) }
            }
            if (isSetup) {
                PrimaryButton("دخول المحل ✓", fontSize = 16.sp, radius = 14.dp) { vm.enterApp() }
            } else {
                PrimaryButton(if (st.onb < 2) "التالي ‹" else "جهّزي محلّك ‹", fontSize = 16.sp, radius = 14.dp) { vm.onbNext() }
            }
            Box(Modifier.fillMaxWidth().padding(top = 12.dp).tap { vm.loadSample() }, contentAlignment = Alignment.Center) {
                Text("أو املئي ببيانات تجريبية ⟲", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = cDebt)
            }
        }
    }
}
