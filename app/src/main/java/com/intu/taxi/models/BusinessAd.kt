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
    val updatedAt: String? = null,
    val city: String = "",
    val offerDetail: String = "",
    val offerPrice: Double? = null,
    val demoPhoto: BusinessPhoto = BusinessPhoto.NONE,
    val menu: List<BusinessMenuItem> = emptyList()
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
        val image = validatedBusinessImage(imageUrl)
        require(city.trim().length <= 50 && offerDetail.trim().length <= 100) { "Revisa la ciudad y el detalle de la oferta." }
        require(offerPrice == null || validBusinessPrice(offerPrice)) { "El precio debe ser de S/ 0.10 a S/ 999.99, con hasta dos decimales." }
        require(menu.size <= 12 && menu.map { it.id }.distinct().size == menu.size) { "Usa hasta 12 productos distintos por menú." }
        return copy(name = name.trim(), title = title.trim(), description = description.trim(),
            address = address.trim(), imageUrl = image, city = city.trim(), offerDetail = offerDetail.trim(), menu = menu.map { it.normalized() })
    }
}

enum class BusinessPhoto(val code: String, val label: String) {
    NONE("", "Sin foto"), CHICKEN("chicken", "Pollo a la brasa"), JUANE("juane", "Juane"), COFFEE("coffee", "Café + sánguche"), CHAUFA("chaufa", "Chaufa de pollo")
}

internal fun validatedBusinessImage(value: String): String = value.trim().also { image ->
    require(image.isEmpty() || (image.length <= 1000 && runCatching {
        val uri = URI(image)
        uri.scheme == "https" && !uri.host.isNullOrBlank() && uri.userInfo == null
    }.getOrDefault(false))) { "La imagen debe usar una dirección HTTPS válida." }
}

internal fun validBusinessPrice(price: Double): Boolean = price.isFinite() && price in 0.10..999.99 &&
    java.math.BigDecimal.valueOf(price).stripTrailingZeros().scale() <= 2

data class BusinessMenuItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String = "",
    val description: String = "",
    val price: Double = 0.0,
    val imageUrl: String = "",
    val demoPhoto: BusinessPhoto = BusinessPhoto.NONE,
    val available: Boolean = true
) {
    fun normalized(): BusinessMenuItem {
        require(runCatching { java.util.UUID.fromString(id).toString() == id }.getOrDefault(false)) { "Producto inválido." }
        require(name.trim().length in 2..80 && description.trim().length <= 160) { "Ingresa un nombre de producto y una descripción breve." }
        require(validBusinessPrice(price)) { "El precio debe ser de S/ 0.10 a S/ 999.99, con hasta dos decimales." }
        return copy(name = name.trim(), description = description.trim(), imageUrl = validatedBusinessImage(imageUrl))
    }
}

/** Display snapshot. Only item IDs and quantities are submitted; the server owns prices. */
data class BusinessOrderItem(val itemId: String, val name: String, val unitPrice: Double, val quantity: Int) {
    val total: java.math.BigDecimal get() = java.math.BigDecimal.valueOf(unitPrice) * quantity.toBigDecimal()
}

fun businessCart(menu: List<BusinessMenuItem>, quantities: Map<String, Int>): List<BusinessOrderItem> {
    require(quantities.values.all { it in 0..10 } && quantities.values.sum() <= 20) { "Máximo 10 unidades por producto y 20 por pedido demo." }
    require(quantities.filterValues { it > 0 }.keys.all { id -> menu.any { it.id == id && it.available } }) { "Un producto ya no está disponible." }
    return menu.filter { (quantities[it.id] ?: 0) > 0 }.map { BusinessOrderItem(it.id, it.name, it.price, quantities.getValue(it.id)) }
}

fun List<BusinessOrderItem>.productsTotal(): Double = fold(java.math.BigDecimal.ZERO) { sum, item -> sum + item.total }.toDouble()

data class BusinessFeed(val enabled: Boolean = false, val ads: List<BusinessAd> = emptyList())
data class BusinessTestCourier(val id: String, val name: String, val plate: String, val selected: Boolean)
data class AdminBusinessState(val enabled: Boolean, val ads: List<BusinessAd>, val couriers: List<BusinessTestCourier>)
