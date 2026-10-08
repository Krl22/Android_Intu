package com.intu.taxi

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory

/**
 * Firebase App Check: Google confirma que las llamadas a Firebase (login, fotos, funciones) vienen de
 * Intu instalada desde Play. Las compilaciones de prueba usan el proveedor de depuración: su token
 * aparece en logcat ("DebugAppCheckProvider") y se registra en Firebase → App Check → tokens de depuración.
 */
class IntuApp : Application() {
    override fun onCreate() {
        super.onCreate()
        com.intu.taxi.updates.AppUpdateWorker.schedule(this)
        FirebaseApp.initializeApp(this)
        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
            if (BuildConfig.DEBUG) DebugAppCheckProviderFactory.getInstance()
            else PlayIntegrityAppCheckProviderFactory.getInstance()
        )
    }
}
