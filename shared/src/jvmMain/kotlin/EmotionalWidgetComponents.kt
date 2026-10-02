package com.example.widget

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.Canvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

// State model representing current emotional axes
data class EmotionalState(
    val active: Float = 5.0f,
    val revenge: Float = 5.0f,
    val fomo: Float = 5.0f,
    val mistake: Float = 5.0f,
    val ego: Float = 5.0f,
    val confidence: Float = 5.0f,
    val focus: Float = 5.0f,
    val probability: Float = 50.0f
)

fun tiltRegime(active: Float): Pair<String, Color> = when (active) {
    in 1.0f..3.9f -> "CALM" to Color(0xFF34D399)
    in 4.0f..6.9f -> "ELEVATED" to Color(0xFFFBBF24)
    else -> "HYPER" to Color(0xFFF87171)
}

// Brand colors — Trade Metrics desk terminal (restrained, institutional)
object EmotionalThemeColors {
    val Active   = Color(0xFF34D399)
    val Revenge  = Color(0xFFF87171)
    val Fomo     = Color(0xFFA78BFA)
    val Mistake  = Color(0xFF38BDF8)
    val Ego      = Color(0xFFFBBF24)

    val Primary      = Color(0xFF60A5FA)
    val DarkBackdrop = Color(0xF20B1017)
    val PanelFill    = Color(0xFF0D1117)
    val GlassBorder  = Color(0x1AFFFFFF)
    val GlassRimTop  = Color(0x22FFFFFF)
    val GlassFill    = Color(0x0AFFFFFF)
    val Hairline     = Color(0x14FFFFFF)
    val Positive     = Color(0xFF34D399)
    val Negative     = Color(0xFFF87171)
}

// Mathematical State Engine Solver
object StateEngine {
    
    private fun Float.roundTo1(): Float = kotlin.math.round(this * 10f) / 10f
    
    // Bottom-Up Logic: Updates a single sub-axis, forced Master state update
    fun updateSubAxis(
        currentState: EmotionalState,
        axisName: String,
        newValue: Float
    ): EmotionalState {
        val clampedVal = newValue.coerceIn(1.0f, 10.0f)
        val r = if (axisName == "REVENGE") clampedVal else currentState.revenge
        val f = if (axisName == "FOMO") clampedVal else currentState.fomo
        val m = if (axisName == "MISTAKE") clampedVal else currentState.mistake
        val e = if (axisName == "EGO") clampedVal else currentState.ego

        // Master (Active) is forced to equal maximum of all subs (Bottom-Up)
        val maxSub = maxOf(r, maxOf(f, maxOf(m, e)))
        return currentState.copy(
            active = maxSub.roundTo1(),
            revenge = r.roundTo1(),
            fomo = f.roundTo1(),
            mistake = m.roundTo1(),
            ego = e.roundTo1()
        )
    }

    fun updateConfidence(currentState: EmotionalState, newValue: Float) = currentState.copy(confidence = newValue.coerceIn(1.0f, 10.0f).roundTo1())
    fun updateFocus(currentState: EmotionalState, newValue: Float) = currentState.copy(focus = newValue.coerceIn(1.0f, 10.0f).roundTo1())
    fun updateProbability(currentState: EmotionalState, newValue: Float) = currentState.copy(probability = newValue.coerceIn(0.0f, 100.0f).roundTo1())

    // Top-Down Logic: Updates master axis, scales sub-axes proportionally
    fun updateMasterAxis(
        currentState: EmotionalState,
        newMaster: Float
    ): EmotionalState {
        val masterClamped = newMaster.coerceIn(1.0f, 10.0f)
        val maxSub = maxOf(currentState.revenge, maxOf(currentState.fomo, maxOf(currentState.mistake, currentState.ego)))
        
        return if (maxSub > 1.0f) {
            // Formula specified: newSub = 1 + (NewMaster - 1) * ((CurrentSub - 1) / (MaxSub - 1))
            val ratio = (masterClamped - 1.0f) / (maxSub - 1.0f)
            
            // Do not prematurely round internal state; let the exact floats preserve perfect vector ratios
            val r = 1.0f + (currentState.revenge - 1.0f) * ratio
            val f = 1.0f + (currentState.fomo - 1.0f) * ratio
            val m = 1.0f + (currentState.mistake - 1.0f) * ratio
            val e = 1.0f + (currentState.ego - 1.0f) * ratio

            EmotionalState(
                active = masterClamped.roundTo1(),
                revenge = r.coerceIn(1.0f, 10.0f).roundTo1(),
                fomo = f.coerceIn(1.0f, 10.0f).roundTo1(),
                mistake = m.coerceIn(1.0f, 10.0f).roundTo1(),
                ego = e.coerceIn(1.0f, 10.0f).roundTo1()
            )
        } else {
            // Guard: All subs are at 1.0f - scale with the Master directly (prevent 0/0 division)
            EmotionalState(
                active = masterClamped.roundTo1(),
                revenge = masterClamped.roundTo1(),
                fomo = masterClamped.roundTo1(),
                mistake = masterClamped.roundTo1(),
                ego = masterClamped.roundTo1()
            )
        }
    }
}

