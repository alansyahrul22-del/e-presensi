package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.KpiCard
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.viewmodel.PresensiViewModel

@Composable
fun MonitorScreen(viewModel: PresensiViewModel) {
    val boot by viewModel.bootConfig.collectAsState()
    val clock by viewModel.currentClock.collectAsState()
    val data by viewModel.dashboardData.collectAsState()
    val summary = data.s

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SlateDark)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Live Header with Solar Clock
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SlateCard),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = GreenPrimary,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "MONITOR LIVE TV",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = boot.nama,
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = clock.hijri.text,
                                color = GreenLight,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = clock.jam,
                                fontSize = 42.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                color = GreenLight,
                                lineHeight = 44.sp
                            )
                            Text(
                                text = "JAM ISTIWAK",
                                color = SlateMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }
            }

            // High-impact KPI grid
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        KpiCard(
                            title = "Total Guru",
                            value = "${summary.total}",
                            gradient = Brush.linearGradient(listOf(Color(0xFF334155), Color(0xFF64748B))),
                            icon = Icons.Default.Groups,
                            modifier = Modifier.weight(1f)
                        )
                        KpiCard(
                            title = "Sudah Scan",
                            value = "${summary.sudah}",
                            gradient = Brush.linearGradient(listOf(Color(0xFF166534), Color(0xFF22C55E))),
                            icon = Icons.Default.CheckCircle,
                            modifier = Modifier.weight(1f)
                        )
                        KpiCard(
                            title = "Belum Presensi",
                            value = "${summary.belum}",
                            gradient = Brush.linearGradient(listOf(Color(0xFF0F172A), Color(0xFF334155))),
                            icon = Icons.Default.PendingActions,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        KpiCard(
                            title = "Tepat Waktu",
                            value = "${summary.hadir}",
                            gradient = Brush.linearGradient(listOf(Color(0xFF15803D), Color(0xFF4ADE80))),
                            icon = Icons.Default.AlarmOn,
                            modifier = Modifier.weight(1f)
                        )
                        KpiCard(
                            title = "Terlambat",
                            value = "${summary.telat}",
                            gradient = Brush.linearGradient(listOf(Color(0xFFC2410C), Color(0xFFFB923C))),
                            icon = Icons.Default.AlarmOff,
                            modifier = Modifier.weight(1f)
                        )
                        KpiCard(
                            title = "Izin / Dinas",
                            value = "${summary.izin + summary.dinas}",
                            gradient = Brush.linearGradient(listOf(Color(0xFF1D4ED8), Color(0xFF60A5FA))),
                            icon = Icons.Default.Assignment,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 2 Columns: Live Presensi Scan & Guru Belum Presensi
            item {
                Text(
                    text = "⏱ 10 PRESENSI TERBARU",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (data.latest.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SlateCard),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Belum ada presensi yang tercatat hari ini.",
                            color = SlateMuted,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                items(data.latest.take(10)) { record ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = SlateCard),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = Color.Black.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = record.jam,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = GreenLight,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = record.nama,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "${record.tempat} • Terminal: ${record.terminal}",
                                        color = SlateMuted,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                            StatusBadge(status = record.status)
                        }
                    }
                }
            }

            // Guru Belum Presensi Section
            item {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⏳ GURU BELUM PRESENSI (${data.belum.size})",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (data.belum.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF064E3B)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "🎉 Alhamdulillah, semua guru telah melakukan presensi!",
                            color = Color(0xFFA7F3D0),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                items(data.belum) { teacher ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = SlateCard),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = teacher.nama,
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "ID: ${teacher.pps} • ${teacher.tempat}",
                                    color = SlateMuted,
                                    fontSize = 12.sp
                                )
                            }
                            Surface(
                                color = Color(0xFF334155),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "BELUM",
                                    color = Color(0xFFCBD5E1),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
