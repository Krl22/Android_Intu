package com.intu.taxi.ui.screens

import com.intu.taxi.ui.theme.AppearanceColors

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material.icons.filled.LocalTaxi
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.automirrored.filled.BrandingWatermark
import androidx.compose.material.icons.filled.ModelTraining
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.rotate
import com.intu.taxi.auth.DriverProfile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverDataCollectionScreen(
    onSubmit: (DriverProfile) -> Unit,
    onCancel: () -> Unit,
    isConversion: Boolean = true,
    isSubmitting: Boolean = false
) {
    var vehicleType by remember { mutableStateOf("Mototaxi") }
    var vehicleBrand by remember { mutableStateOf("") }
    var vehicleModel by remember { mutableStateOf("") }
    var vehicleYear by remember { mutableStateOf("") }
    var licensePlate by remember { mutableStateOf("") }
    var driverLicense by remember { mutableStateOf("") }
    var documentNumber by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var brandExpanded by remember { mutableStateOf(false) }
    var modelExpanded by remember { mutableStateOf(false) }
    var isCustomModel by remember { mutableStateOf(false) }
    var yearExpanded by remember { mutableStateOf(false) }

    // Estados de animación
    val cardVisible = remember { mutableStateOf(false) }
    val titleVisible = remember { mutableStateOf(false) }
    val contentVisible = remember { mutableStateOf(false) }
    val buttonsVisible = remember { mutableStateOf(false) }

    // Generar lista de años (desde 1990 hasta el año actual + 1)
    val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
    val years = (1990..currentYear + 1).map { it.toString() }.reversed()

    // Iniciar animaciones secuenciales con mejor timing
    LaunchedEffect(Unit) {
        // Animación más fluida y rápida
        kotlinx.coroutines.delay(150)
        cardVisible.value = true
        kotlinx.coroutines.delay(200)
        titleVisible.value = true
        kotlinx.coroutines.delay(150)
        contentVisible.value = true
        kotlinx.coroutines.delay(100)
        buttonsVisible.value = true
    }

    // Tipos de vehículo disponibles
    val vehicleTypes = listOf("Mototaxi" to Icons.Default.LocalTaxi, "Moto lineal" to Icons.Default.TwoWheeler)

    // Marcas de vehículo por tipo
    val vehicleBrandsByType = mapOf(
        "Carro" to listOf("Toyota", "Honda", "Nissan", "Chevrolet", "Ford", "Hyundai", "Kia", "Volkswagen", "Mazda", "Otra"),
        "Moto lineal" to listOf("Honda", "Yamaha", "Suzuki", "Kawasaki", "Bajaj", "TVS", "Otra"),
        "Mototaxi" to listOf("Honda", "Bajaj")
    )

    // Modelos de vehículo por marca
    val vehicleModelsByBrand = mapOf(
        "Honda" to listOf("Wave 125", "GL150", "GL125", "CGL 125 Tool", "XR 150", "XR 125", "CB 125", "CB 190", "Otro"),
        "Yamaha" to listOf("XTZ 125", "XT 660", "FZ 25", "MT 15", "Otro"),
        "Suzuki" to listOf("GN 125", "GSX 150", "V-Strom 250", "Otro"),
        "Kawasaki" to listOf("KLX 150", "Ninja 300", "Versys 300", "Otro"),
        "Bajaj" to listOf("Pulsar 135", "Pulsar 180", "Boxer 150", "Otro"),
        "TVS" to listOf("Apache RTR 160", "Star City 125", "Otro"),
        "Toyota" to listOf("Corolla", "Yaris", "Hilux", "RAV4", "Otro"),
        "Nissan" to listOf("Sentra", "Versa", "March", "X-Trail", "Otro"),
        "Chevrolet" to listOf("Spark", "Sail", "Onix", "Otro"),
        "Ford" to listOf("Fiesta", "Focus", "Ranger", "Otro"),
        "Hyundai" to listOf("Accent", "Elantra", "Tucson", "Otro"),
        "Kia" to listOf("Rio", "Cerato", "Sportage", "Otro"),
        "Volkswagen" to listOf("Jetta", "Vento", "Tiguan", "Otro"),
        "Mazda" to listOf("Mazda 2", "Mazda 3", "CX-5", "Otro"),
        "Otra" to listOf("Otro")
    )

    // Fondo con gradiente animado
    val backgroundAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        backgroundAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 1200,
                easing = FastOutSlowInEasing
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF1E1F47).copy(alpha = backgroundAlpha.value), // Índigo oscuro
                        Color(0xFF08817E).copy(alpha = backgroundAlpha.value), // Teal
                        Color(0xFF0FB9B1).copy(alpha = backgroundAlpha.value)  // Teal claro
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                )
            )
    ) {
        // Patrón de fondo con burbujas animadas
        Box(
            modifier = Modifier
                .fillMaxSize()
                .alpha(0.1f * backgroundAlpha.value)
        ) {
            repeat(3) { index ->
                val bubbleAlpha = remember { Animatable(0f) }
                val bubbleOffset = remember { Animatable(50f) }

                LaunchedEffect(Unit) {
                    delay((200 + index * 150).toLong())
                    launch {
                        bubbleAlpha.animateTo(
                            targetValue = 1f,
                            animationSpec = tween(
                                durationMillis = 1000,
                                easing = FastOutSlowInEasing
                            )
                        )
                    }
                    launch {
                        bubbleOffset.animateTo(
                            targetValue = 0f,
                            animationSpec = tween(
                                durationMillis = 1200,
                                easing = FastOutSlowInEasing
                            )
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size((80 + index * 40).dp)
                        .offset(
                            x = (30 + index * 80 + bubbleOffset.value).dp,
                            y = (80 + index * 120 + bubbleOffset.value).dp
                        )
                        .background(
                            Color.White.copy(alpha = 0.15f * bubbleAlpha.value),
                            shape = CircleShape
                        )
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AnimatedVisibility(
                visible = cardVisible.value,
                enter = fadeIn(
                    animationSpec = tween(
                        durationMillis = 1000,
                        easing = FastOutSlowInEasing
                    )
                ) + slideInVertically(
                    initialOffsetY = { it / 6 },
                    animationSpec = tween(
                        durationMillis = 1000,
                        easing = FastOutSlowInEasing
                    )
                )
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.92f),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = AppearanceColors.surface.copy(alpha = 0.95f)
                    ),
                    elevation = CardDefaults.cardElevation(12.dp)
                ) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Icono principal
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            Color(0xFF08817E),
                                            Color(0xFF0FB9B1)
                                        )
                                    ),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DirectionsCar,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(30.dp)
                            )
                        }

                        AnimatedVisibility(
                            visible = titleVisible.value,
                            enter = fadeIn(
                                animationSpec = tween(
                                    durationMillis = 800,
                                    delayMillis = 150,
                                    easing = FastOutSlowInEasing
                                )
                            ) + slideInVertically(
                                initialOffsetY = { it / 8 },
                                animationSpec = tween(
                                    durationMillis = 800,
                                    delayMillis = 150,
                                    easing = FastOutSlowInEasing
                                )
                            )
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    if (isConversion) "Convertirse en conductor" else "Datos de conductor",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = AppearanceColors.ink,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    if (isConversion) "Regístrate con mototaxi o moto lineal para reparto" else "Ingresa la información de tu vehículo",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = AppearanceColors.accent,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        AnimatedVisibility(
                            visible = contentVisible.value,
                            enter = fadeIn(
                                animationSpec = tween(
                                    durationMillis = 700,
                                    delayMillis = 350,
                                    easing = FastOutSlowInEasing
                                )
                            ) + slideInVertically(
                                initialOffsetY = { it / 10 },
                                animationSpec = tween(
                                    durationMillis = 700,
                                    delayMillis = 350,
                                    easing = FastOutSlowInEasing
                                )
                            )
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Tipo de vehículo
                                ExposedDropdownMenuBox(
                                    expanded = expanded,
                                    onExpandedChange = { expanded = !expanded }
                                ) {
                                    TextField(
                                        value = vehicleType,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Tipo de vehículo") },
                                        leadingIcon = {
                                            Icon(
                                                vehicleTypes.find { it.first == vehicleType }?.second ?: Icons.Default.DirectionsCar,
                                                contentDescription = null,
                                                tint = Color(0xFF08817E)
                                            )
                                        },
                                        trailingIcon = {
                                            Icon(
                                                Icons.Default.ArrowDropDown,
                                                contentDescription = "Seleccionar tipo"
                                            )
                                        },
                                        colors = TextFieldDefaults.colors(
                                            focusedContainerColor = Color.Transparent,
                                            unfocusedContainerColor = Color.Transparent,
                                            focusedIndicatorColor = Color(0xFF08817E),
                                            unfocusedIndicatorColor = Color(0xFF08817E).copy(alpha = 0.5f)
                                        ),
                                        modifier = Modifier
                                            .menuAnchor()
                                            .fillMaxWidth(),
                                        textStyle = MaterialTheme.typography.bodyMedium
                                    )
                                    DropdownMenu(
                                        expanded = expanded,
                                        onDismissRequest = { expanded = false }
                                    ) {
                                        vehicleTypes.forEach { (type, icon) ->
                                            DropdownMenuItem(
                                                text = {
                                                    Column {
                                                        Text(type)
                                                        Text(
                                                            com.intu.taxi.auth.DriverVehicleType.from(type)?.serviceLabel.orEmpty(),
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = AppearanceColors.accent
                                                        )
                                                    }
                                                },
                                                leadingIcon = { Icon(icon, contentDescription = null, tint = Color(0xFF08817E)) },
                                                onClick = {
                                                    vehicleType = type
                                                    vehicleBrand = "" // Limpiar la marca cuando cambia el tipo
                                                    vehicleModel = ""
                                                    isCustomModel = false
                                                    expanded = false
                                                }
                                            )
                                        }
                                    }
                                }

                                Text(
                                    if (vehicleType == "Moto lineal")
                                        "Courier / repartidor. Tu solicitud será revisada por Intu antes de recibir pedidos de paquetes pequeños."
                                    else "Transporte de pasajeros. Tu solicitud será revisada por Intu.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AppearanceColors.accent,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                // Marca del vehículo (dinámica según tipo)
                                if (vehicleType.isNotBlank() && vehicleBrandsByType.containsKey(vehicleType)) {
                                    ExposedDropdownMenuBox(
                                        expanded = brandExpanded,
                                        onExpandedChange = { brandExpanded = !brandExpanded }
                                    ) {
                                        TextField(
                                            value = vehicleBrand,
                                            onValueChange = {},
                                            readOnly = true,
                                            label = { Text("Marca del vehículo") },
                                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.BrandingWatermark, contentDescription = null, tint = Color(0xFF08817E)) },
                                            trailingIcon = {
                                                Icon(
                                                    Icons.Default.ArrowDropDown,
                                                    contentDescription = "Seleccionar marca"
                                                )
                                            },
                                            colors = TextFieldDefaults.colors(
                                                focusedContainerColor = Color.Transparent,
                                                unfocusedContainerColor = Color.Transparent,
                                                focusedIndicatorColor = Color(0xFF08817E),
                                                unfocusedIndicatorColor = Color(0xFF08817E).copy(alpha = 0.5f)
                                            ),
                                            modifier = Modifier
                                                .menuAnchor()
                                                .fillMaxWidth(),
                                            textStyle = MaterialTheme.typography.bodyMedium
                                        )
                                        DropdownMenu(
                                            expanded = brandExpanded,
                                            onDismissRequest = { brandExpanded = false }
                                        ) {
                                            vehicleBrandsByType[vehicleType]?.forEach { brand ->
                                                DropdownMenuItem(
                                                    text = { Text(brand) },
                                                    onClick = {
                                                        vehicleBrand = brand
                                                        vehicleModel = "" // Limpiar el modelo cuando cambia la marca
                                                        isCustomModel = false // Resetear el estado de modelo personalizado
                                                        brandExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    // Campo deshabilitado si no hay tipo seleccionado
                                    TextField(
                                        value = vehicleBrand,
                                        onValueChange = { vehicleBrand = it },
                                        label = { Text("Marca del vehículo") },
                                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.BrandingWatermark, contentDescription = null, tint = Color(0xFF08817E)) },
                                        singleLine = true,
                                        enabled = false, // Deshabilitado hasta que se seleccione tipo
                                        colors = TextFieldDefaults.colors(
                                            focusedContainerColor = Color.Transparent,
                                            unfocusedContainerColor = AppearanceColors.tint(Color(0xFFF5F5F5)), // Fondo gris claro para estado deshabilitado
                                            focusedIndicatorColor = Color(0xFF08817E),
                                            unfocusedIndicatorColor = Color(0xFF08817E).copy(alpha = 0.5f),
                                            disabledTextColor = AppearanceColors.secondary(Color.Gray),
                                            disabledLabelColor = AppearanceColors.secondary(Color.Gray),
                                            disabledLeadingIconColor = AppearanceColors.secondary(Color.Gray),
                                            disabledContainerColor = AppearanceColors.tint(Color(0xFFF5F5F5)) // Fondo consistente para estado deshabilitado
                                        ),
                                        modifier = Modifier.fillMaxWidth(),
                                        textStyle = MaterialTheme.typography.bodyMedium
                                    )
                                }

                                // Modelo del vehículo (dinámico según marca)
                                if (vehicleBrand.isNotBlank() && vehicleModelsByBrand.containsKey(vehicleBrand)) {
                                    val availableModels = vehicleModelsByBrand[vehicleBrand] ?: emptyList()

                                    if (isCustomModel) {
                                        // Campo de texto personalizado
                                        TextField(
                                            value = vehicleModel,
                                            onValueChange = { vehicleModel = it },
                                            label = { Text("Modelo personalizado") },
                                            placeholder = { Text("Escribe el modelo del vehículo") },
                                            leadingIcon = { Icon(Icons.Default.ModelTraining, contentDescription = null, tint = Color(0xFF08817E)) },
                                            singleLine = true,
                                            colors = TextFieldDefaults.colors(
                                                focusedContainerColor = Color.Transparent,
                                                unfocusedContainerColor = Color.Transparent,
                                                focusedIndicatorColor = Color(0xFF08817E),
                                                unfocusedIndicatorColor = Color(0xFF08817E).copy(alpha = 0.5f)
                                            ),
                                            modifier = Modifier.fillMaxWidth(),
                                            textStyle = MaterialTheme.typography.bodyMedium
                                        )
                                    } else {
                                        // Dropdown con modelos predefinidos
                                        ExposedDropdownMenuBox(
                                            expanded = modelExpanded,
                                            onExpandedChange = { modelExpanded = !modelExpanded }
                                        ) {
                                            TextField(
                                                value = vehicleModel,
                                                onValueChange = {},
                                                readOnly = true,
                                                label = { Text("Modelo del vehículo") },
                                                leadingIcon = { Icon(Icons.Default.ModelTraining, contentDescription = null, tint = Color(0xFF08817E)) },
                                                trailingIcon = {
                                                    Icon(
                                                        Icons.Default.ArrowDropDown,
                                                        contentDescription = "Seleccionar modelo"
                                                    )
                                                },
                                                colors = TextFieldDefaults.colors(
                                                    focusedContainerColor = Color.Transparent,
                                                    unfocusedContainerColor = Color.Transparent,
                                                    focusedIndicatorColor = Color(0xFF08817E),
                                                    unfocusedIndicatorColor = Color(0xFF08817E).copy(alpha = 0.5f)
                                                ),
                                                modifier = Modifier
                                                    .menuAnchor()
                                                    .fillMaxWidth(),
                                                textStyle = MaterialTheme.typography.bodyMedium
                                            )
                                            DropdownMenu(
                                                expanded = modelExpanded,
                                                onDismissRequest = { modelExpanded = false }
                                            ) {
                                                availableModels.forEach { model ->
                                                    DropdownMenuItem(
                                                        text = { Text(model) },
                                                        onClick = {
                                                            vehicleModel = if (model == "Otro") "" else model
                                                            isCustomModel = (model == "Otro")
                                                            modelExpanded = false
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    // Campo deshabilitado si no hay marca seleccionada
                                    TextField(
                                        value = vehicleModel,
                                        onValueChange = { vehicleModel = it },
                                        label = { Text("Modelo del vehículo") },
                                        leadingIcon = { Icon(Icons.Default.ModelTraining, contentDescription = null, tint = Color(0xFF08817E)) },
                                        singleLine = true,
                                        enabled = false, // Deshabilitado hasta que se seleccione marca
                                        colors = TextFieldDefaults.colors(
                                            focusedContainerColor = Color.Transparent,
                                            unfocusedContainerColor = AppearanceColors.tint(Color(0xFFF5F5F5)), // Fondo gris claro para estado deshabilitado
                                            focusedIndicatorColor = Color(0xFF08817E),
                                            unfocusedIndicatorColor = Color(0xFF08817E).copy(alpha = 0.5f),
                                            disabledTextColor = AppearanceColors.secondary(Color.Gray),
                                            disabledLabelColor = AppearanceColors.secondary(Color.Gray),
                                            disabledLeadingIconColor = AppearanceColors.secondary(Color.Gray),
                                            disabledContainerColor = AppearanceColors.tint(Color(0xFFF5F5F5)) // Fondo consistente para estado deshabilitado
                                        ),
                                        modifier = Modifier.fillMaxWidth(),
                                        textStyle = MaterialTheme.typography.bodyMedium
                                    )
                                }

                                // Año del vehículo con selector simplificado
                                ExposedDropdownMenuBox(
                                    expanded = yearExpanded,
                                    onExpandedChange = { yearExpanded = !yearExpanded }
                                ) {
                                    TextField(
                                        value = vehicleYear,
                                        onValueChange = { vehicleYear = it.filter { char -> char.isDigit() }.take(4) },
                                        label = { Text("Año del vehículo") },
                                        leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null, tint = Color(0xFF08817E)) },
                                        trailingIcon = {
                                            Icon(
                                                Icons.Default.ArrowDropDown,
                                                contentDescription = "Seleccionar año"
                                            )
                                        },
                                        colors = TextFieldDefaults.colors(
                                            focusedContainerColor = Color.Transparent,
                                            unfocusedContainerColor = Color.Transparent,
                                            focusedIndicatorColor = Color(0xFF08817E),
                                            unfocusedIndicatorColor = Color(0xFF08817E).copy(alpha = 0.5f)
                                        ),
                                        modifier = Modifier
                                            .menuAnchor()
                                            .fillMaxWidth(),
                                        textStyle = MaterialTheme.typography.bodyMedium,
                                        readOnly = true
                                    )

                                    DropdownMenu(
                                        expanded = yearExpanded,
                                        onDismissRequest = { yearExpanded = false },
                                        modifier = Modifier
                                            .fillMaxWidth(0.9f)
                                            .heightIn(max = 280.dp)
                                    ) {
                                        years.forEach { year ->
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        text = year,
                                                        style = MaterialTheme.typography.bodyLarge,
                                                        fontWeight = if (year == vehicleYear) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (year == vehicleYear) Color(0xFF08817E) else AppearanceColors.ink
                                                    )
                                                },
                                                onClick = {
                                                    vehicleYear = year
                                                    yearExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }

                                // Placa del vehículo
                                TextField(
                                    value = licensePlate,
                                    onValueChange = { licensePlate = it.uppercase() },
                                    label = { Text("Placa del vehículo") },
                                    leadingIcon = { Icon(Icons.Default.ConfirmationNumber, contentDescription = null, tint = Color(0xFF08817E)) },
                                    singleLine = true,
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        focusedIndicatorColor = Color(0xFF08817E),
                                        unfocusedIndicatorColor = Color(0xFF08817E).copy(alpha = 0.5f)
                                    ),
                                    modifier = Modifier.fillMaxWidth(),
                                    textStyle = MaterialTheme.typography.bodyMedium
                                )

                                // Licencia de conducir
                                TextField(
                                    value = documentNumber,
                                    onValueChange = { documentNumber = it.filter(Char::isDigit).take(8) },
                                    label = { Text("DNI (8 dígitos)") },
                                    leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null, tint = Color(0xFF08817E)) },
                                    singleLine = true,
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        focusedIndicatorColor = Color(0xFF08817E),
                                        unfocusedIndicatorColor = Color(0xFF08817E).copy(alpha = 0.5f)
                                    ),
                                    modifier = Modifier.fillMaxWidth(),
                                    textStyle = MaterialTheme.typography.bodyMedium
                                )

                                // Licencia de conducir
                                TextField(
                                    value = driverLicense,
                                    onValueChange = { driverLicense = it },
                                    label = { Text("Licencia de conducir") },
                                    leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null, tint = Color(0xFF08817E)) },
                                    singleLine = true,
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        focusedIndicatorColor = Color(0xFF08817E),
                                        unfocusedIndicatorColor = Color(0xFF08817E).copy(alpha = 0.5f)
                                    ),
                                    modifier = Modifier.fillMaxWidth(),
                                    textStyle = MaterialTheme.typography.bodyMedium
                                )

                                // Botones de acción con animación suave
                                AnimatedVisibility(
                                    visible = buttonsVisible.value,
                                    enter = fadeIn(
                                        animationSpec = tween(
                                            durationMillis = 600,
                                            delayMillis = 500,
                                            easing = FastOutSlowInEasing
                                        )
                                    ) + slideInVertically(
                                        initialOffsetY = { it / 4 },
                                        animationSpec = tween(
                                            durationMillis = 600,
                                            delayMillis = 500,
                                            easing = FastOutSlowInEasing
                                        )
                                    )
                                ) {
                                    // Acción principal a todo el ancho para que el texto completo quepa;
                                    // "Cancelar" queda debajo como acción secundaria
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                if (vehicleType.isNotBlank() && vehicleBrand.isNotBlank() &&
                                                    vehicleModel.isNotBlank() && vehicleYear.isNotBlank() &&
                                                    licensePlate.isNotBlank() && driverLicense.isNotBlank() && documentNumber.length == 8) {

                                                    val driverProfile = DriverProfile(
                                                        vehicleType = com.intu.taxi.auth.DriverVehicleType.requireCode(vehicleType),
                                                        vehicleBrand = vehicleBrand,
                                                        vehicleModel = vehicleModel,
                                                        vehicleYear = vehicleYear,
                                                        licensePlate = licensePlate,
                                                        driverLicense = driverLicense,
                                                        documentNumber = documentNumber
                                                    )
                                                    onSubmit(driverProfile)
                                                }
                                            },
                                            enabled = !isSubmitting && vehicleType.isNotBlank() && vehicleBrand.isNotBlank() &&
                                                     vehicleModel.isNotBlank() && vehicleYear.isNotBlank() &&
                                                     licensePlate.isNotBlank() && driverLicense.isNotBlank() && documentNumber.length == 8,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(52.dp),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFF08817E),
                                                contentColor = Color.White
                                            )
                                        ) {
                                            Text(
                                                if (isSubmitting) "Enviando…" else if (isConversion) "Enviar solicitud" else "Guardar",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }

                                        TextButton(
                                            onClick = onCancel,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("Cancelar", color = Color(0xFF5F6570))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
