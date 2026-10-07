package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.*
import com.example.service.PresensiApiService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class AppScreen {
    TERMINAL,    // Scan Barcode / Input ID Guru
    MONITOR,     // TV Monitor Live Display
    DASHBOARD,   // KPI Summary & Visual Trend
    REKAP,       // Rekapitulasi Laporan Guru & Export
    TODAY,       // Presensi Hari Ini & Manual Correction
    LEAVE,       // Izin, Sakit, Dinas Input
    SETTINGS     // Pengaturan Jam, Toleransi, Bujur & Endpoint
}

class PresensiViewModel(application: Application) : AndroidViewModel(application) {
    val apiService = PresensiApiService(context = application.applicationContext)

    // Current authenticated user session (null = not logged in / show Login screen)
    private val _currentUser = MutableStateFlow<UserSession?>(null)
    val currentUser: StateFlow<UserSession?> = _currentUser.asStateFlow()

    private val _currentScreen = MutableStateFlow(AppScreen.TERMINAL)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _bootConfig = MutableStateFlow(BootConfig())
    val bootConfig: StateFlow<BootConfig> = _bootConfig.asStateFlow()

    private val _currentClock = MutableStateFlow(ClockInfo())
    val currentClock: StateFlow<ClockInfo> = _currentClock.asStateFlow()

    private val _dashboardData = MutableStateFlow(DashboardData())
    val dashboardData: StateFlow<DashboardData> = _dashboardData.asStateFlow()

    private val _rekapResult = MutableStateFlow(RekapResult())
    val rekapResult: StateFlow<RekapResult> = _rekapResult.asStateFlow()

    private val _scanResult = MutableStateFlow<AttendanceProcessResult?>(null)
    val scanResult: StateFlow<AttendanceProcessResult?> = _scanResult.asStateFlow()

    private val _isProcessingScan = MutableStateFlow(false)
    val isProcessingScan: StateFlow<Boolean> = _isProcessingScan.asStateFlow()

    private val _teachers = MutableStateFlow<List<Teacher>>(emptyList())
    val teachers: StateFlow<List<Teacher>> = _teachers.asStateFlow()

    private val _logs = MutableStateFlow<List<ActivityLog>>(emptyList())
    val logs: StateFlow<List<ActivityLog>> = _logs.asStateFlow()

    private val _leaves = MutableStateFlow<List<LeaveItem>>(emptyList())
    val leaves: StateFlow<List<LeaveItem>> = _leaves.asStateFlow()

    private val _selectedTerminal = MutableStateFlow("T01")
    val selectedTerminal: StateFlow<String> = _selectedTerminal.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    init {
        refreshClock()
        loadBoot()
        loadDashboard()
        loadTeachers()

        // Clock updater every 1 second
        viewModelScope.launch {
            while (true) {
                delay(1000)
                refreshClock()
            }
        }
    }

    fun login(user: String, pass: String): Boolean {
        val session = AuthCredentials.authenticate(user, pass)
        if (session != null) {
            _currentUser.value = session
            if (session.role == UserRole.TERMINAL) {
                val termId = session.terminalId ?: "T01"
                _selectedTerminal.value = termId
                _currentScreen.value = AppScreen.TERMINAL
                loadBoot()
            } else {
                // Admin role: default to DASHBOARD or MONITOR
                _currentScreen.value = AppScreen.DASHBOARD
                loadDashboard()
            }
            showToast("Selamat datang, ${session.displayName}!")
            return true
        }
        return false
    }

    fun logout() {
        _currentUser.value = null
        _scanResult.value = null
        showToast("Anda telah keluar.")
    }

    fun setScreen(screen: AppScreen) {
        val user = _currentUser.value
        // Security gate: If user is terminal, only TERMINAL is permitted
        if (user?.role == UserRole.TERMINAL && screen != AppScreen.TERMINAL) {
            showToast("Akun Terminal hanya memiliki akses ke Scanner.")
            return
        }
        // If user is Admin, TERMINAL scan is hidden/excluded as requested
        if (user?.role == UserRole.ADMIN && screen == AppScreen.TERMINAL) {
            showToast("Akun Admin/Pimpinan mengelola pemantauan dan laporan.")
            return
        }

        _currentScreen.value = screen
        if (screen == AppScreen.DASHBOARD || screen == AppScreen.MONITOR) {
            loadDashboard()
        } else if (screen == AppScreen.REKAP) {
            loadRekap()
        } else if (screen == AppScreen.LEAVE) {
            loadLeaves()
        }
    }

