@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.widget

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Minimize
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.awt.Toolkit
import java.util.Locale
import kotlinx.coroutines.delay

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
    in 1.0f..3.9f -> "CALM" to EmotionalThemeColors.Positive
    in 4.0f..6.9f -> "ELEVATED" to EmotionalThemeColors.Warning
    else -> "HYPER" to EmotionalThemeColors.Danger
}

@Composable
fun rememberPrefersReducedMotion(): Boolean {
    return remember {
        try {
            val toolkit = Toolkit.getDefaultToolkit()
            val fade = toolkit.getDesktopProperty("win.anim.fade")
            fade is Boolean && !fade
        } catch (_: Throwable) {
            false
        }
    }
}

object EmotionalThemeColors {
    val Revenge = Color(0xFFF87171)
    val Fomo = Color(0xFFA78BFA)
    val Mistake = Color(0xFF38BDF8)
    val Ego = Color(0xFFFBBF24)

    val Primary = Color(0xFF60A5FA)
    val DarkBackdrop = Color(0xF20B0F19)
    val PanelFill = Color(0xFF0D1117)
    val GlassBorder = Color(0x1AFFFFFF)
    val Hairline = Color(0x14FFFFFF)
    val TextEyebrow = Color.White.copy(alpha = 0.58f)
    val TextMuted = Color.White.copy(alpha = 0.52f)
    val TextHint = Color.White.copy(alpha = 0.42f)

    val Positive = Color(0xFF10B981)
    val Warning = Color(0xFFF59E0B)
    val Danger = Color(0xFFEF4444)

    val PnlGain = Color(0xFF6EE7B7)
    val PnlLoss = Color(0xFFFECACA)
    val PnlFlat = Color(0xFF94A3B8)
}

object EmotionalMetrics {
    val RadiusShell = 10.dp
    val RadiusCard = 8.dp
    val RadiusControl = 6.dp
    val TextFloor = 9.sp
}

data class DeskPnlDisplay(
    val text: String,
    val color: Color
)

fun formatDeskPnl(amount: Double, currencySymbol: String, zenMode: Boolean): DeskPnlDisplay {
    if (zenMode) {
        return DeskPnlDisplay("${currencySymbol} ***", EmotionalThemeColors.PnlFlat)
    }
    val color = when {
        amount > 0.0 -> EmotionalThemeColors.PnlGain
        amount < 0.0 -> EmotionalThemeColors.PnlLoss
        else -> EmotionalThemeColors.PnlFlat
    }
    val sign = when {
        amount > 0.0 -> "+"
        amount < 0.0 -> "-"
        else -> "\u2007"
    }
    val abs = String.format(Locale.US, "%,.2f", kotlin.math.abs(amount))
    return DeskPnlDisplay("$sign$currencySymbol$abs", color)
}

object StateEngine {

    private fun Float.roundTo1(): Float = kotlin.math.round(this * 10f) / 10f

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

