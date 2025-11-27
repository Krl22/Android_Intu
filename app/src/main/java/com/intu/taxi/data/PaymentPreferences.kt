package com.intu.taxi.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first

// Extension to create DataStore
val Context.paymentDataStore: DataStore<Preferences> by preferencesDataStore(name = "payment_preferences")

class PaymentPreferences(private val context: Context) {
    
    companion object {
        private val PAYMENT_METHOD_KEY = stringPreferencesKey("payment_method")
        private const val DEFAULT_PAYMENT_METHOD = "efectivo"
    }
    
    val paymentMethod: Flow<String> = context.paymentDataStore.data
        .map { preferences ->
            preferences[PAYMENT_METHOD_KEY] ?: DEFAULT_PAYMENT_METHOD
        }
    
    suspend fun savePaymentMethod(method: String) {
        context.paymentDataStore.edit { preferences ->
            preferences[PAYMENT_METHOD_KEY] = method
        }
    }
    
    suspend fun getPaymentMethod(): String {
        return context.paymentDataStore.data
            .map { preferences ->
                preferences[PAYMENT_METHOD_KEY] ?: DEFAULT_PAYMENT_METHOD
            }
            .first()
    }
}