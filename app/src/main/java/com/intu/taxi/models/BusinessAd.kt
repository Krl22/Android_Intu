package com.intu.taxi.models

import com.mapbox.geojson.Point
import java.net.URI

enum class BusinessCategory(val code: String, val label: String) {
    FOOD("food", "Comida"), SHOP("shop", "Tienda"), PHARMACY("pharmacy", "Farmacia")
}

data class BusinessAd(
    val id: String? = null,
    val name: String = "",
    val category: BusinessCategory = BusinessCategory.FOOD,
    val title: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val address: String = "",
    val latitude: Double = -11.2521,
    val longitude: Double = -74.6382,
    val published: Boolean = false,
    val sortOrder: Int = 0,
    val updatedAt: String? = null
) {
    val point: Point get() = Point.fromLngLat(longitude, latitude)
    fun normalized(): BusinessAd {
        require(name.trim().length in 2..100) { "Ingresa el nombre del negocio (2 a 100 caracteres)." }
        require(title.trim().length in 3..100) { "Ingresa el título del anuncio (3 a 100 caracteres)." }
        require(description.trim().length in 3..500) { "Describe el anuncio (3 a 500 caracteres)." }
        require(address.trim().length in 3..200) { "Ingresa la dirección de recojo." }
        require(latitude.isFinite() && latitude in -90.0..90.0 && longitude.isFinite() && longitude in -180.0..180.0) {
            "Selecciona un punto de recojo válido."
        }
        require(sortOrder in 0..999) { "El orden debe estar entre 0 y 999." }
        val image = imageUrl.trim()
        require(image.isEmpty() || (image.length <= 1000 && runCatching {
            val uri = URI(image)
            uri.scheme == "https" && !uri.host.isNullOrBlank() && uri.userInfo == null
        }.getOrDefault(false))) { "La imagen debe usar una dirección HTTPS válida." }
        return copy(name = name.trim(), title = title.trim(), description = description.trim(),
            address = address.trim(), imageUrl = image)
    }
}

data class BusinessFeed(val enabled: Boolean = false, val ads: List<BusinessAd> = emptyList())
data class BusinessTestCourier(val id: String, val name: String, val plate: String, val selected: Boolean)
data class AdminBusinessState(val enabled: Boolean, val ads: List<BusinessAd>, val couriers: List<BusinessTestCourier>)
