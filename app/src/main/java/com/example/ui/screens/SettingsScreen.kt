package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.ui.theme.*
import com.example.viewmodel.PresensiViewModel
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(viewModel: PresensiViewModel) {
    val coroutineScope = rememberCoroutineScope()
    val boot by viewModel.bootConfig.collectAsState()
    val clock by viewModel.currentClock.collectAsState()

    var endpointUrl by remember { mutableStateOf(viewModel.apiService.getEndpointUrl()) }
    var namaSekolah by remember { mutableStateOf(boot.nama) }
    var alamatSekolah by remember { mutableStateOf(boot.alamat) }
    var jamMasuk by remember { mutableStateOf("07:30") }
    var toleransi by remember { mutableStateOf("10") }
    var bujur by remember { mutableStateOf("112.9") }
    var koreksiIstiwak by remember { mutableStateOf("0") }

    LaunchedEffect(Unit) {
        val s = viewModel.apiService.getSettings()
        namaSekolah = s["NAMA SEKOLAH"] ?: namaSekolah
        alamatSekolah = s["ALAMAT"] ?: alamatSekolah
        jamMasuk = s["JAM MASUK"] ?: "07:30"
        toleransi = s["TOLERANSI"] ?: "10"
        bujur = s["BUJUR"] ?: "112.9"
        koreksiIstiwak = s["KOREKSI ISTIWAK MENIT"] ?: "0"
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(SlateBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Live Istiwak Calibration Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = GreenPrimary),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "☀️ Kalibrasi Waktu Istiwak (Solar Time)",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Text(
                        text = "Jam Istiwak saat ini: ${clock.jam} (${clock.hari}, ${clock.tgl})",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                    Text(
                        text = "Berdasarkan Bujur ${bujur}° Bujur Timur dan Rumus Equation of Time (EoT) persis Code.gs",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        // AppScript URL Configuration
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "🔗 Integrasi Google Apps Script",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = SlateDark
                    )
                    Text(
                        text = "URL Web App Apps Script backend presensi guru",
                        fontSize = 12.sp,
                        color = SlateMuted
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = endpointUrl,
                        onValueChange = { endpointUrl = it },
                        label = { Text("Web App URL (/exec)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("appscript_url_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.apiService.setEndpointUrl(endpointUrl)
                                viewModel.showToast("URL Web App berhasil disimpan")
                                viewModel.loadBoot()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Simpan URL")
                        }

                        OutlinedButton(
                            onClick = {
                                viewModel.loadBoot()
                                viewModel.syncTeachersFromRemote()
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sinkron Sheet")
                        }
                    }
                }
            }
        }

        // School & Schedule Settings
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "⚙️ Parameter Jam & Toleransi Masuk",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = SlateDark
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = namaSekolah,
                        onValueChange = { namaSekolah = it },
                        label = { Text("Nama Lembaga / Sekolah") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = alamatSekolah,
                        onValueChange = { alamatSekolah = it },
                        label = { Text("Alamat Sekolah") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = jamMasuk,
                            onValueChange = { jamMasuk = it },
                            label = { Text("Jam Masuk (Istiwak)") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = toleransi,
                            onValueChange = { toleransi = it },
                            label = { Text("Toleransi (Menit)") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = bujur,
                            onValueChange = { bujur = it },
                            label = { Text("Derajat Bujur (°E)") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = koreksiIstiwak,
                            onValueChange = { koreksiIstiwak = it },
                            label = { Text("Koreksi Menit") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            coroutineScope.launch {
                                viewModel.apiService.saveSettings(
                                    mapOf(
                                        "NAMA SEKOLAH" to namaSekolah,
                                        "ALAMAT" to alamatSekolah,
                                        "JAM MASUK" to jamMasuk,
                                        "TOLERANSI" to toleransi,
                                        "BUJUR" to bujur,
                                        "KOREKSI ISTIWAK MENIT" to koreksiIstiwak
                                    )
                                )
                                viewModel.refreshClock()
                                viewModel.loadBoot()
                                viewModel.showToast("Pengaturan sekolah & Istiwak berhasil disimpan!")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("save_settings_button")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("SIMPAN SEMUA PENGATURAN", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
