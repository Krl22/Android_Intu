package com.intu.taxi

import com.intu.taxi.models.ServiceReportCategory as Reason
import com.intu.taxi.push.ServiceReportNotification
import org.junit.Assert.*
import org.junit.Test

class ServiceReportTest {
    @Test fun reasonsFollowParticipantRoleAndServiceType() {
        assertTrue(Reason.LOST_ITEM in Reason.options(false, false, true))
        assertFalse(Reason.LOST_ITEM in Reason.options(false, false, false))
        assertFalse(Reason.LOST_ITEM in Reason.options(true, false, true))
        assertFalse(Reason.LOST_ITEM in Reason.options(false, true, true))
        assertFalse(Reason.DANGEROUS_DRIVING in Reason.options(true, false, true))
        assertTrue(Reason.MISSING_PACKAGE in Reason.options(true, true, true))
        assertFalse(Reason.MISSING_PACKAGE in Reason.options(false, false, true))
    }
    @Test fun descriptionsRejectEmptyOrTooShortDetailsAndOverlongPayloads() {
        assertFalse(Reason.validDescription("          "))
        assertFalse(Reason.validDescription("123456789"))
        assertTrue(Reason.validDescription("  1234567890  "))
        assertTrue(Reason.validDescription("a".repeat(2000)))
        assertFalse(Reason.validDescription("a".repeat(2001)))
    }
    @Test fun reportNotificationsAreDistinctFromRideEventsAndValidateTheReference() {
        val reference = "report-00000000-0000-0000-0000-000000000001"
        assertEquals(ServiceReportNotification(false), ServiceReportNotification.parse(mapOf("status" to "service_report", "rideId" to reference)))
        assertEquals(ServiceReportNotification(true), ServiceReportNotification.parse(mapOf("status" to "admin_service_report", "rideId" to reference)))
        assertNull(ServiceReportNotification.parse(mapOf("status" to "chat", "rideId" to reference)))
        assertNull(ServiceReportNotification.parse(mapOf("status" to "service_report", "rideId" to "invalid")))
    }
}
