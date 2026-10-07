package com.example.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.TrendPoint
import com.example.ui.components.KpiCard
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.viewmodel.PresensiViewModel

@Composable
fun DashboardScreen(viewModel: PresensiViewModel) {
    val boot by viewModel.bootConfig.collectAsState()
    val clock by viewModel.currentClock.collectAsState()
    val data by viewModel.dashboardData.collectAsState()
    val summary = data.s

    var trendMode by remember { mutableStateOf("daily") } // daily, weekly, monthly

    val attendancePct = if (summary.total > 0) {
        Math.round((summary.sudah.toDouble() / summary.total) * 1000.0) / 10.0
    } else 0.0

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(SlateBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${clock.hari}, ${clock.tgl}",
                                fontSize = 12.sp,
                                color = SlateMuted,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = clock.hijri.text.uppercase(),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = GreenDark
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "$attendancePct%",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Black,
                                color = GreenPrimary
                            )
                            Text(
                                text = "KEHADIRAN",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = SlateMuted
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "${boot.nama} • Jam Istiwak: ${clock.jam}",
                        fontSize = 12.sp,
                        color = SlateMuted
                    )
                }
            }
        }

        // 3x2 KPI Grid
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
                        title = "Sudah Presensi",
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
                        title = "Izin/Sakit/Dinas",
                        value = "${summary.izin + summary.sakit + summary.dinas}",
                        gradient = Brush.linearGradient(listOf(Color(0xFF1D4ED8), Color(0xFF60A5FA))),
                        icon = Icons.Default.Assignment,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Native Trend Chart Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Tren Kehadiran Guru",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = SlateDark
                            )
                            Text(
                                text = "Garis Hijau = % Hadir, Batang Oranye = Terlambat",
                                fontSize = 11.sp,
                                color = SlateMuted
                            )
                        }

                        // Segmented control buttons
                        Row(
                            modifier = Modifier
                                .background(SlateBackground, RoundedCornerShape(10.dp))
                                .padding(3.dp)
                        ) {
                            listOf("daily" to "Harian", "weekly" to "Pekanan", "monthly" to "Bulanan").forEach { (key, label) ->
                                val selected = trendMode == key
                                Surface(
                                    onClick = { trendMode = key },
                                    color = if (selected) Color.White else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp),
                                    shadowElevation = if (selected) 1.dp else 0.dp
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (selected) GreenDark else SlateMuted,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    val points = when (trendMode) {
                        "weekly" -> data.trend.weekly
                        "monthly" -> data.trend.monthly
                        else -> data.trend.daily
                    }

                    TrendChartCanvas(
                        points = points,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    )
                }
            }
        }

        // Kehadiran per Tempat & Progress Bars
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "🏫 Kehadiran per Lokasi / Tempat",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = SlateDark
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    data.tempat.forEach { place ->
                        val pct = if (place.total > 0) (place.sudah.toFloat() / place.total.toFloat()) else 0f
                        val pctInt = (pct * 100).toInt()

                        Column(modifier = Modifier.padding(vertical = 6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = place.nama,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = SlateDark
                                )
                                Text(
                                    text = "${place.sudah}/${place.total}  •  $pctInt%",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GreenDark
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { pct },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp),
                                color = GreenPrimary,
                                trackColor = SlateBorder
                            )
                        }
                    }
                }
            }
        }

        // Presensi Terbaru List
        item {
            Text(
                text = "⏱ Presensi Terbaru Hari Ini",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = SlateDark,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (data.latest.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Belum ada catatan presensi hari ini",
                        color = SlateMuted,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(data.latest) { record ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = GreenContainer,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = record.jam,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = GreenDark,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = record.nama,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SlateDark
                                )
                                Text(
                                    text = "${record.tempat} • Terminal: ${record.terminal}",
                                    fontSize = 12.sp,
                                    color = SlateMuted
                                )
                            }
                        }
                        StatusBadge(status = record.status)
                    }
                }
            }
        }
    }
}

@Composable
fun TrendChartCanvas(
    points: List<TrendPoint>,
    modifier: Modifier = Modifier
) {
    if (points.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("Tidak ada data tren", color = SlateMuted, fontSize = 12.sp)
        }
        return
    }

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val bottomMargin = 28f
        val topMargin = 12f
        val chartHeight = height - bottomMargin - topMargin
        val numPoints = points.size

        val spacing = if (numPoints > 1) width / (numPoints - 1) else width / 2

        // Draw horizontal grid lines (0%, 50%, 100%)
        val gridLines = listOf(0f, 50f, 100f)
        gridLines.forEach { value ->
            val y = topMargin + chartHeight * (1f - (value / 100f))
            drawLine(
                color = SlateBorder,
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 1.dp.toPx()
            )
        }

        // Draw orange bars for late count
        val maxLate = (points.maxOfOrNull { it.late } ?: 1).coerceAtLeast(1)
        val barWidth = 14.dp.toPx()

        points.forEachIndexed { index, p ->
            val x = if (numPoints > 1) index * spacing else width / 2
            val lateBarHeight = (p.late.toFloat() / maxLate.toFloat()) * (chartHeight * 0.45f)
            if (p.late > 0) {
                drawRect(
                    color = OrangeWarning.copy(alpha = 0.65f),
                    topLeft = Offset(x - barWidth / 2, height - bottomMargin - lateBarHeight),
                    size = Size(barWidth, lateBarHeight)
                )
            }
        }

        // Build green line path
        val path = Path()
        val fillPath = Path()

        points.forEachIndexed { index, p ->
            val x = if (numPoints > 1) index * spacing else width / 2
            val y = topMargin + chartHeight * (1f - (p.pct.toFloat() / 100f).coerceIn(0f, 1f))
            if (index == 0) {
                path.moveTo(x, y)
                fillPath.moveTo(x, height - bottomMargin)
                fillPath.lineTo(x, y)
            } else {
                path.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
        }

        val lastX = if (numPoints > 1) (numPoints - 1) * spacing else width / 2
        fillPath.lineTo(lastX, height - bottomMargin)
        fillPath.close()

        // Gradient under curve
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(GreenLight.copy(alpha = 0.35f), Color.Transparent),
                startY = topMargin,
                endY = height - bottomMargin
            )
        )

        // Line
        drawPath(
            path = path,
            color = GreenPrimary,
            style = Stroke(width = 3.dp.toPx())
        )

        // Points
        points.forEachIndexed { index, p ->
            val x = if (numPoints > 1) index * spacing else width / 2
            val y = topMargin + chartHeight * (1f - (p.pct.toFloat() / 100f).coerceIn(0f, 1f))
            drawCircle(
                color = Color.White,
                radius = 4.dp.toPx(),
                center = Offset(x, y)
            )
            drawCircle(
                color = GreenPrimary,
                radius = 4.dp.toPx(),
                style = Stroke(width = 2.dp.toPx()),
                center = Offset(x, y)
            )
        }
    }
}
