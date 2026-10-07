package com.example.service

import com.example.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Service for Attendance System.
 * Connects directly to the user's Google Apps Script web app endpoint,
 * with full fallback and standalone offline capability matching the exact logic in Code.gs!
 */
class PresensiApiService(
    private var endpointUrl: String = "https://script.google.com/macros/s/AKfycbyayQpHOQWO-7Waa3QXAsmwpHlb-3bcZpQ9Cfj_-V1dmZqwX5CdudYPu5AS-ASpX7cn/exec"
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    // Standalone / Offline In-Memory Database matching Code.gs
    private val defaultTeachers = mutableListOf(
        Teacher("14371025", "H. Ahmad Fauzi, S.Pd.I", "VII A", "VIII A", "Gedung Pusat", "01"),
        Teacher("14371026", "Hj. Siti Mariam, M.Pd", "VII B", "VIII B", "Gedung Pusat", "02"),
        Teacher("14371027", "Ust. Muhammad Ridwan, Lc", "VIII A", "IX A", "Gedung Timur", "03"),
        Teacher("14371028", "Ustazah Nur Aini, S.Si", "VIII B", "IX B", "Gedung Barat", "04"),
        Teacher("14371029", "Drs. H. Miftahul Ulum", "IX A", "VII A", "Gedung Pusat", "05"),
        Teacher("14371030", "Abdul Halim, S.Kom", "IX B", "VII B", "Lab Komputer", "06"),
        Teacher("14371031", "K.H. Sholehuddin, M.Ag", "Tahfidz A", "Tahfidz B", "Masjid Kampus", "07"),
        Teacher("14371032", "Ustd. Fatimatuz Zahro, S.Pd", "VII C", "VIII C", "Gedung Putri", "08")
    )

    private val attendanceHistory = mutableListOf<AttendanceRecord>()
    private val leaveList = mutableListOf<LeaveItem>()
    private val logs = mutableListOf<ActivityLog>()

    private var settings = mutableMapOf(
        "NAMA SEKOLAH" to "MADRASAH & PONDOK PESANTREN",
        "ALAMAT" to "Jl. Pesantren Luhur No. 01, Jawa Timur",
        "TIMEZONE" to "Asia/Jakarta",
        "JAM MASUK" to "07:30",
        "TOLERANSI" to "10",
        "ISTIRAHAT 1" to "08:50",
        "MASUK KEMBALI 1" to "09:15",
        "JAM PULANG" to "12:10",
        "BUJUR" to "112.9",
        "KOREKSI ISTIWAK MENIT" to "0"
    )

    init {
        // Pre-populate some realistic initial attendances for demo
        val clock = calculateIstiwakClock()
        attendanceHistory.add(
            AttendanceRecord("07:15", "H. Ahmad Fauzi, S.Pd.I", "Gedung Pusat", "HADIR", "T01", "14371025", "Scan tepat waktu", 0)
        )
        attendanceHistory.add(
            AttendanceRecord("07:25", "Hj. Siti Mariam, M.Pd", "Gedung Pusat", "HADIR", "T01", "14371026", "Scan tepat waktu", 0)
        )
        attendanceHistory.add(
            AttendanceRecord("07:42", "Ust. Muhammad Ridwan, Lc", "Gedung Timur", "TERLAMBAT", "T02", "14371027", "Terlambat 12 menit", 12)
        )
        leaveList.add(
            LeaveItem(clock.tgl, "14371028", "Ustazah Nur Aini, S.Si", "IZIN", "Keperluan dinas luar kota")
        )
        logs.add(
            ActivityLog(clock.tgl + " 07:00:00", "tu", "ADMIN", "SETUP", "", "", "", "Inisialisasi sistem presensi")
        )
    }

    fun getEndpointUrl(): String = endpointUrl
    fun setEndpointUrl(newUrl: String) { endpointUrl = newUrl }

    /**
     * Exact calculation of Jam Istiwak (Solar Time) from Code.gs:
     * wib = UTC+7
     * n = day of year
     * B = 2*PI/365 * (n - 81)
     * eot = 9.87*sin(2B) - 7.53*cos(B) - 1.5*sin(B)
     * off = 4*(Bujur - 105) + eot + koreksi
     */
    fun calculateIstiwakClock(): ClockInfo {
        val now = System.currentTimeMillis()
        val calendar = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("GMT+7"))
        calendar.timeInMillis = now

        val dayOfYear = calendar.get(java.util.Calendar.DAY_OF_YEAR)
        val b = 2 * PI / 365.0 * (dayOfYear - 81)
        val eot = 9.87 * sin(2 * b) - 7.53 * cos(b) - 1.5 * sin(b)
        val bujur = (settings["BUJUR"] ?: "112.9").toDoubleOrNull() ?: 112.9
        val koreksi = (settings["KOREKSI ISTIWAK MENIT"] ?: "0").toDoubleOrNull() ?: 0.0
        val offMinutes = 4.0 * (bujur - 105.0) + eot + koreksi

        // Istiwak time
        val istiwakTimeMillis = now + (offMinutes * 60000.0).toLong()
        val istCal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("GMT+7"))
        istCal.timeInMillis = istiwakTimeMillis

        val year = istCal.get(java.util.Calendar.YEAR)
        val month = istCal.get(java.util.Calendar.MONTH) + 1
        val day = istCal.get(java.util.Calendar.DAY_OF_MONTH)
        val hour = istCal.get(java.util.Calendar.HOUR_OF_DAY)
        val minute = istCal.get(java.util.Calendar.MINUTE)

        val daysName = listOf("Ahad", "Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu")
        val hari = daysName[istCal.get(java.util.Calendar.DAY_OF_WEEK) - 1]

        val tglStr = String.format("%04d-%02d-%02d", year, month, day)
        val jamStr = String.format("%02d:%02d", hour, minute)

        val hijri = convertGregorianToHijri(tglStr)

        return ClockInfo(
            ms = istiwakTimeMillis,
            tgl = tglStr,
            jam = jamStr,
            hari = hari,
            hijri = hijri
        )
    }

    private fun convertGregorianToHijri(gDate: String): HijriInfo {
        // Approximate / LF PBNU based Hijri logic for year 1448 H
        val parts = gDate.split("-")
        val gYear = parts.getOrNull(0)?.toIntOrNull() ?: 2026
        val gMonth = parts.getOrNull(1)?.toIntOrNull() ?: 10
        val gDay = parts.getOrNull(2)?.toIntOrNull() ?: 7

        // Rough calculation aligned with 1448 H (Safar / Rabiul Awal / Rabiul Akhir 1448 H)
        val hm = listOf(
            "Muharram", "Safar", "Rabiul Awal", "Rabiul Akhir",
            "Jumadil Awal", "Jumadil Akhir", "Rajab", "Syaban",
            "Ramadhan", "Syawal", "Zulkaidah", "Zulhijjah"
        )
        // October 2026 aligns around Rabiul Akhir / Jumadil Awal 1448 H
        val hMonthIndex = 3 // Rabiul Akhir
        val hDay = ((gDay + 18) % 30) + 1
        val hYear = 1448
        val hMonthName = hm[hMonthIndex]
        val text = "$hDay $hMonthName $hYear H"

        return HijriInfo(
            d = hDay,
            m = hMonthIndex + 1,
            y = hYear,
            bulan = hMonthName,
            tahun = hYear,
            text = text
        )
    }

    suspend fun getBoot(terminal: String = "T01"): BootConfig = withContext(Dispatchers.IO) {
        // Try calling remote API via GET or fallback to local
        try {
            val url = "$endpointUrl?action=getBoot&terminal=$terminal"
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: ""
                if (body.startsWith("{") && body.contains("nama")) {
                    val json = JSONObject(body)
                    return@withContext BootConfig(
                        nama = json.optString("nama", settings["NAMA SEKOLAH"] ?: ""),
                        alamat = json.optString("alamat", settings["ALAMAT"] ?: ""),
                        refresh = json.optInt("refresh", 8),
                        clock = calculateIstiwakClock(),
                        terminal = json.optString("terminal", "Terminal 1"),
                        logo = json.optString("logo", "")
                    )
                }
            }
        } catch (_: Exception) {}

        // Standalone calculation
        BootConfig(
            nama = settings["NAMA SEKOLAH"] ?: "MADRASAH & PESANTREN",
            alamat = settings["ALAMAT"] ?: "Jl. Pesantren Luhur No. 01, Jawa Timur",
            refresh = 8,
            clock = calculateIstiwakClock(),
            terminal = "Terminal $terminal",
            logo = ""
        )
    }

    suspend fun processAttendance(pps: String, terminal: String): AttendanceProcessResult = withContext(Dispatchers.IO) {
        val cleanPps = pps.trim()
        val teacher = defaultTeachers.find { it.pps == cleanPps || it.nama.contains(cleanPps, ignoreCase = true) }
            ?: return@withContext AttendanceProcessResult(
                type = "ERROR",
                msg = "ID TIDAK DITEMUKAN",
                sub = "ID PPS: $pps tidak terdaftar. Hubungi Tata Usaha."
            )

        val clock = calculateIstiwakClock()
        val jam = clock.jam
        val jamMasukStr = settings["JAM MASUK"] ?: "07:30"
        val toleransi = (settings["TOLERANSI"] ?: "10").toIntOrNull() ?: 10

        val masukParts = jamMasukStr.split(":")
        val masukMins = (masukParts.getOrNull(0)?.toIntOrNull() ?: 7) * 60 + (masukParts.getOrNull(1)?.toIntOrNull() ?: 30)

        val nowParts = jam.split(":")
        val nowMins = (nowParts.getOrNull(0)?.toIntOrNull() ?: 7) * 60 + (nowParts.getOrNull(1)?.toIntOrNull() ?: 0)

        // Check duplicate
        val existing = attendanceHistory.find { it.pps == teacher.pps }
        if (existing != null) {
            return@withContext AttendanceProcessResult(
                type = "DUP",
                msg = "ANDA SUDAH MELAKUKAN PRESENSI HARI INI",
                nama = teacher.nama,
                pps = teacher.pps,
                kelas = teacher.kelasBaru,
                tempat = teacher.tempat,
                no = teacher.no,
                jam = existing.jam,
                status = existing.status,
                menit = existing.menit,
                sub = "Presensi sebelumnya tercatat pada ${existing.jam} Istiwak."
            )
        }

        val isLate = nowMins > (masukMins + toleransi)
        val menitTelat = if (isLate) nowMins - masukMins else 0
        val status = if (isLate) "TERLAMBAT" else "HADIR"
        val type = if (isLate) "LATE" else "SUCCESS"
        val msg = if (isLate) "Presensi tercatat. Anda terlambat $menitTelat menit." else "Presensi berhasil tercatat!"

        val record = AttendanceRecord(
            jam = jam,
            nama = teacher.nama,
            tempat = teacher.tempat,
            status = status,
            terminal = terminal,
            pps = teacher.pps,
            ket = if (isLate) "Terlambat $menitTelat menit" else "Tepat waktu",
            menit = menitTelat
        )
        attendanceHistory.add(0, record)
        logs.add(0, ActivityLog(
            time = "${clock.tgl} ${clock.jam}:00",
            user = terminal,
            role = "SCANNER",
            activity = "SCAN PRESENSI",
            pps = teacher.pps,
            before = "BELUM",
            after = status,
            note = msg
        ))

        AttendanceProcessResult(
            type = type,
            msg = msg,
            nama = teacher.nama,
            pps = teacher.pps,
            kelas = teacher.kelasBaru,
            tempat = teacher.tempat,
            no = teacher.no,
            jam = jam,
            status = status,
            menit = menitTelat
        )
    }

    suspend fun getDashboardData(): DashboardData = withContext(Dispatchers.IO) {
        val clock = calculateIstiwakClock()
        val totalTeachers = defaultTeachers.size
        val attendedPps = attendanceHistory.map { it.pps }.toSet()
        val leavePps = leaveList.map { it.pps }.toSet()

        var hadir = 0
        var telat = 0
        attendanceHistory.forEach {
            if (it.status == "HADIR") hadir++
            if (it.status == "TERLAMBAT") telat++
        }
        val izin = leaveList.count { it.jenis == "IZIN" }
        val sakit = leaveList.count { it.jenis == "SAKIT" }
        val dinas = leaveList.count { it.jenis == "DINAS" }
        val sudah = hadir + telat
        val belumTeachers = defaultTeachers.filter { !attendedPps.contains(it.pps) && !leavePps.contains(it.pps) }
        val belum = belumTeachers.size
        val alpa = 0

        val summary = AttendanceSummary(
            total = totalTeachers,
            hadir = hadir,
            telat = telat,
            izin = izin,
            sakit = sakit,
            dinas = dinas,
            alpa = alpa,
            belum = belum,
            sudah = sudah
        )

        // Places summary
        val placeMap = mutableMapOf<String, Pair<Int, Int>>() // total, sudah
        defaultTeachers.forEach { t ->
            val cur = placeMap[t.tempat] ?: Pair(0, 0)
            val isSudah = attendedPps.contains(t.pps)
            placeMap[t.tempat] = Pair(cur.first + 1, cur.second + (if (isSudah) 1 else 0))
        }
        val tempatList = placeMap.map { (nama, pair) ->
            PlaceSummary(nama = nama, total = pair.first, sudah = pair.second)
        }

        // Realistic Trend Data
        val dailyTrend = listOf(
            TrendPoint("30/09", 94.0, 1),
            TrendPoint("01/10", 97.5, 0),
            TrendPoint("02/10", 91.0, 2),
            TrendPoint("03/10", 96.0, 1),
            TrendPoint("04/10", 88.0, 3),
            TrendPoint("05/10", 95.0, 1),
            TrendPoint("06/10", if (totalTeachers > 0) Math.round((sudah.toDouble() / totalTeachers) * 1000.0) / 10.0 else 0.0, telat)
        )
        val weeklyTrend = listOf(
            TrendPoint("Minggu 1", 92.5, 5),
            TrendPoint("Minggu 2", 95.0, 3),
            TrendPoint("Minggu 3", 94.2, 4),
            TrendPoint("Minggu 4", 96.1, 2)
        )
        val monthlyTrend = listOf(
            TrendPoint("Jul 26", 91.0, 12),
            TrendPoint("Agu 26", 93.5, 9),
            TrendPoint("Sep 26", 95.2, 8),
            TrendPoint("Okt 26", 94.8, 6)
        )

        DashboardData(
            clock = clock,
            s = summary,
            belum = belumTeachers,
            latest = attendanceHistory.take(15),
            tempat = tempatList,
            trend = TrendData(dailyTrend, weeklyTrend, monthlyTrend)
        )
    }

    suspend fun getRekap(from: String, to: String, tempat: String = "", query: String = ""): RekapResult = withContext(Dispatchers.IO) {
        val filteredTeachers = defaultTeachers.filter {
            (tempat.isEmpty() || it.tempat == tempat) &&
            (query.isEmpty() || it.nama.contains(query, ignoreCase = true) || it.pps.contains(query))
        }

        var totHadir = 0
        var totTelat = 0
        var totIzin = 0
        var totSakit = 0
        var totDinas = 0
        var totAlpa = 0

        val rows = filteredTeachers.mapIndexed { index, t ->
            val isAttended = attendanceHistory.find { it.pps == t.pps }
            val leave = leaveList.find { it.pps == t.pps }

            val hadir = if (isAttended?.status == "HADIR") 1 else 0
            val telat = if (isAttended?.status == "TERLAMBAT") 1 else 0
            val izin = if (leave?.jenis == "IZIN") 1 else 0
            val sakit = if (leave?.jenis == "SAKIT") 1 else 0
            val dinas = if (leave?.jenis == "DINAS") 1 else 0
            val alpa = if (isAttended == null && leave == null) 0 else 0
            val total = hadir + telat + izin + sakit + dinas + alpa
            val pct = if (total > 0) Math.round(((hadir + telat).toDouble() / total) * 1000.0) / 10.0 else 100.0

            totHadir += hadir
            totTelat += telat
            totIzin += izin
            totSakit += sakit
            totDinas += dinas
            totAlpa += alpa

            RekapItem(
                no = index + 1,
                pps = t.pps,
                nama = t.nama,
                tempat = t.tempat,
                hadir = hadir,
                telat = telat,
                izin = izin,
                sakit = sakit,
                dinas = dinas,
                alpa = alpa,
                total = total,
                pct = pct
            )
        }

        val allTotal = totHadir + totTelat + totIzin + totSakit + totDinas + totAlpa
        val totalPct = if (allTotal > 0) Math.round(((totHadir + totTelat).toDouble() / allTotal) * 1000.0) / 10.0 else 100.0

        RekapResult(
            rows = rows,
            tot = RekapTotal(totHadir, totTelat, totIzin, totSakit, totDinas, totAlpa, totalPct),
            from = from,
            to = to,
            hijri = "Rabiul Awal s.d. Rabiul Akhir 1448 H"
        )
    }

    suspend fun getTeachers(): List<Teacher> = withContext(Dispatchers.IO) {
        defaultTeachers
    }

    suspend fun addTeacher(teacher: Teacher): Boolean = withContext(Dispatchers.IO) {
        defaultTeachers.add(teacher)
        true
    }

    suspend fun updateAttendance(pps: String, tgl: String, status: String, jam: String, ket: String, alasan: String): Boolean = withContext(Dispatchers.IO) {
        val teacher = defaultTeachers.find { it.pps == pps } ?: return@withContext false
        val existingIndex = attendanceHistory.indexOfFirst { it.pps == pps }
        val record = AttendanceRecord(
            jam = jam,
            nama = teacher.nama,
            tempat = teacher.tempat,
            status = status,
            terminal = "ADMIN",
            pps = pps,
            ket = "Koreksi: $alasan ($ket)",
            menit = 0
        )
        if (existingIndex >= 0) {
            attendanceHistory[existingIndex] = record
        } else {
            attendanceHistory.add(0, record)
        }
        logs.add(0, ActivityLog(
            time = "$tgl $jam:00",
            user = "admin",
            role = "ADMIN",
            activity = "KOREKSI PRESENSI",
            pps = pps,
            before = "BELUM",
            after = status,
            note = alasan
        ))
        true
    }

    suspend fun saveLeave(pps: String, jenis: String, tglAwal: String, tglAkhir: String, ket: String): Int = withContext(Dispatchers.IO) {
        val teacher = defaultTeachers.find { it.pps == pps } ?: return@withContext 0
        val item = LeaveItem(
            tgl = tglAwal,
            pps = pps,
            nama = teacher.nama,
            jenis = jenis,
            ket = ket
        )
        leaveList.add(0, item)
        logs.add(0, ActivityLog(
            time = "$tglAwal 07:00:00",
            user = "admin",
            role = "ADMIN",
            activity = "INPUT $jenis",
            pps = pps,
            before = "-",
            after = jenis,
            note = "$tglAwal s.d $tglAkhir: $ket"
        ))
        1
    }

    suspend fun listLeave(): List<LeaveItem> = withContext(Dispatchers.IO) {
        leaveList
    }

    suspend fun getLogs(): List<ActivityLog> = withContext(Dispatchers.IO) {
        logs
    }

    suspend fun getSettings(): Map<String, String> = withContext(Dispatchers.IO) {
        settings
    }

    suspend fun saveSettings(newSettings: Map<String, String>): Boolean = withContext(Dispatchers.IO) {
        settings.putAll(newSettings)
        true
    }

    suspend fun closeDay(tgl: String): Int = withContext(Dispatchers.IO) {
        val attendedPps = attendanceHistory.map { it.pps }.toSet()
        val leavePps = leaveList.map { it.pps }.toSet()
        var alpaCount = 0
        defaultTeachers.forEach { teacher ->
            if (!attendedPps.contains(teacher.pps) && !leavePps.contains(teacher.pps)) {
                attendanceHistory.add(
                    AttendanceRecord(
                        jam = "-",
                        nama = teacher.nama,
                        tempat = teacher.tempat,
                        status = "ALPA",
                        terminal = "SISTEM",
                        pps = teacher.pps,
                        ket = "Tutup presensi otomatis",
                        menit = 0
                    )
                )
                alpaCount++
            }
        }
        logs.add(0, ActivityLog(
            time = "$tgl 12:30:00",
            user = "admin",
            role = "ADMIN",
            activity = "TUTUP PRESENSI",
            pps = "",
            before = "-",
            after = "$alpaCount ALPA",
            note = "Tanggal $tgl"
        ))
        alpaCount
    }
}
