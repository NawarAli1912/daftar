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
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
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
import androidx.compose.ui.platform.LocalLifecycleOwner
import kotlinx.coroutines.launch
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.daftar.app.kernel.theme.Amiri
import com.daftar.app.kernel.theme.Plex

@Composable
fun StoreApp(vm: StoreViewModel = hiltViewModel()) {
    val st by vm.state.collectAsState()
    // The day book must roll past midnight without a relaunch — she leaves the app open.
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) vm.refreshToday() }
        lifecycle.addObserver(obs)
        onDispose { lifecycle.removeObserver(obs) }
    }
    // Back closes an open sheet/overlay first, then falls back to اليوم; only اليوم exits.
    val overlayOpen = st.screen != "home" || st.specifyId != null || st.custPickerOpen ||
        st.custAddOpen || st.detailEntryId != null || st.detailCustomerId != null ||
        st.confirm != null || st.editItemId != null || st.maintOpen || st.shopId != null ||
        st.paperDebtPrompt
    androidx.activity.compose.BackHandler(enabled = overlayOpen || st.tab != "today") {
        when {
            st.confirm != null -> vm.dismissConfirm()
            st.paperDebtPrompt -> vm.closePaperDebt()
            st.maintOpen -> vm.closeMaint()
            st.editItemId != null -> vm.closeEditItem()
            st.custAddOpen -> vm.closeAddCustomer()
            st.custPickerOpen -> vm.closeCustPicker()
            st.detailEntryId != null -> vm.closeEntry()
            st.detailCustomerId != null -> vm.closeCustomer()
            st.shopId != null -> vm.closeShop()
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
                AppBar(st, vm)
                Swap(st.tab, Modifier.weight(1f).fillMaxWidth()) { tab ->
                    Column(
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 8.dp),
                    ) {
                        when (tab) {
                            "today" -> TodayScreen(st, vm)
                            "cust" -> CustScreen(st, vm)
                            "account" -> AccountScreen(st, vm)
                        }
                    }
                }
                // Undo bar (F2): inline, above the button + tabs so it never covers them;
                // swipe sideways or ✕ to dismiss; still auto-hides after 10s.
                if (st.undo != null && st.screen == "home") UndoBar(vm)
                if (st.tab == "today") {
                    Box(Modifier.fillMaxWidth().background(cBg).padding(start = 16.dp, end = 16.dp, top = 9.dp, bottom = 6.dp)) {
                        PrimaryButton("+ قيد جديد", fontSize = fHead) { vm.openChooser() }
                    }
                }
                TabBar(st, vm)
            }
            StoreSheets(st, vm)
        }
    }
}

// F2: a compact undo bar that lives in the layout flow (never over the button/tabs).
// Drag it sideways past a threshold — or tap ✕ — to dismiss; ↺ تراجع still voids.
@Composable
private fun UndoBar(vm: StoreViewModel) {
    val offsetX = remember { androidx.compose.animation.core.Animatable(0f) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .offset { androidx.compose.ui.unit.IntOffset(offsetX.value.toInt(), 0) }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        scope.launch {
                            if (kotlin.math.abs(offsetX.value) > 180f) vm.dismissUndo()
                            else offsetX.animateTo(0f)
                        }
                    },
                ) { change, dragAmount ->
                    change.consume()
                    scope.launch { offsetX.snapTo(offsetX.value + dragAmount) }
                }
            }
            .clip(RoundedCornerShape(rMd)).background(cInk).padding(start = 14.dp, end = 10.dp, top = 11.dp, bottom = 11.dp),
        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("تم التسجيل في الدفتر", fontSize = fBody, fontWeight = FontWeight.SemiBold, color = cCard)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("↺ تراجع", fontSize = fBodyL, fontWeight = FontWeight.ExtraBold, color = cUndoAccent, modifier = Modifier.tap { vm.undoSale() })
            Text("✕", fontSize = fBodyL, fontWeight = FontWeight.Bold, color = cDim, modifier = Modifier.tap { vm.dismissUndo() })
        }
    }
}

