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
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
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
    // Back closes an open sheet/overlay first, then falls back to اليوم; only اليوم exits.
    val overlayOpen = st.screen != "home" || st.specifyId != null || st.custPickerOpen ||
        st.custAddOpen || st.detailEntryId != null || st.detailCustomerId != null ||
        st.confirm != null || st.editItemId != null
    androidx.activity.compose.BackHandler(enabled = overlayOpen || st.tab != "today") {
        when {
            st.confirm != null -> vm.dismissConfirm()
            st.editItemId != null -> vm.closeEditItem()
            st.custAddOpen -> vm.closeAddCustomer()
            st.custPickerOpen -> vm.closeCustPicker()
            st.detailEntryId != null -> vm.closeEntry()
            st.detailCustomerId != null -> vm.closeCustomer()
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
        }
    }
}

@Composable
private fun AppBar(st: StoreState) {
    val title = when (st.tab) {
        "today" -> "دفتر اليوم"; "cust" -> "الزبائن"; else -> "الحساب"
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
        // المواعيد is not a tab (v2 decision 10): reminders live inside each customer's
        // card, and الزبائن sorts most-urgent-first. The daily notification digest remains.
        Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 6.dp)) {
            TabItem("▤", "اليوم", st.tab == "today", Modifier.weight(1f)) { vm.setTab("today") }
            TabItem("☰", "الزبائن", st.tab == "cust", Modifier.weight(1f)) { vm.setTab("cust") }
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

// A dismissible usage tip (replaces the old first-run demo splash).
@Composable
private fun TipBanner() {
    var shown by remember { mutableStateOf(true) }
    if (!shown) return
    val tip = USAGE_TIPS[(st_todayEpochDay() % USAGE_TIPS.size).toInt()]
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(cGreenBg).border(1.dp, cGreenBorder, RoundedCornerShape(12.dp)).padding(start = 13.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("💡 $tip", fontSize = 12.sp, color = cPaid, lineHeight = 17.sp, modifier = Modifier.weight(1f).padding(end = 8.dp))
        Text("✕", fontSize = 15.sp, color = cDim, modifier = Modifier.tap { shown = false })
    }
    Spacer(Modifier.height(12.dp))
}

private fun st_todayEpochDay(): Long = java.time.LocalDate.now().toEpochDay()

// ── اليوم ──
@Composable
private fun TodayScreen(st: StoreState, vm: StoreViewModel) {
    val isToday = st.viewedDay == st.today
    val dayEntries = entriesForDay(st.entries, st.viewedDay)
    if (isToday) TipBanner()
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
                dayEntries.forEach { e -> EntryRow(e) { vm.openEntry(e.id) } }
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
private fun EntryRow(e: DayEntry, onClick: () -> Unit) {
    val amtColor = when (e.cls) { "pos" -> cPaid; "amber" -> cAmber; else -> cInk }
    Row(
        Modifier.fillMaxWidth().tap(onClick).padding(vertical = 12.dp).padding(start = 26.dp)
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
        PopText(value, 23.sp, valueColor, Modifier.padding(top = 3.dp))
    }
}

// A number that springs a little "pop" whenever it changes (satisfying on a new sale).
@Composable
private fun PopText(text: String, fontSize: androidx.compose.ui.unit.TextUnit, color: Color, modifier: Modifier = Modifier) {
    val scale = remember { Animatable(1f) }
    var first by remember { mutableStateOf(true) }
    LaunchedEffect(text) {
        if (first) first = false
        else { scale.snapTo(1.14f); scale.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 520f)) }
    }
    Text(
        text, fontSize = fontSize, fontWeight = FontWeight.Bold, color = color,
        modifier = modifier.graphicsLayer { scaleX = scale.value; scaleY = scale.value; transformOrigin = androidx.compose.ui.graphics.TransformOrigin(1f, 0.5f) },
    )
}

