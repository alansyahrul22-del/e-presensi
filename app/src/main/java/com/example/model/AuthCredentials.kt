package com.example.model

object AuthCredentials {
    // 5 Terminal logins: T01..T05 (Username & Password match) -> Access to SCANNER ONLY
    val TERMINAL_ACCOUNTS = mapOf(
        "T01" to ("T01" to "Terminal 01"),
        "T02" to ("T02" to "Terminal 02"),
        "T03" to ("T03" to "Terminal 03"),
        "T04" to ("T04" to "Terminal 04"),
        "T05" to ("T05" to "Terminal 05")
    )

    // Admin accounts: TU1..TU3, WK1..WK6, Kepsek (Username & Password match) -> Access to Monitor, Dashboard, Rekap, Hari Ini, Izin, Setting
    val ADMIN_ACCOUNTS = mapOf(
        "TU1" to ("TU1" to "Tata Usaha 1"),
        "TU2" to ("TU2" to "Tata Usaha 2"),
        "TU3" to ("TU3" to "Tata Usaha 3"),
        "WK1" to ("WK1" to "Wakil Kepala 1"),
        "WK2" to ("WK2" to "Wakil Kepala 2"),
        "WK3" to ("WK3" to "Wakil Kepala 3"),
        "WK4" to ("WK4" to "Wakil Kepala 4"),
        "WK5" to ("WK5" to "Wakil Kepala 5"),
        "WK6" to ("WK6" to "Wakil Kepala 6"),
        "KEPSEK" to ("KEPSEK" to "Kepala Sekolah")
    )

    fun authenticate(rawUser: String, rawPass: String): UserSession? {
        val u = rawUser.trim().uppercase()
        val p = rawPass.trim()

        // Check Terminal account
        TERMINAL_ACCOUNTS[u]?.let { (expectedPass, displayName) ->
            if (p.equals(expectedPass, ignoreCase = true)) {
                return UserSession(
                    username = u,
                    role = UserRole.TERMINAL,
                    displayName = displayName,
                    terminalId = u
                )
            }
        }

        // Check Admin account
        ADMIN_ACCOUNTS[u]?.let { (expectedPass, displayName) ->
            if (p.equals(expectedPass, ignoreCase = true)) {
                return UserSession(
                    username = u,
                    role = UserRole.ADMIN,
                    displayName = displayName,
                    terminalId = null
                )
            }
        }

        return null
    }
}
