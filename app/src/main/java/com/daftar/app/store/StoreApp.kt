package com.daftar.app.store

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.daftar.app.kernel.theme.Amiri
import com.daftar.app.kernel.theme.Plex

@Composable
fun StoreApp(vm: StoreViewModel = hiltViewModel()) {
    val st by vm.state.collectAsState()
    // Back closes an open sheet first, then falls back to اليوم; only اليوم exits the app.
    val overlayOpen = st.screen != "home" || st.specifyId != null
    androidx.activity.compose.BackHandler(enabled = st.seeded && (overlayOpen || st.tab != "today")) {
        when {
            st.specifyId != null -> vm.closeSpecify()
            st.screen != "home" -> vm.closeSheet()
            else -> vm.setTab("today")
        }
    }
    CompositionLocalProvider(
        LocalTextStyle provides TextStyle(fontFamily = Plex, color = cInk),
    ) {
        Box(Modifier.fillMaxSize().background(cBg)) {
            Column(Modifier.fillMaxSize()) {
                AppBar(st)
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 8.dp),
                ) {
                    when (st.tab) {
                        "today" -> TodayScreen(st, vm)
                        "cust" -> CustScreen(st, vm)
                        "appts" -> ApptsScreen(st)
                        "account" -> AccountScreen(st, vm)
                    }
                }
                if (st.tab == "today") {
                    Box(Modifier.fillMaxWidth().background(cBg).padding(start = 16.dp, end = 16.dp, top = 9.dp, bottom = 6.dp)) {
                        PrimaryButton("+ قيد جديد", fontSize = 17.sp) { vm.openChooser() }
                    }
                }
                TabBar(st, vm)
            }
            StoreSheets(st, vm)
            if (!st.seeded) Onboarding(st, vm)
        }
    }
}

@Composable
private fun AppBar(st: StoreState) {
    val title = when (st.tab) {
        "today" -> "دفتر اليوم"; "cust" -> "الزبائن"; "appts" -> "المواعيد"; else -> "الحساب"
    }
    val aside = if (st.tab == "today") dayLabel(st.viewedDay, st.today) else ""
    Column(Modifier.fillMaxWidth().background(cBg).statusBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("دفتر", fontFamily = Amiri, fontWeight = FontWeight.Bold, fontSize = 21.sp, color = cDebt)
            Text(title, fontSize = 14.5.sp, fontWeight = FontWeight.Bold, color = cInk)
            Text(aside, fontSize = 12.sp, color = cDim, textAlign = TextAlign.End, modifier = Modifier.widthIn(min = 34.dp))
        }
        HorizontalDivider(color = cLine, thickness = 1.dp)
    }
}

@Composable
private fun TabBar(st: StoreState, vm: StoreViewModel) {
    val unspec = st.shelf.count { it.unspecified }
    Column(Modifier.fillMaxWidth().background(cCard)) {
        HorizontalDivider(color = cLine, thickness = 1.dp)
        Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 6.dp)) {
            TabItem("▤", "اليوم", st.tab == "today", Modifier.weight(1f)) { vm.setTab("today") }
            TabItem("☰", "الزبائن", st.tab == "cust", Modifier.weight(1f)) { vm.setTab("cust") }
            TabItem("◔", "المواعيد", st.tab == "appts", Modifier.weight(1f)) { vm.setTab("appts") }
            TabItem("▦", "الحساب", st.tab == "account", Modifier.weight(1f), dot = unspec > 0) { vm.setTab("account") }
        }
    }
}

@Composable
private fun TabItem(glyph: String, label: String, active: Boolean, modifier: Modifier, dot: Boolean = false, onClick: () -> Unit) {
    val col = if (active) cDebt else cDim
    Box(modifier.tap(onClick).padding(top = 9.dp, bottom = 6.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(glyph, fontSize = 18.sp, color = col)
            Text(label, fontSize = 11.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Normal, color = col, modifier = Modifier.padding(top = 1.dp))
        }
        if (dot) {
            Box(
                Modifier.align(Alignment.TopCenter).padding(start = 44.dp).size(8.dp)
                    .clip(RoundedCornerShape(50)).background(cDebt),
            )
        }
    }
}

