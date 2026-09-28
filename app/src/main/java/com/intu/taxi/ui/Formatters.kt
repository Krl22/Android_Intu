package com.intu.taxi.ui

import java.util.Locale

/** Monto en soles peruanos con punto decimal, p. ej. "S/ 9.10". */
fun formatSoles(amount: Double): String = "S/ " + String.format(Locale.US, "%.2f", amount)