// ── الزبائن — the hub (v2 decision 10): urgency banner + most-urgent-first + reminders inside ──
@Composable
private fun CustScreen(st: StoreState, vm: StoreViewModel) {
    var query by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }
    val totalOwed = st.customers.sumOf { maxOf(0, customerBalance(it, st.entries)) }
    val dueCount = st.customers.count { c ->
        customerBalance(c, st.entries) > 0 && (c.dueEpochDay ?: Long.MAX_VALUE) <= st.today
    }
    if (dueCount > 0) {
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(cGreenBg).border(1.dp, cGreenBorder, RoundedCornerShape(12.dp)).padding(horizontal = 13.dp, vertical = 10.dp)) {
            Text("🔔 $dueCount زبائن ديونهن مستحقة — الأعجل أولاً", fontSize = 12.5.sp, color = cPaid)
        }
        Spacer(Modifier.height(12.dp))
    }
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
        // search box (restored from the prototype) — filters by name or phone
        androidx.compose.foundation.text.BasicTextField(
            value = query, onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(cBg).border(1.dp, cLine, RoundedCornerShape(12.dp)).padding(horizontal = 13.dp, vertical = 12.dp),
            textStyle = androidx.compose.ui.text.TextStyle(fontFamily = com.daftar.app.kernel.theme.Plex, fontSize = 13.sp, color = cInk),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(cInk), singleLine = true,
            decorationBox = { inner ->
                Box { if (query.isEmpty()) Text("🔍 بحث عن زبونة…", fontSize = 13.sp, color = cDim); inner() }
            },
        )
        Spacer(Modifier.height(12.dp))
        // most-urgent-first: chase-worthy (debt or أمانة) by due date then amount, then the rest
        val sorted = st.customers.sortedWith(
            compareBy(
                { customerBalance(it, st.entries) <= 0 && customerTrial(it, st.entries) <= 0 },
                { it.dueEpochDay ?: Long.MAX_VALUE },
                { -(customerBalance(it, st.entries) + customerTrial(it, st.entries)) },
            ),
        )
        val filtered = sorted.filter { query.isBlank() || it.name.contains(query.trim(), true) || (it.phone?.contains(query.trim()) == true) }
        if (filtered.isEmpty()) {
            Text("لا نتائج للبحث", fontSize = 13.sp, color = cDim, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp))
        } else {
            Column(Modifier.fillMaxWidth().card().padding(horizontal = 14.dp)) {
                filtered.forEach { c ->
                    val bal = customerBalance(c, st.entries)
                    val trial = customerTrial(c, st.entries)
                    val amt = if (bal > 0) fmt(bal) else if (bal == 0L) "لا شيء" else "لها ${fmt(-bal)}"
                    val sub = when {
                        bal > 0 -> "التسديد: " + dueStatus(c.dueEpochDay, st.today) + (if (trial > 0) " · أمانة ${fmt(trial)}" else "")
                        trial > 0 -> "أمانة ${fmt(trial)} — قد تُعاد"
                        else -> c.phone ?: "زبونة"
                    }
                    StaticListRow(c.name, sub, amt, if (bal > 0) cDebt else cPaid) { vm.openCustomer(c.id) }
                }
            }
        }
    }
    Spacer(Modifier.height(12.dp))
    OutlineButton("+ زبونة جديدة", fontSize = 14.sp, radius = 13.dp, vertical = 13.dp, filledCard = true) { vm.openAddCustomer() }
}

