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
fun LeaveAdminScreen(viewModel: PresensiViewModel) {
    val clock by viewModel.currentClock.collectAsState()
    val teachers by viewModel.teachers.collectAsState()
    val leaves by viewModel.leaves.collectAsState()

    var selectedTeacher by remember { mutableStateOf<Teacher?>(null) }
    var selectedJenis by remember { mutableStateOf("IZIN") }
    var tglAwal by remember { mutableStateOf(clock.tgl) }
    var tglAkhir by remember { mutableStateOf(clock.tgl) }
    var keterangan by remember { mutableStateOf("") }
    var teacherSearch by remember { mutableStateOf("") }

    val filteredTeachers = remember(teachers, teacherSearch) {
        if (teacherSearch.isEmpty()) teachers
        else teachers.filter {
            it.nama.contains(teacherSearch, ignoreCase = true) || it.pps.contains(teacherSearch)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(SlateBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Form Input Izin / Sakit / Dinas
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "📝 Input Izin, Sakit & Dinas Guru",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = GreenDark
                    )
                    Text(
                        text = "Data akan tercatat dalam sheet IZIN dan otomatis mengecualikan dari ALPA",
                        fontSize = 12.sp,
                        color = SlateMuted
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Teacher Picker
                    Text("Pilih Guru:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = teacherSearch,
                        onValueChange = {
                            teacherSearch = it
                            selectedTeacher = null
                        },
                        placeholder = { Text("Ketik nama atau ID PPS...") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("leave_teacher_search")
                    )

                    if (selectedTeacher != null) {
                        Surface(
                            color = GreenContainer,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = selectedTeacher?.nama ?: "",
                                        fontWeight = FontWeight.Bold,
                                        color = GreenDark
                                    )
                                    Text(
                                        text = "ID: ${selectedTeacher?.pps} • ${selectedTeacher?.tempat}",
                                        fontSize = 12.sp,
                                        color = SlateMuted
                                    )
                                }
                                IconButton(onClick = { selectedTeacher = null }) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = GreenDark)
                                }
                            }
                        }
                    } else if (teacherSearch.isNotEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp)
                        ) {
                            filteredTeachers.take(4).forEach { t ->
                                TextButton(
                                    onClick = {
                                        selectedTeacher = t
                                        teacherSearch = t.nama
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(t.nama, color = SlateDark, fontWeight = FontWeight.SemiBold)
                                        Text(t.pps, color = SlateMuted)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Jenis Izin
                    Text("Jenis Keperluan:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("IZIN", "SAKIT", "DINAS").forEach { jenis ->
                            val isSel = selectedJenis == jenis
                            FilterChip(
                                selected = isSel,
                                onClick = { selectedJenis = jenis },
                                label = { Text(jenis, fontWeight = FontWeight.Bold) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = tglAwal,
                            onValueChange = { tglAwal = it },
                            label = { Text("Tgl Mulai") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = tglAkhir,
                            onValueChange = { tglAkhir = it },
                            label = { Text("Tgl Selesai") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = keterangan,
                        onValueChange = { keterangan = it },
                        label = { Text("Keterangan Detail (Alasan / Surat)") },
                        placeholder = { Text("mis. Menghadiri rapat KKM di Kemenag") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("leave_note_input")
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            val teacher = selectedTeacher ?: teachers.firstOrNull()
                            if (teacher != null) {
                                viewModel.saveLeave(
                                    pps = teacher.pps,
                                    jenis = selectedJenis,
                                    tglAwal = tglAwal,
                                    tglAkhir = tglAkhir,
                                    ket = keterangan.ifEmpty { "Disetujui Tata Usaha" }
                                )
                                keterangan = ""
                                selectedTeacher = null
                                teacherSearch = ""
                            } else {
                                viewModel.showToast("Silakan pilih guru terlebih dahulu")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("save_leave_button")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("SIMPAN DATA IZIN / DINAS", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // History of Leaves
        item {
            Text(
                text = "Riwayat Izin & Sakit Terdaftar (${leaves.size})",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = SlateDark
            )
        }

        if (leaves.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Belum ada riwayat izin tercatat",
                        color = SlateMuted,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(leaves) { leave ->
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
                                text = leave.nama,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = SlateDark
                            )
                            Text(
                                text = "Tgl: ${leave.tgl} • ID: ${leave.pps}",
                                fontSize = 12.sp,
                                color = SlateMuted
                            )
                            if (leave.ket.isNotEmpty()) {
                                Text(
                                    text = "Ket: ${leave.ket}",
                                    fontSize = 12.sp,
                                    color = GreenDark
                                )
                            }
                        }
                        StatusBadge(status = leave.jenis)
                    }
                }
            }
        }
    }
}