        val maxSub = maxOf(r, maxOf(f, maxOf(m, e)))
        return currentState.copy(
            active = maxSub.roundTo1(),
            revenge = r.roundTo1(),
            fomo = f.roundTo1(),
            mistake = m.roundTo1(),
            ego = e.roundTo1()
        )
    }

    fun updateConfidence(currentState: EmotionalState, newValue: Float) =
        currentState.copy(confidence = newValue.coerceIn(1.0f, 10.0f).roundTo1())

    fun updateFocus(currentState: EmotionalState, newValue: Float) =
        currentState.copy(focus = newValue.coerceIn(1.0f, 10.0f).roundTo1())

    fun updateProbability(currentState: EmotionalState, newValue: Float) =
        currentState.copy(probability = newValue.coerceIn(0.0f, 100.0f).roundTo1())

    fun updateMasterAxis(
        currentState: EmotionalState,
        newMaster: Float
    ): EmotionalState {
        val masterClamped = newMaster.coerceIn(1.0f, 10.0f)
        val maxSub = maxOf(currentState.revenge, maxOf(currentState.fomo, maxOf(currentState.mistake, currentState.ego)))

        return if (maxSub > 1.0f) {
            val ratio = (masterClamped - 1.0f) / (maxSub - 1.0f)
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
    val reducedMotion = rememberPrefersReducedMotion()
    val shell = EmotionalMetrics.RadiusShell
    val sizeSpec = if (reducedMotion) {
        tween(0)
    } else {
        spring<androidx.compose.ui.unit.IntSize>(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow)
    }

    Card(
        shape = RoundedCornerShape(shell),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, EmotionalThemeColors.GlassBorder),
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                val corner = CornerRadius(shell.toPx(), shell.toPx())
                drawRoundRect(color = EmotionalThemeColors.DarkBackdrop, cornerRadius = corner)
            }
            .animateContentSize(animationSpec = sizeSpec)
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
                            .clip(RoundedCornerShape(EmotionalMetrics.RadiusControl))
                            .background(EmotionalThemeColors.PanelFill)
                            .border(1.dp, Color(0xFF1E3A5F), RoundedCornerShape(EmotionalMetrics.RadiusControl)),
                        contentAlignment = Alignment.Center
                    ) {
                        InstitutionalBrandMark(size = 16f, strokeColor = EmotionalThemeColors.Primary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "TRADE METRICS",
                            fontFamily = FontFamily.Monospace,
                            fontSize = EmotionalMetrics.TextFloor,
                            fontWeight = FontWeight.Bold,
                            color = EmotionalThemeColors.Primary,
                            letterSpacing = 1.6.sp
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "DESK OVERLAY",
                                fontSize = EmotionalMetrics.TextFloor,
                                fontWeight = FontWeight.Medium,
                                color = EmotionalThemeColors.TextMuted,
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
                                fontSize = EmotionalMetrics.TextFloor,
                                fontWeight = FontWeight.Bold,
                                color = if (desk.lastSyncOk) EmotionalThemeColors.Positive else EmotionalThemeColors.TextHint,
                                letterSpacing = 0.6.sp
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    ChromeIconButton(
                        onClick = onMinimizeToTaskbar,
                        tooltip = "Minimize to taskbar",
                        icon = Icons.Default.Remove,
                        tint = Color.White.copy(alpha = 0.45f)
                    )
                    ChromeIconButton(
                        onClick = onCollapseToBubble,
                        tooltip = "Collapse to bubble",
                        icon = Icons.Default.Minimize,
                        tint = Color.White.copy(alpha = 0.55f)
                    )
                    ChromeIconButton(
                        onClick = onCloseClick,
                        tooltip = "Close overlay",
                        icon = Icons.Default.Close,
                        tint = Color.White.copy(alpha = 0.55f)
                    )
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
                fontSize = EmotionalMetrics.TextFloor,
                fontWeight = FontWeight.Bold,
                color = EmotionalThemeColors.TextEyebrow,
                letterSpacing = 1.4.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            FlatCard {
                MotherSlider(
                    label = "CONFIDENCE",
                    value = state.confidence,
                    onValueChange = { onStateChanged(StateEngine.updateConfidence(state, it)) },
                    range = 1f..10f,
                    accentColor = EmotionalThemeColors.Primary
                )
                Spacer(modifier = Modifier.height(6.dp))
                MotherSlider(
                    label = "FOCUS",
                    value = state.focus,
                    onValueChange = { onStateChanged(StateEngine.updateFocus(state, it)) },
                    range = 1f..10f,
                    accentColor = EmotionalThemeColors.Fomo
                )
                Spacer(modifier = Modifier.height(6.dp))
                MotherSlider(
                    label = "PROBABILITY",
                    value = state.probability,
                    onValueChange = { onStateChanged(StateEngine.updateProbability(state, it)) },
                    range = 0f..100f,
                    accentColor = EmotionalThemeColors.Ego
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(EmotionalThemeColors.Hairline))
            Spacer(modifier = Modifier.height(8.dp))

            val colorSpec = if (reducedMotion) tween<Color>(0) else tween(durationMillis = 200)
            val animatedActiveColor by animateColorAsState(
                targetValue = regimeColor,
                animationSpec = colorSpec,
                label = "ActiveSliderColor"
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, EmotionalThemeColors.Hairline, RoundedCornerShape(EmotionalMetrics.RadiusCard))
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

            Surface(
                onClick = { onSubAxesToggle(!subAxesExpanded) },
                shape = RoundedCornerShape(EmotionalMetrics.RadiusCard),
                color = Color.Transparent,
                border = BorderStroke(1.dp, EmotionalThemeColors.Hairline),
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        role = Role.Button
                        stateDescription = if (subAxesExpanded) "Expanded" else "Collapsed"
                    }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.45f),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (subAxesExpanded) "Hide tilt factors" else "Tilt factors",
                            color = EmotionalThemeColors.TextMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = FontFamily.SansSerif,
                            letterSpacing = 0.4.sp
                        )
                    }
                    val chevronRotation by animateFloatAsState(
                        targetValue = if (subAxesExpanded) 180f else 0f,
                        animationSpec = if (reducedMotion) tween(0) else tween(200, easing = FastOutSlowInEasing),
                        label = "chevron"
                    )
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.4f),
                        modifier = Modifier
                            .size(16.dp)
                            .graphicsLayer { rotationZ = chevronRotation }
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(EmotionalMetrics.RadiusCard))
                    .border(1.dp, EmotionalThemeColors.Hairline, RoundedCornerShape(EmotionalMetrics.RadiusCard))
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    val enterFade = if (reducedMotion) fadeIn(tween(0)) else fadeIn(tween(180, delayMillis = 80))
                    val exitFade = if (reducedMotion) fadeOut(tween(0)) else fadeOut(tween(100))
                    AnimatedVisibility(
                        visible = !subAxesExpanded,
                        enter = enterFade,
                        exit = exitFade
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            ProfileMiniBadge(label = "REV", value = state.revenge, color = EmotionalThemeColors.Revenge)
                            ProfileMiniBadge(label = "FOMO", value = state.fomo, color = EmotionalThemeColors.Fomo)
                            ProfileMiniBadge(label = "DRIFT", value = state.mistake, color = EmotionalThemeColors.Mistake)
                            ProfileMiniBadge(label = "EGO", value = state.ego, color = EmotionalThemeColors.Ego)
                        }
                    }

                    AnimatedVisibility(
                        visible = subAxesExpanded,
                        enter = if (reducedMotion) enterFade else enterFade + expandVertically(
                            spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)
                        ),
                        exit = if (reducedMotion) exitFade else exitFade + shrinkVertically(
                            spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 10.dp, end = 10.dp, top = 10.dp, bottom = 4.dp)
                        ) {
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

            Text(
                "Trade Metrics  ·  session overlay  ·  2026",
                fontSize = EmotionalMetrics.TextFloor,
                color = EmotionalThemeColors.TextHint,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center,
                letterSpacing = 0.4.sp,
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
            )
        }
    }
}