    fun setTerminal(term: String) {
        _selectedTerminal.value = term
        loadBoot()
    }

    fun showToast(msg: String) {
        _toastMessage.value = msg
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    fun refreshClock() {
        _currentClock.value = apiService.calculateIstiwakClock()
    }

    fun loadBoot() {
        viewModelScope.launch {
            _bootConfig.value = apiService.getBoot(_selectedTerminal.value)
        }
    }

    fun loadDashboard() {
        viewModelScope.launch {
            _dashboardData.value = apiService.getDashboardData()
        }
    }

    fun loadTeachers() {
        viewModelScope.launch {
            _teachers.value = apiService.getTeachers()
        }
    }

    fun syncTeachersFromRemote() {
        viewModelScope.launch {
            showToast("Menyinkronkan data guru dari Google Sheet...")
            val list = apiService.fetchTeachersFromRemote()
            _teachers.value = list
            loadDashboard()
            showToast("Berhasil memuat ${list.size} guru dari DATA_GURU!")
        }
    }

    fun addNewTeacher(teacher: Teacher) {
        viewModelScope.launch {
            apiService.addTeacher(teacher)
            _teachers.value = apiService.getTeachers()
            loadDashboard()
            showToast("Guru ${teacher.nama} berhasil ditambahkan!")
        }
    }

    fun importTeachersBatch(teachersList: List<Teacher>) {
        viewModelScope.launch {
            val count = apiService.addTeachersBatch(teachersList)
            _teachers.value = apiService.getTeachers()
            loadDashboard()
            showToast("Berhasil mengimpor $count guru ke sistem!")
        }
    }

    fun loadLeaves() {
        viewModelScope.launch {
            _leaves.value = apiService.listLeave()
        }
    }

    fun loadLogs() {
        viewModelScope.launch {
            _logs.value = apiService.getLogs()
        }
    }

    fun loadRekap(from: String = "", to: String = "", tempat: String = "", q: String = "") {
        viewModelScope.launch {
            val clock = _currentClock.value
            val fromDate = if (from.isNotEmpty()) from else clock.tgl.substring(0, 8) + "01"
            val toDate = if (to.isNotEmpty()) to else clock.tgl
            _rekapResult.value = apiService.getRekap(fromDate, toDate, tempat, q)
        }
    }

    fun processAttendance(pps: String) {
        if (pps.isBlank() || _isProcessingScan.value) return
        _isProcessingScan.value = true
        viewModelScope.launch {
            try {
                val res = apiService.processAttendance(pps, _selectedTerminal.value)
                _scanResult.value = res
                loadDashboard()
                loadTeachers()
            } catch (e: Exception) {
                _scanResult.value = AttendanceProcessResult(
                    type = "ERROR",
                    msg = "GAGAL MEMPROSES",
                    sub = e.message ?: "Terjadi kesalahan sistem"
                )
            } finally {
                _isProcessingScan.value = false
            }
        }
    }

    fun clearScanResult() {
        _scanResult.value = null
    }

    fun updateAttendanceManual(pps: String, tgl: String, status: String, jam: String, ket: String, alasan: String) {
        viewModelScope.launch {
            val success = apiService.updateAttendance(pps, tgl, status, jam, ket, alasan)
            if (success) {
                showToast("Presensi $pps berhasil diperbarui: $status")
                loadDashboard()
            } else {
                showToast("Gagal memperbarui presensi")
            }
        }
    }

    fun saveLeave(pps: String, jenis: String, tglAwal: String, tglAkhir: String, ket: String) {
        viewModelScope.launch {
            val count = apiService.saveLeave(pps, jenis, tglAwal, tglAkhir, ket)
            if (count > 0) {
                showToast("Izin/Sakit berhasil disimpan ($count hari)")
                loadLeaves()
                loadDashboard()
            } else {
                showToast("Gagal menyimpan izin")
            }
        }
    }

    fun closeDay(tgl: String) {
        viewModelScope.launch {
            val count = apiService.closeDay(tgl)
            showToast("Presensi ditutup: $count guru berstatus ALPA")
            loadDashboard()
        }
    }
}
