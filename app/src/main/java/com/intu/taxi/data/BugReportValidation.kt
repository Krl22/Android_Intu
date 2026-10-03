package com.intu.taxi.data

/** Matches the server's char_length limits, including text with emoji. */
object BugReportValidation {
    const val MIN_TITLE = 5
    const val MAX_TITLE = 120
    const val MIN_DESCRIPTION = 15
    const val MAX_DESCRIPTION = 4000
    const val MAX_SCREEN = 100

    fun length(value: String): Int = value.trim().let { it.codePointCount(0, it.length) }

    fun titleError(value: String): String? = when {
        length(value) < MIN_TITLE -> "Escribe un título de al menos $MIN_TITLE caracteres."
        length(value) > MAX_TITLE -> "El título puede tener hasta $MAX_TITLE caracteres."
        else -> null
    }

    fun descriptionError(value: String): String? = when {
        length(value) < MIN_DESCRIPTION -> "Describe el problema en al menos $MIN_DESCRIPTION caracteres."
        length(value) > MAX_DESCRIPTION -> "La descripción puede tener hasta $MAX_DESCRIPTION caracteres."
        else -> null
    }

    fun limit(value: String, maximum: Int): String =
        if (value.codePointCount(0, value.length) <= maximum) value else value.substring(0, value.offsetByCodePoints(0, maximum))
}