@Composable
private fun AppBar(st: StoreState, vm: StoreViewModel) {
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
            // Hidden maintainer entrance: a deliberate ~2s hold on the wordmark (D-F4).
            // Nothing destructive stays reachable from الملخّص.
            Text(
                "دفتر", fontFamily = Amiri, fontWeight = FontWeight.Bold, fontSize = 21.sp, color = cDebt,
                modifier = Modifier.pointerInput(Unit) {
                    detectTapGestures(onPress = {
                        val held = try {
                            withTimeout(1800) { awaitRelease(); false }
                        } catch (e: TimeoutCancellationException) { true }
                        if (held) vm.openMaint()
                    })
                },
            )
            Text(title, fontSize = fBodyL, fontWeight = FontWeight.Bold, color = cInk)
            Text(aside, fontSize = fSmall, color = cDim, textAlign = TextAlign.End, modifier = Modifier.widthIn(min = 34.dp))
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
            Text(glyph, fontSize = fGlyph, color = col)
            Text(label, fontSize = fCaption, fontWeight = if (active) FontWeight.Bold else FontWeight.Normal, color = col, modifier = Modifier.padding(top = 1.dp))
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
        Modifier.fillMaxWidth().clip(RoundedCornerShape(rMd)).background(cGreenBg).border(1.dp, cGreenBorder, RoundedCornerShape(rMd)).padding(start = 13.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("💡 $tip", fontSize = fSmall, color = cPaid, lineHeight = 17.sp, modifier = Modifier.weight(1f).padding(end = 8.dp))
        Text("✕", fontSize = fTitle, color = cDim, modifier = Modifier.tap { shown = false })
    }
    Spacer(Modifier.height(12.dp))
}

private fun st_todayEpochDay(): Long = java.time.LocalDate.now().toEpochDay()

// ── اليوم ──
@Composable
private fun TodayScreen(st: StoreState, vm: StoreViewModel) {
    val isToday = st.viewedDay == st.today
    // page-turn direction: moving to a newer day flips forward, an older day flips back
    var lastDay by remember { mutableStateOf(st.viewedDay) }
    val forward = st.viewedDay >= lastDay
    LaunchedEffect(st.viewedDay) { lastDay = st.viewedDay }
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
        Text(dayLabel(st.viewedDay, st.today), fontSize = fBody, fontWeight = FontWeight.Bold, color = cInk)
        DayNavArrow("›", enabled = !isToday) { vm.dayStep(1) }
    }
    PageFlip(st.viewedDay, forward, Modifier.fillMaxWidth()) { day ->
        val entries = entriesForDay(st.entries, day)
        val dayIsToday = day == st.today
        if (entries.isEmpty()) {
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(rLg)).background(cCard)
                    .dashedBorder(cLine, rLg).padding(vertical = 32.dp, horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("📃", fontSize = 30.sp, modifier = Modifier.padding(bottom = 8.dp))
                Text(
                    if (dayIsToday) "لا قيود بعد — ابدئي بأول عملية بيع" else "لا حركات في هذا اليوم",
                    fontSize = fBody, fontWeight = FontWeight.SemiBold, color = cDim,
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
                    entries.forEach { e -> EntryRow(e) { vm.openEntry(e.id) } }
                }
            }
        }
    }
}

