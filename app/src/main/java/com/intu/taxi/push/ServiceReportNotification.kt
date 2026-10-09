package com.intu.taxi.push

data class ServiceReportNotification(val admin: Boolean) {
    companion object {
        fun parse(data: Map<String, String>): ServiceReportNotification? {
            val status = data["status"]
            if (status !in setOf("service_report", "admin_service_report")) return null
            val id = data["rideId"]?.takeIf { it.startsWith("report-") }?.removePrefix("report-") ?: return null
            if (runCatching { java.util.UUID.fromString(id) }.isFailure) return null
            return ServiceReportNotification(admin = status == "admin_service_report")
        }
    }
}