@Composable
private fun FlatCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(EmotionalMetrics.RadiusCard))
            .border(1.dp, EmotionalThemeColors.Hairline, RoundedCornerShape(EmotionalMetrics.RadiusCard))
            .padding(10.dp),
        content = content
    )
}

@Composable
private fun ChromeIconButton(
    onClick: () -> Unit,
    tooltip: String,
    icon: ImageVector,
    tint: Color
) {
    val tooltipState = rememberTooltipState()
    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = {
            PlainTooltip(
                containerColor = EmotionalThemeColors.PanelFill,
                contentColor = Color.White.copy(alpha = 0.88f)
            ) {
                Text(
                    tooltip,
                    fontSize = EmotionalMetrics.TextFloor,
                    fontFamily = FontFamily.Monospace
                )
            }
        },
        state = tooltipState
    ) {
        IconButton(onClick = onClick, modifier = Modifier.size(22.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = tooltip,
                tint = tint,
                modifier = Modifier.size(14.dp)
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
    val awaiting = !desk.lastSyncOk
    val pnl = formatDeskPnl(desk.todayPnl, desk.currencySymbol, desk.zenMode)
    val dash = "—"
    val wrText = when {
        awaiting -> dash
        desk.closedToday == 0 -> dash
        else -> String.format(Locale.US, "%.0f%%", desk.winRateToday)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(EmotionalMetrics.RadiusCard))
                .border(1.dp, EmotionalThemeColors.Hairline, RoundedCornerShape(EmotionalMetrics.RadiusCard))
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.weight(1.15f)) {
                TelemetryCell(
                    label = "SESSION",
                    value = clockText.ifBlank { "--:--:--" },
                    valueColor = Color.White.copy(alpha = 0.82f)
                )
            }
            Box(Modifier.weight(1.55f)) {
                TelemetryCell(
                    label = "TODAY P&L",
                    value = if (awaiting) dash else pnl.text,
                    valueColor = if (awaiting) EmotionalThemeColors.PnlFlat else pnl.color
                )
            }
            Box(Modifier.weight(0.65f)) {
                TelemetryCell(
                    label = "OPEN",
                    value = if (awaiting) dash else desk.openCount.toString(),
                    valueColor = Color.White.copy(alpha = 0.85f)
                )
            }
            Box(Modifier.weight(0.75f)) {
                TelemetryCell(
                    label = "CLOSED",
                    value = if (awaiting) dash else desk.closedToday.toString(),
                    valueColor = Color.White.copy(alpha = 0.85f)
                )
            }
            Box(Modifier.weight(0.6f)) {
                TelemetryCell(label = "WR", value = wrText, valueColor = Color.White.copy(alpha = 0.85f))
            }
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
                        .clip(RoundedCornerShape(EmotionalMetrics.RadiusControl))
                        .background(regimeColor.copy(alpha = 0.12f))
                        .border(1.dp, regimeColor.copy(alpha = 0.35f), RoundedCornerShape(EmotionalMetrics.RadiusControl))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = regimeLabel,
                        fontFamily = FontFamily.Monospace,
                        fontSize = EmotionalMetrics.TextFloor,
                        fontWeight = FontWeight.Bold,
                        color = regimeColor,
                        letterSpacing = 1.2.sp
                    )
                }
                if (desk.watchCount > 0) {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "${desk.watchCount} watch",
                        fontSize = EmotionalMetrics.TextFloor,
                        color = EmotionalThemeColors.TextHint,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
            val limit = desk.dailyLossLimit
            if (!awaiting && limit != null && limit > 0) {
                val used = (kotlin.math.abs(desk.todayPnl.coerceAtMost(0.0)) / limit).toFloat().coerceIn(0f, 1f)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "LOSS LIMIT",
                        fontSize = EmotionalMetrics.TextFloor,
                        color = EmotionalThemeColors.TextEyebrow,
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
                                .background(
                                    if (used > 0.7f) EmotionalThemeColors.Danger else EmotionalThemeColors.Warning
                                )
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
            fontSize = EmotionalMetrics.TextFloor,
            fontWeight = FontWeight.Bold,
            color = EmotionalThemeColors.TextEyebrow,
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
            style = TextStyle(fontFeatureSettings = "tnum"),
            maxLines = 1
        )
    }
}

