package com.intu.taxi.data

import android.content.Context
import com.intu.taxi.models.BookingContact
import org.json.JSONArray
import org.json.JSONObject

/** Contactos elegidos para otros viajes, por cuenta y solo en este teléfono; el más reciente primero. */
class RecentContacts(context: Context, uid: String) {
    private val prefs = context.getSharedPreferences("intu_recent_contacts", Context.MODE_PRIVATE)
    private val key = "contacts_$uid"
    init { require(uid.isNotBlank()) }

    fun read(): List<BookingContact> = runCatching {
        val rows = JSONArray(prefs.getString(key, "[]"))
        (0 until rows.length()).mapNotNull { i ->
            val row = rows.getJSONObject(i)
            runCatching { BookingContact(row.getString("name"), row.getString("phone")).normalized() }.getOrNull()
        }
    }.getOrDefault(emptyList())

    fun remember(contact: BookingContact) = write(withRecent(read(), contact.normalized()))

    fun forget(phone: String) = write(read().filterNot { it.phone == phone })

    private fun write(contacts: List<BookingContact>) {
        val rows = JSONArray()
        contacts.forEach { rows.put(JSONObject().put("name", it.name).put("phone", it.phone)) }
        prefs.edit().putString(key, rows.toString()).apply()
    }

    companion object {
        const val MAX = 6

        /** Los de la cuenta con sesión (sin sesión, una lista aparte que no se mezcla con ninguna cuenta). */
        fun forCurrentUser(context: Context) =
            RecentContacts(context, com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: "signed-out")

        /** Pone [contact] primero, sin repetir el número, y conserva como mucho [MAX]. */
        fun withRecent(current: List<BookingContact>, contact: BookingContact): List<BookingContact> =
            (listOf(contact) + current.filterNot { it.phone == contact.phone }).take(MAX)
    }
}
