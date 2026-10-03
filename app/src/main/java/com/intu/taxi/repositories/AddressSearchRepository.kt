package com.intu.taxi.repositories

import com.intu.taxi.data.PlaceSearchResult
import com.intu.taxi.data.PlaceSearchSource
import com.intu.taxi.data.validPlaceCoordinates
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class AddressSearchRepository(
    private val token: String,
    private val http: OkHttpClient = OkHttpClient.Builder().callTimeout(10, TimeUnit.SECONDS).build()
) {
    suspend fun search(query: String, latitude: Double? = null, longitude: Double? = null): List<PlaceSearchResult> {
        val request = Request.Builder().url(buildAddressSearchUrl(token, query, latitude, longitude)).build()
        val body = suspendCancellableCoroutine<String> { continuation ->
            val call = http.newCall(request)
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    continuation.resumeWith(Result.failure(e))
                }
                override fun onResponse(call: Call, response: Response) {
                    val result = runCatching {
                        response.use {
                            if (!it.isSuccessful) throw IOException("No se pudieron buscar las direcciones (${it.code}).")
                            it.body?.string() ?: throw IOException("La búsqueda no devolvió datos.")
                        }
                    }
                    continuation.resumeWith(result)
                }
            })
        }
        return withContext(Dispatchers.Default) { parseAddressSearchResults(body) }
    }
}

internal fun buildAddressSearchUrl(token: String, query: String, latitude: Double?, longitude: Double?): HttpUrl {
    val text = query.replace(';', ' ').trim().replace(Regex("\\s+"), " ")
    require(text.length in 2..256 && text.split(' ').size <= 20) { "Usa una dirección más corta para buscar." }
    val located = latitude != null && longitude != null && validPlaceCoordinates(latitude, longitude)
    val lat = if (located) latitude!! else -11.2521
    val lng = if (located) longitude!! else -74.6382
    // Preserve the previous autocomplete's 15-mile service area, only for external suggestions.
    val deltaLat = 15.0 / 69.0
    val deltaLng = 15.0 / (69.0 * kotlin.math.cos(Math.toRadians(lat)).coerceAtLeast(0.01))
    val bbox = "${(lng - deltaLng).coerceAtLeast(-180.0)},${(lat - deltaLat).coerceAtLeast(-90.0)}," +
        "${(lng + deltaLng).coerceAtMost(180.0)},${(lat + deltaLat).coerceAtMost(90.0)}"
    return HttpUrl.Builder().scheme("https").host("api.mapbox.com")
        .addPathSegments("geocoding/v5/mapbox.places").addPathSegment("$text.json")
        .addQueryParameter("access_token", token).addQueryParameter("language", "es")
        .addQueryParameter("autocomplete", "true").addQueryParameter("limit", "5")
        .addQueryParameter("types", "address,place,locality,neighborhood")
        .addQueryParameter("country", "pe").addQueryParameter("proximity", "$lng,$lat")
        .addQueryParameter("bbox", bbox).build()
}

internal fun parseAddressSearchResults(body: String): List<PlaceSearchResult> {
    val features = JSONObject(body).optJSONArray("features") ?: return emptyList()
    return (0 until features.length()).mapNotNull feature@{ index ->
        val feature = features.optJSONObject(index) ?: return@feature null
        val center = feature.optJSONArray("center") ?: feature.optJSONObject("geometry")?.optJSONArray("coordinates")
            ?: return@feature null
        val lng = center.optDouble(0, Double.NaN)
        val lat = center.optDouble(1, Double.NaN)
        if (!validPlaceCoordinates(lat, lng)) return@feature null
        val text = feature.optString("text").trim()
        if (text.isBlank()) return@feature null
        val number = feature.optString("address").trim()
        val name = listOf(text, number).filter { it.isNotBlank() }.joinToString(" ")
        val context = feature.optJSONArray("context")
        val subtitle = (0 until (context?.length() ?: 0)).mapNotNull { i ->
            val part = context!!.optJSONObject(i) ?: return@mapNotNull null
            val id = part.optString("id")
            part.optString("text").takeIf { it.isNotBlank() &&
                (id.startsWith("place.") || id.startsWith("locality.") || id.startsWith("neighborhood.")) }
        }.distinct().joinToString(", ")
        val id = feature.optString("id").ifBlank { "$name:$lng:$lat" }
        PlaceSearchResult("mapbox:$id", name, subtitle, lat, lng, PlaceSearchSource.MAPBOX)
    }.distinctBy { it.id }.take(5)
}
