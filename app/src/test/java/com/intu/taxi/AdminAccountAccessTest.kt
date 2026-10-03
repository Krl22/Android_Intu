package com.intu.taxi

import com.intu.taxi.repositories.AdminAccountAccess
import org.junit.Assert.*
import org.junit.Test

class AdminAccountAccessTest {
    @Test fun providersAreJoinedByUidAndNotInferredFromEmailOrPhone() {
        val accounts = AdminAccountAccess.fromCallable(mapOf("accounts" to listOf(
            mapOf("uid" to "google", "status" to "found", "email" to "account@example.com", "emailVerified" to true,
                "providers" to listOf(mapOf("id" to "google.com", "email" to "google@example.com"))),
            mapOf("uid" to "phone", "status" to "found", "phone" to "+51987654321", "email" to "contact@example.com",
                "providers" to listOf(mapOf("id" to "phone"))),
            mapOf("uid" to "missing", "status" to "missing")
        )))
        assertEquals("google@example.com", accounts.getValue("google").google?.email)
        assertFalse(accounts.getValue("google").phoneLinked)
        assertTrue(accounts.getValue("phone").phoneLinked)
        assertNull(accounts.getValue("phone").google)
        assertFalse(accounts.getValue("phone").emailVerified)
        assertEquals("missing", accounts.getValue("missing").status)
    }

    @Test fun linkedProvidersBelongToTheSameAccount() {
        val access = AdminAccountAccess.fromCallable(mapOf("accounts" to listOf(mapOf("uid" to "same", "status" to "found",
            "disabled" to true, "providers" to listOf(mapOf("id" to "phone"), mapOf("id" to "google.com")))))).getValue("same")
        assertTrue(access.phoneLinked)
        assertNotNull(access.google)
        assertTrue(access.disabled)
    }

    @Test fun malformedResponseDoesNotMasqueradeAsUnlinked() {
        for (data in listOf(null, emptyMap<String, Any>(), mapOf("accounts" to listOf(mapOf("uid" to "user", "status" to "found"))))) {
            assertTrue(runCatching { AdminAccountAccess.fromCallable(data) }.isFailure)
        }
    }
}
