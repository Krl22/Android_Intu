package com.intu.taxi.data

enum class PlaceSearchSource { CATALOG, MAPBOX }

/** Mapbox suggestions are transient UI data, separate from the persisted Intu catalog. */
data class PlaceSearchResult(
    val id: String,
    val name: String,
    val subtitle: String,
    val latitude: Double,
    val longitude: Double,
    val source: PlaceSearchSource,
    val openStreetMap: Boolean = false
) {
    init {
        require(id.isNotBlank() && name.isNotBlank())
        require(validPlaceCoordinates(latitude, longitude))
    }
}

fun CatalogPlace.asSearchResult() = PlaceSearchResult(
    "catalog:$id", name, subtitle, latitude, longitude, PlaceSearchSource.CATALOG,
    openStreetMap = source == "OpenStreetMap"
)

/** Keep the catalog's verified pickup point when both sources describe the same nearby place. */
fun mergePlaceSearchResults(catalog: List<CatalogPlace>, addresses: List<PlaceSearchResult>): List<PlaceSearchResult> {
    val local = catalog.filter { it.status == "published" && it.pickupVerified }.map { it.asSearchResult() }
    val merged = local.toMutableList()
    addresses.filter { it.source == PlaceSearchSource.MAPBOX }.forEach { candidate ->
        val duplicate = merged.any { existing ->
            existing.id == candidate.id ||
                (normalizedPlaceText(existing.name) == normalizedPlaceText(candidate.name) &&
                    nearby(existing, candidate))
        }
        if (!duplicate) merged.add(candidate)
    }
    return merged
}

private fun nearby(a: PlaceSearchResult, b: PlaceSearchResult): Boolean {
    val lat = Math.toRadians(b.latitude - a.latitude)
    val lng = Math.toRadians(b.longitude - a.longitude)
    val h = kotlin.math.sin(lat / 2).let { it * it } + kotlin.math.cos(Math.toRadians(a.latitude)) *
        kotlin.math.cos(Math.toRadians(b.latitude)) * kotlin.math.sin(lng / 2).let { it * it }
    return 6_371_000 * 2 * kotlin.math.asin(kotlin.math.sqrt(h.coerceIn(0.0, 1.0))) <= 100
}