// ── اليوم ──
@Composable
private fun TodayScreen(st: StoreState, vm: StoreViewModel) {
    val isToday = st.viewedDay == st.today
    val dayEntries = entriesForDay(st.entries, st.viewedDay)
    val salesLabel = if (isToday) "مبيعات اليوم" else "المبيعات"
    val cashLabel = if (isToday) "قبضنا اليوم" else "المقبوضات"
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StatCard(salesLabel, fmt(salesForDay(st.entries, st.viewedDay)), cInk, Modifier.weight(1f))
        StatCard(cashLabel, fmt(cashForDay(st.entries, st.viewedDay)), cPaid, Modifier.weight(1f))
    }
    // day navigation: ‹ older · date · newer › (never past today)
    Row(
        Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
    ) {
        DayNavArrow("‹", enabled = true) { vm.dayStep(-1) }
        Text(dayLabel(st.viewedDay, st.today), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = cInk)
        DayNavArrow("›", enabled = !isToday) { vm.dayStep(1) }
    }
    if (dayEntries.isEmpty()) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(cCard)
                .dashedBorder(cLine, 14.dp).padding(vertical = 32.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("📃", fontSize = 30.sp, modifier = Modifier.padding(bottom = 8.dp))
            Text(
                if (isToday) "لا قيود بعد — ابدئي بأول عملية بيع" else "لا حركات في هذا اليوم",
                fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = cDim,
            )
        }
    } else {
        Box(
            Modifier.fillMaxWidth().card().drawBehind {
                val x = if (layoutDirection == LayoutDirection.Ltr) 32.dp.toPx() else size.width - 32.dp.toPx()
                drawLine(
                    color = cDebt.copy(alpha = 0.28f),
                    start = androidx.compose.ui.geometry.Offset(x, 0f),
                    end = androidx.compose.ui.geometry.Offset(x, size.height),
                    strokeWidth = 1.5.dp.toPx(),
                )
            }.padding(horizontal = 14.dp),
        ) {
            Column {
                dayEntries.forEach { e -> EntryRow(e) }
            }
        }
    }
}

@Composable
private fun DayNavArrow(sym: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.size(32.dp).clip(RoundedCornerShape(9.dp))
            .background(cCard).border(1.dp, cLine, RoundedCornerShape(9.dp))
            .then(if (enabled) Modifier.tap(onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Text(sym, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = if (enabled) cAccent else cLine)
    }
}

@Composable
private fun EntryRow(e: DayEntry) {
    val amtColor = when (e.cls) { "pos" -> cPaid; "amber" -> cAmber; else -> cInk }
    Row(
        Modifier.fillMaxWidth().padding(vertical = 12.dp).padding(start = 26.dp)
            .drawBottomLine(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Column(Modifier.weight(1f, fill = false)) {
            Text(e.t, fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold, color = cInk, lineHeight = 20.sp)
            Text(e.d, fontSize = 11.5.sp, color = cDim, modifier = Modifier.padding(top = 3.dp))
        }
        Spacer(Modifier.width(10.dp))
        Text(e.amt, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = amtColor)
    }
}

@Composable
private fun StatCard(label: String, value: String, valueColor: Color, modifier: Modifier) {
    Column(modifier.card().padding(horizontal = 13.dp, vertical = 12.dp)) {
        Text(label, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = cDim)
        Text(value, fontSize = 23.sp, fontWeight = FontWeight.Bold, color = valueColor, modifier = Modifier.padding(top = 3.dp))
    }
}

// ── الزبائن ──
@Composable
private fun CustScreen(st: StoreState, vm: StoreViewModel) {
    val totalOwed = st.customers.sumOf { maxOf(0, customerBalance(it, st.entries)) }
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(cBg).border(1.dp, cLine, RoundedCornerShape(12.dp)).padding(horizontal = 13.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("إجمالي الديون للمحل", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = cDim)
        Text(fmt(totalOwed), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = cDebt)
    }
    Spacer(Modifier.height(12.dp))
    if (st.customers.isEmpty()) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(cCard).dashedBorder(cLine, 14.dp).padding(vertical = 28.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("👤", fontSize = 28.sp, modifier = Modifier.padding(bottom = 8.dp))
            Text("لا زبائن بعد — أضيفي أول زبونة", fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = cDim)
        }
    } else {
        Column(Modifier.fillMaxWidth().card().padding(horizontal = 14.dp)) {
            st.customers.forEach { c ->
                val bal = customerBalance(c, st.entries)
                val amt = if (bal > 0) fmt(bal) else if (bal == 0L) "لا شيء" else "لها ${fmt(-bal)}"
                StaticListRow(c.name, c.phone ?: "زبونة", amt, if (bal > 0) cDebt else cPaid)
            }
        }
    }
    Spacer(Modifier.height(12.dp))
    OutlineButton("+ زبونة جديدة", fontSize = 14.sp, radius = 13.dp, vertical = 13.dp, filledCard = true) { vm.openAddCustomer() }
}

// ── المواعيد ──
@Composable
private fun ApptsScreen(st: StoreState) {
    val ds = debtors(st.customers, st.entries)
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(cGreenBg).border(1.dp, cGreenBorder, RoundedCornerShape(12.dp)).padding(horizontal = 13.dp, vertical = 12.dp)) {
        Text(
            if (ds.isEmpty()) "☀️ صباح الخير — لا ديون مستحقة، كل شيء مسدَّد"
            else "🔔 صباح الخير — ${ds.size} زبائن عليهن ديون اليوم",
            fontSize = 12.5.sp, color = cPaid,
        )
    }
    Spacer(Modifier.height(12.dp))
    if (ds.isEmpty()) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(cCard).dashedBorder(cLine, 14.dp).padding(vertical = 28.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("👍", fontSize = 28.sp, modifier = Modifier.padding(bottom = 8.dp))
            Text("لا ديون مستحقة الآن", fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = cDim)
        }
    } else {
        Column(Modifier.fillMaxWidth().card().padding(horizontal = 14.dp)) {
            ds.forEach { d -> StaticListRow(d.customer.name, d.customer.phone ?: "دين مستحق", fmt(d.balance), cDebt) }
        }
    }
}

