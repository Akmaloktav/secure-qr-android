package com.akmal.qrauthenticatorapp

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.akmal.secureqr.DynamicQRGenerator
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder

class MainActivity : AppCompatActivity() {

    private lateinit var ivQrCode: ImageView
    private lateinit var tvTimer: TextView
    private lateinit var btnTestSecurity: Button

    // Handler untuk menjalankan kode berulang kali (setiap detik)
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var qrUpdater: Runnable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ivQrCode = findViewById(R.id.iv_qr_code)
        tvTimer = findViewById(R.id.tv_timer)
        btnTestSecurity = findViewById(R.id.btn_test_security)

        // Mulai proses pembuatan QR dinamis
        startDynamicQrSession()

        btnTestSecurity.setOnClickListener {
            startActivity(Intent(this, ScannerTestActivity::class.java))
        }
    }

    private fun startDynamicQrSession() {
        // --- SIMULASI DATA DARI SERVER ---
        // Di aplikasi nyata, data ini didapat dari panggilan API setelah dosen memulai sesi.
        val mockSessionId = 12345
        val mockSecretKey = "super-rahasia-jangan-disebar-luaskan" // Kunci harus sama dengan yang diverifikasi server

        // 1. Buat instance dari kelas library kita
        val qrGenerator = DynamicQRGenerator(mockSecretKey, 60)

        // 2. Siapkan Runnable yang akan berjalan setiap detik
        qrUpdater = object : Runnable {
            override fun run() {
                // 3. Panggil fungsi dari library kita
                val qrData = qrGenerator.getCurrentQrData(mockSessionId)
                val timeLeft = qrGenerator.getSecondsUntilNextCode()

                // 4. Tampilkan hasilnya ke UI
                displayQrCode(qrData)
                tvTimer.text = "QR baru dalam: $timeLeft detik"

                // Jadwalkan eksekusi berikutnya setelah 1 detik
                handler.postDelayed(this, 1000)
            }
        }

        // Jalankan untuk pertama kali
        handler.post(qrUpdater)
    }

    private fun displayQrCode(data: String) {
        try {
            val barcodeEncoder = BarcodeEncoder()
            val bitmap: Bitmap = barcodeEncoder.encodeBitmap(data, BarcodeFormat.QR_CODE, 800, 800)
            ivQrCode.setImageBitmap(bitmap)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onPause() {
        super.onPause()
        // Hentikan updater jika aplikasi tidak di depan untuk hemat baterai
        if (::qrUpdater.isInitialized) {
            handler.removeCallbacks(qrUpdater)
        }
    }
}