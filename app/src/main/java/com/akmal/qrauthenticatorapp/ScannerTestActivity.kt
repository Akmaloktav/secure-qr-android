package com.akmal.qrauthenticatorapp

import com.akmal.secureqr.SecureProcessor
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.launch
import org.json.JSONObject

class ScannerTestActivity : AppCompatActivity() {

    // Siapkan launcher untuk menerima hasil dari aktivitas scan
    private val barcodeLauncher = registerForActivityResult(ScanContract()) { result ->
        if (result.contents == null) {
            // Ini terjadi jika pengguna menekan tombol kembali tanpa memindai
            Toast.makeText(this, "Scan dibatalkan", Toast.LENGTH_LONG).show()
        } else {
            // Jika berhasil, tampilkan data yang didapat dari QR Code
            val scannedData = result.contents

            // Coba ekstrak sessionId dari data JSON untuk ditampilkan
            try {
                val jsonObject = JSONObject(scannedData)
                val sessionId = jsonObject.getInt("sessionId")
                Toast.makeText(this, "Scan Berhasil! Session ID: $sessionId", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                // Jika data bukan JSON, tampilkan mentahannya
                Toast.makeText(this, "Scan Berhasil! Data: $scannedData", Toast.LENGTH_LONG).show()
            }

            // Setelah scan berhasil, kita bisa lanjutkan ke proses verifikasi biometrik
            prosesVerifikasiLanjutan()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scanner_test)

        val btnScan: Button = findViewById(R.id.btn_simulate_scan) // Kita anggap ID tombolnya sama
        btnScan.text = "Pindai QR Code" // Ganti teks tombol agar lebih sesuai

        btnScan.setOnClickListener {
            // Memulai proses scan saat tombol ditekan
            memulaiScan()
        }
    }

    /**
     * Fungsi untuk mengonfigurasi dan meluncurkan layar pemindai QR.
     */
    private fun memulaiScan() {
        val options = ScanOptions()
        options.setPrompt("Arahkan kamera ke QR Code")
        options.setBeepEnabled(true) // Mainkan suara 'bip' saat berhasil
        options.setOrientationLocked(false) // Izinkan rotasi layar

        barcodeLauncher.launch(options)
    }

    /**
     * Fungsi yang berisi logika verifikasi keamanan (biometrik dan device ID).
     * Dipanggil setelah QR code berhasil dipindai.
     */
    private fun prosesVerifikasiLanjutan() {
        lifecycleScope.launch {
            val authResult = SecureProcessor.requestBiometricAuth(
                activity = this@ScannerTestActivity,
                title = "Verifikasi Identitas",
                subtitle = "Pindai sidik jari atau wajah Anda"
            )

            when (authResult) {
                is SecureProcessor.BiometricResult.Success -> {
                    val deviceId = SecureProcessor.getDeviceIdentifier(this@ScannerTestActivity)
                    val message = "Verifikasi Biometrik Berhasil! Device ID: $deviceId"
                    Toast.makeText(this@ScannerTestActivity, message, Toast.LENGTH_LONG).show()
                }
                is SecureProcessor.BiometricResult.Error -> {
                    Toast.makeText(this@ScannerTestActivity, "Error Biometrik: ${authResult.message}", Toast.LENGTH_SHORT).show()
                }
                is SecureProcessor.BiometricResult.Canceled -> {
                    Toast.makeText(this@ScannerTestActivity, "Verifikasi Biometrik Dibatalkan", Toast.LENGTH_SHORT).show()
                }
                is SecureProcessor.BiometricResult.NotAvailable -> {
                    Toast.makeText(this@ScannerTestActivity, "Fitur biometrik tidak tersedia", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}