@Composable
private fun DayNavArrow(sym: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.size(32.dp).clip(RoundedCornerShape(rXs))
            .background(cCard).border(1.dp, cLine, RoundedCornerShape(rXs))
            .then(if (enabled) Modifier.tap(onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Text(sym, fontSize = fGlyph, fontWeight = FontWeight.Bold, color = if (enabled) cAccent else cLine)
    }
}

@Composable
private fun EntryRow(e: DayEntry, onClick: () -> Unit) {
    val amtColor = when (e.cls) { "pos" -> cPaid; "amber" -> cAmber; "neg" -> cDebt; else -> cInk }
    Row(
        Modifier.fillMaxWidth().tap(onClick).padding(vertical = 12.dp).padding(start = 26.dp)
            .drawBottomLine(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Column(Modifier.weight(1f, fill = false)) {
            Text(e.t, fontSize = fBodyL, fontWeight = FontWeight.SemiBold, color = cInk, lineHeight = 20.sp)
            Text(e.d, fontSize = fCaption, color = cDim, modifier = Modifier.padding(top = 3.dp))
        }
        Spacer(Modifier.width(10.dp))
        Text(e.amt, fontSize = fTitle, fontWeight = FontWeight.Bold, color = amtColor)
    }
}

@Composable
private fun StatCard(label: String, value: String, valueColor: Color, modifier: Modifier) {
    Column(modifier.card().padding(horizontal = 13.dp, vertical = 12.dp)) {
        Text(label, fontSize = fCaption, fontWeight = FontWeight.SemiBold, color = cDim)
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
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(rMd)).background(cGreenBg).border(1.dp, cGreenBorder, RoundedCornerShape(rMd)).padding(horizontal = 13.dp, vertical = 10.dp)) {
            Text("🔔 $dueCount زبائن ديونهن مستحقة — الأعجل أولاً", fontSize = fSmall, color = cPaid)
        }
        Spacer(Modifier.height(12.dp))
    }
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(rMd)).background(cBg).border(1.dp, cLine, RoundedCornerShape(rMd)).padding(horizontal = 13.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("إجمالي الديون للمحل", fontSize = fSmall, fontWeight = FontWeight.SemiBold, color = cDim)
        Text(fmt(totalOwed), fontSize = fHead, fontWeight = FontWeight.Bold, color = cDebt)
    }
    Spacer(Modifier.height(12.dp))
    if (st.customers.isEmpty()) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(rLg)).background(cCard).dashedBorder(cLine, rLg).padding(vertical = 28.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("👤", fontSize = 28.sp, modifier = Modifier.padding(bottom = 8.dp))
            Text("لا زبائن بعد — أضيفي أول زبونة", fontSize = fBody, fontWeight = FontWeight.SemiBold, color = cDim)
        }
    } else {
        // search box (restored from the prototype) — filters by name or phone
        androidx.compose.foundation.text.BasicTextField(
            value = query, onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(rMd)).background(cBg).border(1.dp, cLine, RoundedCornerShape(rMd)).padding(horizontal = 13.dp, vertical = 12.dp),
            textStyle = androidx.compose.ui.text.TextStyle(fontFamily = com.daftar.app.kernel.theme.Plex, fontSize = fBody, color = cInk),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(cInk), singleLine = true,
            decorationBox = { inner ->
                Box { if (query.isEmpty()) Text("🔍 بحث عن زبونة…", fontSize = fBody, color = cDim); inner() }
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
            Text("لا نتائج للبحث", fontSize = fBody, color = cDim, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp))
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
    OutlineButton("+ زبونة جديدة", fontSize = fBodyL, radius = rMd, vertical = 13.dp, filledCard = true) { vm.openAddCustomer() }
}

@Composable
private fun StaticListRow(name: String, sub: String, amt: String, amtColor: Color, amtBold: Boolean = true, onClick: (() -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().then(if (onClick != null) Modifier.tap(onClick) else Modifier).padding(vertical = 13.dp).drawBottomLine(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(name, fontSize = fTitle, fontWeight = FontWeight.Bold, color = cInk)
            Text(sub, fontSize = fCaption, color = cDim)
        }
        Text(amt, fontSize = if (amtBold) 12.5.sp else 13.sp, fontWeight = FontWeight.Bold, color = amtColor)
    }
}

