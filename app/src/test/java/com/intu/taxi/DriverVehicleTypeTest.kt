package com.intu.taxi

import com.intu.taxi.auth.DriverVehicleType
import org.junit.Assert.*
import org.junit.Test

class DriverVehicleTypeTest {
    @Test fun motorcycleLabelsAndDatabaseCodesKeepTheirIdentity() {
        listOf("Moto lineal", " motorcycle ", "Moto", "MOTO LINEAL").forEach {
            assertEquals("motorcycle", DriverVehicleType.requireCode(it))
        }
        assertEquals("mototaxi", DriverVehicleType.requireCode("Mototaxi"))
        assertEquals("Moto lineal", DriverVehicleType.from("motorcycle")?.label)
    }

    @Test fun unsupportedVehicleTypesAreRejectedInsteadOfBecomingMototaxis() {
        listOf("", "car", "camión", "null").forEach {
            assertNull(DriverVehicleType.from(it))
            assertThrows(IllegalArgumentException::class.java) { DriverVehicleType.requireCode(it) }
        }
    }
}
