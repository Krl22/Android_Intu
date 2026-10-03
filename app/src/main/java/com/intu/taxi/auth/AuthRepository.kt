package com.intu.taxi.auth

import android.app.Activity
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ktx.toObject
import com.intu.taxi.data.SupabaseApi
import com.intu.taxi.data.str
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

@IgnoreExtraProperties
data class UserProfile(
    val firstName: String = "",
    val lastName: String = "",
    val birthdate: String = "",
    val number: String = "",
    val email: String? = null,
    val termsAccepted: Boolean = false,
    val isDriver: Boolean = false, // Estado del modo conductor
    // Campos del conductor (opcionales)
    val vehicleType: String? = null, // "car", "motorcycle", "mototaxi"
    val vehicleBrand: String? = null,
    val vehicleModel: String? = null,
    val vehicleYear: String? = null,
    val licensePlate: String? = null,
    val driverLicense: String? = null,
    val documentNumber: String? = null,
    val isApproved: Boolean? = null,
    val approvalDate: String? = null
)

@IgnoreExtraProperties
data class DriverProfile(
    val vehicleType: String = "", // "car", "motorcycle", "mototaxi"
    val vehicleBrand: String = "",
    val vehicleModel: String = "",
    val vehicleYear: String = "",
    val licensePlate: String = "",
    val driverLicense: String = "",
    val documentNumber: String = "",
    val isApproved: Boolean = false,
    val approvalDate: String? = null
)