// ── الحساب ──
@Composable
private fun AccountScreen(st: StoreState, vm: StoreViewModel) {
    // segment switcher
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(rMd)).background(cBg).border(1.dp, cLine, RoundedCornerShape(rMd)).padding(3.dp)) {
        SegBtn("البضاعة", st.accountSeg == "shelf", Modifier.weight(1f)) { vm.setSeg("shelf") }
        SegBtn("المصادر", st.accountSeg == "sources", Modifier.weight(1f)) { vm.setSeg("sources") }
        SegBtn("الملخّص", st.accountSeg == "sum", Modifier.weight(1f)) { vm.setSeg("sum") }
    }
    Spacer(Modifier.height(14.dp))
    Swap(st.accountSeg, Modifier.fillMaxWidth()) { seg ->
        Column(Modifier.fillMaxWidth()) {
            when (seg) {
                "shelf" -> ShelfSeg(st, vm)
                "sources" -> SourcesSeg(st, vm)
                else -> SummarySeg(st, vm)
            }
        }
    }
}

@Composable
private fun SegBtn(label: String, active: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val bg by animateColorAsState(if (active) cAccent else cCard, spring(stiffness = 700f), label = "segbg")
    val fg by animateColorAsState(if (active) cAink else cDim, spring(stiffness = 700f), label = "segfg")
    Box(
        modifier.clip(RoundedCornerShape(rXs)).background(bg).tap(onClick).padding(vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, fontSize = fBody, fontWeight = FontWeight.Bold, color = fg)
    }
}