// Fluent Window Custom Horizontal Slider for Master Axis
@Composable
fun HorizontalEmotionalSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    activeColor: Color,
    modifier: Modifier = Modifier
) {
    // FIX: Capture the latest state lambda so old slider values don't overwrite new ones
    val currentOnValueChange by rememberUpdatedState(onValueChange)
    val currentValue by rememberUpdatedState(value)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(30.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        val widthPx = constraints.maxWidth.toFloat()

        // Custom background track drawing
        val trackHeight = 6.dp
        val density = LocalDensity.current
        val trackHeightPx = with(density) { trackHeight.toPx() }
        val radiusPx = trackHeightPx / 2f

        val fillFraction = (value - 1.0f) / 9.0f

        val thumbSize = 16.dp
        val thumbSizePx = with(density) { thumbSize.toPx() }
        val usableWidth = widthPx - thumbSizePx

        val gestureModifier = Modifier
            .fillMaxSize()
            .pointerInput(widthPx) {
                detectTapGestures { offset ->
                    val adjustedX = (offset.x - (thumbSizePx / 2f)).coerceIn(0f, usableWidth)
                    val frac = adjustedX / usableWidth
                    val rawVal = 1.0f + frac * 9.0f
                    val steppedVal = (rawVal * 10f).roundToInt() / 10f
                    currentOnValueChange(steppedVal)
                }
            }
            .pointerInput(widthPx) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    val adjustedX = (change.position.x - (thumbSizePx / 2f)).coerceIn(0f, usableWidth)
                    val frac = adjustedX / usableWidth
                    val rawVal = 1.0f + frac * 9.0f
                    val steppedVal = (rawVal * 10f).roundToInt() / 10f
                    currentOnValueChange(steppedVal)
                }
            }
            // Premium Mouse Wheel Scroll Support
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val scrollDeltaY = event.changes.firstOrNull()?.scrollDelta?.y ?: 0f
                        if (scrollDeltaY != 0f) {
                            // Scroll up (negative delta) increases value, scroll down decreases
                            val step = if (scrollDeltaY > 0) -0.1f else 0.1f // Smooth 0.1 precision steps
                            val steppedVal = ((currentValue + step).coerceIn(1.0f, 10.0f) * 10f).roundToInt() / 10f
                            currentOnValueChange(steppedVal)
                            event.changes.forEach { it.consume() }
                        }
                    }
                }
            }

        Box(
            modifier = gestureModifier
                .drawBehind {
                    // Gray Track Background
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.12f),
                        topLeft = Offset(0f, (size.height - trackHeightPx) / 2f),
                        size = Size(size.width, trackHeightPx),
                        cornerRadius = CornerRadius(radiusPx, radiusPx)
                    )
                    
                    // Connected Active Fill with beautiful glowing gradient and aesthetic neon aura
                    // Math Fix: Fill width perfectly tracks the center of the thumb, not the raw fraction
                    val thumbCenterPx = (usableWidth * fillFraction) + (thumbSizePx / 2f)
                    val fillWidth = thumbCenterPx

                    if (fillWidth > 0f) {
                        // Soft outer neon glow aura
                        drawRoundRect(
                            color = activeColor.copy(alpha = 0.15f),
                            topLeft = Offset(0f, (size.height - trackHeightPx * 2.2f) / 2f),
                            size = Size(fillWidth, trackHeightPx * 2.2f),
                            cornerRadius = CornerRadius(radiusPx * 2.2f, radiusPx * 2.2f)
                        )
                        // Precise foreground slider path
                        drawRoundRect(
                            brush = Brush.horizontalGradient(
                                colors = listOf(activeColor.copy(alpha = 0.6f), activeColor)
                            ),
                            topLeft = Offset(0f, (size.height - trackHeightPx) / 2f),
                            size = Size(fillWidth, trackHeightPx),
                            cornerRadius = CornerRadius(radiusPx, radiusPx)
                        )
                    }
                    
                    // Minor Tweak division lines (1 to 10)
                    val stepOffset = size.width / 9f
                    for (i in 0..9) {
                        val tickX = i * stepOffset
                        val tickRadius = if (i == 0 || i == 9) 3.5f else 2.5f
                        val isHigh = value >= (1.0f + i)
                        val maxLimitX = size.width - tickRadius
                        val finalTickX = if (maxLimitX >= tickRadius) tickX.coerceIn(tickRadius, maxLimitX) else tickRadius
                        drawCircle(
                            color = if (isHigh) Color.White.copy(alpha = 0.9f) else Color.White.copy(alpha = 0.35f),
                            radius = tickRadius,
                            center = Offset(finalTickX, size.height / 2f)
                        )
                    }
                }
        ) {
            // Glowing slider thumb with dual-layer glassmorphic aura
            val thumbOffset = usableWidth * fillFraction
            val thumbDpOffset = with(density) { thumbOffset.toDp() }
            
            // Outer dynamic blooming halo
            Box(
                modifier = Modifier
                    .offset(x = thumbDpOffset - 4.dp)
                    .align(Alignment.CenterStart)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(activeColor.copy(alpha = 0.25f))
            )
            // Precise central button
            Box(
                modifier = Modifier
                    .offset(x = thumbDpOffset)
                    .align(Alignment.CenterStart)
                    .size(thumbSize)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(3.dp, activeColor, CircleShape)
            )
        }
    }
}

