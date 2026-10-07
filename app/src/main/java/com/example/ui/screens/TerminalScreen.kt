package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AttendanceProcessResult
import com.example.ui.theme.*
import com.example.viewmodel.PresensiViewModel

@Composable
fun TerminalScreen(viewModel: PresensiViewModel) {
    val boot by viewModel.bootConfig.collectAsState()
    val clock by viewModel.currentClock.collectAsState()
    val scanResult by viewModel.scanResult.collectAsState()
    val isProcessing by viewModel.isProcessingScan.collectAsState()
    val terminal by viewModel.selectedTerminal.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    var inputBarcode by remember { mutableStateOf("") }

    val gradientBg = Brush.verticalGradient(
        colors = listOf(GreenDark, GreenPrimary, GreenLight)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradientBg)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 560.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Badge
                Surface(
                    color = GreenContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            tint = GreenDark,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "TERMINAL DIGITAL SCANNER",
                            color = GreenDark,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                // Ganti judul utama sesuai permintaan user
                Text(
                    text = "Presensi Guru MMU Idadiyah",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = GreenDark,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = clock.hijri.text.uppercase(),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SlateMuted,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Jam Istiwak Big Display
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = SlateBackground),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = clock.jam,
                            fontSize = 52.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = SlateDark
                        )
                        Text(
                            text = "WAKTU ISTIWAK (SOLAR TIME)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = GreenPrimary
                        )
                        Text(
                            text = "${clock.hari}, ${clock.tgl}",
                            fontSize = 12.sp,
                            color = SlateMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Scanner / Input Field
                OutlinedTextField(
                    value = inputBarcode,
                    onValueChange = { inputBarcode = it },
                    label = { Text("SCAN BARCODE / KETIK ID PPS / NAMA") },
                    placeholder = { Text("Contoh: 14371025 atau nama") },
                    leadingIcon = {
                        Icon(Icons.Default.Pin, contentDescription = null, tint = GreenPrimary)
                    },
                    trailingIcon = {
                        if (inputBarcode.isNotEmpty()) {
                            IconButton(onClick = { inputBarcode = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (inputBarcode.isNotBlank()) {
                                viewModel.processAttendance(inputBarcode)
                                inputBarcode = ""
                            }
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("scan_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Button submit scan
                Button(
                    onClick = {
                        if (inputBarcode.isNotBlank()) {
                            viewModel.processAttendance(inputBarcode)
                            inputBarcode = ""
                        }
                    },
                    enabled = inputBarcode.isNotBlank() && !isProcessing,
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("submit_scan_button")
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("PROSES PRESENSI", fontWeight = FontWeight.Bold)
                    }
                }

                // Quick Select Demo Teachers Chips
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "ID Cepat untuk Uji Coba:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SlateMuted
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    listOf("14371025", "14371027", "14371028", "14371030").forEach { ppsDemo ->
                        AssistChip(
                            onClick = {
                                inputBarcode = ppsDemo
                                viewModel.processAttendance(ppsDemo)
                                inputBarcode = ""
                            },
                            label = { Text(ppsDemo, fontSize = 11.sp) },
                            modifier = Modifier.padding(horizontal = 2.dp)
                        )
                    }
                }

                // Scan Result Display
                AnimatedVisibility(
                    visible = scanResult != null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    scanResult?.let { res ->
                        ScanResultCard(res = res, onDismiss = { viewModel.clearScanResult() })
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = SlateBorder)
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = GreenContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "TERHUBUNG: $terminal",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = GreenDark,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    TextButton(
                        onClick = { viewModel.logout() },
                        colors = ButtonDefaults.textButtonColors(contentColor = RedDanger)
                    ) {
                        Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Keluar", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun ScanResultCard(res: AttendanceProcessResult, onDismiss: () -> Unit) {
    val (bgColor, titleText) = when (res.type) {
        "SUCCESS" -> GreenPrimary to "PRESENSI BERHASIL"
        "LATE" -> OrangeWarning to "PRESENSI TERLAMBAT"
        "DUP" -> Color(0xFFD97706) to "SUDAH PRESENSI HARI INI"
        "WARNING" -> Color(0xFFD97706) to "PERINGATAN"
        else -> RedDanger to "ERROR / GAGAL"
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = titleText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Color.White)
                }
            }

            if (res.nama.isNotEmpty()) {
                Text(
                    text = res.nama,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Text(
                    text = "ID PPS: ${res.pps} • ${res.tempat} ${if (res.no.isNotEmpty()) "(${res.no})" else ""}",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.9f)
                )
                Text(
                    text = "Kelas: ${res.kelas}",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }

            if (res.jam.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    color = Color.Black.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Jam: ${res.jam} Istiwak  •  Status: ${res.status} ${if (res.menit > 0) "(${res.menit} mnt)" else ""}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Text(
                text = res.msg,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White,
                modifier = Modifier.padding(top = 8.dp)
            )

            if (res.sub.isNotEmpty()) {
                Text(
                    text = res.sub,
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}
