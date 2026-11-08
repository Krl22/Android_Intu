package com.intu.taxi.guide

import java.net.URI
import java.net.URLDecoder

/**
 * Destino que llega desde la guía web «Descubre Satipo» (intu://destino?lat=…&lng=…&nombre=…).
 * Cualquier app o página puede abrir ese enlace, así que solo se aceptan puntos dentro del Perú
 * y un nombre corto de una línea. El pasajero siempre confirma el viaje antes de pedirlo.
 */
data class GuideDestination(val latitude: Double, val longitude: Double, val name: String) {
    companion object {
        const val GUIDE_URL = "https://viajaconintu.pages.dev/descubre"
        const val EXTRA_LATITUDE = "com.intu.taxi.guide.LATITUDE"
        const val EXTRA_LONGITUDE = "com.intu.taxi.guide.LONGITUDE"
        const val EXTRA_NAME = "com.intu.taxi.guide.NAME"
        private const val DEFAULT_NAME = "Destino de Descubre Satipo"

        fun fromValues(latitude: Double?, longitude: Double?, name: String?): GuideDestination? {
            if (latitude == null || longitude == null || !latitude.isFinite() || !longitude.isFinite()) return null
            if (latitude !in -18.5..0.1 || longitude !in -81.5..-68.5) return null
            val label = name.orEmpty().replace(Regex("[\\p{Cntrl}\\s]+"), " ").trim().take(120)
            return GuideDestination(latitude, longitude, label.ifEmpty { DEFAULT_NAME })
        }

        /** Lee el enlace intu://destino. Devuelve null ante cualquier dato inválido. */
        fun parse(link: String?): GuideDestination? {
            if (link.isNullOrBlank() || link.length > 2048) return null
            val uri = runCatching { URI(link) }.getOrNull() ?: return null
            if (!uri.scheme.equals("intu", ignoreCase = true) || !uri.host.equals("destino", ignoreCase = true)) return null
            val params = uri.rawQuery.orEmpty().split('&').mapNotNull { pair ->
                val key = pair.substringBefore('=', "")
                if (key.isEmpty()) return@mapNotNull null
                val value = runCatching { URLDecoder.decode(pair.substringAfter('=', ""), "UTF-8") }.getOrNull()
                    ?: return@mapNotNull null
                key to value
            }.toMap()
            return fromValues(params["lat"]?.toDoubleOrNull(), params["lng"]?.toDoubleOrNull(), params["nombre"])
        }
    }
}
