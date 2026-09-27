package com.example

import com.example.telephony.OtpDetector
import com.example.telephony.PhoneNumberUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SalimLogicUnitTest {

    @Test
    fun testPhoneNumberNormalization() {
        assertEquals("+15551234567", PhoneNumberUtils.normalizeNumber("+1 (555) 123-4567"))
        assertEquals("5551234567", PhoneNumberUtils.normalizeNumber("(555) 123-4567"))
        assertEquals("+447911123456", PhoneNumberUtils.normalizeNumber("+44 7911 123456"))
        assertEquals("12345", PhoneNumberUtils.normalizeNumber("12-345"))
    }

    @Test
    fun testPhoneNumberDisplayFormatting() {
        assertEquals("(555) 123-4567", PhoneNumberUtils.formatForDisplay("5551234567"))
        assertEquals("+1 (555) 123-4567", PhoneNumberUtils.formatForDisplay("+15551234567"))
    }

    @Test
    fun testGetInitials() {
        assertEquals("JD", PhoneNumberUtils.getInitials("John Doe"))
        assertEquals("S", PhoneNumberUtils.getInitials("Salim"))
        assertEquals("#", PhoneNumberUtils.getInitials("+15551234567"))
    }

    @Test
    fun testSmsSegmentation() {
        val shortAscii = "Hello from Salim!"
        val shortResult = PhoneNumberUtils.calculateSmsSegments(shortAscii)
        assertEquals(1, shortResult.segmentCount)
        assertFalse(shortResult.isUnicode)
        assertEquals(160 - shortAscii.length, shortResult.remainingInSegment)

        val longAscii = "A".repeat(165)
        val longResult = PhoneNumberUtils.calculateSmsSegments(longAscii)
        assertEquals(2, longResult.segmentCount)
        assertFalse(longResult.isUnicode)

        val unicodeText = "Hello 👋"
        val unicodeResult = PhoneNumberUtils.calculateSmsSegments(unicodeText)
        assertTrue(unicodeResult.isUnicode)
    }

    @Test
    fun testOtpDetection() {
        val msg1 = "Your verification code is 482910. Do not share this with anyone."
        assertTrue(OtpDetector.isOtpMessage(msg1))
        assertEquals("482910", OtpDetector.extractOtp(msg1))

        val msg2 = "Use OTP: 9384 to log into your account."
        assertTrue(OtpDetector.isOtpMessage(msg2))
        assertEquals("9384", OtpDetector.extractOtp(msg2))

        val msg3 = "Hey are you coming to dinner tonight?"
        assertFalse(OtpDetector.isOtpMessage(msg3))
        assertNull(OtpDetector.extractOtp(msg3))
    }
}
