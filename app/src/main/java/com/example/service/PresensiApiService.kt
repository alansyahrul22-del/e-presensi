package com.example.service

import android.content.Context
import com.example.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Service for Attendance System MMU Idadiyah.
 * Supports:
 * 1. Synchronizing teachers from DATA_GURU sheet (or CSV/JSON/Apps Script remote).
 * 2. Saving attendance rows to PRESENSI sheet structure and locally persisting them.
 * 3. Importing & adding teachers directly into the system.
 */
class PresensiApiService(
    private val context: Context? = null,
    private var endpointUrl: String = "https://script.google.com/macros/s/AKfycbyayQpHOQWO-7Waa3QXAsmwpHlb-3bcZpQ9Cfj_-V1dmZqwX5CdudYPu5AS-ASpX7cn/exec"
) {
    private val localStore = context?.let { PresensiLocalStore(it) }

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    // Standalone / Offline in-memory teachers matching sheet DATA_GURU
    private val defaultTeachers = mutableListOf(
        Teacher("14371025", "H. Ahmad Fauzi, S.Pd.I", "VII A", "VIII A", "Gedung Pusat", "01"),
        Teacher("14371026", "Hj. Siti Mariam, M.Pd", "VII B", "VIII B", "Gedung Pusat", "02"),
        Teacher("14371027", "Ust. Muhammad Ridwan, Lc", "VIII A", "IX A", "Gedung Timur", "03"),
        Teacher("14371028", "Ustazah Nur Aini, S.Si", "VIII B", "IX B", "Gedung Barat", "04"),
        Teacher("14371029", "Drs. H. Miftahul Ulum", "IX A", "VII A", "Gedung Pusat", "05"),
        Teacher("14371030", "Abdul Halim, S.Kom", "IX B", "VII B", "Lab Komputer", "06"),
        Teacher("14371031", "K.H. Sholehuddin, M.Ag", "Tahfidz A", "Tahfidz B", "Masjid Kampus", "07"),
        Teacher("14371032", "Ustd. Fatimatuz Zahro, S.Pd", "VII C", "VIII C", "Gedung Putri", "08"),
        Teacher("14371033", "Ust. Ahmad Dahlan, S.Pd", "VIII C", "IX C", "Gedung Timur", "09"),
        Teacher("14371034", "Ustazah Lailatul Badriyah, S.Ag", "VII A", "VII B", "Gedung Putri", "10"),
        Teacher("14371035", "M. Hasan Basri, M.H.I", "IX C", "IX A", "Gedung Pusat", "11"),
        Teacher("14371036", "Ust. Zainal Abidin, S.Pd.I", "Tahfidz B", "Tahfidz A", "Masjid Kampus", "12")
    )

    private val attendanceHistory = mutableListOf<AttendanceRecord>()
    private val leaveList = mutableListOf<LeaveItem>()
    private val logs = mutableListOf<ActivityLog>()

    private var settings = mutableMapOf(
        "NAMA SEKOLAH" to "Presensi Guru MMU Idadiyah",
        "ALAMAT" to "Madrasah Miftahul Ulum (MMU) Idadiyah",
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
        // Restore cached endpoint URL
        localStore?.let { store ->
            endpointUrl = store.loadEndpointUrl(endpointUrl)
            val savedTeachers = store.loadTeachers()
            if (!savedTeachers.isNullOrEmpty()) {
                defaultTeachers.clear()
                defaultTeachers.addAll(savedTeachers)
            }
            val savedAttendance = store.loadAttendanceHistory()
            if (!savedAttendance.isNullOrEmpty()) {
                attendanceHistory.clear()
                attendanceHistory.addAll(savedAttendance)
            }
        }

        // Pre-populate some realistic initial attendances if empty
        if (attendanceHistory.isEmpty()) {
            val clock = calculateIstiwakClock()
            attendanceHistory.add(
                AttendanceRecord(
                    id = "PRS001",
                    timestamp = "${clock.tgl} 07:15:00",
                    tglMasehi = clock.tgl,
                    hari = clock.hari,
                    tglHijriah = clock.hijri.text,
                    jam = "07:15",
                    nama = "H. Ahmad Fauzi, S.Pd.I",
                    tempat = "Gedung Pusat",
                    status = "HADIR",
                    terminal = "T01",
                    pps = "14371025",
                    kelasAsal = "VII A",
                    kelasBaru = "VIII A",
                    no = "01",
                    ket = "Scan tepat waktu",
                    menit = 0
                )
            )
            attendanceHistory.add(
                AttendanceRecord(
                    id = "PRS002",
                    timestamp = "${clock.tgl} 07:25:00",
                    tglMasehi = clock.tgl,
                    hari = clock.hari,
                    tglHijriah = clock.hijri.text,
                    jam = "07:25",
                    nama = "Hj. Siti Mariam, M.Pd",
                    tempat = "Gedung Pusat",
                    status = "HADIR",
                    terminal = "T01",
                    pps = "14371026",
                    kelasAsal = "VII B",
                    kelasBaru = "VIII B",
                    no = "02",
                    ket = "Scan tepat waktu",
                    menit = 0
                )
            )
            attendanceHistory.add(
                AttendanceRecord(
                    id = "PRS003",
                    timestamp = "${clock.tgl} 07:42:00",
                    tglMasehi = clock.tgl,
                    hari = clock.hari,
                    tglHijriah = clock.hijri.text,
                    jam = "07:42",
                    nama = "Ust. Muhammad Ridwan, Lc",
                    tempat = "Gedung Timur",
                    status = "TERLAMBAT",
                    terminal = "T02",
                    pps = "14371027",
                    kelasAsal = "VIII A",
                    kelasBaru = "IX A",
                    no = "03",
                    ket = "Terlambat 12 menit",
                    menit = 12
                )
            )
            leaveList.add(
                LeaveItem(clock.tgl, "14371028", "Ustazah Nur Aini, S.Si", "IZIN", "Keperluan dinas luar kota")
            )
            persistAttendance()
        }
    }

    private fun persistTeachers() {
        localStore?.saveTeachers(defaultTeachers)
    }

    private fun persistAttendance() {
        localStore?.saveAttendanceHistory(attendanceHistory)
    }

    fun getEndpointUrl(): String = endpointUrl

    fun setEndpointUrl(newUrl: String) {
        endpointUrl = newUrl.trim()
        localStore?.saveEndpointUrl(endpointUrl)
    }

    /**
     * Exact calculation of Jam Istiwak (Solar Time) from Code.gs
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
        val parts = gDate.split("-")
        val gDay = parts.getOrNull(2)?.toIntOrNull() ?: 7

        val hm = listOf(
            "Muharram", "Safar", "Rabiul Awal", "Rabiul Akhir",
            "Jumadil Awal", "Jumadil Akhir", "Rajab", "Syaban",
            "Ramadhan", "Syawal", "Zulkaidah", "Zulhijjah"
        )
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
        // Attempt remote fetch
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
                        terminal = json.optString("terminal", "Terminal $terminal"),
                        logo = json.optString("logo", "")
                    )
                }
            }
        } catch (_: Exception) {}

        BootConfig(
            nama = settings["NAMA SEKOLAH"] ?: "Presensi Guru MMU Idadiyah",
            alamat = settings["ALAMAT"] ?: "Madrasah Miftahul Ulum (MMU) Idadiyah",
            refresh = 8,
            clock = calculateIstiwakClock(),
            terminal = "Terminal $terminal",
            logo = ""
        )
    }

    /**
     * Membaca dan Menyinkronkan semua data guru dari remote Google Sheet (sheet DATA_GURU)
     */
    suspend fun fetchTeachersFromRemote(): List<Teacher> = withContext(Dispatchers.IO) {
        try {
            val url = "$endpointUrl?action=getTeachers"
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: ""
                if (body.startsWith("[")) {
                    val arr = JSONArray(body)
                    val remoteTeachers = mutableListOf<Teacher>()
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        remoteTeachers.add(
                            Teacher(
                                pps = obj.optString("pps", ""),
                                nama = obj.optString("nama", ""),
                                kelasAsal = obj.optString("kelasAsal", ""),
                                kelasBaru = obj.optString("kelasBaru", ""),
                                tempat = obj.optString("tempat", ""),
                                no = obj.optString("no", "")
                            )
                        )
                    }
                    if (remoteTeachers.isNotEmpty()) {
                        defaultTeachers.clear()
                        defaultTeachers.addAll(remoteTeachers)
                        persistTeachers()
                        return@withContext remoteTeachers
                    }
                }
            }
        } catch (_: Exception) {}
        defaultTeachers
    }

    /**
     * Memproses presensi guru:
     * - Menyimpan ke database / sheet PRESENSI (lokal dan diteruskan ke remote Apps Script)
     * - Terbaca langsung di seluruh aplikasi (Dashboard, Monitor, Rekap, Hari Ini)
     */
    suspend fun processAttendance(pps: String, terminal: String): AttendanceProcessResult = withContext(Dispatchers.IO) {
        val cleanPps = pps.trim()
        val teacher = defaultTeachers.find { it.pps == cleanPps || it.nama.contains(cleanPps, ignoreCase = true) }
            ?: return@withContext AttendanceProcessResult(
                type = "ERROR",
                msg = "ID TIDAK DITEMUKAN",
                sub = "ID PPS: $pps tidak terdaftar di DATA GURU. Hubungi Tata Usaha."
            )

        val clock = calculateIstiwakClock()
        val jam = clock.jam
        val jamMasukStr = settings["JAM MASUK"] ?: "07:30"
        val toleransi = (settings["TOLERANSI"] ?: "10").toIntOrNull() ?: 10

        val masukParts = jamMasukStr.split(":")
        val masukMins = (masukParts.getOrNull(0)?.toIntOrNull() ?: 7) * 60 + (masukParts.getOrNull(1)?.toIntOrNull() ?: 30)

        val nowParts = jam.split(":")
        val nowMins = (nowParts.getOrNull(0)?.toIntOrNull() ?: 7) * 60 + (nowParts.getOrNull(1)?.toIntOrNull() ?: 0)

        // Cek duplikasi presensi hari ini
        val existing = attendanceHistory.find { it.pps == teacher.pps && it.tglMasehi == clock.tgl }
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
                sub = "Presensi sebelumnya tercatat pada ${existing.jam} Istiwak di ${existing.terminal}."
            )
        }

        val isLate = nowMins > (masukMins + toleransi)
        val menitTelat = if (isLate) nowMins - masukMins else 0
        val status = if (isLate) "TERLAMBAT" else "HADIR"
        val type = if (isLate) "LATE" else "SUCCESS"
        val msg = if (isLate) "Presensi tercatat. Anda terlambat $menitTelat menit." else "Presensi berhasil tercatat!"

        val newRecordId = "PRS-" + UUID.randomUUID().toString().substring(0, 8).uppercase()
        val timestampStr = "${clock.tgl} ${clock.jam}:00"

        val record = AttendanceRecord(
            id = newRecordId,
            timestamp = timestampStr,
            tglMasehi = clock.tgl,
            hari = clock.hari,
            tglHijriah = clock.hijri.text,
            jam = jam,
            nama = teacher.nama,
            tempat = teacher.tempat,
            status = status,
            terminal = terminal,
            pps = teacher.pps,
            kelasAsal = teacher.kelasAsal,
            kelasBaru = teacher.kelasBaru,
            no = teacher.no,
            ket = if (isLate) "Terlambat $menitTelat menit" else "Scan tepat waktu",
            menit = menitTelat
        )

        // Tambahkan ke riwayat presensi lokal & persisten
        attendanceHistory.add(0, record)
        persistAttendance()

        logs.add(0, ActivityLog(
            time = timestampStr,
            user = terminal,
            role = "SCANNER",
            activity = "SCAN PRESENSI",
            pps = teacher.pps,
            before = "BELUM",
            after = status,
            note = msg
        ))

        // Kirim asinkron ke remote Apps Script jika online
        sendAttendanceToRemote(record)

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

    private fun sendAttendanceToRemote(record: AttendanceRecord) {
        try {
            val url = "$endpointUrl?action=processAttendance&pps=${URLEncoder.encode(record.pps, "UTF-8")}&terminal=${record.terminal}"
            val request = Request.Builder().url(url).build()
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: java.io.IOException) {}
                override fun onResponse(call: Call, response: Response) {
                    response.close()
                }
            })
        } catch (_: Exception) {}
    }

    suspend fun getDashboardData(): DashboardData = withContext(Dispatchers.IO) {
        val clock = calculateIstiwakClock()
        val totalTeachers = defaultTeachers.size

        // Attendance hari ini
        val todayAttendances = attendanceHistory.filter { it.tglMasehi.isEmpty() || it.tglMasehi == clock.tgl }
        val attendedPps = todayAttendances.map { it.pps }.toSet()
        val leavePps = leaveList.filter { it.tgl == clock.tgl }.map { it.pps }.toSet()

        var hadir = 0
        var telat = 0
        var alpa = 0
        todayAttendances.forEach {
            when (it.status) {
                "HADIR" -> hadir++
                "TERLAMBAT" -> telat++
                "ALPA" -> alpa++
            }
        }
        val izin = leaveList.count { it.tgl == clock.tgl && it.jenis == "IZIN" }
        val sakit = leaveList.count { it.tgl == clock.tgl && it.jenis == "SAKIT" }
        val dinas = leaveList.count { it.tgl == clock.tgl && it.jenis == "DINAS" }
        val sudah = hadir + telat

        val belumTeachers = defaultTeachers.filter { !attendedPps.contains(it.pps) && !leavePps.contains(it.pps) }
        val belum = belumTeachers.size

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

        // Rekap per tempat
        val placeMap = mutableMapOf<String, Pair<Int, Int>>()
        defaultTeachers.forEach { t ->
            val cur = placeMap[t.tempat] ?: Pair(0, 0)
            val isSudah = attendedPps.contains(t.pps)
            placeMap[t.tempat] = Pair(cur.first + 1, cur.second + (if (isSudah) 1 else 0))
        }
        val tempatList = placeMap.map { (nama, pair) ->
            PlaceSummary(nama = nama, total = pair.first, sudah = pair.second)
        }

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
            latest = todayAttendances.take(20),
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
            val matches = attendanceHistory.filter { it.pps == t.pps && (it.tglMasehi.isEmpty() || (it.tglMasehi >= from && it.tglMasehi <= to)) }
            val leaveMatches = leaveList.filter { it.pps == t.pps && (it.tgl.isEmpty() || (it.tgl >= from && it.tgl <= to)) }

            val hadir = matches.count { it.status == "HADIR" }
            val telat = matches.count { it.status == "TERLAMBAT" }
            val alpa = matches.count { it.status == "ALPA" }
            val izin = leaveMatches.count { it.jenis == "IZIN" }
            val sakit = leaveMatches.count { it.jenis == "SAKIT" }
            val dinas = leaveMatches.count { it.jenis == "DINAS" }

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
        val existing = defaultTeachers.indexOfFirst { it.pps == teacher.pps }
        if (existing >= 0) {
            defaultTeachers[existing] = teacher
        } else {
            defaultTeachers.add(teacher)
        }
        persistTeachers()
        true
    }

    suspend fun addTeachersBatch(newTeachers: List<Teacher>): Int = withContext(Dispatchers.IO) {
        var added = 0
        newTeachers.forEach { t ->
            val existing = defaultTeachers.indexOfFirst { it.pps == t.pps }
            if (existing >= 0) {
                defaultTeachers[existing] = t
            } else {
                defaultTeachers.add(t)
                added++
            }
        }
        persistTeachers()
        added
    }

    suspend fun getAttendanceHistory(): List<AttendanceRecord> = withContext(Dispatchers.IO) {
        attendanceHistory
    }

    suspend fun updateAttendance(pps: String, tgl: String, status: String, jam: String, ket: String, alasan: String): Boolean = withContext(Dispatchers.IO) {
        val teacher = defaultTeachers.find { it.pps == pps } ?: return@withContext false
        val existingIndex = attendanceHistory.indexOfFirst { it.pps == pps && it.tglMasehi == tgl }
        val clock = calculateIstiwakClock()

        val record = AttendanceRecord(
            id = "PRS-KOR-" + UUID.randomUUID().toString().substring(0, 6).uppercase(),
            timestamp = "$tgl $jam:00",
            tglMasehi = tgl,
            hari = clock.hari,
            tglHijriah = clock.hijri.text,
            jam = jam,
            nama = teacher.nama,
            tempat = teacher.tempat,
            status = status,
            terminal = "ADMIN",
            pps = pps,
            kelasAsal = teacher.kelasAsal,
            kelasBaru = teacher.kelasBaru,
            no = teacher.no,
            ket = "Koreksi: $alasan ($ket)",
            menit = 0
        )
        if (existingIndex >= 0) {
            attendanceHistory[existingIndex] = record
        } else {
            attendanceHistory.add(0, record)
        }
        persistAttendance()

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
        val clock = calculateIstiwakClock()
        val attendedPps = attendanceHistory.filter { it.tglMasehi == tgl }.map { it.pps }.toSet()
        val leavePps = leaveList.filter { it.tgl == tgl }.map { it.pps }.toSet()
        var alpaCount = 0

        defaultTeachers.forEach { teacher ->
            if (!attendedPps.contains(teacher.pps) && !leavePps.contains(teacher.pps)) {
                val record = AttendanceRecord(
                    id = "PRS-ALP-" + UUID.randomUUID().toString().substring(0, 6).uppercase(),
                    timestamp = "$tgl 12:30:00",
                    tglMasehi = tgl,
                    hari = clock.hari,
                    tglHijriah = clock.hijri.text,
                    jam = "-",
                    nama = teacher.nama,
                    tempat = teacher.tempat,
                    status = "ALPA",
                    terminal = "SISTEM",
                    pps = teacher.pps,
                    kelasAsal = teacher.kelasAsal,
                    kelasBaru = teacher.kelasBaru,
                    no = teacher.no,
                    ket = "Tutup presensi otomatis",
                    menit = 0
                )
                attendanceHistory.add(record)
                alpaCount++
            }
        }
        persistAttendance()

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
