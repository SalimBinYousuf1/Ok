package com.example.telephony

object OtpDetector {

    // Regex pattern matching 4-8 digit numeric or alphanumeric codes preceded or succeeded by verification keywords
    private val CODE_REGEX = Regex(
        "(?:code|otp|pin|verification|passcode|secret|login|verify)\\b[^0-9A-Z]{0,10}([0-9]{4,8}|[A-Z0-9]{6})\\b",
        RegexOption.IGNORE_CASE
    )

    private val DIGITS_STANDALONE = Regex("\\b([0-9]{4,8})\\b")

    private val KEYWORDS = listOf(
        "code", "otp", "pin", "verification", "verify", "password", "passcode",
        "security code", "one-time", "two-factor", "2fa", "authentication"
    )

    fun extractOtp(body: String): String? {
        val lower = body.lowercase()
        val hasKeyword = KEYWORDS.any { lower.contains(it) }
        if (!hasKeyword) return null

        val match = CODE_REGEX.find(body)
        if (match != null && match.groupValues.size > 1) {
            val code = match.groupValues[1].trim()
            if (code.isNotEmpty() && !code.all { it == '0' }) {
                return code
            }
        }

        // Fallback: search for 4-8 digit block if keywords are present
        val standalone = DIGITS_STANDALONE.findAll(body)
        for (m in standalone) {
            val candidate = m.groupValues[1]
            // Avoid years like 2024, 2025, 2026 if length is 4 and starts with 19 or 20
            if (candidate.length == 4 && (candidate.startsWith("19") || candidate.startsWith("20"))) {
                continue
            }
            if (candidate.length in 4..8) {
                return candidate
            }
        }

        return null
    }

    fun isOtpMessage(body: String): Boolean {
        return extractOtp(body) != null
    }
}