@Composable
private fun StaticListRow(name: String, sub: String, amt: String, amtColor: Color, amtBold: Boolean = true, onClick: (() -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().then(if (onClick != null) Modifier.tap(onClick) else Modifier).padding(vertical = 13.dp).drawBottomLine(),
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
    val bg by animateColorAsState(if (active) cAccent else cCard, spring(stiffness = 700f), label = "segbg")
    val fg by animateColorAsState(if (active) cAink else cDim, spring(stiffness = 700f), label = "segfg")
    Box(
        modifier.clip(RoundedCornerShape(9.dp)).background(bg).tap(onClick).padding(vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = fg)
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

// v2: a clean tappable row — everything (name, tasira, count, buy, source) edits in ONE sheet.
@Composable
private fun ShelfRow(r: Shelf, vm: StoreViewModel) {
    val oh = r.onHand
    val onHandColor = if (oh < 0) cDebt else if (oh == 0) cDim else cInk
    Column(Modifier.fillMaxWidth().tap { vm.openEditItem(r.id) }.padding(vertical = 12.dp).drawBottomLine()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("${r.name} ✎", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = cInk, modifier = Modifier.weight(1f, fill = false))
            Text(fmt(r.tasira), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = cInk)
        }
        Row(Modifier.fillMaxWidth().padding(top = 7.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(
                buildString {
                    append("على الرف ")
                    append(oh)
                    if (r.buy != null) append(" · شراء ${fmt(r.buy)}")
                },
                fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = onHandColor,
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (r.unspecified) Box(Modifier.size(7.dp).clip(RoundedCornerShape(50)).background(cDebt))
                Text(
                    if (r.unspecified) "غير محدد" else vm.sourceLabelFor(r.sourceId),
                    fontSize = 12.sp, fontWeight = FontWeight.Bold,
                    color = if (r.unspecified) cDebt else cDim,
                )
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

// v2 decision 11: قبل التطبيق is a fixed bucket; شراء من السوق is ONE card whose children
// are the shops (name ✎, her debt to them, their purchases); bales are the only
// stand-alone creatable source.
@Composable
private fun SourcesSeg(st: StoreState, vm: StoreViewModel) {
    Text("من أين أتت البضاعة وكم كلّفت — لتري أي مصدر ربح.", fontSize = 12.sp, color = cDim, lineHeight = 17.sp, modifier = Modifier.padding(bottom = 10.dp))
    UsdRateRow(st.usdRate, vm)
    Spacer(Modifier.height(12.dp))

    val views = sourceViews(st.sources, st.shelf, st.usdRate)
    val pre = views.find { it.id == PRE_ID }

    // قبل التطبيق — everything from before the app whose source nobody remembers
    Column(Modifier.fillMaxWidth().padding(bottom = 10.dp).card().padding(horizontal = 15.dp, vertical = 13.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("قبل التطبيق", fontSize = 15.5.sp, fontWeight = FontWeight.Bold, color = cInk)
            Box(Modifier.clip(RoundedCornerShape(6.dp)).background(cBg).border(1.dp, cLine, RoundedCornerShape(6.dp)).padding(horizontal = 7.dp, vertical = 2.dp)) {
                Text("بضاعة قديمة", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = cDim)
            }
        }
        Text(
            "كل ما كان قبل التطبيق ولا نستطيع تذكّر مصدره — بلا كلفة، والربح «—» بصدق. على الرف: ${pre?.remain ?: 0} قطعة",
            fontSize = 11.5.sp, color = cDim, lineHeight = 17.sp, modifier = Modifier.padding(top = 8.dp),
        )
    }

    MarketCard(st, vm, views)

    views.filter { it.isBale }.forEach { sv -> SourceCard(sv, vm) }
    OutlineButton("+ بالة جديدة", fontSize = 14.sp, radius = 13.dp, vertical = 13.dp, filledCard = true) { vm.openAddSource() }
}

// شراء من السوق — the one card: combined economics + the shops living inside it.
@Composable
private fun MarketCard(st: StoreState, vm: StoreViewModel, views: List<SourceView>) {
    val marketViews = views.filter { it.kind == Kind.MARKET }
    val shopViews = marketViews.filter { it.id != MKT_ID }
    val cost = marketViews.sumOf { it.costLocal ?: 0 }
    val revenue = marketViews.sumOf { it.revenue }
    val profit = revenue - cost
    val owed = marketViews.sumOf { it.debt }

    Column(Modifier.fillMaxWidth().padding(bottom = 10.dp).card().padding(horizontal = 15.dp, vertical = 13.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("شراء من السوق", fontSize = 15.5.sp, fontWeight = FontWeight.Bold, color = cInk)
            if (owed > 0) Text("عليكِ للمحلات ${fmt(owed)}", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = cDebt)
        }
        Row(Modifier.fillMaxWidth().padding(top = 11.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            SourceStat("التكلفة", fmt(cost), cInk)
            SourceStat("الإيراد المنسوب", fmt(revenue), cInk)
            SourceStat("الربح تقريباً", (if (profit >= 0) "+ " else "− ") + fmt(kotlin.math.abs(profit)), if (profit >= 0) cPaid else cDebt, bold = true)
        }

        shopViews.forEach { shop -> ShopRow(st, vm, shop) }

        if (st.shopAddOpen) {
            Column(Modifier.fillMaxWidth().padding(top = 11.dp).drawTopLine().padding(top = 11.dp)) {
                androidx.compose.foundation.text.BasicTextField(
                    value = st.shopName, onValueChange = vm::setShopName,
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(cBg).border(1.dp, cLine, RoundedCornerShape(10.dp)).padding(horizontal = 12.dp, vertical = 11.dp),
                    textStyle = androidx.compose.ui.text.TextStyle(fontFamily = com.daftar.app.kernel.theme.Plex, fontSize = 14.sp, color = cInk),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(cInk), singleLine = true,
                    decorationBox = { inner -> Box { if (st.shopName.isEmpty()) Text("اسم المحل — مثال: محل أم علي", fontSize = 14.sp, color = cDim); inner() } },
                )
                Row(Modifier.fillMaxWidth().padding(top = 9.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("دين أول (إن أخذتِ بالدَّين)", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = cDim)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        StepBtn("−", 26.dp, 8.dp, 1.5.dp, cLine, cAccent, 16.sp) { vm.shopDebtStep(-1) }
                        Text(fmt(st.shopDebt), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = cInk, textAlign = TextAlign.Center, modifier = Modifier.widthIn(min = 52.dp))
                        StepBtn("+", 26.dp, 8.dp, 1.5.dp, cLine, cAccent, 16.sp) { vm.shopDebtStep(1) }
                    }
                }
                Spacer(Modifier.height(10.dp))
                PrimaryButton("أضيفي المحل ✓", fontSize = 13.5.sp, radius = 11.dp, vertical = 10.dp) { vm.addShop() }
            }
        } else {
            Box(Modifier.fillMaxWidth().padding(top = 11.dp).clip(RoundedCornerShape(10.dp)).background(cBg).border(1.dp, cLine, RoundedCornerShape(10.dp)).tap { vm.toggleShopAdd() }.padding(vertical = 9.dp), contentAlignment = Alignment.Center) {
                Text("+ محل جديد", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = cAccent)
            }
        }
    }
}

@Composable
private fun ShopRow(st: StoreState, vm: StoreViewModel, shop: SourceView) {
    val purchases = st.shelf.filter { it.sourceId == shop.id }
    Column(Modifier.fillMaxWidth().padding(top = 11.dp).drawTopLine().padding(top = 11.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            if (st.shopRenameId == shop.id) {
                androidx.compose.foundation.text.BasicTextField(
                    value = st.shopName, onValueChange = vm::setShopName,
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(cBg).border(1.dp, cLine, RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 7.dp),
                    textStyle = androidx.compose.ui.text.TextStyle(fontFamily = com.daftar.app.kernel.theme.Plex, fontSize = 14.sp, color = cInk),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(cInk), singleLine = true,
                )
                Text("حفظ", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = cAccent, modifier = Modifier.padding(start = 8.dp).tap { vm.saveRenameShop() })
            } else {
                Text("🏪 ${shop.label} ✎", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = cInk, modifier = Modifier.tap { vm.startRenameShop(shop.id) })
                Text("كلفة بضاعته: ${fmt(shop.costLocal ?: 0)}", fontSize = 11.sp, color = cDim)
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (shop.debt > 0) "دينه علينا" else "لا دين له",
                fontSize = 11.5.sp, fontWeight = FontWeight.Bold,
                color = if (shop.debt > 0) cDebt else cPaid,
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                StepBtn("−", 24.dp, 7.dp, 1.dp, cLine, cDim, 14.sp) { vm.shopOwedStep(shop.id, -1) }
                Text(fmt(shop.debt), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (shop.debt > 0) cDebt else cDim, textAlign = TextAlign.Center, modifier = Modifier.widthIn(min = 52.dp))
                StepBtn("+", 24.dp, 7.dp, 1.dp, cLine, cDim, 14.sp) { vm.shopOwedStep(shop.id, 1) }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 7.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (purchases.isEmpty()) "لا مشتريات بعد"
                else purchases.joinToString(" · ") { "${it.name} ×${it.shelved}" + (it.buy?.let { b -> " @${fmt(b)}" } ?: "") },
                fontSize = 11.sp, color = cDim, modifier = Modifier.weight(1f, fill = false),
            )
            Text("+ صنف", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = cAccent, modifier = Modifier.padding(start = 8.dp).tap { vm.openAddItemFor(shop.id) })
        }
    }
}

// The editable "today's rate" for turning a bale's USD cost into local money.
@Composable
private fun UsdRateRow(rate: Long, vm: StoreViewModel) {
    Row(
        Modifier.fillMaxWidth().card(12.dp).padding(horizontal = 13.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text("سعر صرف الدولار اليوم", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = cInk)
            Text("لحساب تكلفة البالات", fontSize = 10.5.sp, color = cDim)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("$1 =", fontSize = 12.sp, color = cDim)
            androidx.compose.foundation.text.BasicTextField(
                value = if (rate == 0L) "" else rate.toString(),
                onValueChange = { s -> vm.setUsdRate(s.filter { it.isDigit() }.take(9).toLongOrNull() ?: 0L) },
                modifier = Modifier.widthIn(min = 62.dp).clip(RoundedCornerShape(8.dp)).background(cBg).border(1.dp, cLine, RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 7.dp),
                textStyle = androidx.compose.ui.text.TextStyle(fontFamily = com.daftar.app.kernel.theme.Plex, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = cInk, textAlign = TextAlign.Center),
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(cInk),
            )
        }
    }
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
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val importer = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            val json = runCatching { ctx.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } }.getOrNull()
            val ok = json != null && vm.importJson(json)
            android.widget.Toast.makeText(ctx, if (ok) "تمت الاستعادة" else "تعذّرت قراءة النسخة", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
    Column(Modifier.fillMaxWidth().card().padding(15.dp)) {
        Text("إجمالي المحل", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = cDim, modifier = Modifier.padding(bottom = 12.dp))
        SummaryRow("أصناف على الرف", "$totalOnHand قطعة", cInk, cInk, line = true)
        SummaryRow("مصادر مسجّلة", "${st.sources.size}", cInk, cInk, line = true)
        SummaryRow("تحتاج تحديد مصدر", "$unspec", cDebt, cDebt, line = false)
    }
    // backup — so the ledger is never lost
    SectionLabel("النسخة الاحتياطية")
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.weight(1f)) { PrimaryButton("⤓ حفظ نسخة", fontSize = 13.5.sp, radius = 12.dp, vertical = 13.dp) { shareBackup(ctx, vm.exportJson()) } }
        Box(Modifier.weight(1f)) { OutlineButton("⤒ استعادة", fontSize = 13.5.sp, radius = 12.dp, vertical = 13.dp, filledCard = true) { importer.launch(arrayOf("application/json", "text/*", "*/*")) } }
    }
    // sync bridge (FR-8.3): optional one-way push to the owner-tools API; never blocks the app
    SectionLabel("المزامنة (اختياري)")
    var syncUrl by remember { mutableStateOf(com.daftar.app.sync.SyncWorker.syncUrl(ctx)) }
    Column(Modifier.fillMaxWidth().card(12.dp).padding(13.dp)) {
        Text("ترسل نسخة الدفتر إلى حاسوب نوّار عند توفر الاتصال — المحل يعمل دونها تماماً.", fontSize = 11.5.sp, color = cDim, lineHeight = 17.sp, modifier = Modifier.padding(bottom = 9.dp))
        androidx.compose.foundation.text.BasicTextField(
            value = syncUrl,
            onValueChange = { syncUrl = it; com.daftar.app.sync.SyncWorker.setSyncUrl(ctx, it) },
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(cBg).border(1.dp, cLine, RoundedCornerShape(10.dp)).padding(horizontal = 12.dp, vertical = 10.dp),
            textStyle = androidx.compose.ui.text.TextStyle(fontFamily = com.daftar.app.kernel.theme.Plex, fontSize = 12.5.sp, color = cInk),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(cInk), singleLine = true,
            decorationBox = { inner -> Box { if (syncUrl.isEmpty()) Text("عنوان الخادم — http://…/import", fontSize = 12.5.sp, color = cDim); inner() } },
        )
        if (syncUrl.isNotBlank()) {
            Spacer(Modifier.height(9.dp))
            OutlineButton("مزامنة الآن ↥", fontSize = 13.sp, radius = 11.dp, vertical = 10.dp) {
                com.daftar.app.sync.SyncWorker.syncNow(ctx)
                android.widget.Toast.makeText(ctx, "ستُرسل النسخة عند توفر الاتصال", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }
    Spacer(Modifier.height(14.dp))
    Box(Modifier.fillMaxWidth().card(12.dp).tap { vm.askConfirm("sample") }.padding(13.dp), contentAlignment = Alignment.Center) {
        Text("بيانات تجريبية ⟲", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = cInk)
    }
    Box(Modifier.fillMaxWidth().padding(top = 14.dp).tap { vm.askConfirm("reset") }, contentAlignment = Alignment.Center) {
        Text("مسح الكل — إظهار البداية", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = cDebt)
    }
}

// Write the JSON backup to cacheDir and hand it to the Android share sheet.
private fun shareBackup(ctx: android.content.Context, json: String) {
    val dir = java.io.File(ctx.cacheDir, "backups").apply { mkdirs() }
    val file = java.io.File(dir, "daftar-backup.json")
    file.writeText(json)
    val uri = androidx.core.content.FileProvider.getUriForFile(ctx, ctx.packageName + ".fileprovider", file)
    val send = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
        type = "application/json"
        putExtra(android.content.Intent.EXTRA_STREAM, uri)
        putExtra(android.content.Intent.EXTRA_SUBJECT, "نسخة احتياطية — دفتر")
        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    ctx.startActivity(android.content.Intent.createChooser(send, "حفظ نسخة احتياطية").apply {
        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
    })
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
