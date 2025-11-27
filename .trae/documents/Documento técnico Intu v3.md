# Intu v3 — Documento técnico para reproducir la app

## Resumen
- App Android nativa para solicitudes y gestión de viajes tipo taxi/mototaxi.
- UI con Jetpack Compose, mapas con Mapbox, datos en Firebase (Auth, Firestore, Realtime DB).
- Navegación por pantallas con Navigation Compose; flujo de autenticación por Google y teléfono.

## Stack Tecnológico
- Lenguaje: Kotlin (JVM target 11).
- UI: Jetpack Compose (`material3`, `navigation-compose`).
- Mapas: Mapbox Maps SDK (`mapbox-maps-android`).
- Datos: Firebase Auth, Firestore, Realtime Database.
- Utilidades: OkHttp, Coil, DataStore Preferences.
- Build: Gradle (Kotlin DSL), AGP 8.13.1, Kotlin 2.0.21.

## Estructura del Proyecto
- `app/` módulo principal Android.
  - `app/src/main/java/com/intu/taxi/` código Kotlin de la app.
    - `auth/` lógica de autenticación y perfiles.
    - `repositories/` acceso a datos (Realtime DB, Firestore).
    - `models/` modelos serializables para Firebase.
    - `ui/` pantallas, componentes, tema y mapa.
    - `MainActivity.kt` shell y configuración de navegación.
  - `app/src/main/res/` recursos Android.
  - `app/src/main/AndroidManifest.xml` permisos y actividad principal.
  - `app/build.gradle.kts` dependencias, Compose, tokens de recursos.
- Raíz del repo:
  - `settings.gradle.kts` repositorios, Mapbox Maven con token.
  - `gradle/libs.versions.toml` catálogo de versiones.
  - `firestore_rules.txt` y `realtime_database_rules.json` reglas de seguridad.
  - `ARCHITECTURE_DATA.md` criterios de uso Realtime DB vs PostgreSQL.

## Configuración y Variables
- Token de Mapbox como string de recursos: `app/build.gradle.kts:46`.
- Repositorio Maven de Mapbox con `MAPBOX_DOWNLOADS_TOKEN`: `settings.gradle.kts:20-29`.
- Firebase `google-services.json` ubicado en `app/google-services.json` (claves del proyecto `intu-e8403`).
- SDK y targets: `minSdk=24`, `targetSdk=36`: `app/build.gradle.kts:16-18`.

## Dependencias Clave
- Compose BOM y módulos: `app/build.gradle.kts:54-61`.
- Mapbox: `gradle/libs.versions.toml:31` y `app/build.gradle.kts:63`.
- Firebase BOM y módulos: `app/build.gradle.kts:71-74`.
- Google Sign-In: `app/build.gradle.kts:76`.
- OkHttp, Coil, DataStore: `app/build.gradle.kts:66-78`.

## Navegación y Flujo de Usuario
- Raíz y navegación: `MainActivity.kt:59-373`.
- Pantallas principales:
  - Splash: decide destino según autenticación y perfil: `MainActivity.kt:103-147`.
  - Login: selección de proveedor: `MainActivity.kt:149-158`.
  - Google Auth: `MainActivity.kt:160-197` y `ui/screens/GoogleAuthScreen`.
  - Phone Auth (OTP): `MainActivity.kt:199-249` y `ui/screens/PhoneAuthScreen`.
  - Completar perfil: `MainActivity.kt:251-269`.
  - Home/DriverHome: `MainActivity.kt:315-332` (cambia según `isDriverMode`).
  - Trips y Account: `MainActivity.kt:334-370`.

## Autenticación
- Google OAuth: `AuthRepository.kt:57-61`, `AuthRepository.kt:69-79`.
- Teléfono (OTP): `AuthRepository.kt:81-98`, `AuthRepository.kt:100-103`.
- Perfiles de usuario/conductor en Firestore:
  - Guardar/leer perfil: `AuthRepository.kt:105-142`.
  - Perfil de conductor y validación: `AuthRepository.kt:144-196`.
  - Modo conductor (`isDriver`): `AuthRepository.kt:198-209`.

## Mapas y Geolocalización
- Carga y estilo de Mapbox: `ui/screens/HomeScreen.kt:591-607`.
- Cámara y ubicación del usuario: `ui/screens/HomeScreen.kt:609-637`.
- Anotaciones del conductor y seguimiento: `ui/screens/HomeScreen.kt:824-907`.
- Cálculo y ajuste de cámara para ruta: `ui/screens/HomeScreen.kt:1051-1074`, `2080-2084`.

## Flujo de Solicitud de Viaje
- Selección de destino y sugerencias:
  - Geocodificación y búsqueda por texto: `ui/screens/HomeScreen.kt:1177-1183`, `1245-1251`.
  - Confirmación y centrar cámara: `ui/screens/HomeScreen.kt:1414-1416`, `1583-1585`, `2128-2130`.
