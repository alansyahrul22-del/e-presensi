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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Teacher
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.viewmodel.PresensiViewModel

@Composable
fun TodayAdminScreen(viewModel: PresensiViewModel) {
    val clock by viewModel.currentClock.collectAsState()
    val data by viewModel.dashboardData.collectAsState()
    val teachers by viewModel.teachers.collectAsState()

    var showCorrectionDialog by remember { mutableStateOf(false) }
    var selectedTeacher by remember { mutableStateOf<Teacher?>(null) }
    var selectedStatus by remember { mutableStateOf("HADIR") }
    var inputJam by remember { mutableStateOf(clock.jam) }
    var inputAlasan by remember { mutableStateOf("") }
    var inputKet by remember { mutableStateOf("") }
    var filterQuery by remember { mutableStateOf("") }

    val attendedMap = remember(data.latest) {
        data.latest.associateBy { it.pps }
    }

    val filteredTeachers = remember(teachers, filterQuery) {
        teachers.filter {
            filterQuery.isEmpty() ||
            it.nama.contains(filterQuery, ignoreCase = true) ||
            it.pps.contains(filterQuery) ||
            it.tempat.contains(filterQuery, ignoreCase = true)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(SlateBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header & Quick Action Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "✅ Kelola Presensi Hari Ini",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = GreenDark
                            )
                            Text(
                                text = "${clock.hari}, ${clock.tgl}  •  ${clock.hijri.text}",
                                fontSize = 12.sp,
                                color = SlateMuted
                            )
                        }

                        Button(
                            onClick = {
                                selectedTeacher = null
                                selectedStatus = "HADIR"
                                inputJam = clock.jam
                                inputAlasan = "Input manual pengurus"
                                inputKet = ""
                                showCorrectionDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Manual", fontSize = 13.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = filterQuery,
                        onValueChange = { filterQuery = it },
                        label = { Text("Cari Guru / ID PPS / Gedung") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("today_search_input")
                    )
                }
            }
        }

        // Close Day Warning Banner
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
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
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "🔒 Tutup Presensi Hari Ini",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = RedDanger
                        )
                        Text(
                            text = "Guru yang belum presensi akan otomatis berstatus ALPA",
                            fontSize = 12.sp,
                            color = SlateDark
                        )
                    }

                    Button(
                        onClick = { viewModel.closeDay(clock.tgl) },
                        colors = ButtonDefaults.buttonColors(containerColor = RedDanger),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Tutup Hari", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Teacher List
        items(filteredTeachers) { teacher ->
            val attendance = attendedMap[teacher.pps]
            val status = attendance?.status ?: "BELUM"
            val jam = attendance?.jam ?: "-"

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
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = teacher.nama,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = SlateDark
                        )
                        Text(
                            text = "ID: ${teacher.pps} • ${teacher.tempat} • Jam: $jam",
                            fontSize = 12.sp,
                            color = SlateMuted
                        )
                        if (attendance != null && attendance.ket.isNotEmpty()) {
                            Text(
                                text = "Ket: ${attendance.ket}",
                                fontSize = 11.sp,
                                color = GreenDark
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StatusBadge(status = status)
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                selectedTeacher = teacher
                                selectedStatus = if (status != "BELUM") status else "HADIR"
                                inputJam = if (jam != "-") jam else clock.jam
                                inputAlasan = ""
                                inputKet = attendance?.ket ?: ""
                                showCorrectionDialog = true
                            }
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Koreksi", tint = GreenPrimary)
                        }
                    }
                }
            }
        }
    }

    // Koreksi / Input Dialog
    if (showCorrectionDialog) {
        AlertDialog(
            onDismissRequest = { showCorrectionDialog = false },
            title = {
                Text(
                    text = if (selectedTeacher != null) "Koreksi Presensi" else "Presensi Manual",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (selectedTeacher != null) {
                        Text(
                            text = "${selectedTeacher?.nama} (${selectedTeacher?.pps})",
                            fontWeight = FontWeight.SemiBold,
                            color = GreenDark
                        )
                    } else {
                        Text("Pilih dari daftar guru", fontSize = 12.sp, color = SlateMuted)
                    }

                    // Status Dropdown / Buttons
                    Text("Pilih Status Kehadiran:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("HADIR", "TERLAMBAT", "IZIN", "SAKIT", "ALPA").forEach { st ->
                            val isSel = selectedStatus == st
                            Surface(
                                onClick = { selectedStatus = st },
                                color = if (isSel) GreenPrimary else SlateBackground,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = st,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) Color.White else SlateDark,
                                    modifier = Modifier.padding(vertical = 6.dp, horizontal = 2.dp)
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = inputJam,
                        onValueChange = { inputJam = it },
                        label = { Text("Jam Istiwak (HH:mm)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = inputAlasan,
                        onValueChange = { inputAlasan = it },
                        label = { Text("Alasan Koreksi (Wajib)") },
                        placeholder = { Text("mis. Mesin scanner offline") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = inputKet,
                        onValueChange = { inputKet = it },
                        label = { Text("Keterangan Tambahan") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val pps = selectedTeacher?.pps ?: teachers.firstOrNull()?.pps ?: ""
                        if (pps.isNotEmpty()) {
                            viewModel.updateAttendanceManual(
                                pps = pps,
                                tgl = clock.tgl,
                                status = selectedStatus,
                                jam = inputJam,
                                ket = inputKet,
                                alasan = inputAlasan.ifEmpty { "Koreksi petugas tata usaha" }
                            )
                        }
                        showCorrectionDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
                ) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCorrectionDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
}