// Fluent Window Custom Vertical Slider for Sub-Axes
@Composable
fun VerticalEmotionalSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    activeColor: Color,
    modifier: Modifier = Modifier
) {
    // FIX: Capture the latest state lambda so old slider values don't overwrite new ones
    val currentOnValueChange by rememberUpdatedState(onValueChange)
    val currentValue by rememberUpdatedState(value)

    BoxWithConstraints(
        modifier = modifier
            .width(36.dp)
            .fillMaxHeight(),
        contentAlignment = Alignment.BottomCenter
    ) {
        val heightPx = constraints.maxHeight.toFloat()
        val fillFraction = (value - 1.0f) / 9.0f
        val density = LocalDensity.current
        val trackWidth = 6.dp
        val trackWidthPx = with(density) { trackWidth.toPx() }
        val radiusPx = trackWidthPx / 2f

        val thumbSize = 14.dp
        val thumbSizePx = with(density) { thumbSize.toPx() }
        val usableHeight = heightPx - thumbSizePx

        val gestureModifier = Modifier
            .fillMaxSize()
            .pointerInput(heightPx) {
                detectTapGestures { offset ->
                    val adjustedY = (offset.y - (thumbSizePx / 2f)).coerceIn(0f, usableHeight)
                    val frac = 1.0f - (adjustedY / usableHeight)
                    val rawVal = 1.0f + frac * 9.0f
                    val steppedVal = (rawVal * 10f).roundToInt() / 10f
                    currentOnValueChange(steppedVal)
                }
            }
            .pointerInput(heightPx) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    val adjustedY = (change.position.y - (thumbSizePx / 2f)).coerceIn(0f, usableHeight)
                    val frac = 1.0f - (adjustedY / usableHeight)
                    val rawVal = 1.0f + frac * 9.0f
                    val steppedVal = (rawVal * 10f).roundToInt() / 10f
                    currentOnValueChange(steppedVal)
                }
            }
            // Premium Mouse Wheel Scroll Support
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val scrollDeltaY = event.changes.firstOrNull()?.scrollDelta?.y ?: 0f
                        if (scrollDeltaY != 0f) {
                            // Scroll up (negative delta) increases value, scroll down decreases
                            val step = if (scrollDeltaY > 0) -0.1f else 0.1f // Smooth 0.1 precision steps
                            val steppedVal = ((currentValue + step).coerceIn(1.0f, 10.0f) * 10f).roundToInt() / 10f
                            currentOnValueChange(steppedVal)
                            event.changes.forEach { it.consume() }
                        }
                    }
                }
            }

        Box(
            modifier = gestureModifier
                .drawBehind {
                    // Track path with enhanced contrast
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.12f),
                        topLeft = Offset((size.width - trackWidthPx) / 2f, 0f),
                        size = Size(trackWidthPx, size.height),
                        cornerRadius = CornerRadius(radiusPx, radiusPx)
                    )
                    
                    // Fill path with elegant neon aura
                    // Math Fix: Fill height perfectly tracks the center of the thumb, preventing visual slipping
                    val thumbCenterY = (usableHeight * (1.0f - fillFraction)) + (thumbSizePx / 2f)
                    val fillHeight = size.height - thumbCenterY

                    if (fillHeight > 0f) {
                        // Soft outer vertical glow aura
                        drawRoundRect(
                            color = activeColor.copy(alpha = 0.15f),
                            topLeft = Offset((size.width - trackWidthPx * 2.2f) / 2f, thumbCenterY),
                            size = Size(trackWidthPx * 2.2f, fillHeight),
                            cornerRadius = CornerRadius(radiusPx * 2.2f, radiusPx * 2.2f)
                        )
                        // Sharp inner core fill path
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(activeColor, activeColor.copy(alpha = 0.5f))
                            ),
                            topLeft = Offset((size.width - trackWidthPx) / 2f, thumbCenterY),
                            size = Size(trackWidthPx, fillHeight),
                            cornerRadius = CornerRadius(radiusPx, radiusPx)
                        )
                    }
                    
                    // Step dividers
                    val stepOffset = size.height / 9f
                    for (i in 0..9) {
                        val tickY = size.height - (i * stepOffset)
                        val tickRadius = if (i == 0 || i == 9) 3.5f else 2.5f
                        val isHigh = value >= (1.0f + i)
                        val maxLimitY = size.height - tickRadius
                        val finalTickY = if (maxLimitY >= tickRadius) tickY.coerceIn(tickRadius, maxLimitY) else tickRadius
                        drawCircle(
                            color = if (isHigh) Color.White.copy(alpha = 0.9f) else Color.White.copy(alpha = 0.3f),
                            radius = tickRadius,
                            center = Offset(size.width / 2f, finalTickY)
                        )
                    }
                }
        ) {
            // Slider vertical thumb circle with premium halo styling
            val thumbOffset = usableHeight * (1.0f - fillFraction)
            val thumbDpOffset = with(density) { thumbOffset.toDp() }
            
            // Outer dynamic blooming halo
            Box(
                modifier = Modifier
                    .offset(y = thumbDpOffset - 4.dp)
                    .align(Alignment.TopCenter)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(activeColor.copy(alpha = 0.25f))
            )
            // Precise central button
            Box(
                modifier = Modifier
                    .offset(y = thumbDpOffset)
                    .align(Alignment.TopCenter)
                    .size(thumbSize)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(3.dp, activeColor, CircleShape)
            )
        }
    }
}

