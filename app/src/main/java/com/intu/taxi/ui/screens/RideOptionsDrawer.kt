package com.intu.taxi.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.intu.taxi.models.MotoOption
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.first

/** Two bounded heights: dragging can reveal the route, but can never dismiss this step. */
@Composable
internal fun RideOptionsDrawer(
    fare: Double, deliveryFare: Double, distanceKm: Double, durationMinutes: Double,
    selectedOption: MotoOption?, paymentMethod: String, confirmEnabled: Boolean, error: String?,
    onSelect: (MotoOption) -> Unit, onChangePayment: () -> Unit, onConfirm: () -> Unit,
    onVisibleHeightChanged: (Int) -> Unit, modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val scroll = rememberScrollState()
    var expanded by rememberSaveable { mutableStateOf(true) }
    val bottomInset = WindowInsets.navigationBars.getBottom(density) + with(density) { 12.dp.toPx() }
    BoxWithConstraints(modifier.fillMaxSize().padding(horizontal = 12.dp).padding(bottom = 12.dp).navigationBarsPadding(),
        contentAlignment = Alignment.BottomCenter) {
        val expandedHeight = maxHeight * .65f
        val peekHeight = minOf((220 * density.fontScale).dp, expandedHeight - 48.dp).coerceAtLeast(120.dp)
        val maximum = with(density) { expandedHeight.toPx() }
        val minimum = with(density) { peekHeight.toPx() }.coerceAtMost(maximum)
        var dragging by remember { mutableStateOf(false) }
        var dragHeight by remember(maximum, minimum) { mutableFloatStateOf(if (expanded) maximum else minimum) }
        val height by animateFloatAsState(
            targetValue = if (dragging) dragHeight else if (expanded) maximum else minimum,
            animationSpec = if (dragging) snap() else spring(dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow), label = "moto-drawer-height")
        val currentHeight = rememberUpdatedState(height)
        val drag = rememberUpdatedState<(Float) -> Float> { delta ->
            if (!dragging) { dragHeight = height; dragging = true }
            val previous = dragHeight
            dragHeight = (previous - delta).coerceIn(minimum, maximum)
            previous - dragHeight
        }
        val settle = rememberUpdatedState<(Float) -> Unit> { velocity ->
            expanded = if (abs(velocity) > with(density) { 180.dp.toPx() }) velocity < 0
                else dragHeight >= (maximum + minimum) / 2
            dragging = false
        }
        val connection = remember(maximum, minimum, scroll) {
            object : NestedScrollConnection {
                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    if (source != NestedScrollSource.UserInput) return Offset.Zero
                    val expandFirst = available.y < 0 && currentHeight.value < maximum - 1
                    val collapseAtTop = available.y > 0 && scroll.value == 0
                    return if (expandFirst || collapseAtTop) Offset(0f, drag.value(available.y)) else Offset.Zero
                }
                override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset =
                    if (source == NestedScrollSource.UserInput && available.y != 0f) Offset(0f, drag.value(available.y)) else Offset.Zero
                override suspend fun onPreFling(available: Velocity): Velocity {
                    if (!dragging) return Velocity.Zero
                    settle.value(available.y)
                    return Velocity(0f, available.y)
                }
                override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                    if (dragging) settle.value(available.y)
                    return Velocity.Zero
                }
            }
        }
        LaunchedEffect(expanded, dragging, maximum, minimum, bottomInset) {
            if (!dragging) {
                if (!expanded) scroll.scrollTo(0)
                val target = if (expanded) maximum else minimum
                snapshotFlow { height }.first { abs(it - target) < 1f }
                onVisibleHeightChanged((target + bottomInset).roundToInt())
            }
        }
        RideOptionsSheet(fare, deliveryFare, distanceKm, durationMinutes, selectedOption, paymentMethod,
            confirmEnabled, error, onSelect, onChangePayment, onConfirm,
            modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth().height(with(density) { height.toDp() })
                .nestedScroll(connection).draggable(rememberDraggableState { drag.value(it) }, Orientation.Vertical,
                    onDragStopped = { settle.value(it) }),
            compact = !expanded && !dragging,
            onToggleExpansion = { dragging = false; expanded = !expanded }, scrollState = scroll)
    }
}
