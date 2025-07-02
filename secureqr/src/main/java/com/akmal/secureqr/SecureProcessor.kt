package com.akmal.secureqr

import android.content.Context
import android.provider.Settings
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Menyediakan perkakas keamanan opsional untuk sisi pemindai (mahasiswa).
 * Termasuk otentikasi biometrik dan identifikasi perangkat.
 */
object SecureProcessor {

    /**
     * Hasil dari proses otentikasi biometrik.
     */
    sealed class BiometricResult {
        object Success : BiometricResult() // Otentikasi berhasil
        data class Error(val message: String) : BiometricResult() // Terjadi error
        object NotAvailable : BiometricResult() // Perangkat tidak mendukung biometrik
        object Canceled : BiometricResult() // Pengguna membatalkan
    }

    /**
     * Menghasilkan ID unik untuk perangkat Android.
     * Berguna untuk fitur 'Device Binding'.
     *
     * @param context Context aplikasi.
     * @return String ID unik perangkat.
     */
    fun getDeviceIdentifier(context: Context): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
    }

    /**
     * Memulai proses otentikasi biometrik (sidik jari/wajah).
     * Fungsi ini adalah 'suspend function' sehingga harus dipanggil dari dalam coroutine.
     *
     * @param activity Activity yang sedang berjalan (harus FragmentActivity).
     * @param title Judul yang ditampilkan di dialog biometrik.
     * @param subtitle Sub-judul atau deskripsi singkat.
     * @return Sebuah [BiometricResult] yang menandakan hasil proses.
     */
    suspend fun requestBiometricAuth(
        activity: FragmentActivity,
        title: String,
        subtitle: String
    ): BiometricResult {
        // Cek dulu apakah biometrik tersedia di perangkat
        val biometricManager = BiometricManager.from(activity)
        if (biometricManager.canAuthenticate(BIOMETRIC_STRONG) != BiometricManager.BIOMETRIC_SUCCESS) {
            return BiometricResult.NotAvailable
        }

        // Menggunakan suspendCancellableCoroutine untuk mengubah callback menjadi coroutine
        return suspendCancellableCoroutine { continuation ->
            val executor = ContextCompat.getMainExecutor(activity)

            val callback = object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    if (continuation.isActive) {
                        continuation.resume(BiometricResult.Success)
                    }
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    // Jika pengguna menekan tombol "cancel", jangan dianggap error fatal
                    if (errorCode == BiometricPrompt.ERROR_USER_CANCELED || errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                        if (continuation.isActive) {
                            continuation.resume(BiometricResult.Canceled)
                        }
                    } else if (continuation.isActive) {
                        continuation.resume(BiometricResult.Error(errString.toString()))
                    }
                }
            }

            val prompt = BiometricPrompt(activity, executor, callback)
            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setNegativeButtonText("Batal")
                .build()

            prompt.authenticate(promptInfo)
        }
    }
}