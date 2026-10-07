package com.example.service

import android.content.Context
import com.example.model.AttendanceRecord
import com.example.model.Teacher
import org.json.JSONArray
import org.json.JSONObject

class PresensiLocalStore(context: Context) {
    private val prefs = context.getSharedPreferences("presensi_mmu_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_TEACHERS = "key_teachers_data"
        private const val KEY_ATTENDANCE = "key_attendance_data"
        private const val KEY_ENDPOINT = "key_endpoint_url"
    }

    fun saveTeachers(teachers: List<Teacher>) {
        val array = JSONArray()
        teachers.forEach { t ->
            val obj = JSONObject()
            obj.put("pps", t.pps)
            obj.put("nama", t.nama)
            obj.put("kelasAsal", t.kelasAsal)
            obj.put("kelasBaru", t.kelasBaru)
            obj.put("tempat", t.tempat)
            obj.put("no", t.no)
            array.put(obj)
        }
        prefs.edit().putString(KEY_TEACHERS, array.toString()).apply()
    }

    fun loadTeachers(): List<Teacher>? {
        val raw = prefs.getString(KEY_TEACHERS, null) ?: return null
        return try {
            val array = JSONArray(raw)
            val list = mutableListOf<Teacher>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
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
            if (list.isNotEmpty()) list else null
        } catch (_: Exception) {
            null
        }
    }

    fun saveAttendanceHistory(records: List<AttendanceRecord>) {
        val array = JSONArray()
        records.forEach { r ->
            val obj = JSONObject()
            obj.put("id", r.id)
            obj.put("timestamp", r.timestamp)
            obj.put("tglMasehi", r.tglMasehi)
            obj.put("hari", r.hari)
            obj.put("tglHijriah", r.tglHijriah)
            obj.put("jam", r.jam)
            obj.put("nama", r.nama)
            obj.put("tempat", r.tempat)
            obj.put("status", r.status)
            obj.put("terminal", r.terminal)
            obj.put("pps", r.pps)
            obj.put("kelasAsal", r.kelasAsal)
            obj.put("kelasBaru", r.kelasBaru)
            obj.put("no", r.no)
            obj.put("ket", r.ket)
            obj.put("menit", r.menit)
            array.put(obj)
        }
        prefs.edit().putString(KEY_ATTENDANCE, array.toString()).apply()
    }

    fun loadAttendanceHistory(): List<AttendanceRecord>? {
        val raw = prefs.getString(KEY_ATTENDANCE, null) ?: return null
        return try {
            val array = JSONArray(raw)
            val list = mutableListOf<AttendanceRecord>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    AttendanceRecord(
                        id = obj.optString("id", ""),
                        timestamp = obj.optString("timestamp", ""),
                        tglMasehi = obj.optString("tglMasehi", ""),
                        hari = obj.optString("hari", ""),
                        tglHijriah = obj.optString("tglHijriah", ""),
                        jam = obj.optString("jam", ""),
                        nama = obj.optString("nama", ""),
                        tempat = obj.optString("tempat", ""),
                        status = obj.optString("status", ""),
                        terminal = obj.optString("terminal", ""),
                        pps = obj.optString("pps", ""),
                        kelasAsal = obj.optString("kelasAsal", ""),
                        kelasBaru = obj.optString("kelasBaru", ""),
                        no = obj.optString("no", ""),
                        ket = obj.optString("ket", ""),
                        menit = obj.optInt("menit", 0)
                    )
                )
            }
            list
        } catch (_: Exception) {
            null
        }
    }

    fun saveEndpointUrl(url: String) {
        prefs.edit().putString(KEY_ENDPOINT, url).apply()
    }

    fun loadEndpointUrl(defaultUrl: String): String {
        return prefs.getString(KEY_ENDPOINT, defaultUrl) ?: defaultUrl
    }
}
