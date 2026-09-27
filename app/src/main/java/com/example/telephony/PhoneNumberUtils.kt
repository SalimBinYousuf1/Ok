package com.example.telephony

object PhoneNumberUtils {

    fun normalizeNumber(number: String): String {
        val trimmed = number.trim()
        if (trimmed.isEmpty()) return ""
        val hasPlus = trimmed.startsWith("+")
        val digitsOnly = trimmed.filter { it.isDigit() }
        return if (hasPlus) "+$digitsOnly" else digitsOnly
    }

    fun formatForDisplay(number: String): String {
        val normalized = normalizeNumber(number)
        if (normalized.length == 10 && !normalized.startsWith("+")) {
            // Standard US 10-digit: (XXX) XXX-XXXX
            val area = normalized.substring(0, 3)
            val prefix = normalized.substring(3, 6)
            val line = normalized.substring(6, 10)
            return "($area) $prefix-$line"
        } else if (normalized.length == 12 && normalized.startsWith("+1")) {
            val area = normalized.substring(2, 5)
            val prefix = normalized.substring(5, 8)
            val line = normalized.substring(8, 12)
            return "+1 ($area) $prefix-$line"
        } else if (normalized.length == 11 && normalized.startsWith("1")) {
            // US 11-digit starting with 1
            val area = normalized.substring(1, 4)
            val prefix = normalized.substring(4, 7)
            val line = normalized.substring(7, 11)
            return "+1 ($area) $prefix-$line"
        }
        return number.trim()
    }

    fun getInitials(nameOrNumber: String): String {
        val trimmed = nameOrNumber.trim()
        if (trimmed.isEmpty()) return "?"
        val words = trimmed.split("\\s+".toRegex()).filter { it.isNotEmpty() }
        return when {
            words.size >= 2 -> "${words[0].first().uppercaseChar()}${words[1].first().uppercaseChar()}"
            words.isNotEmpty() && words[0].first().isLetter() -> words[0].first().uppercaseChar().toString()
            else -> "#"
        }
    }

    fun calculateSmsSegments(text: String): SmsSegmentInfo {
        val length = text.length
        val is7Bit = text.all { it.code in 0..127 }
        val maxSingle = if (is7Bit) 160 else 70
        val maxMultipart = if (is7Bit) 153 else 67

        return if (length <= maxSingle) {
            SmsSegmentInfo(
                charCount = length,
                segmentCount = if (length == 0) 0 else 1,
                remainingInSegment = maxSingle - length,
                isUnicode = !is7Bit
            )
        } else {
            val segments = (length + maxMultipart - 1) / maxMultipart
            val usedInLast = length % maxMultipart
            val remaining = if (usedInLast == 0) 0 else maxMultipart - usedInLast
            SmsSegmentInfo(
                charCount = length,
                segmentCount = segments,
                remainingInSegment = remaining,
                isUnicode = !is7Bit
            )
        }
    }
}

data class SmsSegmentInfo(
    val charCount: Int,
    val segmentCount: Int,
    val remainingInSegment: Int,
    val isUnicode: Boolean
)
