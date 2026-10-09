package com.intu.taxi.repositories

import com.intu.taxi.data.SupabaseApi
import com.intu.taxi.data.str
import com.intu.taxi.models.ServiceReport
import com.intu.taxi.models.ServiceReportMessage
import org.json.JSONArray
import org.json.JSONObject

class ServiceReportRepository {
    suspend fun create(requestId: String, rideId: String, category: String, description: String): ServiceReport = parse(
        SupabaseApi.rpc("create_service_report", JSONObject().put("p_request_id", requestId).put("p_ride_id", rideId)
            .put("p_category", category).put("p_description", description.trim())))

    suspend fun list(admin: Boolean = false, rideId: String? = null): List<ServiceReport> {
        val rows = JSONArray(SupabaseApi.request("POST", "rpc/" + if (admin) "admin_service_reports" else "my_service_reports",
            if (admin) JSONObject() else JSONObject().put("p_ride_id", rideId ?: JSONObject.NULL)))
        return (0 until rows.length()).map { parse(rows.getJSONObject(it)) }
    }
    suspend fun respond(id: String, state: String): ServiceReport = parse(SupabaseApi.rpc("respond_lost_item",
        JSONObject().put("p_report_id", id).put("p_state", state)))
    suspend fun message(id: String, text: String): ServiceReport = parse(SupabaseApi.rpc("send_service_report_message",
        JSONObject().put("p_report_id", id).put("p_body", text.trim())))
    suspend fun review(id: String, status: String, response: String, note: String): ServiceReport = parse(
        SupabaseApi.rpc("admin_review_service_report", JSONObject().put("p_report_id", id).put("p_status", status)
            .put("p_response", response).put("p_note", note)))

    private fun parse(r: JSONObject): ServiceReport {
        val messages = r.optJSONArray("messages") ?: JSONArray()
        return ServiceReport(r.str("id"), r.str("ride_id"), r.str("category"), r.str("description"), r.str("status"),
            r.str("lost_state").ifBlank { null }, r.str("admin_response"), r.str("admin_note"), r.str("reporter_role"),
            r.str("created_at"), r.str("updated_at"), r.optBoolean("is_reporter"), r.optBoolean("can_check_item"),
            r.str("reporter_name"), r.str("reported_name"), r.str("origin_address"), r.str("destination_address"),
            r.str("vehicle_plate"), r.str("service_kind"), (0 until messages.length()).map { i -> messages.getJSONObject(i).let {
                ServiceReportMessage(it.str("id"), it.str("body"), it.str("role"), it.optBoolean("mine"), it.str("created_at"))
            } })
    }
}
