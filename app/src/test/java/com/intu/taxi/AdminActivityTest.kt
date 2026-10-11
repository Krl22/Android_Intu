package com.intu.taxi

import com.intu.taxi.push.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class AdminActivityTest {
    @Test fun testerNotificationsUseOnlyTheirOwnValidEventReferences() {
        val id = "tester:94aeb10e-c2ea-4405-93c4-4b03f28224e6"
        assertTrue(AdminActivityMessage.isTesterEvent(id))
        assertFalse(AdminActivityMessage.isTesterEvent("tester:foo"))
        assertFalse(AdminActivityMessage.isTesterEvent("bug_report:94aeb10e-c2ea-4405-93c4-4b03f28224e6"))
        assertFalse(AdminActivityMessage.isTesterEvent(null))
    }
    private val data = mapOf("kind" to "admin_activity", "eventType" to "ride_request",
        "eventId" to "ride_request:qa", "recipientUid" to "admin-qa", "title" to "QA", "body" to "Test")

    @Test fun masterMuteAndIndividualEventsAreIndependent() {
        for (type in AdminActivityType.entries) assertTrue(AdminNotificationPreferences().allows(type))
        for (type in AdminActivityType.entries) assertFalse(AdminNotificationPreferences(enabled=false).allows(type))
        val muted = AdminNotificationPreferences(rideRequests=false)
        assertFalse(muted.allows(AdminActivityType.RIDE_REQUEST))
        assertTrue(muted.allows(AdminActivityType.NEW_USER))
        assertFalse(muted.copy(enabled=false).copy(enabled=true).rideRequests)
    }
    @Test fun malformedEventsAreIgnoredAndCopyIsBounded() {
        assertNotNull(AdminActivityMessage.parse(data))
        for (pair in listOf("eventType" to "login", "kind" to "ride", "eventId" to "", "recipientUid" to "", "title" to ""))
            assertNull(AdminActivityMessage.parse(data + pair))
        assertEquals(240, AdminActivityMessage.parse(data + ("body" to "x".repeat(1000)))!!.body.length)
    }
    @Test fun signedOutWrongAccountMutedAndMidRequestAccountSwitchDoNotDisplay() = runBlocking {
        val message = AdminActivityMessage.parse(data)!!
        assertFalse(canDisplayAdminActivity(message, { null }, { error("Must not fetch") }))
        assertFalse(canDisplayAdminActivity(message, { "other" }, { error("Must not fetch") }))
        assertFalse(canDisplayAdminActivity(message, { "admin-qa" }, { AdminNotificationPreferences(rideRequests=false) }))
        var uid = "admin-qa"
        assertFalse(canDisplayAdminActivity(message, { uid }, { uid="other"; AdminNotificationPreferences() }))
        assertTrue(canDisplayAdminActivity(message, { "admin-qa" }, { AdminNotificationPreferences() }))
    }
    @Test fun permissionFailureCannotAuthorizeDelivery() = runBlocking {
        val failure = runCatching { canDisplayAdminActivity(AdminActivityMessage.parse(data)!!,
            { "admin-qa" }, { throw IllegalStateException("not_admin") }) }
        assertTrue(failure.isFailure)
    }
}