@Composable
private fun ShelfSeg(st: StoreState, vm: StoreViewModel) {
    val unspec = st.shelf.count { it.unspecified }
    Text("محلّك — ما لديك للبيع. البيع يقترح من هنا فقط.", fontSize = fSmall, color = cDim, lineHeight = 17.sp, modifier = Modifier.padding(bottom = 10.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.padding(bottom = 12.dp)) {
        FilterChip("الكل", st.shelfFilter == "all") { vm.setFilter("all") }
        FilterChip("غير محدد ($unspec)", st.shelfFilter == "unspec", dot = true) { vm.setFilter("unspec") }
    }
    val rows = if (st.shelfFilter == "unspec") st.shelf.filter { it.unspecified } else st.shelf
    Column(Modifier.fillMaxWidth().card().padding(horizontal = 14.dp)) {
        rows.forEach { r -> ShelfRow(r, vm) }
        Row(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.weight(1f)) { PrimaryButton("+ صنف للمحل", fontSize = fBody, radius = rSm, vertical = 11.dp) { vm.openAddItem() } }
            Box(Modifier.weight(1f)) { OutlineButton("+ بالة", fontSize = fBody, radius = rSm, vertical = 11.dp) { vm.openAddSource() } }
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
            Text("${r.name} ✎", fontSize = fTitle, fontWeight = FontWeight.Bold, color = cInk, modifier = Modifier.weight(1f, fill = false))
            Text(fmt(r.tasira), fontSize = fBodyL, fontWeight = FontWeight.Bold, color = cInk)
        }
        Row(Modifier.fillMaxWidth().padding(top = 7.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(
                buildString {
                    append("في المحل ")
                    append(oh)
                    if (r.buy != null) append(" · شراء ${fmt(r.buy)}")
                },
                fontSize = fCaption, fontWeight = FontWeight.SemiBold, color = onHandColor,
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (r.unspecified) Box(Modifier.size(7.dp).clip(RoundedCornerShape(50)).background(cDebt))
                Text(
                    if (r.unspecified) "غير محدد" else vm.sourceLabelFor(r.sourceId),
                    fontSize = fSmall, fontWeight = FontWeight.Bold,
                    color = if (r.unspecified) cDebt else cDim,
                )
            }
        }
        if (oh < 0) {
            Row(
                Modifier.fillMaxWidth().padding(top = 9.dp).clip(RoundedCornerShape(rSm)).background(cAmberBg).border(1.dp, cAmberBorder, RoundedCornerShape(rSm)).tap { vm.reconcile(r.id) }.padding(horizontal = 11.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("بيع ${r.sold} · معدود ${r.shelved} — أعيدي العدّ؟", fontSize = fCaption, fontWeight = FontWeight.SemiBold, color = cAmber, modifier = Modifier.weight(1f, fill = false))
                Text("توفيق ←", fontSize = fCaption, fontWeight = FontWeight.Bold, color = cAccent)
            }
        }
    }
}

// v2 decision 11: قبل التطبيق is a fixed bucket; شراء من السوق is ONE card whose children
// are the shops (name ✎, her debt to them, their purchases); bales are the only
// stand-alone creatable source.
@Composable
private fun SourcesSeg(st: StoreState, vm: StoreViewModel) {
    Text("من أين أتت البضاعة وكم كلّفت — لتري أي مصدر ربح.", fontSize = fSmall, color = cDim, lineHeight = 17.sp, modifier = Modifier.padding(bottom = 10.dp))
    UsdRateRow(st.usdRate, vm)
    Spacer(Modifier.height(12.dp))

    val views = sourceViews(st.sources, st.shelf, st.usdRate)
    val pre = views.find { it.id == PRE_ID }

    // قبل التطبيق — everything from before the app whose source nobody remembers
    Column(Modifier.fillMaxWidth().padding(bottom = 10.dp).card().padding(horizontal = 15.dp, vertical = 13.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("قبل التطبيق", fontSize = fTitle, fontWeight = FontWeight.Bold, color = cInk)
            Box(Modifier.clip(RoundedCornerShape(rXs)).background(cBg).border(1.dp, cLine, RoundedCornerShape(rXs)).padding(horizontal = 7.dp, vertical = 2.dp)) {
                Text("بضاعة قديمة", fontSize = fCaption, fontWeight = FontWeight.Bold, color = cDim)
            }
        }
        Text(
            "كل ما كان قبل التطبيق ولا نستطيع تذكّر مصدره — بلا كلفة، والربح «—» بصدق. في المحل: ${pre?.remain ?: 0} قطعة",
            fontSize = fCaption, color = cDim, lineHeight = 17.sp, modifier = Modifier.padding(top = 8.dp),
        )
    }

    MarketCard(st, vm, views)

    views.filter { it.isBale }.forEach { sv -> SourceCard(sv, vm) }
    OutlineButton("+ بالة جديدة", fontSize = fBodyL, radius = rMd, vertical = 13.dp, filledCard = true) { vm.openAddSource() }
}

// شراء من السوق — the one card: combined economics + the shops living inside it.
@Composable
private fun MarketCard(st: StoreState, vm: StoreViewModel, views: List<SourceView>) {
    val marketViews = views.filter { it.kind == Kind.MARKET }
    val shopViews = marketViews.filter { it.id != MKT_ID }
    val cost = marketViews.sumOf { it.costLocal ?: 0 }
    val revenue = marketViews.sumOf { it.revenue }
    val profit = revenue - cost
    // D68: what's still owed derives down through supplier-payment entries
    val owed = marketViews.sumOf { it.debt - supplierPaid(st.entries, it.id) }

    Column(Modifier.fillMaxWidth().padding(bottom = 10.dp).card().padding(horizontal = 15.dp, vertical = 13.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("شراء من السوق", fontSize = fTitle, fontWeight = FontWeight.Bold, color = cInk)
            if (owed > 0) Text("عليكِ للمحلات ${fmt(owed)}", fontSize = fCaption, fontWeight = FontWeight.Bold, color = cDebt)
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
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(rSm)).background(cBg).border(1.dp, cLine, RoundedCornerShape(rSm)).padding(horizontal = 12.dp, vertical = 11.dp),
                    textStyle = androidx.compose.ui.text.TextStyle(fontFamily = com.daftar.app.kernel.theme.Plex, fontSize = fBodyL, color = cInk),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(cInk), singleLine = true,
                    decorationBox = { inner -> Box { if (st.shopName.isEmpty()) Text("اسم المحل — مثال: محل أم علي", fontSize = fBodyL, color = cDim); inner() } },
                )
                Row(Modifier.fillMaxWidth().padding(top = 9.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("دين أول (إن أخذتِ بالدَّين)", fontSize = fCaption, fontWeight = FontWeight.SemiBold, color = cDim)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        StepBtn("−", 26.dp, 8.dp, 1.5.dp, cLine, cAccent, 16.sp) { vm.shopDebtStep(-1) }
                        Text(fmt(st.shopDebt), fontSize = fBodyL, fontWeight = FontWeight.Bold, color = cInk, textAlign = TextAlign.Center, modifier = Modifier.widthIn(min = 52.dp))
                        StepBtn("+", 26.dp, 8.dp, 1.5.dp, cLine, cAccent, 16.sp) { vm.shopDebtStep(1) }
                    }
                }
                Spacer(Modifier.height(10.dp))
                PrimaryButton("أضيفي المحل ✓", fontSize = fBody, radius = rSm, vertical = 10.dp) { vm.addShop() }
            }
        } else {
            Box(Modifier.fillMaxWidth().padding(top = 11.dp).clip(RoundedCornerShape(rSm)).background(cBg).border(1.dp, cLine, RoundedCornerShape(rSm)).tap { vm.toggleShopAdd() }.padding(vertical = 9.dp), contentAlignment = Alignment.Center) {
                Text("+ محل جديد", fontSize = fSmall, fontWeight = FontWeight.Bold, color = cAccent)
            }
        }
    }
}

