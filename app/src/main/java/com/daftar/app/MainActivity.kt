package com.daftar.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.daftar.app.customers.CustomersScreen
import com.daftar.app.reminders.RemindersScreen
import com.daftar.app.kernel.i18n.Str
import com.daftar.app.kernel.theme.Amiri
import com.daftar.app.kernel.theme.DaftarColors
import com.daftar.app.kernel.theme.DaftarTheme
import com.daftar.app.stock.StockScreen
import com.daftar.app.today.TodayScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // SDK 36 enforces edge-to-edge; opt in explicitly and pad insets in Compose
        setContent {
            DaftarTheme {
                MainScaffold()
            }
        }
    }
}

private data class Tab(val title: String, val icon: ImageVector)

@Composable
private fun MainScaffold() {
    var selected by rememberSaveable { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Back from any tab returns to اليوم rather than dropping out of the app;
    // only اليوم (the home page) exits on back. Overlays/sheets handle their own back first.
    BackHandler(enabled = selected != 0) { selected = 0 }

    // Ask for the notification permission once (Android 13+) so the daily reminder
    // digest can be delivered; declining leaves the in-app المواعيد tab fully working.
    val context = LocalContext.current
    val notifPermission =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val tabs = listOf(
        Tab(Str.tabToday, Icons.Outlined.MenuBook),
        Tab(Str.tabCustomers, Icons.Outlined.Group),
        Tab(Str.tabReminders, Icons.Outlined.Alarm),
        Tab(Str.tabAccount, Icons.Outlined.Assessment),
    )

    Scaffold(
        containerColor = DaftarColors.Surface0,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding() // keep the header clear of the status bar / notch
                    .padding(start = 16.dp, end = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text( // the ledger wordmark — Amiri serif, oxblood (matches the prototype)
                    "دفتر",
                    fontFamily = Amiri,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    color = DaftarColors.Oxblood,
                )
                TextButton(onClick = { Str.arabic = !Str.arabic }) {
                    Text(Str.appToggle, color = DaftarColors.TextSecondary)
                }
            }
        },
        bottomBar = {
            NavigationBar(containerColor = DaftarColors.Surface1) {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selected == index,
                        onClick = { selected = index },
                        icon = { Icon(tab.icon, contentDescription = tab.title) },
                        label = { Text(tab.title) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = DaftarColors.OnTeal,
                            selectedTextColor = DaftarColors.Teal,
                            indicatorColor = DaftarColors.Teal,
                            unselectedIconColor = DaftarColors.TextSecondary,
                            unselectedTextColor = DaftarColors.TextSecondary,
                        ),
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding)) {
            when (selected) {
                0 -> TodayScreen(snackbarHostState)
                1 -> CustomersScreen()
                2 -> RemindersScreen()
                3 -> StockScreen()
            }
        }
    }
}
