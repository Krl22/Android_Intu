package com.intu.taxi.auth

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthException

fun authErrorMessage(error: Throwable, linking: Boolean = false): String {
    if (error is FirebaseNetworkException) return "No hay conexión. Revisa tu internet e intenta de nuevo."
    if (error is FirebaseTooManyRequestsException) return "Se hicieron demasiados intentos. Espera unos minutos antes de volver a intentarlo."
    return when ((error as? FirebaseAuthException)?.errorCode) {
        "ERROR_INVALID_VERIFICATION_CODE" -> "El código no es correcto. Revisa los 6 dígitos del SMS e intenta de nuevo."
        "ERROR_SESSION_EXPIRED", "ERROR_INVALID_VERIFICATION_ID" -> "El código venció. Solicita uno nuevo."
        "ERROR_INVALID_PHONE_NUMBER", "ERROR_MISSING_PHONE_NUMBER" -> "Ingresa un celular válido. En Perú son 9 dígitos y comienza con 9."
        "ERROR_CREDENTIAL_ALREADY_IN_USE", "ERROR_EMAIL_ALREADY_IN_USE", "ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL" ->
            if (linking) "Este número o Google ya pertenece a otra cuenta de Intu. No se vinculó ni se cambiaron tus datos. Entra con ese método para acceder a la otra cuenta."
            else "Este acceso pertenece a una cuenta que usa otro método. Entra con el método original y vincúlalo desde Cuenta."
        "ERROR_PROVIDER_ALREADY_LINKED" -> "Este método ya está vinculado a tu cuenta."
        "ERROR_REQUIRES_RECENT_LOGIN" -> "Por seguridad, vuelve a iniciar sesión con tu método actual antes de vincular o cambiar tus accesos."
        "ERROR_USER_MISMATCH" -> "Ese código pertenece a otro número. Usa el número de esta cuenta o vuelve al inicio de sesión."
        "ERROR_OPERATION_NOT_ALLOWED" -> "Este método de acceso no está disponible. Contacta con soporte de Intu."
        "ERROR_QUOTA_EXCEEDED", "ERROR_TOO_MANY_REQUESTS" -> "No se pueden enviar más códigos por ahora. Intenta más tarde."
        "ERROR_APP_NOT_AUTHORIZED", "ERROR_INVALID_APP_CREDENTIAL", "ERROR_MISSING_APP_CREDENTIAL", "ERROR_CAPTCHA_CHECK_FAILED" ->
            "No pudimos verificar esta instalación de Intu. Actualiza la app e intenta de nuevo."
        "ERROR_USER_DISABLED" -> "Esta cuenta está deshabilitada. Contacta con soporte de Intu."
        else -> if (error is IllegalStateException || error is IllegalArgumentException) error.message ?: "No se pudo completar la verificación."
            else "No se pudo completar la verificación. Revisa tu conexión e intenta de nuevo."
    }
}
