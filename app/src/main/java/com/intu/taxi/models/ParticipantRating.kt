package com.intu.taxi.models

import org.json.JSONObject
import java.util.Locale

/** Promedio de estrellas del otro participante, copiado en el viaje solo si un admin lo activó. */
data class ParticipantRating(val average: Double, val count: Int) {
    /** Con pocas calificaciones el promedio engaña: se muestra "Nuevo". */
    val isNew: Boolean get() = count < MIN_COUNT
    val label: String get() = if (isNew) "Nuevo" else String.format(Locale.US, "%.1f", average)

    companion object {
        const val MIN_COUNT = 3

        /** Lee `<prefix>_rating` y `<prefix>_rating_count`; null si el servidor no los envió. */
        fun from(json: JSONObject, prefix: String): ParticipantRating? {
            val countKey = "${prefix}_rating_count"
            if (!json.has(countKey) || json.isNull(countKey)) return null
            val ratingKey = "${prefix}_rating"
            val average = if (json.isNull(ratingKey)) 0.0 else json.optDouble(ratingKey, 0.0)
            return ParticipantRating(average, json.optInt(countKey, 0))
        }
    }
}
