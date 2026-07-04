package com.daftar.app.reminders

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.daftar.app.kernel.i18n.Str
import com.daftar.app.kernel.ledger.ReminderMath
import com.daftar.app.kernel.theme.DaftarColors
import com.daftar.app.kernel.ui.Eyebrow
import com.daftar.app.kernel.ui.Hairline
import com.daftar.app.kernel.ui.LedgerRow
import com.daftar.app.kernel.ui.SectionCard
import kotlin.math.abs

@Composable
fun RemindersScreen(viewModel: RemindersViewModel = hiltViewModel()) {
    val rows by viewModel.rows.collectAsState()
    var snoozeFor by remember { mutableStateOf<RemindersViewModel.Row?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 24.dp),
    ) {
        item {
            Eyebrow(Str.remindersTitle)
            Spacer(Modifier.height(4.dp))
        }
        if (rows.isEmpty()) {
            item {
                Spacer(Modifier.height(48.dp))
                Text(
                    text = Str.noReminders,
                    style = MaterialTheme.typography.bodyLarge,
                    color = DaftarColors.TextSecondary,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            item {
                SectionCard(modifier = Modifier.padding(top = 8.dp)) {
                    rows.forEachIndexed { index, row ->
                        if (index > 0) Hairline(Modifier.padding(horizontal = 16.dp))
                        ReminderRow(row, onClick = { snoozeFor = row })
                    }
                }
            }
        }
    }

    snoozeFor?.let { row ->
        SnoozeSheet(
            row = row,
            onPick = { snooze ->
                viewModel.snooze(row.customer.id, snooze)
                snoozeFor = null
            },
            onDismiss = { snoozeFor = null },
        )
    }
}

@Composable
private fun ReminderRow(row: RemindersViewModel.Row, onClick: () -> Unit) {
    val color = when (row.urgency) {
        ReminderMath.Urgency.OVERDUE -> DaftarColors.Red
        ReminderMath.Urgency.DUE_SOON -> DaftarColors.Amber
        ReminderMath.Urgency.UPCOMING -> DaftarColors.TextPrimary
    }
    Column(Modifier.clickable(onClick = onClick)) {
        LedgerRow(
            title = row.customer.name,
            subtitle = dueText(row),
            amountText = Str.money(row.balance),
            amountColor = color,
        )
    }
}

private fun dueText(row: RemindersViewModel.Row): String {
    val n = abs(row.daysUntil).toInt()
    val unit = if (n == 1) Str.day else Str.days
    return when {
        row.daysUntil < 0L -> "${Str.overduePrefix} ${Str.count(n)} $unit"
        row.daysUntil == 0L -> Str.dueToday
        else -> "${Str.dueInPrefix} ${Str.count(n)} $unit"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SnoozeSheet(
    row: RemindersViewModel.Row,
    onPick: (ReminderMath.Snooze) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = DaftarColors.Surface1) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
        ) {
            Text(row.customer.name, style = MaterialTheme.typography.titleLarge)
            Text(
                Str.remindAgain,
                style = MaterialTheme.typography.bodyMedium,
                color = DaftarColors.TextSecondary,
                modifier = Modifier.padding(bottom = 4.dp),
            )
            SnoozeOption(Str.snoozeWeek) { onPick(ReminderMath.Snooze.WEEK) }
            Hairline()
            SnoozeOption(Str.snoozeTwoWeeks) { onPick(ReminderMath.Snooze.TWO_WEEKS) }
            Hairline()
            SnoozeOption(Str.snoozeMonth) { onPick(ReminderMath.Snooze.MONTH) }
        }
    }
}

@Composable
private fun SnoozeOption(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        style = MaterialTheme.typography.bodyLarge,
        color = DaftarColors.Teal,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
    )
}
