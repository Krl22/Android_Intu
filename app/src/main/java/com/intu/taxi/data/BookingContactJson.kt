package com.intu.taxi.data

import com.intu.taxi.models.BookingContact
import org.json.JSONObject

internal fun JSONObject.ridePassenger(): BookingContact? {
    val row = optJSONObject("ride_passengers") ?: optJSONArray("ride_passengers")?.optJSONObject(0) ?: return null
    return BookingContact(row.str("name"), row.str("phone"))
}
