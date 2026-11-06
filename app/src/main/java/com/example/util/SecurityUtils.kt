package com.example.util

import java.security.MessageDigest
import java.security.SecureRandom

object SecurityUtils {
    private val secureRandom = SecureRandom()

    fun generateSixDigitOtp(): String {
        val code = 100000 + secureRandom.nextInt(900000)
        return code.toString()
    }

    fun hashPassword(rawPassword: String, mobileSalt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val input = "${mobileSalt.trim()}:${rawPassword}"
        val hashBytes = digest.digest(input.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    fun isValidUpiId(upi: String): Boolean {
        val trimmed = upi.trim()
        return trimmed.length in 5..64 &&
            trimmed.matches(Regex("^[a-zA-Z0-9._\\-]{2,48}@[a-zA-Z0-9]{2,20}$"))
    }
}