- Creación de solicitud `RideRequest` en Realtime DB: `ui/screens/HomeScreen.kt:1913-1970` llama a repositorio.
- Repositorio de solicitudes: `RideRequestRepository.kt:22-87` (crear), `RideRequestRepository.kt:90-113` (escuchar), `RideRequestRepository.kt:115-127` (status), `RideRequestRepository.kt:129-142` (cancelar), `RideRequestRepository.kt:144-159` (aceptar).

## Viaje Activo
- Repositorio: `ActiveRideRepository.kt:20-37` (escuchar por `requestId`), `ActiveRideRepository.kt:39-82` (crear), `ActiveRideRepository.kt:84-100`/`102-118` (ubicaciones), `168-184` (completar), `186-197` (cancelar), `199-215` (ruta).
- Modelos:
  - `RideRequest`: `models/RideRequest.kt:7-82` campos, `toMap()`: `99-127`, `fromMap()`: `129-159`.
  - `ActiveRide`: `models/ActiveRide.kt:6-57` campos, `fromMap()`: `72-91`, `toMap()`: `95-115`.

## Disponibilidad de Conductores
- Repositorio: `DriverAvailabilityRepository.kt:24-61` (crear), `63-85` (actualizar ubicación), `87-100` (remover), `102-116` (consultar), `118-131` (iniciar actualizaciones).
- Nodo RTDB: `rides/AvailableDrivers` con índices y validaciones: `realtime_database_rules.json:16-22`.

## Reglas de Seguridad
- Firestore:
  - Usuarios: `firestore_rules.txt:5-8`.
  - Rides (colección Firestore opcional): `firestore_rules.txt:11-14`.
- Realtime Database:
  - `rides/requests`: `.read/.write` autenticados, validar esquema y ownership: `realtime_database_rules.json:6-14`.
  - `rides/AvailableDrivers`: índices y validación de campos: `realtime_database_rules.json:16-23`.
  - `rides/activeRides`: ownership de `driverId`/`clientId` y validación: `realtime_database_rules.json:24-31`.

## Permisos y Manifest
- Permisos de red y ubicación: `app/src/main/AndroidManifest.xml:6-9`.
- `MainActivity` exportada y launcher: `app/src/main/AndroidManifest.xml:21-30`.

## Estilos/Theme
- Tema Compose `IntuTheme` y `BottomBar` en `ui/theme` y `ui/BottomBar` (referenciados en `MainActivity.kt:28-35, 90-95`).

## Construcción y Ejecución
- Requisitos: Android Studio (Arctic/Koala), SDK 36, JDK 11.
- Configurar `local.properties` con `sdk.dir`.
- Establecer `MAPBOX_DOWNLOADS_TOKEN`:
  - En `gradle.properties`: `MAPBOX_DOWNLOADS_TOKEN=xxxxxxxx`.
  - O variable de entorno del sistema.
- Colocar `app/google-services.json` del proyecto Firebase.
- Compilar:
  - Windows: `./gradlew.bat assembleDebug`.
  - macOS/Linux: `./gradlew assembleDebug`.
- Ejecutar en dispositivo/emulador desde Android Studio.

## Paso a Paso para Reproducir
1. Crear proyecto Firebase y habilitar Auth (teléfono y Google), Firestore y Realtime Database.
2. Descargar `google-services.json` y ubicar en `app/`.
3. Configurar reglas (ajustar según tu necesidad):
   - Copiar `firestore_rules.txt` y `realtime_database_rules.json` al proyecto Firebase.
4. Obtener token de Mapbox y definir `MAPBOX_DOWNLOADS_TOKEN` y `mapbox_access_token`.
5. Instalar dependencias con Gradle (Android Studio lo hará al abrir).
6. Verificar permisos de ubicación en el dispositivo.
7. Compilar y ejecutar.

## Testing
- JUnit y AndroidX Test presentes: `app/build.gradle.kts:79-85`.
- Compose UI tests: `app/build.gradle.kts:82-85`.

## Consideraciones y Buenas Prácticas
- No almacenar secretos en código; usar `gradle.properties`/entorno para tokens.
- Validar inputs en RTDB según reglas.
- Manejar errores de Auth y red con feedback al usuario (snackbar/toast).
- Limpieza de rides antiguos: `RideRequestRepository.kt:161-180`.

## Extensiones Futuras
- Migración a PostgreSQL para histórico y analytics: ver `ARCHITECTURE_DATA.md`.
- Servicio backend (FastAPI) para consolidar datos y pagos.

---
Este documento cubre la arquitectura y los pasos para reconstruir la app Intu v3 fielmente. Tras tu aprobación, puedo generar un `.md` dentro del repositorio y/o adaptar instrucciones específicas para tu entorno.