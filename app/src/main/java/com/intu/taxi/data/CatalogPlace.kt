package com.intu.taxi.data

import java.text.Normalizer
import java.util.Locale
import kotlin.math.*

data class CatalogPlace(
    val id: String,
    val name: String,
    val aliases: List<String> = emptyList(),
    val category: String = "place",
    val locality: String = "Satipo",
    val address: String = "",
    val latitude: Double,
    val longitude: Double,
    val status: String = "draft",
    val pickupVerified: Boolean = false,
    val source: String = "manual",
    val sourceUrl: String = "",
    val attribution: String = "",
    val license: String = "",
    val coordinateKind: String = "manual",
    val updatedAt: String = ""
) {
    init {
        require(id.isNotBlank() && name.isNotBlank())
        require(validPlaceCoordinates(latitude, longitude))
    }
    val subtitle: String get() = listOf(categoryLabel(category), address.ifBlank { locality }).joinToString(" · ")
}

fun validPlaceCoordinates(latitude: Double, longitude: Double): Boolean =
    latitude.isFinite() && longitude.isFinite() && latitude in -90.0..90.0 && longitude in -180.0..180.0

fun normalizedPlaceText(text: String): String = Normalizer.normalize(text, Normalizer.Form.NFD)
    .replace(Regex("\\p{M}+"), "").lowercase(Locale.ROOT)
    .replace(Regex("[^\\p{L}\\p{N}]+"), " ").trim()

fun categoryLabel(category: String): String = when (category) {
    "restaurant" -> "Restaurante"
    "cafe" -> "Cafetería"
    "fast_food" -> "Comida rápida"
    "ice_cream" -> "Heladería"
    "hospital" -> "Hospital"
    "clinic", "doctors" -> "Clínica"
    "pharmacy" -> "Farmacia"
    "mall" -> "Centro comercial"
    "marketplace" -> "Mercado"
    "square" -> "Plaza"
    "park" -> "Parque"
    "stadium" -> "Estadio"
    "pitch", "sports_centre" -> "Cancha deportiva"
    "school" -> "Centro educativo"
    "college" -> "Instituto"
    "university" -> "Universidad"
    "kindergarten" -> "Jardín de infancia"
    "townhall" -> "Municipalidad"
    "courthouse" -> "Juzgado"
    "parking" -> "Estacionamiento"
    "convenience" -> "Tienda"
    "mortuary" -> "Funeraria"
    "party" -> "Organización política"
    "fuel" -> "Grifo"
    "hotel" -> "Hotel"
    "bus_station" -> "Terminal"
    "place_of_worship" -> "Iglesia"
    "fire_station" -> "Bomberos"
    "bank", "atm" -> "Banco / cajero"
    "police" -> "Comisaría"
    else -> "Lugar"
}

/** Entirely local. Text relevance precedes distance; missing GPS never hides results. */
fun searchCatalog(
    query: String, places: List<CatalogPlace>, latitude: Double? = null, longitude: Double? = null,
    limit: Int = 6
): List<CatalogPlace> {
    val needle = normalizedPlaceText(query)
    if (needle.length < 2 || limit <= 0) return emptyList()
    val tokens = needle.split(' ')
    val located = latitude != null && longitude != null && validPlaceCoordinates(latitude, longitude)
    return places.asSequence().filter { it.status == "published" && it.pickupVerified }
        .mapNotNull { place ->
            val name = normalizedPlaceText(place.name)
            val aliases = place.aliases.map(::normalizedPlaceText)
            val searchable = (listOf(name, normalizedPlaceText(place.address), normalizedPlaceText(place.locality),
                normalizedPlaceText(categoryLabel(place.category))) + aliases).joinToString(" ")
            if (!tokens.all { it in searchable }) return@mapNotNull null
            val rank = when {
                name == needle -> 0
                needle in aliases -> 1
                name.startsWith(needle) -> 2
                aliases.any { it.startsWith(needle) } -> 3
                tokens.all { token -> name.split(' ').any { it.startsWith(token) } } -> 4
                else -> 5
            }
            val distance = if (located) {
                val latDelta = Math.toRadians(place.latitude - latitude!!)
                val lngDelta = Math.toRadians(place.longitude - longitude!!)
                // Great-circle ordering, clamped against floating-point rounding.
                val a = sin(latDelta / 2).pow(2) + cos(Math.toRadians(latitude)) *
                    cos(Math.toRadians(place.latitude)) * sin(lngDelta / 2).pow(2)
                2 * asin(sqrt(a.coerceIn(0.0, 1.0)))
            } else 0.0
            Triple(place, rank, distance)
        }.sortedWith(compareBy<Triple<CatalogPlace, Int, Double>> { it.second }
            .thenBy { it.third }.thenBy { normalizedPlaceText(it.first.name) }.thenBy { it.first.id })
        .take(limit).map { it.first }.toList()
}
