package com.intu.taxi.repositories

import com.intu.taxi.data.SupabaseApi
import com.intu.taxi.data.str
import org.json.JSONObject

data class TesterRequest(val id: String, val email: String, val status: String, val createdAt: String)

class TesterRequestRepository {
    suspend fun list(): List<TesterRequest> {
        val rows = SupabaseApi.rpc("admin_tester_requests").getJSONArray("requests")
        return (0 until rows.length()).map { index ->
            val row = rows.getJSONObject(index)
            TesterRequest(row.str("id"), row.str("email"), row.str("status"), row.str("created_at"))
        }
    }

    suspend fun review(id: String, status: String) {
        SupabaseApi.rpc("admin_review_tester_request", JSONObject().put("p_id", id).put("p_status", status))
    }
}
