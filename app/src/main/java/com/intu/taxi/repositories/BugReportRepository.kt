package com.intu.taxi.repositories

import android.os.Build
import com.intu.taxi.BuildConfig
import com.intu.taxi.data.SupabaseApi
import com.intu.taxi.data.str
import com.intu.taxi.data.BugReportValidation
import org.json.JSONObject

data class BugReport(val id: String, val title: String, val description: String,
    val screen: String, val appVersion: String, val deviceInfo: String,
    val status: String, val createdAt: String, val reporterName: String)

class BugReportRepository {
    suspend fun submit(title: String, description: String, screen: String) {
        BugReportValidation.titleError(title)?.let { throw IllegalArgumentException(it) }
        BugReportValidation.descriptionError(description)?.let { throw IllegalArgumentException(it) }
        SupabaseApi.request("POST", "bug_reports", JSONObject()
            .put("title", title.trim()).put("description", description.trim())
            .put("screen", BugReportValidation.limit(screen.trim(), BugReportValidation.MAX_SCREEN))
            .put("app_version", "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
            .put("device_info", "${Build.MANUFACTURER} ${Build.MODEL}, Android ${Build.VERSION.RELEASE}".take(200)),
            "return=minimal")
    }

    suspend fun list(): List<BugReport> {
        val rows = SupabaseApi.rpcRows("admin_list_bug_reports")
        return (0 until rows.length()).map { i ->
            val r = rows.getJSONObject(i)
            BugReport(r.str("id"), r.str("title"), r.str("description"), r.str("screen"),
                r.str("app_version"), r.str("device_info"), r.str("status"), r.str("created_at"), r.str("reporter_name"))
        }
    }

    suspend fun setStatus(id: String, status: String) {
        SupabaseApi.rpc("admin_set_bug_report_status", JSONObject().put("p_report_id", id).put("p_status", status))
    }
}
