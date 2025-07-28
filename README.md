# secure-qr-android
Sebuah library Android yang menyediakan toolset keamanan berlapis untuk sistem absensi berbasis QR Code. Library ini dirancang untuk mencegah kecurangan umum seperti penggunaan ulang QR Code dan berbagi akun.

---

## Deskripsi
secure-qr-android membungkus logika keamanan yang kompleks ke dalam komponen yang sederhana dan mudah digunakan. Tujuannya adalah untuk memungkinkan pengembang Android mengintegrasikan sistem absensi yang aman dengan cepat tanpa harus mengimplementasikan algoritma keamanan dari nol.

---

## Prasyarat Sistem
> ⚠️ Penting: Library ini adalah komponen sisi klien (client-side). Untuk dapat berfungsi penuh, Anda wajib memiliki backend/server yang menangani logika berikut:

Endpoint Pembuatan Sesi: Harus ada endpoint API yang mampu menghasilkan sessionSecretKey yang aman secara kriptografis dan mengirimkannya ke klien.

Endpoint Validasi Absensi: Harus ada endpoint API yang mampu menerima sessionId dan totp dari klien, lalu melakukan validasi TOTP di sisi server untuk memastikan keaslian kode.

---

## Fitur ✨
- QR Code Dinamis: Menghasilkan QR Code yang berubah secara periodik (default 30 detik) menggunakan standar keamanan TOTP. <br> 
- Otentikasi Biometrik: Helper untuk menampilkan dialog otentikasi sidik jari atau wajah. <br> 
- Device Binding Helper: Menyediakan fungsi untuk mendapatkan ID unik perangkat. <br>
- Fleksibel: Durasi validitas QR Code dapat diatur sesuai kebutuhan. <br>

  ---

  ## Instalasi Gradle
1. Tambahkan repository JitPack ke file settings.gradle.kts Anda. <br> 
```Kotlin

dependencyResolutionManagement {
    repositories {
        // ...
        maven { url 'https://jitpack.io' }
    }
}
```

2. Tambahkan dependensi ke file build.gradle.kts modul aplikasi Anda. Ganti Tag dengan versi rilis terbaru (misal: 1.0.2). <br> 
```Kotlin

dependencies {
    implementation 'com.github.Akmaloktav:secure-qr-android:Tag'
}
```

---

## Panduan API
### 1. Komponen DynamicQRGenerator
Komponen ini digunakan untuk menghasilkan data QR Code yang dinamis.

Fungsi Utama:
* getCurrentQrData(sessionId: Int): String
    * Tugas: Menghasilkan data QR dinamis dalam format String JSON.
    * Parameter: Menerima sessionId (Int) yang didapat dari server Anda.
    * Mengembalikan: Sebuah String JSON, contoh: {"sessionId":123,"totp":"456789"}.

* getSecondsUntilNextCode(): Int
    * Tugas: Menghitung sisa waktu sebelum kode TOTP berganti.
    * Mengembalikan: Sebuah Int yang mewakili sisa detik (misal: 25).

### 2. Komponen SecureProcessor
Komponen ini menyediakan lapisan keamanan tambahan sebelum data dikirim ke server.

Fungsi Utama:
* requestBiometricAuth(...)
  * Tugas: Menampilkan dialog otentikasi biometrik (sidik jari/wajah).
  * Parameter: Membutuhkan activity, title, dan subtitle untuk dialog.
  * Mengembalikan: Sebuah BiometricResult yang menandakan hasil proses (Success, Error, Canceled, NotAvailable).

* getDeviceIdentifier(context: Context): String
  * Tugas: Mendapatkan ID unik dari perangkat Android.
  * Parameter: Membutuhkan Context.
  * Mengembalikan: Sebuah String yang merupakan ID unik perangkat.

---

## Cara Penggunaan
**1. Menampilkan QR Code Dinamis <br>**

Gunakan DynamicQRGenerator untuk menampilkan QR Code. Anda perlu menginisialisasinya dengan secretKey yang didapat dari server Anda.

> Catatan: Library ini hanya menghasilkan data QR Code. Untuk menampilkannya sebagai gambar, Anda memerlukan library lain seperti ZXing.

