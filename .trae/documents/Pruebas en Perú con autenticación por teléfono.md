## Objetivo
Validar la app en Perú con usuarios reales usando autenticación por número de teléfono (+51), asegurando entrega de OTP, distribución controlada, cumplimiento de políticas y observabilidad.

## Requisitos Técnicos
1. Firebase Auth (Teléfono) activado y `google-services.json` configurado en `:app`.
2. Añadir huellas de firma (`SHA‑256` y `SHA‑1`) del APK/App Signing a Firebase para Android, para mejorar auto‑verificación sin reCAPTCHA.
3. Play Integrity habilitado (reemplaza SafetyNet) en el proyecto Firebase; usar builds firmados.
4. E.164 obligatorio: números peruanos como `+51XXXXXXXXX`.
5. Revisar cuotas de verificación de teléfono y habilitar facturación del proyecto Firebase si se planea volumen alto.
6. Verificar permisos de ubicación y red en `AndroidManifest.xml` (ya presentes: `ACCESS_FINE_LOCATION`, `INTERNET`).
7. Mapbox: token válido en recursos (`app/build.gradle.kts:45-47`) y repo Mapbox con `MAPBOX_DOWNLOADS_TOKEN` (`settings.gradle.kts:20-29`).
8. Confirmar flujo OTP: el auto‑retrieval expira a 120s (ver `AuthRepository.startPhoneVerification` en `app/src/main/java/com/intu/taxi/auth/AuthRepository.kt:81-98`).

## Distribución a Testers
1. Google Play Console:
   - Crear pista de "Prueba interna" para equipo y "Prueba cerrada" para usuarios peruanos.
   - Segmentar a Perú y definir cuestionario/consentimiento de prueba.
   - Cargar APK/AAB firmados, completar Ficha de Play y publicar a pistas.
2. Firebase App Distribution (alternativa o previa):
   - Subir builds y invitar testers por email.
   - Proveer instrucciones para instalar y reportar feedback.

## Cumplimiento y Políticas
1. Política de privacidad y términos accesibles desde la app (pantalla de login; `MainActivity.kt:149-158`).
2. Ficha "Seguridad de datos" en Play Console declarando recolección de: ubicación, identificadores, información de cuenta.
3. Avisos en app para uso de ubicación en segundo plano si aplica.
4. Consentimiento explícito para participación en pruebas y tratamiento de datos.

## Operación en Campo
1. Proveer soporte cuando el SMS no llegue (segundo intento, ingreso manual de código, ventana de 10 min).
2. Guía para ingresar teléfono en formato `+51` y permitir reenvío controlado.
3. Cuidar límites por dispositivo/IP y horarios de envío; planificar lotes pequeños.

## Observabilidad y Calidad
1. Activar Crashlytics y Analytics (Firebase BOM ya presente) para eventos clave: login, solicitud de viaje, aceptación, cancelación.
2. Registrar métricas de éxito del OTP: enviado/recibido/tiempo/errores.
3. Minimizar logs sensibles y evitar exponer tokens (Mapbox/Firebase).

## Seguridad de Datos
1. Firestore y RTDB restringidos a usuarios autenticados (ya configurado; ver `firestore_rules.txt` y `realtime_database_rules.json`).
2. Revisar reglas para evitar escrituras cruzadas entre rider/driver.

## Plan de Prueba Piloto
1. Cohorte: 20–50 testers en Lima (diversos operadores). 2 semanas.
2. Escenarios:
   - Registro por teléfono, completar perfil, solicitud de viaje, aceptación, seguimiento en tiempo real y finalización.
   - Cancelación y reintentos de OTP.
3. KPIs:
   - Tasa de entrega de OTP y tiempo medio de verificación.
   - Errores de login y caídas.
   - Éxito de creación/actualización en RTDB (`rides/requests`, `activeRides`).

## Checklist de Configuración
- Firebase Auth (Teléfono) ON y proyecto con facturación.
- `SHA‑256/SHA‑1` agregados en Firebase (debug/release).
- Play Integrity activo; builds firmados.
- Ficha de Play: Data Safety, políticas, segmentación Perú.
- Pistas de prueba internas/cerradas y testers cargados.
- Crashlytics/Analytics integrados y eventos definidos.
- Guía a testers: formato `+51`, ventanas OTP, reenvíos.

## Referencias de Código
- `app/build.gradle.kts:45-47` token de Mapbox en recursos.
- `app/src/main/AndroidManifest.xml:5-10` permisos de red/ubicación.
- `app/src/main/java/com/intu/taxi/auth/AuthRepository.kt:81-98` verificación de teléfono y timeout.
- `app/src/main/java/com/intu/taxi/MainActivity.kt:149-158` pantalla de login y flujos de auth.
- `realtime_database_rules.json:6-33` reglas de RTDB para rides.
- `firestore_rules.txt:5-14` reglas de Firestore para `users` y `rides`.

## Entregables
- Pistas de prueba activas con testers en Perú.
- Documentación breve para testers y canal de soporte.
- Tablero básico (Analytics/Crashlytics) con KPIs definidos.

¿Confirmas que procedamos con este plan para preparar y lanzar la prueba en Perú? 