@Composable
private fun ShopRow(st: StoreState, vm: StoreViewModel, shop: SourceView) {
    // F2: one clean tappable line per محل — all management lives in its detail sheet
    val itemKinds = st.shelf.count { it.sourceId == shop.id }
    val debtNow = shop.debt - supplierPaid(st.entries, shop.id)
    Row(
        Modifier.fillMaxWidth().padding(top = 11.dp).drawTopLine().padding(top = 11.dp).tap { vm.openShop(shop.id) },
        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("🏪 ${shop.label} ←", fontSize = fBodyL, fontWeight = FontWeight.Bold, color = cInk)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(if (itemKinds == 0) "لا مشتريات" else "$itemKinds أصناف", fontSize = fCaption, color = cDim)
            Text(
                if (debtNow > 0) "دينه علينا ${fmt(debtNow)}" else "لا دين له",
                fontSize = fCaption, fontWeight = FontWeight.Bold,
                color = if (debtNow > 0) cDebt else cPaid,
            )
        }
    }
}

// The editable "today's rate" for turning a bale's USD cost into local money.
@Composable
private fun UsdRateRow(rate: Long, vm: StoreViewModel) {
    Row(
        Modifier.fillMaxWidth().card(rMd).padding(horizontal = 13.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text("سعر صرف الدولار اليوم", fontSize = fSmall, fontWeight = FontWeight.SemiBold, color = cInk)
            Text("لحساب تكلفة البالات", fontSize = fCaption, color = cDim)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("$1 =", fontSize = fSmall, color = cDim)
            androidx.compose.foundation.text.BasicTextField(
                value = if (rate == 0L) "" else rate.toString(),
                onValueChange = { s -> vm.setUsdRate(s.filter { it.isDigit() }.take(9).toLongOrNull() ?: 0L) },
                modifier = Modifier.widthIn(min = 62.dp).clip(RoundedCornerShape(rXs)).background(cBg).border(1.dp, cLine, RoundedCornerShape(rXs)).padding(horizontal = 10.dp, vertical = 7.dp),
                textStyle = androidx.compose.ui.text.TextStyle(fontFamily = com.daftar.app.kernel.theme.Plex, fontSize = fTitle, fontWeight = FontWeight.Bold, color = cInk, textAlign = TextAlign.Center),
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
        Modifier.fillMaxWidth().padding(bottom = 10.dp).card()
            // F1: the whole bale card opens its screen — no separate tap target to find
            .then(if (sv.isBale) Modifier.tap { vm.openPackage(sv.id) } else Modifier)
            .drawBehind {
                val w = 3.dp.toPx()
                val x = if (layoutDirection == LayoutDirection.Ltr) 0f else size.width - w
                drawRect(stripe, androidx.compose.ui.geometry.Offset(x, 0f), androidx.compose.ui.geometry.Size(w, size.height))
            },
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 13.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(sv.label, fontSize = fTitle, fontWeight = FontWeight.Bold, color = cInk)
                Box(Modifier.clip(RoundedCornerShape(rXs)).background(cBg).border(1.dp, cLine, RoundedCornerShape(rXs)).padding(horizontal = 7.dp, vertical = 2.dp)) {
                    Text(sv.kindLabel, fontSize = fCaption, fontWeight = FontWeight.Bold, color = cDim)
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 11.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                SourceStat("التكلفة", sv.costFmt, cInk)
                SourceStat("الإيراد المنسوب", sv.revFmt, cInk)
                SourceStat("الربح تقريباً", sv.profitFmt, if (sv.profit == null) cDim else if (sv.profit >= 0) cPaid else cDebt, bold = true)
            }
            Text("في المحل الآن: ${sv.remain} قطعة", fontSize = fCaption, color = cDim, modifier = Modifier.padding(top = 9.dp))
            if (sv.isBale) {
                Row(
                    Modifier.fillMaxWidth().padding(top = 9.dp).clip(RoundedCornerShape(rSm)).background(cBg).border(1.dp, cLine, RoundedCornerShape(rSm)).padding(horizontal = 11.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("إدارة البالة ←", fontSize = fSmall, fontWeight = FontWeight.Bold, color = cAccent)
                    Text("${sv.inPkg} في البالة", fontSize = fCaption, fontWeight = FontWeight.Bold, color = cAmber)
                }
            }
        }
    }
}

@Composable
private fun SourceStat(label: String, value: String, color: Color, bold: Boolean = false) {
    Column {
        Text(label, fontSize = fCaption, fontWeight = FontWeight.SemiBold, color = cDim)
        Text(value, fontSize = fTitle, fontWeight = if (bold) FontWeight.ExtraBold else FontWeight.Bold, color = color, modifier = Modifier.padding(top = 1.dp))
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
        Text("إجمالي المحل", fontSize = fSmall, fontWeight = FontWeight.Bold, color = cDim, modifier = Modifier.padding(bottom = 12.dp))
        SummaryRow("أصناف في المحل", "$totalOnHand قطعة", cInk, cInk, line = true)
        SummaryRow("مصادر مسجّلة", "${st.sources.size}", cInk, cInk, line = true)
        SummaryRow("تحتاج تحديد مصدر", "$unspec", cDebt, cDebt, line = false)
    }
    // backup — so the ledger is never lost
    SectionLabel("النسخة الاحتياطية")
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.weight(1f)) { PrimaryButton("⤓ حفظ نسخة", fontSize = fBody, radius = rMd, vertical = 13.dp) { shareBackup(ctx, vm.exportJson()) } }
        Box(Modifier.weight(1f)) { OutlineButton("⤒ استعادة", fontSize = fBody, radius = rMd, vertical = 13.dp, filledCard = true) { importer.launch(arrayOf("application/json", "text/*", "*/*")) } }
    }
}