> ⚠️ Penting: Mengelola Lifecycle untuk Mencegah Kebocoran Memori.
> Karena QR Code perlu diperbarui setiap detik menggunakan Handler atau Coroutine, sangat penting untuk memulai dan menghentikan proses ini sesuai dengan lifecycle Activity atau Fragment.

```Kotlin

import com.akmal.secureqr.DynamicQRGenerator
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.google.zxing.BarcodeFormat

// Inisialisasi generator dari library
val qrGenerator = DynamicQRGenerator(secretKeyFromServer)

// Fungsi untuk memperbarui UI setiap detik
fun updateQRCode() {
    lifecycleScope.launch {
        // Loop ini akan berhenti otomatis saat halaman ditutup
        while (isActive) {
            val qrData = qrGenerator.getCurrentQrData(currentSessionId)
            val timeLeft = qrGenerator.getSecondsUntilNextCode()

            // Update UI Anda
            displayQrImage(qrData, yourImageView)
            timerView.text = "Sisa waktu: $timeLeft detik"

            // Tunggu 1 detik sebelum iterasi berikutnya
            delay(1000)
        }
    }
}

// Helper untuk merender data string menjadi gambar Bitmap
private fun displayQrImage(data: String, imageView: ImageView) {
    try {
        val barcodeEncoder = BarcodeEncoder()
        val bitmap: Bitmap = barcodeEncoder.encodeBitmap(data, BarcodeFormat.QR_CODE, 800, 800)
        imageView.setImageBitmap(bitmap)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
```

**2. Meminta Verifikasi Keamanan <br>**

Gunakan SecureProcessor untuk menambahkan lapisan keamanan sebelum mengirim data absensi ke server.

```Kotlin

import com.akmal.secureqr.SecureProcessor
import kotlinx.coroutines.launch

// Setelah QR berhasil dipindai...
fun onQrScanned(qrContent: String) {
    lifecycleScope.launch {
        // 1. Minta verifikasi biometrik
        val authResult = SecureProcessor.requestBiometricAuth(
            activity = this@MainActivity,
            title = "Konfirmasi Absensi",
            subtitle = "Gunakan sidik jari/wajah Anda"
        )

        // 2. Cek jika verifikasi berhasil
        if (authResult is SecureProcessor.BiometricResult.Success) {
            // 3. Ambil ID perangkat
            val deviceId = SecureProcessor.getDeviceIdentifier(this@MainActivity)

            // 4. Kirim semua data (qrContent + deviceId) ke server Anda
            sendAttendanceToServer(qrContent, deviceId)
        } else {
            Toast.makeText(this, "Verifikasi gagal, absensi dibatalkan.", Toast.LENGTH_SHORT).show()
        }
    }
}
```

---

## 🧪 Contoh Cepat & Uji Coba Lokal (Tanpa Backend)
Jika Anda ingin mencoba fungsionalitas library ini dengan cepat tanpa perlu menyiapkan backend, Anda bisa mensimulasikan secretKey secara lokal.

Peringatan: Alur ini TIDAK AMAN untuk lingkungan produksi dan hanya ditujukan untuk keperluan demo dan uji coba.

```Kotlin

// Contoh di dalam sebuah Activity untuk demo
private fun startDemoSession() {
    // Simulasi data dari server
    val mockSessionId = 999
    val mockSecretKey = "kunci-rahasia-untuk-keperluan-demo-saja"

    val qrGenerator = DynamicQRGenerator(mockSecretKey)

    // Gunakan Handler atau Coroutine untuk memperbarui UI setiap detik
    val handler = Handler(Looper.getMainLooper())
    handler.post(object : Runnable {
        override fun run() {
            val qrData = qrGenerator.getCurrentQrData(mockSessionId)
            val timeLeft = qrGenerator.getSecondsUntilNextCode()

            // Tampilkan hasilnya ke UI (membutuhkan fungsi displayQrImage)
            displayQrImage(qrData, yourImageView) 
            timerView.text = "Sisa waktu: $timeLeft detik"

            handler.postDelayed(this, 1000)
        }
    })
}
```

---

## Lisensi
Proyek ini dilisensikan di bawah MIT License. Lihat file LICENSE untuk detailnya.

