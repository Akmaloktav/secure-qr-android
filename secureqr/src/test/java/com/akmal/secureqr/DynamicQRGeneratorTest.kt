package com.akmal.secureqr

import org.json.JSONObject
import org.junit.Assert.*

import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class DynamicQRGeneratorTest {

    private lateinit var mockTimeProvider: TimeProvider

    @Before
    fun setUp() {
        mockTimeProvider = mock()
    }

    @Test
    fun `getCurrentQrData returns correct TOTP for a given time`() {
        // 1. Persiapan
        val secretKey = "MySuperSecretKey"
        val sessionId = 123
        val period = 30

        // Kita tentukan waktu spesifik, misal: 1 Januari 2023, jam 00:00:00 GMT
        val specificTime = 1672531200L
        whenever(mockTimeProvider.time).thenReturn(specificTime)

        // Buat generator dengan time provider palsu kita
        val dynamicQRGenerator = DynamicQRGenerator(
            sessionSecretKey = secretKey,
            periodInSeconds = period,
            timeProvider = mockTimeProvider
        )

        // Kode TOTP yang diharapkan untuk kunci, waktu, dan periode di atas
        // (Nilai ini sudah dihitung sebelumnya untuk keperluan tes)
        val expectedTotp = "108598"

        // 2. Act (Eksekusi)
        val qrDataString = dynamicQRGenerator.getCurrentQrData(sessionId)

        // 3. Assert (Verifikasi)
        // Parse string JSON untuk memeriksa isinya
        val resultJson = JSONObject(qrDataString)
        val actualTotp = resultJson.getString("totp")
        val actualSessionId = resultJson.getInt("sessionId")

        assertEquals("TOTP code should match the expected value", expectedTotp, actualTotp)
        assertEquals("Session ID should match", sessionId, actualSessionId)
    }

    @Test
    fun `getSecondsUntilNextCode returns correct remaining seconds`() {
        // 1. Arrange
        val period = 30

        // Kita tentukan waktu yang tidak pas di awal periode, misal 5 detik setelah awal periode.
        val specificTime = 1672531205L // ...00:00:05 GMT
        whenever(mockTimeProvider.time).thenReturn(specificTime)

        val dynamicQRGenerator = DynamicQRGenerator(
            sessionSecretKey = "any_key",
            periodInSeconds = period,
            timeProvider = mockTimeProvider
        )

        // Jika periode 30 detik dan sudah berjalan 5 detik, maka sisa waktunya adalah 25 detik.
        val expectedRemainingSeconds = 25

        // 2. Act
        val actualRemainingSeconds = dynamicQRGenerator.getSecondsUntilNextCode()

        // 3. Assert
        assertEquals(
            "Remaining seconds should be the period minus the current time's remainder",
            expectedRemainingSeconds,
            actualRemainingSeconds
        )
    }
}