@Composable
private fun StaticListRow(name: String, sub: String, amt: String, amtColor: Color, amtBold: Boolean = true) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 13.dp).drawBottomLine(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = cInk)
            Text(sub, fontSize = 11.5.sp, color = cDim)
        }
        Text(amt, fontSize = if (amtBold) 12.5.sp else 13.sp, fontWeight = FontWeight.Bold, color = amtColor)
    }
}

// ── الحساب ──
@Composable
private fun AccountScreen(st: StoreState, vm: StoreViewModel) {
    // segment switcher
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(cBg).border(1.dp, cLine, RoundedCornerShape(12.dp)).padding(3.dp)) {
        SegBtn("البضاعة", st.accountSeg == "shelf", Modifier.weight(1f)) { vm.setSeg("shelf") }
        SegBtn("المصادر", st.accountSeg == "sources", Modifier.weight(1f)) { vm.setSeg("sources") }
        SegBtn("الملخّص", st.accountSeg == "sum", Modifier.weight(1f)) { vm.setSeg("sum") }
    }
    Spacer(Modifier.height(14.dp))
    when (st.accountSeg) {
        "shelf" -> ShelfSeg(st, vm)
        "sources" -> SourcesSeg(st, vm)
        else -> SummarySeg(st, vm)
    }
}

@Composable
private fun SegBtn(label: String, active: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier.clip(RoundedCornerShape(9.dp)).background(if (active) cAccent else cCard).tap(onClick).padding(vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (active) cAink else cDim)
    }
}

@Composable
private fun ShelfSeg(st: StoreState, vm: StoreViewModel) {
    val unspec = st.shelf.count { it.unspecified }
    Text("رفّك — ما لديك للبيع. البيع يقترح من هنا فقط.", fontSize = 12.sp, color = cDim, lineHeight = 17.sp, modifier = Modifier.padding(bottom = 10.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.padding(bottom = 12.dp)) {
        FilterChip("الكل", st.shelfFilter == "all") { vm.setFilter("all") }
        FilterChip("غير محدد ($unspec)", st.shelfFilter == "unspec", dot = true) { vm.setFilter("unspec") }
    }
    val rows = if (st.shelfFilter == "unspec") st.shelf.filter { it.unspecified } else st.shelf
    Column(Modifier.fillMaxWidth().card().padding(horizontal = 14.dp)) {
        rows.forEach { r -> ShelfRow(r, vm) }
        Row(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.weight(1f)) { PrimaryButton("+ صنف للرف", fontSize = 13.5.sp, radius = 11.dp, vertical = 11.dp) { vm.openAddItem() } }
            Box(Modifier.weight(1f)) { OutlineButton("+ بالة", fontSize = 13.5.sp, radius = 11.dp, vertical = 11.dp) { vm.openAddSource() } }
        }
    }
}

