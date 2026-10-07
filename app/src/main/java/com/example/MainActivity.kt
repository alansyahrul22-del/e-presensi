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
import com.example.ui.screens.*
import com.example.ui.theme.GreenPrimary
import com.example.ui.theme.MyApplicationTheme
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
    val currentScreen by viewModel.currentScreen.collectAsState()
    val toastMsg by viewModel.toastMessage.collectAsState()
    val boot by viewModel.bootConfig.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(toastMsg) {
        toastMsg?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = when (currentScreen) {
                                AppScreen.TERMINAL -> "Terminal Presensi"
                                AppScreen.MONITOR -> "Monitor TV Live"
                                AppScreen.DASHBOARD -> "Dashboard Pimpinan"
                                AppScreen.REKAP -> "Rekapitulasi Laporan"
                                AppScreen.TODAY -> "Presensi Hari Ini"
                                AppScreen.LEAVE -> "Izin & Sakit Guru"
                                AppScreen.SETTINGS -> "Pengaturan Sistem"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = boot.nama,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                },
                actions = {
                    if (currentScreen != AppScreen.TERMINAL) {
                        IconButton(
                            onClick = { viewModel.setScreen(AppScreen.TERMINAL) },
                            modifier = Modifier.testTag("nav_quick_terminal")
                        ) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = "Terminal Scanner", tint = GreenPrimary)
                        }
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
                    selected = currentScreen == AppScreen.TERMINAL,
                    onClick = { viewModel.setScreen(AppScreen.TERMINAL) },
                    icon = { Icon(Icons.Default.QrCodeScanner, contentDescription = "Terminal") },
                    label = { Text("Terminal", fontSize = 11.sp) },
                    modifier = Modifier.testTag("nav_terminal")
                )
                NavigationBarItem(
                    selected = currentScreen == AppScreen.MONITOR,
                    onClick = { viewModel.setScreen(AppScreen.MONITOR) },
                    icon = { Icon(Icons.Default.Tv, contentDescription = "Monitor") },
                    label = { Text("Monitor", fontSize = 11.sp) },
                    modifier = Modifier.testTag("nav_monitor")
                )
                NavigationBarItem(
                    selected = currentScreen == AppScreen.DASHBOARD,
                    onClick = { viewModel.setScreen(AppScreen.DASHBOARD) },
                    icon = { Icon(Icons.Default.BarChart, contentDescription = "Dashboard") },
                    label = { Text("Dashboard", fontSize = 11.sp) },
                    modifier = Modifier.testTag("nav_dashboard")
                )
                NavigationBarItem(
                    selected = currentScreen == AppScreen.REKAP,
                    onClick = { viewModel.setScreen(AppScreen.REKAP) },
                    icon = { Icon(Icons.Default.Description, contentDescription = "Rekap") },
                    label = { Text("Rekap", fontSize = 11.sp) },
                    modifier = Modifier.testTag("nav_rekap")
                )
                NavigationBarItem(
                    selected = currentScreen == AppScreen.TODAY,
                    onClick = { viewModel.setScreen(AppScreen.TODAY) },
                    icon = { Icon(Icons.Default.CheckCircle, contentDescription = "Hari Ini") },
                    label = { Text("Hari Ini", fontSize = 11.sp) },
                    modifier = Modifier.testTag("nav_today")
                )
                NavigationBarItem(
                    selected = currentScreen == AppScreen.LEAVE,
                    onClick = { viewModel.setScreen(AppScreen.LEAVE) },
                    icon = { Icon(Icons.Default.EventNote, contentDescription = "Izin") },
                    label = { Text("Izin", fontSize = 11.sp) },
                    modifier = Modifier.testTag("nav_leave")
                )
                NavigationBarItem(
                    selected = currentScreen == AppScreen.SETTINGS,
                    onClick = { viewModel.setScreen(AppScreen.SETTINGS) },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Pengaturan") },
                    label = { Text("Setting", fontSize = 11.sp) },
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
                AppScreen.TERMINAL -> TerminalScreen(viewModel = viewModel)
                AppScreen.MONITOR -> MonitorScreen(viewModel = viewModel)
                AppScreen.DASHBOARD -> DashboardScreen(viewModel = viewModel)
                AppScreen.REKAP -> RekapScreen(viewModel = viewModel)
                AppScreen.TODAY -> TodayAdminScreen(viewModel = viewModel)
                AppScreen.LEAVE -> LeaveAdminScreen(viewModel = viewModel)
                AppScreen.SETTINGS -> SettingsScreen(viewModel = viewModel)
            }
        }
    }
}
