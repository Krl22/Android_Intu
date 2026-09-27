package com.intu.taxi.auth

import android.app.Activity
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
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

    suspend fun linkWithCredential(credential: PhoneAuthCredential): FirebaseUser? {
        val user = auth.currentUser ?: return null
        val result = user.linkWithCredential(credential).await()
        return result.user
    }

    suspend fun linkWithGoogleAccount(account: GoogleSignInAccount): FirebaseUser? {
        val user = auth.currentUser ?: return null
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        return try {
            val result = user.linkWithCredential(credential).await()
            result.user
        } catch (e: FirebaseAuthUserCollisionException) {
            // Ya existe otra cuenta con ese correo de Google; informar a la UI para resolver.
            throw e
        }
    }

    fun startPhoneVerification(
        activity: Activity,
        phoneE164: String,
        timeoutMinutes: Long = 10,
        callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks,
        forceResendingToken: PhoneAuthProvider.ForceResendingToken? = null
    ) {
        // Firebase solo soporta 0–120s para auto-retrieval en Android.
        // El código SMS puede seguir siendo válido ~10 minutos, pero el auto-retrieval expira antes.
        val safeSeconds = minOf(TimeUnit.MINUTES.toSeconds(timeoutMinutes), 120)
        val builder = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneE164)
            .setTimeout(safeSeconds, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
        if (forceResendingToken != null) builder.setForceResendingToken(forceResendingToken)
        PhoneAuthProvider.verifyPhoneNumber(builder.build())
    }

    suspend fun signInWithPhoneCredential(credential: PhoneAuthCredential): FirebaseUser? {
        val result = auth.signInWithCredential(credential).await()
        return result.user
    }

    suspend fun saveUserProfile(uid: String, profile: UserProfile) {
        db.collection("users").document(uid).set(profile).await()
        SupabaseApi.ensureCurrentProfile(
            firstName = profile.firstName,
            lastName = profile.lastName,
            phone = profile.number.takeIf { it.startsWith("+") },
            email = profile.email,
            driverMode = profile.isDriver
        )
    }

    suspend fun getUserProfile(uid: String): UserProfile? {
        return try {
            val snap = db.collection("users").document(uid).get().await()
            println("DEBUG AuthRepository: Document exists: ${snap.exists()}")
            if (snap.exists()) {
                val profile = snap.toObject<com.intu.taxi.auth.UserProfile>()
                println("DEBUG AuthRepository: Profile loaded - email: ${profile?.email}, firstName: ${profile?.firstName}, isApproved: ${profile?.isApproved}")
                println("DEBUG AuthRepository: Raw document data: ${snap.data}")
                
                // Verificar campo por campo
                println("DEBUG AuthRepository: Checking individual fields:")
                println("DEBUG AuthRepository: - email field: ${snap.getString("email")}")
                println("DEBUG AuthRepository: - firstName field: ${snap.getString("firstName")}")
                println("DEBUG AuthRepository: - isApproved field: ${snap.getBoolean("isApproved")}")
                println("DEBUG AuthRepository: - isDriver field: ${snap.getBoolean("isDriver")}")
                
                // Verificar TODOS los campos disponibles en el documento
                println("DEBUG AuthRepository: ALL AVAILABLE FIELDS:")
                snap.data?.forEach { (key, value) ->
                    println("DEBUG AuthRepository: - $key: $value (type: ${value?.javaClass?.simpleName})")
                }
                
                profile
            } else {
                // El documento no existe, es normal para usuarios nuevos
                println("DEBUG AuthRepository: Document does not exist for user: $uid")
                null
            }
        } catch (e: Exception) {
            // Error al obtener el documento
            println("DEBUG AuthRepository: Error loading profile: ${e.message}")
            throw Exception("Error al obtener perfil: ${e.message}")
        }
    }

    suspend fun getDriverProfile(uid: String): DriverProfile? {
        return try {
            val userProfile = getUserProfile(uid)
            if (userProfile != null && !userProfile.vehicleType.isNullOrBlank()) {
                DriverProfile(
                    vehicleType = userProfile.vehicleType ?: "",
                    vehicleBrand = userProfile.vehicleBrand ?: "",
                    vehicleModel = userProfile.vehicleModel ?: "",
                    vehicleYear = userProfile.vehicleYear ?: "",
                    licensePlate = userProfile.licensePlate ?: "",
                    driverLicense = userProfile.driverLicense ?: "",
                    documentNumber = userProfile.documentNumber ?: "",
                    isApproved = getIsApprovedValue(uid) == true,
                    approvalDate = userProfile.approvalDate
                )
            } else {
                null
            }
        } catch (e: Exception) {
            // Error al obtener el documento
            throw Exception("Error al obtener perfil de conductor: ${e.message}")
        }
    }

    suspend fun saveDriverProfile(uid: String, driverProfile: DriverProfile) {
        // Actualizar solo los campos del conductor en el documento del usuario
        val driverData = mapOf(
            "vehicleType" to driverProfile.vehicleType,
            "vehicleBrand" to driverProfile.vehicleBrand,
            "vehicleModel" to driverProfile.vehicleModel,
            "vehicleYear" to driverProfile.vehicleYear,
            "licensePlate" to driverProfile.licensePlate,
            "driverLicense" to driverProfile.driverLicense,
            "documentNumber" to driverProfile.documentNumber,
            "isApproved" to driverProfile.isApproved,
            "approvalDate" to driverProfile.approvalDate
        )
        db.collection("users").document(uid).update(driverData).await()
        SupabaseApi.syncDriver(
            documentNumber = driverProfile.documentNumber,
            licenseNumber = driverProfile.driverLicense,
            vehicleType = "mototaxi",
            brand = driverProfile.vehicleBrand,
            model = driverProfile.vehicleModel,
            year = driverProfile.vehicleYear.toIntOrNull(),
            plate = driverProfile.licensePlate
        )
    }

    suspend fun hasCompleteDriverProfile(uid: String): Boolean {
        return try {
            val driverProfile = getDriverProfile(uid)
            driverProfile != null && 
            driverProfile.vehicleType.isNotBlank() &&
            driverProfile.vehicleBrand.isNotBlank() &&
            driverProfile.vehicleModel.isNotBlank() &&
            driverProfile.vehicleYear.isNotBlank() &&
            driverProfile.licensePlate.isNotBlank() &&
            driverProfile.driverLicense.isNotBlank() &&
            driverProfile.documentNumber.matches(Regex("^[0-9]{8}$"))
        } catch (e: Exception) {
            // Si hay error al obtener el perfil de conductor, asumimos que no está completo
            false
        }
    }

    suspend fun setDriverMode(uid: String, isDriver: Boolean) {
        db.collection("users").document(uid).update("isDriver", isDriver).await()
        SupabaseApi.ensureCurrentProfile(driverMode = isDriver)
    }

    suspend fun getDriverMode(uid: String): Boolean {
        return try {
            val userProfile = getUserProfile(uid)
            userProfile?.isDriver ?: false
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

    // Función para verificar campos crudos en Firestore (debugging)
    suspend fun checkRawFirestoreData(uid: String): Map<String, Any>? {
        return try {
            val snap = db.collection("users").document(uid).get().await()
            if (snap.exists()) {
                val data = snap.data
                println("DEBUG AuthRepository: Raw Firestore data for UID $uid: $data")
                
                // Verificar específicamente el campo isApproved
                val isApprovedValue = data?.get("isApproved")
                println("DEBUG AuthRepository: Raw isApproved value: $isApprovedValue (type: ${isApprovedValue?.javaClass})")
                
                // Verificar también con diferentes capitalizaciones
                println("DEBUG AuthRepository: Checking different field name variations:")
                println("DEBUG AuthRepository: - 'isapproved' (lowercase): ${data?.get("isapproved")}")
                println("DEBUG AuthRepository: - 'IsApproved' (PascalCase): ${data?.get("IsApproved")}")
                println("DEBUG AuthRepository: - 'ISAPPROVED' (uppercase): ${data?.get("ISAPPROVED")}")
                
                // Si el valor es booleano, verificar su valor real
                if (isApprovedValue is Boolean) {
                    println("DEBUG AuthRepository: Boolean isApproved value: $isApprovedValue")
                } else if (isApprovedValue is String) {
                    println("DEBUG AuthRepository: String isApproved value: '$isApprovedValue'")
                    println("DEBUG AuthRepository: String to boolean conversion: ${isApprovedValue.toBoolean()}")
                }
                
                data
            } else {
                println("DEBUG AuthRepository: Document does not exist for UID: $uid")
                null
            }
        } catch (e: Exception) {
            println("DEBUG AuthRepository: Error checking raw data: ${e.message}")
            null
        }
    }

    fun sendEmailVerification(): Boolean {
        val user = auth.currentUser ?: return false
        user.sendEmailVerification()
        return true
    }
}