@Composable
private fun CooldownBanner(untilMs: Long?) {
    var remaining by remember(untilMs) { mutableStateOf(0L) }
    LaunchedEffect(untilMs) {
        while (true) {
            remaining = ((untilMs ?: 0L) - System.currentTimeMillis()).coerceAtLeast(0L)
            delay(250L)
        }
    }
    val h = remaining / 3_600_000
    val m = (remaining % 3_600_000) / 60_000
    val s = (remaining % 60_000) / 1000
    val clock = if (h > 0) String.format(Locale.US, "%02d:%02d:%02d", h, m, s) else String.format(Locale.US, "%02d:%02d", m, s)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(EmotionalMetrics.RadiusCard))
            .background(EmotionalThemeColors.Danger.copy(alpha = 0.12f))
            .border(1.dp, EmotionalThemeColors.Danger.copy(alpha = 0.28f), RoundedCornerShape(EmotionalMetrics.RadiusCard))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "COOLDOWN",
            fontSize = EmotionalMetrics.TextFloor,
            fontWeight = FontWeight.Bold,
            color = EmotionalThemeColors.Danger,
            letterSpacing = 1.1.sp,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = clock,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = EmotionalThemeColors.Danger,
            fontFamily = FontFamily.Monospace,
            style = TextStyle(fontFeatureSettings = "tnum")
        )
    }
}