@Composable
private fun ShelfRow(r: Shelf, vm: StoreViewModel) {
    val oh = r.onHand
    val onHandColor = if (oh < 0) cDebt else if (oh == 0) cDim else cInk
    Column(Modifier.fillMaxWidth().padding(vertical = 12.dp).drawBottomLine()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(r.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = cInk, modifier = Modifier.weight(1f, fill = false))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("التسعيرة", fontSize = 10.5.sp, color = cDim)
                StepBtn("−", 26.dp, 8.dp, 1.5.dp, cLine, cAccent, 16.sp) { vm.tasiraStep(r.id, -1) }
                Text(fmt(r.tasira), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = cInk, textAlign = TextAlign.Center, modifier = Modifier.widthIn(min = 52.dp))
                StepBtn("+", 26.dp, 8.dp, 1.5.dp, cLine, cAccent, 16.sp) { vm.tasiraStep(r.id, 1) }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 9.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("على الرف", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = cDim)
                StepBtn("−", 24.dp, 7.dp, 1.dp, cLine, cDim, 14.sp) { vm.onhandStep(r.id, -1) }
                Text("$oh", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = onHandColor, textAlign = TextAlign.Center, modifier = Modifier.widthIn(min = 26.dp))
                StepBtn("+", 24.dp, 7.dp, 1.dp, cLine, cDim, 14.sp) { vm.onhandStep(r.id, 1) }
            }
            val provBorder = if (r.unspecified) cUnspecBorder else cLine
            val provColor = if (r.unspecified) cDebt else cDim
            Row(
                Modifier.clip(RoundedCornerShape(9.dp)).background(cBg).border(1.dp, provBorder, RoundedCornerShape(9.dp)).tap { vm.openSpecify(r.id) }.padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (r.unspecified) Box(Modifier.size(7.dp).clip(RoundedCornerShape(50)).background(cDebt))
                val label = if (r.unspecified) "غير محدد" else (vm.sourceLabelFor(r.sourceId))
                Text("$label ✎", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = provColor)
            }
        }
        if (oh < 0) {
            Row(
                Modifier.fillMaxWidth().padding(top = 9.dp).clip(RoundedCornerShape(10.dp)).background(cAmberBg).border(1.dp, cAmberBorder, RoundedCornerShape(10.dp)).tap { vm.reconcile(r.id) }.padding(horizontal = 11.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("بيع ${r.sold} · معدود ${r.shelved} — أعيدي العدّ؟", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = cAmber, modifier = Modifier.weight(1f, fill = false))
                Text("توفيق ←", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = cAccent)
            }
        }
    }
}

@Composable
private fun SourcesSeg(st: StoreState, vm: StoreViewModel) {
    Text("من أين أتت البضاعة وكم كلّفت — لتري أي مصدر ربح.", fontSize = 12.sp, color = cDim, lineHeight = 17.sp, modifier = Modifier.padding(bottom = 12.dp))
    sourceViews(st.sources, st.shelf).forEach { sv -> SourceCard(sv, vm) }
    OutlineButton("+ مصدر جديد", fontSize = 14.sp, radius = 13.dp, vertical = 13.dp, filledCard = true) { vm.openAddSource() }
}

@Composable
private fun SourceCard(sv: SourceView, vm: StoreViewModel) {
    val stripe = if (sv.kindLabel == Kind.PRE_APP.label) cDim
    else if (sv.profit != null && sv.profit >= 0) cPaid else cDebt
    Box(
        Modifier.fillMaxWidth().padding(bottom = 10.dp).card().drawBehind {
            val w = 3.dp.toPx()
            val x = if (layoutDirection == LayoutDirection.Ltr) 0f else size.width - w
            drawRect(stripe, androidx.compose.ui.geometry.Offset(x, 0f), androidx.compose.ui.geometry.Size(w, size.height))
        },
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 13.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(sv.label, fontSize = 15.5.sp, fontWeight = FontWeight.Bold, color = cInk)
                Box(Modifier.clip(RoundedCornerShape(6.dp)).background(cBg).border(1.dp, cLine, RoundedCornerShape(6.dp)).padding(horizontal = 7.dp, vertical = 2.dp)) {
                    Text(sv.kindLabel, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = cDim)
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 11.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                SourceStat("التكلفة", sv.costFmt, cInk)
                SourceStat("الإيراد المنسوب", sv.revFmt, cInk)
                SourceStat("الربح تقريباً", sv.profitFmt, if (sv.profit == null) cDim else if (sv.profit >= 0) cPaid else cDebt, bold = true)
            }
            Text("على الرف الآن: ${sv.remain} قطعة", fontSize = 11.sp, color = cDim, modifier = Modifier.padding(top = 9.dp))
            if (sv.isBale) {
                Row(
                    Modifier.fillMaxWidth().padding(top = 9.dp).clip(RoundedCornerShape(10.dp)).background(cBg).border(1.dp, cLine, RoundedCornerShape(10.dp)).tap { vm.openPackage(sv.id) }.padding(horizontal = 11.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("عدّ ورفّ البضاعة ←", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = cAccent)
                    Text("${sv.inPkg} في البالة", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = cAmber)
                }
            }
        }
    }
}

