package com.daftar.app.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.daftar.app.kernel.format.ArabicNumbers
import com.daftar.app.kernel.ledger.EntryKind
import com.daftar.app.kernel.theme.DaftarColors
import com.daftar.app.payments.PaymentSheet
import com.daftar.app.payments.PaymentViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

private val timeFormat = DateTimeFormatter.ofPattern("HH:mm")

@Composable
fun TodayScreen(
    snackbarHostState: SnackbarHostState,
    todayViewModel: TodayViewModel = hiltViewModel(),
    paymentViewModel: PaymentViewModel = hiltViewModel(),
) {
    val state by todayViewModel.state.collectAsState()
    val customers by paymentViewModel.customers.collectAsState()
    var showPaymentSheet by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            TotalsHeader(paymentsTotal = state.paymentsTotal)
            if (state.items.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "صفحة اليوم فارغة — سجّلي أول دفعة",
                        style = MaterialTheme.typography.bodyLarge,
                        color = DaftarColors.TextSecondary,
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.items, key = { it.entry.id }) { item -> EntryCard(item) }
                }
            }
        }
        ExtendedFloatingActionButton(
            onClick = { showPaymentSheet = true },
            icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
            text = { Text("دفعة") },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp),
        )
    }

    if (showPaymentSheet) {
        PaymentSheet(
            customers = customers,
            onSave = { amount, customerId ->
                showPaymentSheet = false
                scope.launch {
                    val entryId = paymentViewModel.record(amount, customerId)
                    val result = snackbarHostState.showSnackbar(
                        message = "تم تسجيل الدفعة — ${ArabicNumbers.format(amount)}",
                        actionLabel = "تراجع",
                        duration = SnackbarDuration.Long,
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        paymentViewModel.undo(entryId)
                    }
                }
            },
            onDismiss = { showPaymentSheet = false },
        )
    }
}

@Composable
private fun TotalsHeader(paymentsTotal: Long) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = DaftarColors.Surface1),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("دفتر اليوم", style = MaterialTheme.typography.headlineSmall)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "مقبوضات اليوم",
                    style = MaterialTheme.typography.bodyMedium,
                    color = DaftarColors.TextSecondary,
                )
                Text(
                    ArabicNumbers.format(paymentsTotal),
                    style = MaterialTheme.typography.titleMedium,
                    color = DaftarColors.Teal,
                )
            }
        }
    }
}

@Composable
private fun EntryCard(item: TodayViewModel.Item) {
    val time = Instant.ofEpochMilli(item.entry.happenedAt)
        .atZone(ZoneId.systemDefault())
        .format(timeFormat)
    Card(colors = CardDefaults.cardColors(containerColor = DaftarColors.Surface1)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val kindLabel = when (item.entry.kind) {
                EntryKind.OPENING_BALANCE.name -> "دين قديم"
                else -> "دفعة"
            }
            Column {
                Text(
                    "$kindLabel — ${item.customerName ?: "غير محدد"}",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    time,
                    style = MaterialTheme.typography.labelMedium,
                    color = DaftarColors.TextSecondary,
                )
            }
            Text(
                ArabicNumbers.format(item.entry.amount),
                style = MaterialTheme.typography.titleMedium,
                color = if (item.entry.kind == EntryKind.OPENING_BALANCE.name) DaftarColors.Amber else DaftarColors.Teal,
            )
        }
    }
}
