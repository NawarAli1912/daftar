package com.daftar.app.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.daftar.app.kernel.i18n.Str
import com.daftar.app.kernel.ledger.EntryKind
import com.daftar.app.kernel.theme.DaftarColors
import com.daftar.app.kernel.ui.Eyebrow
import com.daftar.app.kernel.ui.Hairline
import com.daftar.app.kernel.ui.LedgerRow
import com.daftar.app.kernel.ui.SectionCard
import com.daftar.app.kernel.ui.SumLine
import com.daftar.app.payments.PaymentSheet
import com.daftar.app.payments.PaymentViewModel
import com.daftar.app.sales.SaleScreen
import com.daftar.app.sales.SalesViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

private val timeFormat = DateTimeFormatter.ofPattern("HH:mm")

private fun formatTime(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).format(timeFormat)

@Composable
fun TodayScreen(
    snackbarHostState: SnackbarHostState,
    todayViewModel: TodayViewModel = hiltViewModel(),
    paymentViewModel: PaymentViewModel = hiltViewModel(),
    salesViewModel: SalesViewModel = hiltViewModel(),
) {
    val state by todayViewModel.state.collectAsState()
    val customers by paymentViewModel.customers.collectAsState()
    var showChooser by remember { mutableStateOf(false) }
    var entrySheet by remember { mutableStateOf<EntryType?>(null) }
    var showSaleScreen by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    AnimatedVisibility(
        visible = showSaleScreen,
        enter = slideInVertically(initialOffsetY = { it / 3 }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it / 3 }) + fadeOut(),
    ) {
        SaleScreen(
            onSave = { lines, customerId, paidNow ->
                showSaleScreen = false
                scope.launch {
                    val saleId = salesViewModel.record(lines, customerId, paidNow)
                    val result = snackbarHostState.showSnackbar(
                        message = Str.saleSaved,
                        actionLabel = Str.undo,
                        duration = SnackbarDuration.Long,
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        salesViewModel.undo(saleId)
                    }
                }
            },
            onClose = { showSaleScreen = false },
        )
    }
    if (showSaleScreen) return

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 160.dp),
        ) {
            item {
                Eyebrow(Str.todayBook)
                Spacer(Modifier.height(4.dp))
            }
            if (state.cards.isEmpty()) {
                item {
                    Spacer(Modifier.height(48.dp))
                    Text(
                        Str.emptyDay,
                        style = MaterialTheme.typography.bodyLarge,
                        color = DaftarColors.TextSecondary,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                item {
                    SectionCard(modifier = Modifier.padding(top = 8.dp).animateContentSize()) {
                        state.cards.forEachIndexed { index, card ->
                            if (index > 0) Hairline(Modifier.padding(horizontal = 16.dp))
                            when (card) {
                                is DayCard.Ledger -> LedgerEntryRow(card)
                                is DayCard.Sale -> SaleEntryRow(card)
                            }
                        }
                        SumLine(Str.receivedToday, Str.money(state.paymentsTotal))
                    }
                }
            }
        }
        ExtendedFloatingActionButton(
            onClick = { showChooser = true },
            icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
            text = { Text(Str.newEntry) },
            containerColor = DaftarColors.Teal,
            contentColor = DaftarColors.OnTeal,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp),
        )
    }

    if (showChooser) {
        EntryChooser(
            onPick = { type ->
                showChooser = false
                when (type) {
                    EntryType.SALE -> showSaleScreen = true
                    else -> entrySheet = type
                }
            },
            onDismiss = { showChooser = false },
        )
    }

    if (entrySheet != null) {
        val isReturn = entrySheet == EntryType.RETURN
        val types by paymentViewModel.types.collectAsState()
        PaymentSheet(
            customers = customers,
            types = types,
            suggestionLabel = paymentViewModel::suggestionLabel,
            title = if (isReturn) Str.newReturn else Str.newPayment,
            saveLabel = if (isReturn) Str.saveReturn else Str.savePayment,
            onAddCustomer = { name, onCreated ->
                scope.launch { paymentViewModel.addCustomerInline(name)?.let(onCreated) }
            },
            onSave = { amount, customerId, itemTypeId, askedUnit ->
                entrySheet = null
                scope.launch {
                    val entryId = paymentViewModel.record(
                        amount, customerId, itemTypeId, askedUnit,
                        kind = if (isReturn) EntryKind.RETURN else EntryKind.PAYMENT,
                    )
                    val label = if (isReturn) Str.saveReturn else Str.paymentSaved
                    val result = snackbarHostState.showSnackbar(
                        message = "$label — ${Str.money(amount)}",
                        actionLabel = Str.undo,
                        duration = SnackbarDuration.Long,
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        paymentViewModel.undo(entryId)
                    }
                }
            },
            onDismiss = { entrySheet = null },
        )
    }
}

@Composable
private fun LedgerEntryRow(card: DayCard.Ledger) {
    val kindLabel = when (card.entry.kind) {
        EntryKind.OPENING_BALANCE.name -> Str.oldDebt
        EntryKind.RETURN.name -> Str.entryReturn
        else -> Str.payment
    }
    val color = when (card.entry.kind) {
        EntryKind.OPENING_BALANCE.name -> DaftarColors.Amber
        EntryKind.RETURN.name -> DaftarColors.Green
        else -> DaftarColors.Teal
    }
    LedgerRow(
        title = card.customerName ?: Str.unspecified,
        subtitle = "$kindLabel · ${formatTime(card.entry.happenedAt)}",
        amountText = Str.money(card.entry.amount),
        amountColor = color,
    )
}

@Composable
private fun SaleEntryRow(card: DayCard.Sale) {
    LedgerRow(
        title = card.customerName ?: Str.unspecified,
        subtitle = buildString {
            append(Str.sale)
            if (card.linesSummary.isNotEmpty()) append(" · ${card.linesSummary}")
            append(" · ${formatTime(card.happenedAt)}")
        },
        amountText = Str.money(card.total),
        amountColor = DaftarColors.Teal,
        trailingNote = if (card.paidNow in 1 until card.total) {
            "${Str.paidShort} ${Str.money(card.paidNow)}"
        } else null,
    )
}
