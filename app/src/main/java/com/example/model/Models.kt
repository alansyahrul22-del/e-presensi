package com.example.model

data class ClockInfo(
    val ms: Long = 0L,
    val tgl: String = "",
    val jam: String = "",
    val hari: String = "",
    val hijri: HijriInfo = HijriInfo()
)

data class HijriInfo(
    val d: Int = 1,
    val m: Int = 1,
    val y: Int = 1448,
    val bulan: String = "",
    val tahun: Int = 1448,
    val text: String = ""
)

data class BootConfig(
    val nama: String = "SISTEM PRESENSI DIGITAL GURU",
    val alamat: String = "Alamat Sekolah",
    val refresh: Int = 8,
    val clock: ClockInfo = ClockInfo(),
    val terminal: String? = "Terminal 1",
    val logo: String = ""
)

data class AttendanceSummary(
    val total: Int = 0,
    val hadir: Int = 0,
    val telat: Int = 0,
    val izin: Int = 0,
    val sakit: Int = 0,
    val dinas: Int = 0,
    val alpa: Int = 0,
    val belum: Int = 0,
    val sudah: Int = 0
)

data class AttendanceRecord(
    val jam: String = "",
    val nama: String = "",
    val tempat: String = "",
    val status: String = "",
    val terminal: String = "",
    val pps: String = "",
    val ket: String = "",
    val menit: Int = 0
)

data class Teacher(
    val pps: String = "",
    val nama: String = "",
    val kelasAsal: String = "",
    val kelasBaru: String = "",
    val tempat: String = "",
    val no: String = ""
)

data class TrendPoint(
    val label: String = "",
    val pct: Double = 0.0,
    val late: Int = 0
)

data class TrendData(
    val daily: List<TrendPoint> = emptyList(),
    val weekly: List<TrendPoint> = emptyList(),
    val monthly: List<TrendPoint> = emptyList()
)

data class PlaceSummary(
    val nama: String = "",
    val total: Int = 0,
    val sudah: Int = 0
)

data class DashboardData(
    val clock: ClockInfo = ClockInfo(),
    val s: AttendanceSummary = AttendanceSummary(),
    val belum: List<Teacher> = emptyList(),
    val latest: List<AttendanceRecord> = emptyList(),
    val tempat: List<PlaceSummary> = emptyList(),
    val trend: TrendData = TrendData()
)

data class AttendanceProcessResult(
    val type: String, // SUCCESS, LATE, DUP, WARNING, ERROR
    val msg: String,
    val nama: String = "",
    val pps: String = "",
    val kelas: String = "",
    val tempat: String = "",
    val no: String = "",
    val jam: String = "",
    val status: String = "",
    val menit: Int = 0,
    val sub: String = ""
)

data class RekapItem(
    val no: Int = 0,
    val pps: String = "",
    val nama: String = "",
    val tempat: String = "",
    val hadir: Int = 0,
    val telat: Int = 0,
    val izin: Int = 0,
    val sakit: Int = 0,
    val dinas: Int = 0,
    val alpa: Int = 0,
    val total: Int = 0,
    val pct: Double = 0.0
)

data class RekapTotal(
    val hadir: Int = 0,
    val telat: Int = 0,
    val izin: Int = 0,
    val sakit: Int = 0,
    val dinas: Int = 0,
    val alpa: Int = 0,
    val pct: Double = 0.0
)

data class RekapResult(
    val rows: List<RekapItem> = emptyList(),
    val tot: RekapTotal = RekapTotal(),
    val from: String = "",
    val to: String = "",
    val hijri: String = ""
)

data class LeaveItem(
    val tgl: String = "",
    val pps: String = "",
    val nama: String = "",
    val jenis: String = "",
    val ket: String = ""
)

data class ActivityLog(
    val time: String = "",
    val user: String = "",
    val role: String = "",
    val activity: String = "",
    val pps: String = "",
    val before: String = "",
    val after: String = "",
    val note: String = ""
)

data class UserSession(
    val token: String,
    val nama: String,
    val role: String
)
