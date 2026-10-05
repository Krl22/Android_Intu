package com.intu.taxi.data

import com.intu.taxi.models.DeliveryDetails
import com.intu.taxi.models.DeliveryPayer
import org.json.JSONObject

/** PostgREST embeds the one-to-one detail row; RLS returns null for unassigned couriers. */
internal fun JSONObject.deliveryDetails(): DeliveryDetails? {
    val detail = optJSONObject("delivery_details") ?: optJSONArray("delivery_details")?.optJSONObject(0) ?: return null
    val payer = DeliveryPayer.entries.firstOrNull { it.code == detail.str("payer") } ?: return null
    return DeliveryDetails(detail.str("recipient_name"), detail.str("recipient_phone"), detail.str("description"),
        detail.str("pickup_reference"), detail.str("delivery_reference"), payer,
        paymentCollected = detail.has("payment_collected_at") && !detail.isNull("payment_collected_at"),
        smallPackageConfirmed = detail.optBoolean("small_package_confirmed", false),
        businessName = detail.str("business_name").ifBlank { null },
        sender = detail.str("sender_name").takeIf { it.isNotBlank() }?.let { com.intu.taxi.models.BookingContact(it, detail.str("sender_phone")) },
        businessItems = detail.optJSONArray("business_order_items")?.let { rows -> (0 until rows.length()).map { i ->
            rows.getJSONObject(i).let { com.intu.taxi.models.BusinessOrderItem(it.getString("item_id"), it.getString("name"),
                it.getDouble("unit_price"), it.getInt("quantity")) }
        } }.orEmpty())
}
