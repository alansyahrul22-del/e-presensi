package com.example.ui.screens

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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.viewmodel.PresensiViewModel

@Composable
fun LoginScreen(viewModel: PresensiViewModel) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val clock by viewModel.currentClock.collectAsState()

    val gradientBg = Brush.verticalGradient(
        colors = listOf(GreenDark, GreenPrimary, SlateDark)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradientBg)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 440.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(26.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Badge
                Surface(
                    color = GreenContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Text(
                        text = "SISTEM AUTENTIKASI MASUK",
                        color = GreenDark,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                Text(
                    text = "Presensi Guru MMU Idadiyah",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = GreenDark,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Jam Istiwak: ${clock.jam}  •  ${clock.hijri.text}",
                    fontSize = 12.sp,
                    color = SlateMuted,
                    modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
                )

                OutlinedTextField(
                    value = username,
                    onValueChange = {
                        username = it
                        errorMessage = null
                    },
                    label = { Text("Username") },
                    placeholder = { Text("T01-T05 / TU / WK / Kepsek") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = GreenPrimary) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("login_username_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        errorMessage = null
                    },
                    label = { Text("Sandi (Password)") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = GreenPrimary) },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (passwordVisible) "Sembunyikan" else "Tampilkan"
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (!viewModel.login(username, password)) {
                                errorMessage = "Username atau sandi tidak sesuai!"
                            }
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("login_password_input")
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = RedDanger,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = {
                        if (!viewModel.login(username, password)) {
                            errorMessage = "Username atau sandi tidak sesuai. Periksa kembali!"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("login_submit_button")
                ) {
                    Icon(Icons.Default.Login, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("MASUK APLIKASI", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = SlateBorder)
                Spacer(modifier = Modifier.height(12.dp))

                // Panduan akun & bantuan login
                Text(
                    text = "Pilih Cepat Akun Demo / Uji:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = SlateMuted
                )

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Akses Khusus Scanner (Hanya Scan):",
                    fontSize = 11.sp,
                    color = GreenDark,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    listOf("T01", "T02", "T03", "T04", "T05").forEach { t ->
                        SuggestionChip(
                            onClick = {
                                username = t
                                password = t
                                errorMessage = null
                            },
                            label = { Text(t, fontSize = 10.sp) },
                            modifier = Modifier.padding(horizontal = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Akses Admin (Monitor, Dashboard, Rekap, dll):",
                    fontSize = 11.sp,
                    color = BlueInfo,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    listOf("TU1", "TU2", "WK1", "WK2", "Kepsek").forEach { a ->
                        SuggestionChip(
                            onClick = {
                                username = a
                                password = a
                                errorMessage = null
                            },
                            label = { Text(a, fontSize = 10.sp) },
                            modifier = Modifier.padding(horizontal = 2.dp)
                        )
                    }
                }
            }
        }
    }
}