class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    fun currentUser(): FirebaseUser? = auth.currentUser

    suspend fun signInWithGoogleAccount(account: GoogleSignInAccount): FirebaseUser? {
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        val result = auth.signInWithCredential(credential).await()
        return result.user
    }

    suspend fun linkWithCredential(credential: PhoneAuthCredential, expectedUid: String): FirebaseUser {
        val user = requireCurrentUser(expectedUid)
        val linked = if (user.providerData.any { it.providerId == PhoneAuthProvider.PROVIDER_ID }) {
            user.updatePhoneNumber(credential).await()
            user
        } else user.linkWithCredential(credential).await().user ?: error("No se pudo vincular el número.")
        check(linked.uid == expectedUid) { "La cuenta cambió durante la verificación. Intenta de nuevo desde Cuenta." }
        requireCurrentUser(expectedUid).getIdToken(true).await()
        return linked
    }

    suspend fun linkWithGoogleAccount(account: GoogleSignInAccount, expectedUid: String): FirebaseUser {
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        return linkGoogleCredential(credential, expectedUid)
    }

    suspend fun linkGoogleCredential(credential: AuthCredential, expectedUid: String): FirebaseUser {
        require(credential.provider == GoogleAuthProvider.PROVIDER_ID)
        val user = requireCurrentUser(expectedUid)
        val linked = user.linkWithCredential(credential).await().user ?: error("No se pudo vincular Google.")
        check(linked.uid == expectedUid) { "La cuenta cambió. Intenta de nuevo desde Cuenta." }
        requireCurrentUser(expectedUid).getIdToken(true).await()
        return linked
    }

    private fun requireCurrentUser(expectedUid: String): FirebaseUser =
        auth.currentUser?.takeIf { it.uid == expectedUid } ?: error("Tu sesión cambió. Inicia sesión y vuelve a intentarlo.")

    fun startPhoneVerification(
        activity: Activity,
        phoneE164: String,
        timeoutMinutes: Long = 1,
        callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks,
        forceResendingToken: PhoneAuthProvider.ForceResendingToken? = null
    ) {
        require(PhoneFormatter.normalizeMobile(phoneE164) == phoneE164) { "Ingresa un celular válido para recibir el SMS." }
        auth.setLanguageCode("es")
        val safeSeconds = TimeUnit.MINUTES.toSeconds(timeoutMinutes).coerceIn(0, 120)
        val builder = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneE164)
            .setTimeout(safeSeconds, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
        if (forceResendingToken != null) builder.setForceResendingToken(forceResendingToken)
        PhoneAuthProvider.verifyPhoneNumber(builder.build())
    }

    suspend fun signInWithPhoneCredential(credential: PhoneAuthCredential): FirebaseUser? {
        auth.currentUser?.let { current ->
            check(current.providerData.any { it.providerId == PhoneAuthProvider.PROVIDER_ID }) { "Ya tienes una sesión. Vincula el número desde Cuenta." }
            // A failed profile fetch may leave the SMS sign-in complete. Retrying must retain this account.
            current.reauthenticate(credential).await()
            return requireCurrentUser(current.uid)
        }
        val result = auth.signInWithCredential(credential).await()
        return result.user
    }

    /** Guarda el perfil en Supabase, la única fuente de datos del perfil. */
    suspend fun saveUserProfile(uid: String, profile: UserProfile) {
        requireCurrentUser(uid)
        SupabaseApi.ensureCurrentProfile(
            firstName = profile.firstName,
            lastName = profile.lastName,
            phone = profile.number,
            email = profile.email,
            driverMode = profile.isDriver,
            birthdate = profile.birthdate,
            termsAccepted = profile.termsAccepted
        )
    }

    /**
     * Deja creado y al día el perfil de Supabase (los viajes toman de ahí nombre y foto).
     * getUserProfile ya copia los datos antiguos de Firestore si hace falta.
     */
    suspend fun syncProfileToSupabase(uid: String) {
        val profile = runCatching { getUserProfile(uid) }.getOrNull()
        SupabaseApi.ensureCurrentProfile(
            firstName = profile?.firstName.orEmpty(),
            lastName = profile?.lastName.orEmpty(),
            phone = profile?.number,
            email = profile?.email
        )
    }

    /**
     * Sube la foto de perfil (reducida a 640 px, JPEG) a Firebase Storage en avatars/{uid}.jpg y la
     * guarda como foto de la cuenta. Supabase la toma de ahí y la copia a cada viaje, para que
     * pasajero y conductor se reconozcan. Devuelve la URL pública de la foto.
     */
    suspend fun uploadProfilePhoto(context: android.content.Context, uri: android.net.Uri): String {
        val user = auth.currentUser ?: error("Inicia sesión para continuar.")
        val jpeg = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            compressProfilePhoto(context, uri)
        } ?: error("No se pudo leer la foto. Prueba con otra imagen.")

        val ref = com.google.firebase.storage.FirebaseStorage.getInstance().reference
            .child("avatars/${user.uid}.jpg")
        val metadata = com.google.firebase.storage.StorageMetadata.Builder()
            .setContentType("image/jpeg")
            .build()
        ref.putBytes(jpeg, metadata).await()
        // Al cambiar de foto la URL de Storage puede repetirse; el parámetro v evita ver la foto vieja en caché
        val url = ref.downloadUrl.await().toString() + "&v=${System.currentTimeMillis()}"

        user.updateProfile(
            com.google.firebase.auth.UserProfileChangeRequest.Builder()
                .setPhotoUri(android.net.Uri.parse(url))
                .build()
        ).await()
        SupabaseApi.ensureCurrentProfile(photoUrl = url)
        return url
    }

    /** Reduce la foto a 640 px como máximo, respeta la rotación de la cámara y la comprime a JPEG. */
    private fun compressProfilePhoto(context: android.content.Context, uri: android.net.Uri): ByteArray? {
        val resolver = context.contentResolver
        val bounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { android.graphics.BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val maxSide = 640
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2
        val decoded = resolver.openInputStream(uri)?.use {
            android.graphics.BitmapFactory.decodeStream(it, null, android.graphics.BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: return null

        val rotation = runCatching {
            resolver.openInputStream(uri)?.use { input ->
                when (android.media.ExifInterface(input).getAttributeInt(
                    android.media.ExifInterface.TAG_ORIENTATION, android.media.ExifInterface.ORIENTATION_NORMAL
                )) {
                    android.media.ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    android.media.ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    android.media.ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
            } ?: 0f
        }.getOrDefault(0f)

        val scale = minOf(1f, maxSide.toFloat() / maxOf(decoded.width, decoded.height))
        val matrix = android.graphics.Matrix().apply {
            postScale(scale, scale)
            postRotate(rotation)
        }
        val finalBitmap = android.graphics.Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
        return java.io.ByteArrayOutputStream().use { out ->
            finalBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 82, out)
            out.toByteArray()
        }
    }

    /**
     * Perfil del usuario desde Supabase. Las cuentas creadas antes de unificar los datos tenían el
     * perfil en Firestore: si en Supabase falta algo y Firestore lo tiene, se copia una sola vez.
     */
    suspend fun getUserProfile(uid: String): UserProfile? {
        requireCurrentUser(uid)
        val remote = SupabaseApi.currentProfile()?.toUserProfile()
        if (remote != null && remote.isComplete()) return remote

        val legacy = legacyFirestoreProfile(uid) ?: return remote
        val merged = UserProfile(
            firstName = remote?.firstName?.ifBlank { null } ?: legacy.firstName,
            lastName = remote?.lastName?.ifBlank { null } ?: legacy.lastName,
            birthdate = remote?.birthdate?.ifBlank { null } ?: legacy.birthdate,
            number = remote?.number?.ifBlank { null } ?: legacy.number,
            email = remote?.email ?: legacy.email,
            termsAccepted = remote?.termsAccepted == true || legacy.termsAccepted,
            isDriver = remote?.isDriver == true || legacy.isDriver
        )
        val profileSaved = runCatching { saveUserProfile(uid, merged) }.isSuccess
        // Datos del vehículo que solo estaban en Firestore
        var driverSaved = true
        if (!legacy.vehicleBrand.isNullOrBlank() && SupabaseApi.currentDriver() == null) {
            driverSaved = runCatching {
                SupabaseApi.syncDriver(
                    documentNumber = legacy.documentNumber.orEmpty(),
                    licenseNumber = legacy.driverLicense.orEmpty(),
                    vehicleType = if (legacy.vehicleType.isNullOrBlank()) DriverVehicleType.MOTOTAXI.code
                        else DriverVehicleType.requireCode(legacy.vehicleType),
                    brand = legacy.vehicleBrand.orEmpty(),
                    model = legacy.vehicleModel.orEmpty(),
                    year = legacy.vehicleYear?.toIntOrNull(),
                    plate = legacy.licensePlate.orEmpty(),
                    markDriverMode = false
                )
            }.isSuccess
        }
        // Copiado todo a Supabase, el perfil antiguo se borra: así la copia ocurre una sola vez
        // y un reinicio de cuenta desde el panel de administración no lo vuelve a traer.
        if (profileSaved && driverSaved) {
            runCatching { db.collection("users").document(uid).delete().await() }
        }
        return merged
    }

    /** Perfil antiguo guardado en Firestore (solo para copiarlo a Supabase). */
    private suspend fun legacyFirestoreProfile(uid: String): UserProfile? = runCatching {
        val snap = db.collection("users").document(uid).get().await()
        if (snap.exists()) snap.toObject<UserProfile>() else null
    }.getOrNull()

    private fun org.json.JSONObject.toUserProfile() = UserProfile(
        firstName = str("first_name"),
        lastName = str("last_name"),
        birthdate = str("birthdate"),
        number = str("phone"),
        email = str("email").ifBlank { null },
        termsAccepted = !isNull("terms_accepted_at"),
        isDriver = optBoolean("driver_mode", false)
    )

    private fun UserProfile.isComplete() =
        firstName.isNotBlank() && lastName.isNotBlank() && birthdate.isNotBlank() && number.isNotBlank()

    /** Datos de conductor desde Supabase (drivers + vehículo activo). */
    suspend fun getDriverProfile(uid: String): DriverProfile? {
        if (auth.currentUser?.uid != uid) return null
        getUserProfile(uid) // copia datos antiguos de Firestore si aún no están en Supabase
        val (driver, vehicle) = SupabaseApi.currentDriver() ?: return null
        vehicle ?: return null
        return DriverProfile(
            vehicleType = vehicle.str("vehicle_type"),
            vehicleBrand = vehicle.str("brand"),
            vehicleModel = vehicle.str("model"),
            vehicleYear = if (vehicle.isNull("year")) "" else vehicle.optInt("year").toString(),
            licensePlate = vehicle.str("plate"),
            driverLicense = driver.str("license_number"),
            documentNumber = driver.str("document_number"),
            isApproved = driver.str("status") == "approved"
        )
    }

    suspend fun saveDriverProfile(uid: String, driverProfile: DriverProfile) {
        check(auth.currentUser?.uid == uid) { "Inicia sesión para continuar." }
        val vehicleType = DriverVehicleType.requireCode(driverProfile.vehicleType)
        SupabaseApi.syncDriver(
            documentNumber = driverProfile.documentNumber,
            licenseNumber = driverProfile.driverLicense,
            vehicleType = vehicleType,
            brand = driverProfile.vehicleBrand,
            model = driverProfile.vehicleModel,
            year = driverProfile.vehicleYear.toIntOrNull(),
            plate = driverProfile.licensePlate
        )
    }

    suspend fun hasCompleteDriverProfile(uid: String): Boolean {
        return try {
            val driverProfile = getDriverProfile(uid)
            driverProfile?.isCompleteDriverProfile() == true
        } catch (e: Exception) {
            // Si hay error al obtener el perfil de conductor, asumimos que no está completo
            false
        }
    }

    private fun DriverProfile.isCompleteDriverProfile() =
        DriverVehicleType.from(vehicleType) != null && vehicleBrand.isNotBlank() &&
            vehicleModel.isNotBlank() && vehicleYear.isNotBlank() && licensePlate.isNotBlank() &&
            driverLicense.isNotBlank() && documentNumber.matches(Regex("^[0-9]{8}$"))

    suspend fun getDriverAccess(uid: String): DriverAccess {
        requireCurrentUser(uid)
        val driver = getDriverProfile(uid)
        val type = DriverVehicleType.from(driver?.vehicleType)
        val available = type?.let {
            SupabaseApi.rows("vehicle_types?code=eq.${SupabaseApi.encode(it.code)}&select=is_active&limit=1")
                .optJSONObject(0)?.optBoolean("is_active", false) == true
        } ?: false
        return DriverAccess(
            status = SupabaseApi.driverStatus(),
            completeProfile = driver?.isCompleteDriverProfile() == true,
            serviceAvailable = available,
            vehicleType = type
        )
    }

    suspend fun setDriverMode(uid: String, isDriver: Boolean) {
        check(auth.currentUser?.uid == uid) { "Inicia sesión para continuar." }
        if (isDriver) {
            val access = getDriverAccess(uid)
            check(access.canDrive) {
                access.message ?: "Tu solicitud de conductor debe estar aprobada antes de activar este modo."
            }
        }
        SupabaseApi.ensureCurrentProfile(driverMode = isDriver)
    }

    suspend fun getDriverMode(uid: String): Boolean {
        return try {
            val userProfile = getUserProfile(uid)
            userProfile?.isDriver == true && getDriverAccess(uid).canDrive
        } catch (e: Exception) {
            false
        }
    }

    // Función temporal para aprobar conductores (solo para pruebas)
    suspend fun approveDriver(uid: String): Boolean {
        // La aprobación es administrativa y se realiza fuera de la app.
        return false
    }

    // Función para obtener el valor de isApproved con manejo de diferentes tipos y nombres
    suspend fun getIsApprovedValue(uid: String): Boolean? {
        if (auth.currentUser?.uid != uid) return false
        return runCatching { SupabaseApi.driverStatus() == "approved" }.getOrDefault(false)
    }


    suspend fun sendEmailVerification(): Boolean {
        val user = auth.currentUser ?: return false
        if (user.email.isNullOrBlank()) return false
        user.sendEmailVerification().await()
        return true
    }
}
