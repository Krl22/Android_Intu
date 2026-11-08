package com.intu.taxi.guide

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.intu.taxi.MainActivity

/**
 * Puente invisible para los enlaces intu://destino. Valida el destino y lo entrega a la
 * MainActivity existente (o la abre), sin exponer MainActivity a datos de enlaces externos
 * ni crear una segunda copia de la app encima de la que ya está abierta.
 */
class GuideLinkActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val destination = GuideDestination.parse(intent?.dataString)
        startActivity(Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            destination?.let {
                putExtra(GuideDestination.EXTRA_LATITUDE, it.latitude)
                putExtra(GuideDestination.EXTRA_LONGITUDE, it.longitude)
                putExtra(GuideDestination.EXTRA_NAME, it.name)
            }
        })
        finish()
    }
}