// Maintainer tools (sync bridge, sample data, full wipe) — deliberately NOT part of الملخّص.
// Reached only via the hidden long-press on the دفتر wordmark (SPEC F4): the owner's screen
// carries nothing that can destroy her ledger.
@Composable
internal fun MaintContent(vm: StoreViewModel) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    // sync bridge (FR-8.3): optional one-way push to the owner-tools API; never blocks the app
    Text("المزامنة (اختياري)", fontSize = fSmall, fontWeight = FontWeight.Bold, color = cDim, modifier = Modifier.padding(start = 2.dp, bottom = 8.dp))
    var syncUrl by remember { mutableStateOf(com.daftar.app.sync.SyncWorker.syncUrl(ctx)) }
    Column(Modifier.fillMaxWidth().card(rMd).padding(13.dp)) {
        Text("ترسل نسخة الدفتر إلى حاسوب نوّار عند توفر الاتصال — المحل يعمل دونها تماماً.", fontSize = fCaption, color = cDim, lineHeight = 17.sp, modifier = Modifier.padding(bottom = 9.dp))
        androidx.compose.foundation.text.BasicTextField(
            value = syncUrl,
            onValueChange = { syncUrl = it; com.daftar.app.sync.SyncWorker.setSyncUrl(ctx, it) },
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(rSm)).background(cBg).border(1.dp, cLine, RoundedCornerShape(rSm)).padding(horizontal = 12.dp, vertical = 10.dp),
            textStyle = androidx.compose.ui.text.TextStyle(fontFamily = com.daftar.app.kernel.theme.Plex, fontSize = fSmall, color = cInk),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(cInk), singleLine = true,
            decorationBox = { inner -> Box { if (syncUrl.isEmpty()) Text("عنوان الخادم — http://…/import", fontSize = fSmall, color = cDim); inner() } },
        )
        if (syncUrl.isNotBlank()) {
            Spacer(Modifier.height(9.dp))
            OutlineButton("مزامنة الآن ↥", fontSize = fBody, radius = rSm, vertical = 10.dp) {
                com.daftar.app.sync.SyncWorker.syncNow(ctx)
                android.widget.Toast.makeText(ctx, "ستُرسل النسخة عند توفر الاتصال", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }
    Spacer(Modifier.height(14.dp))
    Box(Modifier.fillMaxWidth().card(rMd).tap { vm.askConfirm("sample") }.padding(13.dp), contentAlignment = Alignment.Center) {
        Text("بيانات تجريبية ⟲", fontSize = fBody, fontWeight = FontWeight.Bold, color = cInk)
    }
    Box(Modifier.fillMaxWidth().padding(top = 14.dp).tap { vm.askConfirm("reset") }, contentAlignment = Alignment.Center) {
        Text("مسح الكل — إظهار البداية", fontSize = fBody, fontWeight = FontWeight.Bold, color = cDebt)
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
        Text(label, fontSize = fBody, color = labelColor)
        Text(value, fontSize = fTitle, fontWeight = FontWeight.Bold, color = valueColor)
    }
}

// ── shared small pieces ──
@Composable
internal fun SectionLabel(text: String) {
    Text(
        text, fontSize = fCaption, letterSpacing = 0.6.sp, fontWeight = FontWeight.Bold, color = cDim,
        modifier = Modifier.padding(top = 18.dp, bottom = 8.dp, start = 2.dp, end = 2.dp),
    )
}

@Composable
internal fun FilterChip(label: String, active: Boolean, dot: Boolean = false, onClick: () -> Unit) {
    Row(
        Modifier.clip(RoundedCornerShape(rXs)).background(if (active) cAccent else cCard).border(1.dp, cLine, RoundedCornerShape(rXs)).tap(onClick).padding(horizontal = 13.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (dot) Box(Modifier.size(7.dp).clip(RoundedCornerShape(50)).background(cDebt))
        Text(label, fontSize = fSmall, fontWeight = FontWeight.Bold, color = if (active) cAink else cDim)
    }
}

@Composable
internal fun PrimaryButton(label: String, fontSize: androidx.compose.ui.unit.TextUnit = fHead, radius: androidx.compose.ui.unit.Dp = rLg, vertical: androidx.compose.ui.unit.Dp = 15.dp, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(radius)).background(cAccent).tap(onClick).padding(vertical = vertical),
        contentAlignment = Alignment.Center,
    ) { Text(label, fontSize = fontSize, fontWeight = FontWeight.Bold, color = cAink) }
}

@Composable
internal fun OutlineButton(label: String, fontSize: androidx.compose.ui.unit.TextUnit = fBodyL, radius: androidx.compose.ui.unit.Dp = rMd, vertical: androidx.compose.ui.unit.Dp = 13.dp, filledCard: Boolean = false, onClick: () -> Unit) {
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
