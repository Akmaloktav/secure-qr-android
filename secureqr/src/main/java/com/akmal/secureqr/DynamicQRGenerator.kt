package com.akmal.secureqr

import dev.samstevens.totp.code.DefaultCodeGenerator
import dev.samstevens.totp.code.HashingAlgorithm
import dev.samstevens.totp.time.SystemTimeProvider
import org.json.JSONObject

/**
 * Kelas utama yang bertanggung jawab untuk menghasilkan data QR code dinamis.
 *
 * @param sessionSecretKey Kunci rahasia unik untuk satu sesi absensi.
 * @param periodInSeconds Durasi validitas setiap kode dalam detik. Defaultnya adalah 30.
 */
class DynamicQRGenerator(
    sessionSecretKey: String,
    private val periodInSeconds: Int = 30, // <-- PARAMETER BARU DITAMBAHKAN
    private val timeProvider: TimeProvider
) {
    constructor(sessionSecretKey: String, periodInSeconds: Int = 30): this (
        sessionSecretKey,
        periodInSeconds,
        com.akmal.secureqr.SystemTimeProvider()
    )

    init {
        require(periodInSeconds > 0) {"Periode waktu (periodInSeconds) harus lebih besar daei 0."}
    }

//    private val timeProvider = SystemTimeProvider()
    private val codeGenerator = DefaultCodeGenerator(HashingAlgorithm.SHA256, 6)
    private val secret = sessionSecretKey

    /**
     * Menghasilkan string JSON berisi sessionId dan TOTP yang dapat digunakan
     * untuk menghasilkan QR code dinamis pada sisi klien.
     *
     * @param sessionId ID unik untuk sesi absensi yang sedang berjalan.
     * @return String JSON yang siap dikonversi menjadi QR code.
     */

    fun getCurrentQrData(sessionId: Int): String {
        // Gunakan `periodInSeconds` yang sudah dikonfigurasi
        val counter = timeProvider.time / periodInSeconds //
        val currentTotp = codeGenerator.generate(secret, counter)

        val qrJson = JSONObject()
        qrJson.put("sessionId", sessionId)
        qrJson.put("totp", currentTotp)

        return qrJson.toString()
    }

    fun getSecondsUntilNextCode(): Int {
        val epochSeconds = timeProvider.time
        // Gunakan `periodInSeconds` yang sudah dikonfigurasi
        return periodInSeconds - (epochSeconds % periodInSeconds).toInt()
    }
}