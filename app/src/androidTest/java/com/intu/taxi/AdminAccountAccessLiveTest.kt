package com.intu.taxi

import com.google.firebase.auth.FirebaseAuth
import com.intu.taxi.repositories.AdminRepository
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.Assert.*
import org.junit.Test

/** Solo lectura con la sesión admin existente: no vincula, modifica ni elimina cuentas. */
class AdminAccountAccessLiveTest {
    @Test fun deployedCallableMatchesCurrentFirebaseIdentityAndReturnsAllAdminProfiles() = runBlocking {
        val user = FirebaseAuth.getInstance().currentUser ?: error("Requiere la sesión admin de QA.")
        val repo = AdminRepository()
        assertTrue("Requiere una cuenta admin", repo.isAdmin())
        val profiles = repo.listUsers()
        assertTrue(profiles.isNotEmpty())
        val identities = repo.listAccountAccess(profiles.map { it.id })
        assertEquals(profiles.map { it.id }.toSet(), identities.keys)
        user.reload().await()
        val current = identities.getValue(user.uid)
        assertEquals("found", current.status)
        assertEquals(user.email.orEmpty(), current.email)
        assertEquals(user.phoneNumber.orEmpty(), current.phone)
        assertEquals(user.isEmailVerified, current.emailVerified)
        assertEquals(user.providerData.map { it.providerId }.filter { it != "firebase" }.toSet(), current.providers.map { it.id }.toSet())
    }
}
