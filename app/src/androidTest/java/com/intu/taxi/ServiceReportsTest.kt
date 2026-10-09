package com.intu.taxi

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.intu.taxi.models.ServiceReport
import com.intu.taxi.ui.screens.ServiceReportDetailContent
import com.intu.taxi.ui.screens.ServiceReportFormContent
import com.intu.taxi.ui.theme.IntuTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class ServiceReportsTest {
    @get:Rule val compose = createComposeRule()
    private fun lost(reporter: Boolean = false) = ServiceReport("qa", "ride", "lost_item", "Bolso rojo bajo el asiento.",
        "open", "awaiting_check", "", "Nota interna", "rider", "2026-10-08", "2026-10-08", reporter, !reporter,
        "Pasajero QA", "Conductor QA", "Recojo QA", "Destino QA", "QA123", "passenger")

    @Test fun passengerChoosesLostItemAndCannotSubmitShortDescription() {
        var submitted: Pair<String,String>? = null
        compose.setContent { IntuTheme { Surface(Modifier.fillMaxSize()) {
            ServiceReportFormContent(false,false,true,false,null,{}, { a,b -> submitted = a to b })
        } } }
        compose.onNodeWithTag("service-report-submit").assertIsNotEnabled()
        compose.onNodeWithTag("report-reason-lost_item").performScrollTo().performClick()
        compose.onNodeWithTag("service-report-description").performScrollTo().performTextInput("Bolso rojo bajo el asiento.")
        compose.onNodeWithTag("service-report-submit").assertIsEnabled().performClick()
        compose.runOnIdle { assertEquals("lost_item" to "Bolso rojo bajo el asiento.", submitted) }
        captureNativeScreenshot(compose,"service-report-form.png")
    }
    @Test fun driverCanConfirmFindingButCannotConfirmTheReturn() {
        var result: String? = null
        compose.setContent { IntuTheme { Surface(Modifier.fillMaxSize()) {
            Column { ServiceReportDetailContent(lost(),false,false,{result=it},{},{_,_,_->}) }
        } } }
        compose.onNodeWithTag("lost-item-found").performScrollTo().performClick()
        compose.runOnIdle { assertEquals("found",result) }
        compose.onNodeWithTag("lost-item-returned").assertDoesNotExist()
        compose.onNodeWithText("Nota interna").assertDoesNotExist()
        captureNativeScreenshot(compose,"lost-item-driver-check.png")
    }
    @Test fun passengerMustConfirmReceiptBeforeResolvingTheCase() {
        val report = mutableStateOf(lost(true).copy(lostState="found"))
        var returned: String? = null
        compose.setContent { IntuTheme { Surface(Modifier.fillMaxSize()) {
            Column { ServiceReportDetailContent(report.value,false,false,{returned=it},{},{_,_,_->}) }
        } } }
        compose.onNodeWithTag("lost-item-returned").performScrollTo().performClick()
        compose.runOnIdle { assertNull(returned) }
        compose.onNodeWithText("Todavía no").performClick()
        compose.runOnIdle { assertNull(returned) }
        compose.onNodeWithTag("lost-item-returned").performClick()
        compose.onNodeWithText("Sí, lo recibí").performClick()
        compose.runOnIdle { assertEquals("returned",returned); report.value=report.value.copy(status="resolved",lostState="returned") }
        compose.onNodeWithTag("lost-item-message").assertDoesNotExist()
    }
}
