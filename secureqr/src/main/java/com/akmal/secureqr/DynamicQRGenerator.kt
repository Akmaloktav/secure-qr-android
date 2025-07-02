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
    private val periodInSeconds: Int = 30 // <-- PARAMETER BARU DITAMBAHKAN
) {

    private val timeProvider = SystemTimeProvider()
    private val codeGenerator = DefaultCodeGenerator(HashingAlgorithm.SHA256, 6)
    private val secret = sessionSecretKey

    fun getCurrentQrData(sessionId: Int): String {
        // Gunakan `periodInSeconds` yang sudah dikonfigurasi
        val counter = timeProvider.time / periodInSeconds // <-- LOGIKA DIPERBARUI
        val currentTotp = codeGenerator.generate(secret, counter)

        val qrJson = JSONObject()
        qrJson.put("sessionId", sessionId)
        qrJson.put("totp", currentTotp)

        return qrJson.toString()
    }

    fun getSecondsUntilNextCode(): Int {
        val epochSeconds = timeProvider.time
        // Gunakan `periodInSeconds` yang sudah dikonfigurasi
        return periodInSeconds - (epochSeconds % periodInSeconds).toInt() // <-- LOGIKA DIPERBARUI
    }
}