@Composable
private fun SourceStat(label: String, value: String, color: Color, bold: Boolean = false) {
    Column {
        Text(label, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, color = cDim)
        Text(value, fontSize = 15.sp, fontWeight = if (bold) FontWeight.ExtraBold else FontWeight.Bold, color = color, modifier = Modifier.padding(top = 1.dp))
    }
}

@Composable
private fun SummarySeg(st: StoreState, vm: StoreViewModel) {
    val unspec = st.shelf.count { it.unspecified }
    val totalOnHand = st.shelf.sumOf { maxOf(0, it.onHand) }
    Column(Modifier.fillMaxWidth().card().padding(15.dp)) {
        Text("إجمالي المحل", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = cDim, modifier = Modifier.padding(bottom = 12.dp))
        SummaryRow("أصناف على الرف", "$totalOnHand قطعة", cInk, cInk, line = true)
        SummaryRow("مصادر مسجّلة", "${st.sources.size}", cInk, cInk, line = true)
        SummaryRow("تحتاج تحديد مصدر", "$unspec", cDebt, cDebt, line = false)
    }
    Spacer(Modifier.height(12.dp))
    Box(Modifier.fillMaxWidth().card(12.dp).tap { vm.loadSample() }.padding(13.dp), contentAlignment = Alignment.Center) {
        Text("بيانات تجريبية ⟲", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = cInk)
    }
    Box(Modifier.fillMaxWidth().padding(top = 14.dp).tap { vm.resetApp() }, contentAlignment = Alignment.Center) {
        Text("مسح الكل — إظهار البداية", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = cDebt)
    }
}

@Composable
private fun SummaryRow(label: String, value: String, labelColor: Color, valueColor: Color, line: Boolean) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp).let { if (line) it.drawBottomLine() else it },
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, fontSize = 13.5.sp, color = labelColor)
        Text(value, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = valueColor)
    }
}

// ── shared small pieces ──
@Composable
internal fun SectionLabel(text: String) {
    Text(
        text, fontSize = 11.sp, letterSpacing = 0.6.sp, fontWeight = FontWeight.Bold, color = cDim,
        modifier = Modifier.padding(top = 18.dp, bottom = 8.dp, start = 2.dp, end = 2.dp),
    )
}

@Composable
internal fun FilterChip(label: String, active: Boolean, dot: Boolean = false, onClick: () -> Unit) {
    Row(
        Modifier.clip(RoundedCornerShape(9.dp)).background(if (active) cAccent else cCard).border(1.dp, cLine, RoundedCornerShape(9.dp)).tap(onClick).padding(horizontal = 13.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (dot) Box(Modifier.size(7.dp).clip(RoundedCornerShape(50)).background(cDebt))
        Text(label, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = if (active) cAink else cDim)
    }
}

@Composable
internal fun PrimaryButton(label: String, fontSize: androidx.compose.ui.unit.TextUnit = 16.sp, radius: androidx.compose.ui.unit.Dp = 15.dp, vertical: androidx.compose.ui.unit.Dp = 15.dp, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(radius)).background(cAccent).tap(onClick).padding(vertical = vertical),
        contentAlignment = Alignment.Center,
    ) { Text(label, fontSize = fontSize, fontWeight = FontWeight.Bold, color = cAink) }
}

@Composable
internal fun OutlineButton(label: String, fontSize: androidx.compose.ui.unit.TextUnit = 14.sp, radius: androidx.compose.ui.unit.Dp = 13.dp, vertical: androidx.compose.ui.unit.Dp = 13.dp, filledCard: Boolean = false, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(radius)).background(if (filledCard) cCard else Color.Transparent).border(1.5.dp, cAccent, RoundedCornerShape(radius)).tap(onClick).padding(vertical = vertical),
        contentAlignment = Alignment.Center,
    ) { Text(label, fontSize = fontSize, fontWeight = FontWeight.Bold, color = cAccent) }
}

// hairline under a row (border-bottom in the prototype)
internal fun Modifier.drawBottomLine(): Modifier = this.drawBehind {
    drawLine(cLine, androidx.compose.ui.geometry.Offset(0f, size.height), androidx.compose.ui.geometry.Offset(size.width, size.height), 1.dp.toPx())
}

// hairline above a row (border-top in the prototype)
internal fun Modifier.drawTopLine(): Modifier = this.drawBehind {
    drawLine(cLine, androidx.compose.ui.geometry.Offset(0f, 0f), androidx.compose.ui.geometry.Offset(size.width, 0f), 1.dp.toPx())
}
