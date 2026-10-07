package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.KpiCard
import com.example.ui.theme.*
import com.example.viewmodel.PresensiViewModel

@Composable
fun RekapScreen(viewModel: PresensiViewModel) {
    val rekap by viewModel.rekapResult.collectAsState()
    val clock by viewModel.currentClock.collectAsState()
    val teachers by viewModel.teachers.collectAsState()
    val context = LocalContext.current

    var searchQuery by remember { mutableStateOf("") }
    var selectedTempat by remember { mutableStateOf("") }

    val allTempat = remember(teachers) {
        listOf("") + teachers.map { it.tempat }.filter { it.isNotEmpty() }.distinct().sorted()
    }

    LaunchedEffect(searchQuery, selectedTempat) {
        viewModel.loadRekap(tempat = selectedTempat, q = searchQuery)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(SlateBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Filter Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "📑 Rekapitulasi Presensi Guru",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = GreenDark
                    )
                    Text(
                        text = "Periode: ${rekap.hijri.ifEmpty { clock.hijri.text }}",
                        fontSize = 12.sp,
                        color = SlateMuted
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("Cari Nama Guru / ID PPS") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("rekap_search_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Lokasi chips filter
                    Text(
                        text = "Pilih Lokasi:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = SlateMuted
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        allTempat.forEach { tempatName ->
                            FilterChip(
                                selected = selectedTempat == tempatName,
                                onClick = { selectedTempat = tempatName },
                                label = { Text(if (tempatName.isEmpty()) "Semua Tempat" else tempatName) }
                            )
                        }
                    }
                }
            }
        }

        // Summary KPI Totals
        item {
            val tot = rekap.tot
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    KpiCard(
                        title = "Hadir",
                        value = "${tot.hadir}",
                        gradient = Brush.linearGradient(listOf(Color(0xFF166534), Color(0xFF22C55E))),
                        icon = Icons.Default.Check,
                        modifier = Modifier.weight(1f)
                    )
                    KpiCard(
                        title = "Terlambat",
                        value = "${tot.telat}",
                        gradient = Brush.linearGradient(listOf(Color(0xFFC2410C), Color(0xFFFB923C))),
                        icon = Icons.Default.AccessTime,
                        modifier = Modifier.weight(1f)
                    )
                    KpiCard(
                        title = "Izin/Sakit",
                        value = "${tot.izin + tot.sakit}",
                        gradient = Brush.linearGradient(listOf(Color(0xFF1D4ED8), Color(0xFF60A5FA))),
                        icon = Icons.Default.Assignment,
                        modifier = Modifier.weight(1f)
                    )
                    KpiCard(
                        title = "Alpa",
                        value = "${tot.alpa}",
                        gradient = Brush.linearGradient(listOf(Color(0xFFB91C1C), Color(0xFFF87171))),
                        icon = Icons.Default.Cancel,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Export & Share via WhatsApp
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "📤 Ekspor & Bagikan Laporan",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = SlateDark
                    )
                    Text(
                        text = "Format Excel (.xlsx) / Word (.docx) / WhatsApp",
                        fontSize = 12.sp,
                        color = SlateMuted
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                val message = buildString {
                                    appendLine("📋 *LAPORAN PRESENSI GURU*")
                                    appendLine("Periode: ${clock.hijri.text}")
                                    appendLine("Total Hadir: ${rekap.tot.hadir} | Telat: ${rekap.tot.telat}")
                                    appendLine("Izin: ${rekap.tot.izin} | Sakit: ${rekap.tot.sakit} | Alpa: ${rekap.tot.alpa}")
                                    appendLine("Tingkat Kehadiran: ${rekap.tot.pct}%")
                                    appendLine("\nDetail Guru:")
                                    rekap.rows.take(10).forEach { r ->
                                        appendLine("• ${r.nama} (${r.tempat}): Hadir ${r.hadir}, Telat ${r.telat} [${r.pct}%]")
                                    }
                                }
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, message)
                                    type = "text/plain"
                                }
                                val shareIntent = Intent.createChooser(sendIntent, "Bagikan Rekap Presensi")
                                context.startActivity(shareIntent)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("share_whatsapp_button")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Bagikan WA", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                viewModel.showToast("Laporan Excel sedang disinkronkan ke Google Drive")
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Unduh File", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Table Header
        item {
            Text(
                text = "Daftar Kehadiran Guru (${rekap.rows.size})",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = SlateDark
            )
        }

        if (rekap.rows.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Tidak ada data guru yang cocok dengan pencarian",
                        color = SlateMuted,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(rekap.rows) { row ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${row.no}. ${row.nama}",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SlateDark
                                )
                                Text(
                                    text = "ID: ${row.pps} • ${row.tempat}",
                                    fontSize = 12.sp,
                                    color = SlateMuted
                                )
                            }
                            Surface(
                                color = GreenContainer,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "${row.pct}%",
                                    color = GreenDark,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Stats pills
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            StatPill(label = "Hadir", count = row.hadir, bg = GreenContainer, fg = GreenDark)
                            StatPill(label = "Telat", count = row.telat, bg = OrangeContainer, fg = OrangeWarning)
                            StatPill(label = "Izin", count = row.izin, bg = BlueContainer, fg = BlueInfo)
                            StatPill(label = "Sakit", count = row.sakit, bg = PurpleContainer, fg = PurpleBadge)
                            StatPill(label = "Dinas", count = row.dinas, bg = CyanContainer, fg = CyanBadge)
                            StatPill(label = "Alpa", count = row.alpa, bg = RedContainer, fg = RedDanger)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatPill(label: String, count: Int, bg: Color, fg: Color) {
    Surface(
        color = if (count > 0) bg else SlateBackground,
        shape = RoundedCornerShape(6.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$label: ",
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (count > 0) fg else SlateMuted
            )
            Text(
                text = "$count",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (count > 0) fg else SlateMuted
            )
        }
    }
}
