package com.intu.taxi

import android.util.Base64
import androidx.test.platform.app.InstrumentationRegistry
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.*
import com.google.firebase.firestore.FirebaseFirestore
import com.intu.taxi.auth.AuthRepository
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*

/** Uses a named DEMO app, never the user's production Firebase session or Supabase. */
class AccountLinkingTest {
    private lateinit var app: FirebaseApp
    private lateinit var auth: FirebaseAuth
    private lateinit var repo: AuthRepository
    private val suffix = System.nanoTime().toString()
    private val http = OkHttpClient()
    @Before fun setup() {
        app = FirebaseApp.initializeApp(InstrumentationRegistry.getInstrumentation().targetContext,
            FirebaseOptions.Builder().setApplicationId("1:123456789:android:0000000000000000")
                .setApiKey("fake-api-key").setProjectId("demo-intu-qa").build(), "qa-$suffix")
        auth = FirebaseAuth.getInstance(app)
        auth.useEmulator("127.0.0.1", 9099)
        repo = AuthRepository(auth, FirebaseFirestore.getInstance(app))
    }
    @After fun teardown() { auth.signOut(); app.delete() }
    private fun google(label: String): AuthCredential {
        fun encode(s: String) = Base64.encodeToString(s.toByteArray(), Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        val email = "$label-$suffix@intu-qa.example"
        val now = System.currentTimeMillis() / 1000
        val payload = JSONObject().put("sub", email).put("email", email).put("email_verified", true)
            .put("iss", "https://accounts.google.com").put("aud", "qa").put("iat", now).put("exp", now + 3600)
        return GoogleAuthProvider.getCredential("${encode("{\"alg\":\"none\",\"typ\":\"JWT\"}")}.${encode(payload.toString())}.", null)
    }
    private fun phone(number: String): PhoneAuthCredential {
        val body = JSONObject().put("phoneNumber", number).toString().toRequestBody("application/json".toMediaType())
        val session = http.newCall(Request.Builder().url("http://127.0.0.1:9099/identitytoolkit.googleapis.com/v1/accounts:sendVerificationCode?key=fake-api-key").post(body).build())
            .execute().use { response -> check(response.isSuccessful); JSONObject(response.body!!.string()).getString("sessionInfo") }
        val codes = http.newCall(Request.Builder().url("http://127.0.0.1:9099/emulator/v1/projects/demo-intu-qa/verificationCodes").build())
            .execute().use { JSONObject(it.body!!.string()).getJSONArray("verificationCodes") }
        val row = (0 until codes.length()).map { codes.getJSONObject(it) }.first { it.getString("sessionInfo") == session }
        return PhoneAuthProvider.getCredential(session, row.getString("code"))
    }
    @Test fun googleThenSmsAndSmsThenGoogleKeepSameUid() = runBlocking {
        val account = google("google-first")
        val original = auth.signInWithCredential(account).await().user!!.uid
        val number = "+519" + suffix.takeLast(8)
        assertEquals(original, repo.linkWithCredential(phone(number), original).uid)
        auth.signOut()
        assertEquals(original, repo.signInWithPhoneCredential(phone(number))!!.uid)
        auth.signOut()
        assertEquals(original, auth.signInWithCredential(account).await().user!!.uid)
        auth.signOut()
        val otherNumber = "+519" + (suffix.takeLast(8).toInt() + 1).toString().padStart(8, '0').takeLast(8)
        val phoneUid = repo.signInWithPhoneCredential(phone(otherNumber))!!.uid
        val otherGoogle = google("phone-first")
        assertEquals(phoneUid, repo.linkGoogleCredential(otherGoogle, phoneUid).uid)
        auth.signOut()
        assertEquals(phoneUid, auth.signInWithCredential(otherGoogle).await().user!!.uid)
    }
    @Test fun collisionsAndStaleSessionNeverChangeTheSignedInAccount() = runBlocking {
        val googleA = google("owner")
        val ownerUid = auth.signInWithCredential(googleA).await().user!!.uid
        val ownerNumber = "+519" + suffix.takeLast(8)
        repo.linkWithCredential(phone(ownerNumber), ownerUid)
        auth.signOut()
        val otherUid = auth.signInWithCredential(google("different")).await().user!!.uid
        val collision = runCatching { repo.linkWithCredential(phone(ownerNumber), otherUid) }.exceptionOrNull()
        assertTrue("Phone collision must be reported by Firebase", collision is FirebaseAuthUserCollisionException)
        assertEquals(otherUid, auth.currentUser!!.uid)
        val stale = runCatching { repo.linkWithCredential(phone(ownerNumber), ownerUid) }.exceptionOrNull()
        assertTrue(stale is IllegalStateException)
        assertEquals(otherUid, auth.currentUser!!.uid)
        auth.signOut()
        val secondNumber = "+519" + (suffix.takeLast(8).toInt() + 2).toString().padStart(8, '0').takeLast(8)
        val phoneUid = repo.signInWithPhoneCredential(phone(secondNumber))!!.uid
        val googleCollision = runCatching { repo.linkGoogleCredential(googleA, phoneUid) }.exceptionOrNull()
        assertTrue("Google collision must be reported by Firebase", googleCollision is FirebaseAuthUserCollisionException)
        assertEquals(phoneUid, auth.currentUser!!.uid)
        val switched = runCatching { repo.signInWithPhoneCredential(phone(ownerNumber)) }.exceptionOrNull()
        assertTrue("Retrying SMS must not silently switch accounts", switched is FirebaseAuthException)
        assertEquals(phoneUid, auth.currentUser!!.uid)
    }

    @Test fun changingVerifiedPhoneKeepsUidAndGoogleAndRejectsAnotherAccountsPhone() = runBlocking {
        val ownerNumber = "+519" + suffix.takeLast(8)
        val ownerUid = repo.signInWithPhoneCredential(phone(ownerNumber))!!.uid
        auth.signOut()
        val account = google("phone-change")
        val uid = auth.signInWithCredential(account).await().user!!.uid
        val firstNumber = "+519" + (suffix.takeLast(8).toInt() + 3).toString().padStart(8, '0').takeLast(8)
        repo.linkWithCredential(phone(firstNumber), uid)
        val newNumber = "+519" + (suffix.takeLast(8).toInt() + 4).toString().padStart(8, '0').takeLast(8)
        assertEquals(uid, repo.linkWithCredential(phone(newNumber), uid).uid)
        assertEquals(newNumber, auth.currentUser!!.phoneNumber)
        assertTrue(auth.currentUser!!.providerData.any { it.providerId == GoogleAuthProvider.PROVIDER_ID })
        val collision = runCatching { repo.linkWithCredential(phone(ownerNumber), uid) }.exceptionOrNull()
        assertTrue(collision is FirebaseAuthUserCollisionException)
        assertEquals(uid, auth.currentUser!!.uid)
        assertEquals(newNumber, auth.currentUser!!.phoneNumber)
        auth.signOut()
        assertEquals(uid, repo.signInWithPhoneCredential(phone(newNumber))!!.uid)
        auth.signOut()
        assertEquals(ownerUid, repo.signInWithPhoneCredential(phone(ownerNumber))!!.uid)
    }
}
