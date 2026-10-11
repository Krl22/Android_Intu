package com.intu.taxi.driver

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import java.util.Locale

/** Apps de mapas con las que el conductor puede navegar fuera de Intu (sin costo para Intu). */
enum class NavigationApp(val label: String, val packageName: String) {
    GOOGLE_MAPS("Google Maps", "com.google.android.apps.maps"),
    WAZE("Waze", "com.waze")
}

/**
 * Abre Google Maps o Waze con la navegación paso a paso hasta el recojo o el destino, como Uber.
 * Intu sigue en segundo plano (el servicio del conductor mantiene la ubicación y los avisos).
 */
object ExternalNavigation {
    private const val PREFS = "intu_driver"
    private const val KEY_APP = "navigation_app"

    // Siempre con punto decimal: con el idioma del teléfono en español saldría "-8,3791" y el mapa no lo entiende
    private fun coordinates(latitude: Double, longitude: Double) =
        String.format(Locale.US, "%.6f,%.6f", latitude, longitude)

    fun url(app: NavigationApp, latitude: Double, longitude: Double): String = when (app) {
        NavigationApp.GOOGLE_MAPS -> "google.navigation:q=${coordinates(latitude, longitude)}&mode=d"
        NavigationApp.WAZE -> "https://waze.com/ul?ll=${coordinates(latitude, longitude)}&navigate=yes"
    }

    /** Sin Google Maps ni Waze: ruta en Google Maps desde el navegador. */
    fun webUrl(latitude: Double, longitude: Double): String =
        "https://www.google.com/maps/dir/?api=1&destination=${coordinates(latitude, longitude)}&travelmode=driving"

    fun installedApps(context: Context): List<NavigationApp> = NavigationApp.entries.filter { app ->
        try {
            context.packageManager.getPackageInfo(app.packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun preferredApp(context: Context): NavigationApp? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_APP, null)
            ?.let { saved -> NavigationApp.entries.firstOrNull { it.name == saved } }

    fun rememberApp(context: Context, app: NavigationApp) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_APP, app.name).apply()
    }

    /** Abre la app elegida (o el navegador si es null). Devuelve false si no se pudo abrir nada. */
    fun launch(context: Context, app: NavigationApp?, latitude: Double, longitude: Double): Boolean {
        val intent = if (app == null) Intent(Intent.ACTION_VIEW, Uri.parse(webUrl(latitude, longitude)))
            else Intent(Intent.ACTION_VIEW, Uri.parse(url(app, latitude, longitude))).setPackage(app.packageName)
        return try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        } catch (e: ActivityNotFoundException) {
            false
        }
    }
}
