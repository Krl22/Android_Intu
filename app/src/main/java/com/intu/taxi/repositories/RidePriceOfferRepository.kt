package com.intu.taxi.repositories

import com.intu.taxi.data.SupabaseApi
import com.intu.taxi.data.str
import com.intu.taxi.models.RidePriceOffer
import org.json.JSONArray
import org.json.JSONObject

class RidePriceOfferRepository {
    suspend fun list(rideId: String? = null): List<RidePriceOffer> {
        val rows = JSONArray(SupabaseApi.request("POST", "rpc/my_ride_price_offers", JSONObject()
            .put("p_ride_id", rideId ?: JSONObject.NULL)))
        return (0 until rows.length()).map { index -> rows.getJSONObject(index).let {
            RidePriceOffer(it.getString("id"), it.getString("ride_id"), it.getDouble("amount"), it.getDouble("app_fare"),
                it.getString("status"), it.str("driver_name").ifBlank { "Conductor" },
                it.str("driver_photo").ifBlank { null }, it.str("vehicle"))
        } }
    }
    suspend fun propose(rideId: String, amount: Double) {
        SupabaseApi.request("POST", "rpc/propose_ride_price", JSONObject().put("p_ride_id", rideId).put("p_amount", amount))
    }
    suspend fun respond(offerId: String, accept: Boolean) {
        SupabaseApi.request("POST", "rpc/respond_ride_price_offer", JSONObject().put("p_offer_id", offerId).put("p_accept", accept))
    }
}
