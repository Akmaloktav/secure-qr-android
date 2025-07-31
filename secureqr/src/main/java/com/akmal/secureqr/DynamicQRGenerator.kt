package com.akmal.secureqr

import dev.samstevens.totp.code.DefaultCodeGenerator
import dev.samstevens.totp.code.HashingAlgorithm
import dev.samstevens.totp.time.SystemTimeProvider
import org.json.JSONObject
import java.util.Base64
import org.apache.commons.codec.binary.Base32
import java.nio.ByteBuffer
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.math.pow

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

    private val codeDigits = 6
    private val secretBytes: ByteArray

    init {
        require(periodInSeconds > 0) {"Periode waktu (periodInSeconds) harus lebih besar daei 0."}
        val base32 = Base32()
        this.secretBytes = base32.decode(sessionSecretKey)
    }

//    private val timeProvider = SystemTimeProvider()
//    private val codeGenerator = DefaultCodeGenerator(HashingAlgorithm.SHA256, 6)
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
//        val currentTotp = codeGenerator.generate(secretBytes, counter)
        val currentTotp = generateTotp(secretBytes, counter, codeDigits, "HmacSHA256")

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

    private fun generateTotp(key: ByteArray, counter: Long, digits: Int, algorithm: String): String {
        // 1. Ubah counter menjadi array 8 byte
        val counterBytes = ByteBuffer.allocate(8).putLong(counter).array()

        // 2. Buat instance HMAC dengan algoritma yang sesuai (HmacSHA256)
        val mac = Mac.getInstance(algorithm)
        val keySpec = SecretKeySpec(key, "RAW")
        mac.init(keySpec)

        // 3. Hitung hash dari counter menggunakan secret key
        val hash = mac.doFinal(counterBytes)

        // 4. Proses 'truncation' untuk mendapatkan kode numerik
        // Ambil 4 bit terakhir dari hash sebagai offset
        val offset = hash[hash.size - 1].toInt() and 0x0F

        // Ambil 4 byte dari hash dimulai dari offset
        val truncatedHash = ByteBuffer.wrap(hash, offset, 4).int

        // Hilangkan bit paling signifikan (MSB)
        val code = truncatedHash and 0x7FFFFFFF

        // Ambil sisa bagi dengan 10^jumlah_digit
        val finalCode = code % (10.0.pow(digits)).toInt()

        // 5. Format menjadi string dengan padding nol di depan jika perlu
        return finalCode.toString().padStart(digits, '0')
    }
}