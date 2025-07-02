package com.akmal.qrauthenticatorapp

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.akmal.secureqr.SecureProcessor // <-- Impor library kita
import kotlinx.coroutines.launch

class ScannerTestActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scanner_test)

        val btnSimulateScan: Button = findViewById(R.id.btn_simulate_scan)

        btnSimulateScan.setOnClickListener {
            // Kita tidak perlu memindai QR sungguhan, kita langsung uji proses keamanannya
            // Memulai coroutine untuk memanggil fungsi suspend dari library
            lifecycleScope.launch {
                val authResult = SecureProcessor.requestBiometricAuth(
                    activity = this@ScannerTestActivity,
                    title = "Verifikasi Identitas",
                    subtitle = "Pindai sidik jari atau wajah Anda"
                )

                // Cek hasil dari verifikasi biometrik
                when (authResult) {
                    is SecureProcessor.BiometricResult.Success -> {
                        // Jika berhasil, dapatkan ID perangkat & tampilkan Toast
                        val deviceId = SecureProcessor.getDeviceIdentifier(this@ScannerTestActivity)
                        val message = "Verifikasi Berhasil! Device ID: $deviceId"
                        Toast.makeText(this@ScannerTestActivity, message, Toast.LENGTH_LONG).show()
                    }
                    is SecureProcessor.BiometricResult.Error -> {
                        Toast.makeText(this@ScannerTestActivity, "Error: ${authResult.message}", Toast.LENGTH_SHORT).show()
                    }
                    is SecureProcessor.BiometricResult.Canceled -> {
                        Toast.makeText(this@ScannerTestActivity, "Dibatalkan oleh pengguna", Toast.LENGTH_SHORT).show()
                    }
                    is SecureProcessor.BiometricResult.NotAvailable -> {
                        Toast.makeText(this@ScannerTestActivity, "Fitur biometrik tidak tersedia", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}