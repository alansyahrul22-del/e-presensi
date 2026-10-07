package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.UserRole
import com.example.ui.screens.*
import com.example.ui.theme.GreenPrimary
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.RedDanger
import com.example.viewmodel.AppScreen
import com.example.viewmodel.PresensiViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: PresensiViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppScaffold(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScaffold(viewModel: PresensiViewModel) {
    val currentUser by viewModel.currentUser.collectAsState()
    val currentScreen by viewModel.currentScreen.collectAsState()
    val toastMsg by viewModel.toastMessage.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(toastMsg) {
        toastMsg?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }

    // Jika belum login -> Tampilkan LoginScreen
    if (currentUser == null) {
        LoginScreen(viewModel = viewModel)
        return
    }

    val user = currentUser!!

    // KETIKA LOGIN SEBAGAI TERMINAL (T01 - T05):
    // Hanya boleh melihat Scanner (TerminalScreen) tanpa bottom bar dan tanpa akses fitur lain!
    if (user.role == UserRole.TERMINAL) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "Presensi Guru MMU Idadiyah",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 17.sp
                            )
                            Text(
                                text = "Mode Kios Scanner: ${user.displayName} (${user.terminalId})",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { viewModel.logout() },
                            modifier = Modifier.testTag("terminal_logout_button")
                        ) {
                            Icon(Icons.Default.Logout, contentDescription = "Keluar", tint = RedDanger)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                TerminalScreen(viewModel = viewModel)
            }
        }
        return
    }

    // KETIKA LOGIN SEBAGAI ADMIN (TU1..TU3, WK1..WK6, Kepsek):
    // Memiliki akses ke SEMUA FITUR SELAIN SCAN: Monitor, Dashboard, Rekap, Hari Ini, Izin, Setting
    // Label navigasi dibuat rapi sesuai dengan gambar kedua yang diberikan pengguna (Monitor, Dashboard, Rekap, Hari Ini, Izin, Setting)
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Presensi Guru MMU Idadiyah",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Text(
                            text = "${user.displayName} (${user.username})  •  " + when (currentScreen) {
                                AppScreen.MONITOR -> "Monitor TV Live"
                                AppScreen.DASHBOARD -> "Dashboard Pimpinan"
                                AppScreen.REKAP -> "Rekap Laporan"
                                AppScreen.TODAY -> "Presensi Hari Ini"
                                AppScreen.LEAVE -> "Izin Guru"
                                AppScreen.SETTINGS -> "Pengaturan"
                                else -> "Beranda"
                            },
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.logout() },
                        modifier = Modifier.testTag("admin_logout_button")
                    ) {
                        Icon(Icons.Default.Logout, contentDescription = "Keluar Akun", tint = RedDanger)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = currentScreen == AppScreen.MONITOR,
                    onClick = { viewModel.setScreen(AppScreen.MONITOR) },
                    icon = { Icon(Icons.Default.Tv, contentDescription = "Monitor") },
                    label = { Text("Monitor", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                    modifier = Modifier.testTag("nav_monitor")
                )
                NavigationBarItem(
                    selected = currentScreen == AppScreen.DASHBOARD,
                    onClick = { viewModel.setScreen(AppScreen.DASHBOARD) },
                    icon = { Icon(Icons.Default.BarChart, contentDescription = "Dashboard") },
                    label = { Text("Dashboard", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                    modifier = Modifier.testTag("nav_dashboard")
                )
                NavigationBarItem(
                    selected = currentScreen == AppScreen.REKAP,
                    onClick = { viewModel.setScreen(AppScreen.REKAP) },
                    icon = { Icon(Icons.Default.Description, contentDescription = "Rekap") },
                    label = { Text("Rekap", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                    modifier = Modifier.testTag("nav_rekap")
                )
                NavigationBarItem(
                    selected = currentScreen == AppScreen.TODAY,
                    onClick = { viewModel.setScreen(AppScreen.TODAY) },
                    icon = { Icon(Icons.Default.CheckCircle, contentDescription = "Hari Ini") },
                    label = { Text("Hari Ini", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                    modifier = Modifier.testTag("nav_today")
                )
                NavigationBarItem(
                    selected = currentScreen == AppScreen.LEAVE,
                    onClick = { viewModel.setScreen(AppScreen.LEAVE) },
                    icon = { Icon(Icons.Default.EventNote, contentDescription = "Izin") },
                    label = { Text("Izin", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                    modifier = Modifier.testTag("nav_leave")
                )
                NavigationBarItem(
                    selected = currentScreen == AppScreen.SETTINGS,
                    onClick = { viewModel.setScreen(AppScreen.SETTINGS) },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Setting") },
                    label = { Text("Setting", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                    modifier = Modifier.testTag("nav_settings")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                AppScreen.MONITOR -> MonitorScreen(viewModel = viewModel)
                AppScreen.DASHBOARD -> DashboardScreen(viewModel = viewModel)
                AppScreen.REKAP -> RekapScreen(viewModel = viewModel)
                AppScreen.TODAY -> TodayAdminScreen(viewModel = viewModel)
                AppScreen.LEAVE -> LeaveAdminScreen(viewModel = viewModel)
                AppScreen.SETTINGS -> SettingsScreen(viewModel = viewModel)
                else -> DashboardScreen(viewModel = viewModel)
            }
        }
    }
}
