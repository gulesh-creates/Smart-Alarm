package com.zenith.study.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UserNameValidationTest {

    private fun sanitizeUserName(input: String): String {
        return input.trim().take(30)
    }

    private fun formatGreeting(name: String): String {
        val trimmed = name.trim()
        return if (trimmed.isNotBlank()) "Hi $trimmed 👋" else "Welcome 👋"
    }

    @Test
    fun whitespaceIsTrimmed() {
        val raw = "   Alex Rivera   "
        val sanitized = sanitizeUserName(raw)
        assertEquals("Alex Rivera", sanitized)
        assertEquals("Hi Alex Rivera 👋", formatGreeting(sanitized))
    }

    @Test
    fun blankOrEmptyName_returnsNeutralGreeting() {
        assertEquals("Welcome 👋", formatGreeting(""))
        assertEquals("Welcome 👋", formatGreeting("   "))
    }

    @Test
    fun unicodeAndInternationalNames_areSupported() {
        val hindiName = "गिलेश पटेल"
        assertEquals(hindiName, sanitizeUserName(hindiName))
        assertEquals("Hi $hindiName 👋", formatGreeting(hindiName))

        val accentedName = "José-María"
        assertEquals(accentedName, sanitizeUserName(accentedName))
        assertEquals("Hi $accentedName 👋", formatGreeting(accentedName))
    }

    @Test
    fun nameExceeding30Chars_isCleanlyTruncated() {
        val longName = "A".repeat(45)
        val sanitized = sanitizeUserName(longName)
        assertEquals(30, sanitized.length)
        assertEquals("A".repeat(30), sanitized)
    }
}

