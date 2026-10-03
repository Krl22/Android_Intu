package com.intu.taxi

import com.intu.taxi.data.BugReportValidation
import org.junit.Assert.*
import org.junit.Test

class BugReportValidationTest {
    @Test fun emojiCharacterCountsAgreeWithPostgresAndTruncationKeepsWholeCharacters() {
        // Three emoji occupy six UTF-16 units, but are still below the server's five-character minimum.
        assertNotNull(BugReportValidation.titleError("🚕🚕🚕"))
        assertNull(BugReportValidation.titleError("🚕🚕🚕🚕🚕"))
        assertNotNull(BugReportValidation.descriptionError("🚕".repeat(14)))
        assertNull(BugReportValidation.descriptionError("🚕".repeat(15)))
        val limited = BugReportValidation.limit("🚕".repeat(121), 120)
        assertEquals("🚕".repeat(120), limited)
        assertEquals(120, BugReportValidation.length(limited))
        assertNull(BugReportValidation.titleError(limited))
    }
}