@Composable
fun Windows11WidgetPanel(
    state: EmotionalState,
    onStateChanged: (EmotionalState) -> Unit,
    subAxesExpanded: Boolean,
    onSubAxesToggle: (Boolean) -> Unit,
    onCollapseToBubble: () -> Unit,
    onMinimizeToTaskbar: () -> Unit,
    onCloseClick: () -> Unit,
    desk: DeskSnapshot = DeskSnapshot(),
    clockText: String = "",
    modifier: Modifier = Modifier
) {
    val (regimeLabel, regimeColor) = tiltRegime(state.active)
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, EmotionalThemeColors.GlassBorder),
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                val corner = CornerRadius(14.dp.toPx(), 14.dp.toPx())
                drawRoundRect(color = EmotionalThemeColors.DarkBackdrop, cornerRadius = corner)
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            EmotionalThemeColors.Primary.copy(alpha = 0.08f),
                            Color.Transparent,
                            regimeColor.copy(alpha = 0.06f)
                        )
                    ),
                    cornerRadius = corner
                )
            }
            .animateContentSize(animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.Top
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF0F1923))
                            .border(1.dp, Color(0xFF1E3A5F), RoundedCornerShape(6.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        InstitutionalBrandMark(size = 16f, strokeColor = EmotionalThemeColors.Primary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "TRADE METRICS",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmotionalThemeColors.Primary,
                            letterSpacing = 1.6.sp
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "DESK OVERLAY",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White.copy(alpha = 0.42f),
                                letterSpacing = 0.8.sp
                            )
                            Spacer(Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (desk.lastSyncOk) EmotionalThemeColors.Positive
                                        else Color.White.copy(alpha = 0.25f)
                                    )
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = if (desk.lastSyncOk) "LIVE" else "STANDBY",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (desk.lastSyncOk) EmotionalThemeColors.Positive else Color.White.copy(alpha = 0.35f),
                                letterSpacing = 0.6.sp
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onMinimizeToTaskbar, modifier = Modifier.size(22.dp)) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Minimize to Taskbar",
                            tint = Color.White.copy(alpha = 0.4f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    IconButton(onClick = onCollapseToBubble, modifier = Modifier.size(22.dp)) {
                        Icon(
                            imageVector = Icons.Default.Minimize,
                            contentDescription = "Minimize to Bubble",
                            tint = Color.White.copy(alpha = 0.55f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    IconButton(onClick = onCloseClick, modifier = Modifier.size(22.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Stop Service",
                            tint = Color.White.copy(alpha = 0.55f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            DeskTelemetryBar(desk = desk, clockText = clockText, regimeLabel = regimeLabel, regimeColor = regimeColor)

            if (desk.cooldownActive) {
                Spacer(modifier = Modifier.height(6.dp))
                CooldownBanner(untilMs = desk.cooldownUntilMs)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "READINESS",
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.32f),
                letterSpacing = 1.4.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            // 2. Monitoring Metrics Glass Card (rim-lit, inner-radius = 12 - 10 = 2dp for inner elements)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(EmotionalThemeColors.GlassFill)
                    .drawBehind {
                        // Rim-light: bright top edge simulates overhead lighting on glass
                        drawLine(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    EmotionalThemeColors.GlassRimTop,
                                    EmotionalThemeColors.GlassRimTop,
                                    Color.Transparent
                                )
                            ),
                            start = Offset(16f, 0f),
                            end = Offset(size.width - 16f, 0f),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                    .border(1.dp, EmotionalThemeColors.GlassBorder, RoundedCornerShape(12.dp))
                    .padding(10.dp)
            ) {
                Column {
                    MotherSlider(
                        label = "CONFIDENCE",
                        value = state.confidence,
                        onValueChange = { onStateChanged(StateEngine.updateConfidence(state, it)) },
                        range = 1f..10f,
                        accentColor = Color(0xFF60A5FA)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    MotherSlider(
                        label = "FOCUS",
                        value = state.focus,
                        onValueChange = { onStateChanged(StateEngine.updateFocus(state, it)) },
                        range = 1f..10f,
                        accentColor = Color(0xFFC084FC) // Lavender
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    MotherSlider(
                        label = "PROBABILITY",
                        value = state.probability,
                        onValueChange = { onStateChanged(StateEngine.updateProbability(state, it)) },
                        range = 0f..100f,
                        accentColor = Color(0xFFFBBF24) // Warm Honey
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            // Divider
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.1f)))
            Spacer(modifier = Modifier.height(8.dp))

            // 2. Main horizontal master slider ("ACTIVE")
            val animatedActiveColor by animateColorAsState(
                targetValue = regimeColor,
                animationSpec = tween(durationMillis = 300),
                label = "ActiveSliderColor"
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .drawBehind {
                        drawRoundRect(
                            color = animatedActiveColor.copy(alpha = 0.08f),
                            cornerRadius = CornerRadius(8.dp.toPx())
                        )
                    }
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                MotherSlider(
                    label = "TILT LOAD",
                    value = state.active,
                    onValueChange = { newVal -> onStateChanged(StateEngine.updateMasterAxis(state, newVal)) },
                    range = 1f..10f,
                    accentColor = animatedActiveColor
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 3. Sub-axes toggle button — clean pill style
            Surface(
                onClick = { onSubAxesToggle(!subAxesExpanded) },
                shape = RoundedCornerShape(8.dp),
                color = Color.White.copy(alpha = 0.03f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (subAxesExpanded) Icons.Default.SettingsApplications else Icons.Default.Tune,
                            contentDescription = "Sub-profiles toggle",
                            tint = Color.White.copy(alpha = 0.4f),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (subAxesExpanded) "Tilt factors" else "Tilt factors",
                            color = Color.White.copy(alpha = 0.55f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = FontFamily.SansSerif,
                            letterSpacing = 0.4.sp
                        )
                    }
                    // Animated chevron rotates 180° on expand
                    val chevronRotation by animateFloatAsState(
                        targetValue = if (subAxesExpanded) 180f else 0f,
                        animationSpec = tween(200, easing = FastOutSlowInEasing),
                        label = "chevron"
                    )
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Expand indicator",
                        tint = Color.White.copy(alpha = 0.35f),
                        modifier = Modifier
                            .size(16.dp)
                            .graphicsLayer { rotationZ = chevronRotation }
                    )
                }
            }

            // 4 & 5. Unified glass container — always present, content swaps inside.
            // Using AnimatedVisibility (not AnimatedContent) so each state correctly
            // collapses to zero height when hidden — no extra space reserved.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(EmotionalThemeColors.GlassFill)
                    .drawBehind {
                        drawLine(
                            brush = Brush.horizontalGradient(
                                listOf(
                                    Color.Transparent,
                                    EmotionalThemeColors.GlassRimTop,
                                    EmotionalThemeColors.GlassRimTop,
                                    Color.Transparent
                                )
                            ),
                            start = Offset(16f, 0f),
                            end = Offset(size.width - 16f, 0f),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                    .border(1.dp, EmotionalThemeColors.GlassBorder, RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Collapsed: mini badge row
                    AnimatedVisibility(
                        visible = !subAxesExpanded,
                        enter = fadeIn(tween(180, delayMillis = 80)),
                        exit = fadeOut(tween(100))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            ProfileMiniBadge(label = "REV",  value = state.revenge, color = EmotionalThemeColors.Revenge)
                            ProfileMiniBadge(label = "FOMO", value = state.fomo,    color = EmotionalThemeColors.Fomo)
                            ProfileMiniBadge(label = "DRIFT", value = state.mistake, color = EmotionalThemeColors.Mistake)
                            ProfileMiniBadge(label = "EGO",  value = state.ego,     color = EmotionalThemeColors.Ego)
                        }
                    }

                    // Expanded: full child sliders
                    AnimatedVisibility(
                        visible = subAxesExpanded,
                        enter = fadeIn(tween(180, delayMillis = 80)) +
                            expandVertically(spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)),
                        exit = fadeOut(tween(100)) +
                            shrinkVertically(spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow))
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(start = 10.dp, end = 10.dp, top = 10.dp, bottom = 4.dp)) {
                            ChildSlider(
                                label = "REVENGE",
                                value = state.revenge,
                                onValueChange = { onStateChanged(StateEngine.updateSubAxis(state, "REVENGE", it)) },
                                accentColor = EmotionalThemeColors.Revenge
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            ChildSlider(
                                label = "FOMO",
                                value = state.fomo,
                                onValueChange = { onStateChanged(StateEngine.updateSubAxis(state, "FOMO", it)) },
                                accentColor = EmotionalThemeColors.Fomo
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            ChildSlider(
                                label = "PROCESS DRIFT",
                                value = state.mistake,
                                onValueChange = { onStateChanged(StateEngine.updateSubAxis(state, "MISTAKE", it)) },
                                accentColor = EmotionalThemeColors.Mistake
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            ChildSlider(
                                label = "EGO",
                                value = state.ego,
                                onValueChange = { onStateChanged(StateEngine.updateSubAxis(state, "EGO", it)) },
                                accentColor = EmotionalThemeColors.Ego
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            
            // ── Copyright Watermark ──
            Text(
                "Trade Metrics  ·  session overlay  ·  2026",
                fontSize = 8.sp,
                color = Color.White.copy(alpha = 0.18f),
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center,
                letterSpacing = 0.4.sp,
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
            )
        }
    }
}

@Composable
fun InstitutionalBrandMark(
    size: Float = 16f,
    strokeColor: Color = Color.White,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(size.dp)) {
        val w = this.size.width
        val h = this.size.height
        val strokePx = (1.5f / 32f) * w
        val c = strokeColor
        drawLine(c, Offset(w * 4 / 32f, h * 4 / 32f), Offset(w * 4 / 32f, h * 9 / 32f), strokePx)
        drawLine(c, Offset(w * 4 / 32f, h * 4 / 32f), Offset(w * 28 / 32f, h * 4 / 32f), strokePx)
        drawLine(c, Offset(w * 28 / 32f, h * 4 / 32f), Offset(w * 28 / 32f, h * 28 / 32f), strokePx)
        drawLine(c, Offset(w * 28 / 32f, h * 28 / 32f), Offset(w * 12 / 32f, h * 28 / 32f), strokePx)
        drawLine(c, Offset(w * 12 / 32f, h * 12 / 32f), Offset(w * 20 / 32f, h * 12 / 32f), strokePx)
        drawLine(c, Offset(w * 16 / 32f, h * 12 / 32f), Offset(w * 16 / 32f, h * 22 / 32f), strokePx)
    }
}

@Composable
private fun DeskTelemetryBar(
    desk: DeskSnapshot,
    clockText: String,
    regimeLabel: String,
    regimeColor: Color
) {
    val pnlColor = when {
        desk.todayPnl > 0 -> EmotionalThemeColors.Positive
        desk.todayPnl < 0 -> EmotionalThemeColors.Negative
        else -> Color.White.copy(alpha = 0.75f)
    }
    val pnlText = when {
        desk.zenMode -> "${desk.currencySymbol} ***"
        desk.todayPnl > 0 -> "${desk.currencySymbol}${String.format("%,.2f", desk.todayPnl)}"
        desk.todayPnl < 0 -> "-${desk.currencySymbol}${String.format("%,.2f", kotlin.math.abs(desk.todayPnl))}"
        else -> "${desk.currencySymbol}${String.format("%,.2f", 0.0)}"
    }
    val wrText = if (desk.closedToday == 0) "—" else String.format("%.0f%%", desk.winRateToday)

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(EmotionalThemeColors.GlassFill)
                .border(1.dp, EmotionalThemeColors.Hairline, RoundedCornerShape(10.dp))
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.weight(1.15f)) { TelemetryCell(label = "SESSION", value = clockText.ifBlank { "--:--:--" }, valueColor = Color.White.copy(alpha = 0.8f)) }
            Box(Modifier.weight(1.45f)) { TelemetryCell(label = "TODAY P&L", value = pnlText, valueColor = pnlColor) }
            Box(Modifier.weight(0.7f)) { TelemetryCell(label = "OPEN", value = desk.openCount.toString(), valueColor = Color.White.copy(alpha = 0.85f)) }
            Box(Modifier.weight(0.8f)) { TelemetryCell(label = "CLOSED", value = desk.closedToday.toString(), valueColor = Color.White.copy(alpha = 0.85f)) }
            Box(Modifier.weight(0.65f)) { TelemetryCell(label = "WR", value = wrText, valueColor = Color.White.copy(alpha = 0.85f)) }
        }
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(regimeColor.copy(alpha = 0.12f))
                        .border(1.dp, regimeColor.copy(alpha = 0.35f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = regimeLabel,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = regimeColor,
                        letterSpacing = 1.2.sp
                    )
                }
                if (desk.watchCount > 0) {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "${desk.watchCount} watch",
                        fontSize = 9.sp,
                        color = Color.White.copy(alpha = 0.38f),
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
            val limit = desk.dailyLossLimit
            if (limit != null && limit > 0) {
                val used = (kotlin.math.abs(desk.todayPnl.coerceAtMost(0.0)) / limit).toFloat().coerceIn(0f, 1f)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "LOSS LIMIT",
                        fontSize = 8.sp,
                        color = Color.White.copy(alpha = 0.35f),
                        letterSpacing = 0.6.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .width(72.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color.White.copy(alpha = 0.1f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(used)
                                .background(if (used > 0.7f) EmotionalThemeColors.Negative else EmotionalThemeColors.Ego)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TelemetryCell(label: String, value: String, valueColor: Color) {
    Column(horizontalAlignment = Alignment.Start) {
        Text(
            text = label,
            fontSize = 7.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White.copy(alpha = 0.32f),
            letterSpacing = 0.7.sp,
            fontFamily = FontFamily.Monospace
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = valueColor,
            fontFamily = FontFamily.Monospace,
            maxLines = 1
        )
    }
}

@Composable
private fun CooldownBanner(untilMs: Long?) {
    val remaining = ((untilMs ?: 0L) - System.currentTimeMillis()).coerceAtLeast(0L)
    val h = remaining / 3_600_000
    val m = (remaining % 3_600_000) / 60_000
    val s = (remaining % 60_000) / 1000
    val clock = if (h > 0) String.format("%02d:%02d:%02d", h, m, s) else String.format("%02d:%02d", m, s)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(EmotionalThemeColors.Negative.copy(alpha = 0.12f))
            .border(1.dp, EmotionalThemeColors.Negative.copy(alpha = 0.28f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "COOLDOWN",
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = EmotionalThemeColors.Negative,
            letterSpacing = 1.1.sp,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = clock,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = EmotionalThemeColors.Negative,
            fontFamily = FontFamily.Monospace
        )
    }
}

// Mini badge helper for collapsed profiling view
@Composable
fun ProfileMiniBadge(
    label: String,
    value: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.05f))
            .border(1.dp, color.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
            .padding(horizontal = 6.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.4f),
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = String.format("%.1f", value),
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = color
            )
        }
    }
}


@Composable
fun MotherSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    range: ClosedFloatingPointRange<Float>,
    accentColor: Color
) {
    var dragValue by remember(value) { mutableStateOf(value) }
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Inter for labels — premium, readable sans-serif
            Text(
                text = label,
                fontFamily = FontFamily.SansSerif,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.75f),
                letterSpacing = 0.8.sp
            )
            // JetBrains Mono for values — monospaced, no jitter
            AnimatedContent(
                targetState = if (range.endInclusive == 100f) "${dragValue.toInt()}%" else String.format("%.1f", dragValue),
                transitionSpec = {
                    (slideInVertically { height -> height } + fadeIn(animationSpec = tween(120)))
                        .togetherWith(slideOutVertically { height -> -height } + fadeOut(animationSpec = tween(80)))
                }
            ) { targetText ->
                Text(
                    text = targetText,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                    style = androidx.compose.ui.text.TextStyle(
                        fontFeatureSettings = "tnum"
                    )
                )
            }
        }
        Spacer(modifier = Modifier.height(1.dp))
        androidx.compose.material3.Slider(
            value = dragValue,
            onValueChange = { dragValue = it },
            onValueChangeFinished = { onValueChange(dragValue) },
            valueRange = range,
            colors = androidx.compose.material3.SliderDefaults.colors(
                thumbColor = accentColor,
                activeTrackColor = accentColor,
                inactiveTrackColor = accentColor.copy(alpha = 0.18f)
            ),
            modifier = Modifier.height(22.dp)
        )
    }
}

@Composable
fun ChildSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    accentColor: Color
) {
    var dragValue by remember(value) { mutableStateOf(value) }
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                fontFamily = FontFamily.SansSerif,
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.6f),
                letterSpacing = 0.6.sp
            )
            AnimatedContent(
                targetState = String.format("%.1f", dragValue),
                transitionSpec = {
                    (slideInVertically { height -> height } + fadeIn(animationSpec = tween(120)))
                        .togetherWith(slideOutVertically { height -> -height } + fadeOut(animationSpec = tween(80)))
                }
            ) { targetText ->
                Text(
                    text = targetText,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                    style = androidx.compose.ui.text.TextStyle(
                        fontFeatureSettings = "tnum"
                    )
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        // Clip the Material3 Slider's invisible bottom touch-target padding
        // that would otherwise create a gap below the last slider in the card.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
        ) {
            androidx.compose.material3.Slider(
                value = dragValue,
                onValueChange = { dragValue = it },
                onValueChangeFinished = { onValueChange(dragValue) },
                valueRange = 1f..10f,
                colors = androidx.compose.material3.SliderDefaults.colors(
                    thumbColor = accentColor,
                    activeTrackColor = accentColor,
                    inactiveTrackColor = accentColor.copy(alpha = 0.15f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = (-1).dp) // slight shift so it's not clipped at top
            )
        }
    }
}