@Composable
fun ProfileMiniBadge(
    label: String,
    value: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(EmotionalMetrics.RadiusControl))
            .border(1.dp, color.copy(alpha = 0.22f), RoundedCornerShape(EmotionalMetrics.RadiusControl))
            .padding(horizontal = 6.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                fontSize = EmotionalMetrics.TextFloor,
                fontWeight = FontWeight.Bold,
                color = EmotionalThemeColors.TextEyebrow,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = String.format(Locale.US, "%.1f", value),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = color,
                fontFamily = FontFamily.Monospace,
                style = TextStyle(fontFeatureSettings = "tnum")
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
    val reducedMotion = rememberPrefersReducedMotion()
    val valueText = if (range.endInclusive == 100f) "${dragValue.toInt()}%" else String.format(Locale.US, "%.1f", dragValue)
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                fontFamily = FontFamily.SansSerif,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.78f),
                letterSpacing = 0.8.sp
            )
            if (reducedMotion) {
                MetricValue(valueText, accentColor, 14.sp)
            } else {
                AnimatedContent(
                    targetState = valueText,
                    transitionSpec = {
                        (slideInVertically { height -> height } + fadeIn(animationSpec = tween(120)))
                            .togetherWith(slideOutVertically { height -> -height } + fadeOut(animationSpec = tween(80)))
                    }
                ) { targetText ->
                    MetricValue(targetText, accentColor, 14.sp)
                }
            }
        }
        Spacer(modifier = Modifier.height(1.dp))
        Slider(
            value = dragValue,
            onValueChange = { dragValue = it },
            onValueChangeFinished = { onValueChange(dragValue) },
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = accentColor,
                activeTrackColor = accentColor,
                inactiveTrackColor = Color.White.copy(alpha = 0.12f)
            ),
            modifier = Modifier.height(20.dp)
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
    val reducedMotion = rememberPrefersReducedMotion()
    val valueText = String.format(Locale.US, "%.1f", dragValue)
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                fontFamily = FontFamily.SansSerif,
                fontSize = EmotionalMetrics.TextFloor,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.62f),
                letterSpacing = 0.6.sp
            )
            if (reducedMotion) {
                MetricValue(valueText, accentColor, 12.sp)
            } else {
                AnimatedContent(
                    targetState = valueText,
                    transitionSpec = {
                        (slideInVertically { height -> height } + fadeIn(animationSpec = tween(120)))
                            .togetherWith(slideOutVertically { height -> -height } + fadeOut(animationSpec = tween(80)))
                    }
                ) { targetText ->
                    MetricValue(targetText, accentColor, 12.sp)
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(22.dp)
                .clip(RoundedCornerShape(4.dp))
        ) {
            Slider(
                value = dragValue,
                onValueChange = { dragValue = it },
                onValueChangeFinished = { onValueChange(dragValue) },
                valueRange = 1f..10f,
                colors = SliderDefaults.colors(
                    thumbColor = accentColor,
                    activeTrackColor = accentColor,
                    inactiveTrackColor = Color.White.copy(alpha = 0.12f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = (-1).dp)
            )
        }
    }
}

@Composable
private fun MetricValue(text: String, color: Color, size: androidx.compose.ui.unit.TextUnit) {
    Text(
        text = text,
        fontFamily = FontFamily.Monospace,
        fontSize = size,
        fontWeight = FontWeight.Bold,
        color = color,
        style = TextStyle(fontFeatureSettings = "tnum")
    )
}
