package com.example.viewmodel

import androidx.lifecycle.ViewModel
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

class PresensiViewModel : ViewModel() {
    val apiService = PresensiApiService()

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

    fun setScreen(screen: AppScreen) {
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
