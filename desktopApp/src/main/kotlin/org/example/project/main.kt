import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import com.example.widget.DeskSnapshot
import com.example.widget.EmotionalMetrics
import com.example.widget.EmotionalState
import com.example.widget.EmotionalThemeColors
import com.example.widget.Windows11WidgetPanel
import com.example.widget.formatDeskPnl
import com.example.widget.rememberPrefersReducedMotion
import com.example.widget.tiltRegime
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun TradeMetricsBrandMark(
    size: Float = 36f,
    strokeColor: Color = Color.White,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.Canvas(modifier = modifier.size(size.dp)) {
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
private fun LoginEyebrow(text: String) {
    Text(
        text = text,
        fontFamily = FontFamily.Monospace,
        fontSize = EmotionalMetrics.TextFloor,
        fontWeight = FontWeight.Bold,
        color = EmotionalThemeColors.TextEyebrow,
        letterSpacing = 1.4.sp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp)
    )
}

@OptIn(ExperimentalComposeUiApi::class)
fun main() = application {
    var subAxesExpanded by remember { mutableStateOf(true) }
    var isBubbleMode by remember { mutableStateOf(false) }

    var userUid by remember { mutableStateOf<String?>(null) }
    var authToken by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        val saved = com.example.widget.SupabaseAuth.getSavedCredentials()
        if (saved != null) {
            val result = com.example.widget.SupabaseAuth.login(saved.first, saved.second)
            if (result != null) {
                val (uid, token) = result
                com.example.widget.SupabaseSync.bindUser(uid, token)
                authToken = token
                userUid = uid
            }
        }
    }

    LaunchedEffect(userUid) {
        if (userUid != null) {
            while (true) {
                delay(50 * 60 * 1000L)
                val saved = com.example.widget.SupabaseAuth.getSavedCredentials()
                if (saved != null) {
                    val result = com.example.widget.SupabaseAuth.login(saved.first, saved.second)
                    if (result != null) {
                        val (uid, token) = result
                        authToken = token
                        com.example.widget.SupabaseSync.bindUser(uid, token)
                    }
                }
            }
        }
    }

    val reducedMotion = rememberPrefersReducedMotion()
    val targetWidth = if (isBubbleMode) 128.dp else 400.dp
    val targetHeight = when {
        userUid == null -> 430.dp
        isBubbleMode -> 128.dp
        subAxesExpanded -> 640.dp
        else -> 504.dp
    }

    val sizeSpec = if (reducedMotion) {
        tween<Dp>(0)
    } else {
        spring<Dp>(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow)
    }
    val animatedWidth by animateDpAsState(targetValue = targetWidth, animationSpec = sizeSpec)
    val animatedHeight by animateDpAsState(targetValue = targetHeight, animationSpec = sizeSpec)

    val windowState = remember {
        WindowState(size = DpSize(animatedWidth.coerceAtLeast(1.dp), animatedHeight.coerceAtLeast(1.dp)))
    }
    LaunchedEffect(animatedWidth, animatedHeight) {
        windowState.size = DpSize(animatedWidth.coerceAtLeast(1.dp), animatedHeight.coerceAtLeast(1.dp))
    }

    Window(
        onCloseRequest = ::exitApplication,
        state = windowState,
        title = "Trade Metrics Desk",
        transparent = true,
        undecorated = true,
        alwaysOnTop = true,
        icon = androidx.compose.ui.res.painterResource("icon.svg")
    ) {
        if (userUid == null) {
            var email by remember { mutableStateOf("") }
            var password by remember { mutableStateOf("") }
            var isLoading by remember { mutableStateOf(false) }
            var showPassword by remember { mutableStateOf(false) }
            var errorMessage by remember { mutableStateOf<String?>(null) }

            val emailFocus = remember { FocusRequester() }
            val passwordFocus = remember { FocusRequester() }
            val fieldColors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = EmotionalThemeColors.Primary,
                unfocusedBorderColor = Color(0xFF1E293B),
                cursorColor = EmotionalThemeColors.Primary,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White.copy(alpha = 0.85f),
                focusedContainerColor = EmotionalThemeColors.PanelFill,
                unfocusedContainerColor = EmotionalThemeColors.PanelFill,
                focusedPlaceholderColor = EmotionalThemeColors.TextHint,
                unfocusedPlaceholderColor = EmotionalThemeColors.TextHint
            )

            val doLogin: () -> Unit = {
                if (!isLoading && email.isNotBlank() && password.isNotBlank()) {
                    isLoading = true
                    errorMessage = null
                    scope.launch {
                        val authResult = com.example.widget.SupabaseAuth.login(email.trim(), password)
                        if (authResult != null) {
                            val (uid, token) = authResult
                            com.example.widget.SupabaseSync.bindUser(uid, token)
                            authToken = token
                            userUid = uid
                        } else {
                            errorMessage = "Invalid email or password"
                            isLoading = false
                        }
                    }
                }
            }

            LaunchedEffect(Unit) { emailFocus.requestFocus() }

            WindowDraggableArea(modifier = Modifier.fillMaxSize()) {
                val shell = EmotionalMetrics.RadiusShell
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(shell))
                        .background(EmotionalThemeColors.DarkBackdrop)
                        .border(1.dp, EmotionalThemeColors.GlassBorder, RoundedCornerShape(shell)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 22.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(EmotionalMetrics.RadiusCard))
                                .background(EmotionalThemeColors.PanelFill)
                                .border(1.dp, Color(0xFF1E3A5F), RoundedCornerShape(EmotionalMetrics.RadiusCard)),
                            contentAlignment = Alignment.Center
                        ) {
                            TradeMetricsBrandMark(
                                size = 26f,
                                strokeColor = EmotionalThemeColors.Primary,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Spacer(Modifier.height(14.dp))
                        Text(
                            "TRADE METRICS",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmotionalThemeColors.Primary,
                            letterSpacing = 2.sp
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Desk Terminal",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Normal,
                            color = EmotionalThemeColors.TextMuted
                        )

                        Spacer(Modifier.height(18.dp))
                        LoginEyebrow("EMAIL")
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it; errorMessage = null },
                            placeholder = { Text("trader@desk", fontSize = 12.sp) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(onNext = { passwordFocus.requestFocus() }),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(emailFocus)
                                .onKeyEvent { event ->
                                    if (event.key == Key.Tab && event.type == KeyEventType.KeyDown) {
                                        passwordFocus.requestFocus(); true
                                    } else false
                                },
                            textStyle = TextStyle(fontSize = 12.sp, color = Color.White),
                            colors = fieldColors,
                            shape = RoundedCornerShape(EmotionalMetrics.RadiusCard)
                        )

                        Spacer(Modifier.height(10.dp))
                        LoginEyebrow("PASSWORD")
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it; errorMessage = null },
                            placeholder = { Text("••••••••", fontSize = 12.sp) },
                            singleLine = true,
                            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(onDone = { doLogin() }),
                            trailingIcon = {
                                IconButton(onClick = { showPassword = !showPassword }, modifier = Modifier.size(32.dp)) {
                                    Icon(
                                        imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Toggle password",
                                        tint = Color.White.copy(alpha = 0.45f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(passwordFocus)
                                .onKeyEvent { event ->
                                    when {
                                        event.key == Key.Tab && event.type == KeyEventType.KeyDown -> {
                                            emailFocus.requestFocus(); true
                                        }
                                        event.key == Key.Enter && event.type == KeyEventType.KeyDown -> {
                                            doLogin(); true
                                        }
                                        else -> false
                                    }
                                },
                            textStyle = TextStyle(fontSize = 12.sp, color = Color.White),
                            colors = fieldColors,
                            shape = RoundedCornerShape(EmotionalMetrics.RadiusCard)
                        )

                        AnimatedVisibility(visible = errorMessage != null) {
                            Text(
                                text = errorMessage ?: "",
                                fontSize = 11.sp,
                                color = EmotionalThemeColors.Danger,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }

                        Spacer(Modifier.height(16.dp))
                        Button(
                            enabled = !isLoading && email.isNotBlank() && password.isNotBlank(),
                            onClick = doLogin,
                            modifier = Modifier.fillMaxWidth().height(40.dp),
                            shape = RoundedCornerShape(EmotionalMetrics.RadiusCard),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmotionalThemeColors.Primary,
                                contentColor = EmotionalThemeColors.PanelFill,
                                disabledContainerColor = Color(0xFF1E293B),
                                disabledContentColor = Color.White.copy(alpha = 0.25f)
                            )
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    color = EmotionalThemeColors.PanelFill,
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Connecting...", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            } else {
                                Text("Connect Desk Overlay", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            }
                        }

                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Tab switches fields · Enter connects",
                            fontSize = EmotionalMetrics.TextFloor,
                            color = EmotionalThemeColors.TextHint,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Trade Metrics  ·  2026",
                            fontSize = EmotionalMetrics.TextFloor,
                            color = EmotionalThemeColors.TextHint,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            var state by remember { mutableStateOf(EmotionalState()) }
            var desk by remember { mutableStateOf(DeskSnapshot()) }
            var clockText by remember {
                mutableStateOf(SimpleDateFormat("HH:mm:ss", Locale.US).format(Date()))
            }

            LaunchedEffect(state) {
                delay(400)
                com.example.widget.SupabaseSync.pushState(state)
            }

            LaunchedEffect(userUid) {
                while (true) {
                    clockText = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
                    delay(1000)
                }
            }

            LaunchedEffect(userUid) {
                while (true) {
                    desk = com.example.widget.SupabaseSync.fetchDeskSnapshot()
                    com.example.widget.SupabaseSync.pushState(state)
                    delay(20_000L)
                }
            }

            WindowDraggableArea(modifier = Modifier.fillMaxSize()) {
                if (isBubbleMode) {
                    FloatingBubble(
                        state = state,
                        desk = desk,
                        onClick = { isBubbleMode = false }
                    )
                } else {
                    Windows11WidgetPanel(
                        state = state,
                        onStateChanged = { state = it },
                        subAxesExpanded = subAxesExpanded,
                        onSubAxesToggle = { subAxesExpanded = it },
                        onCollapseToBubble = { isBubbleMode = true },
                        onMinimizeToTaskbar = { windowState.isMinimized = true },
                        onCloseClick = ::exitApplication,
                        desk = desk,
                        clockText = clockText
                    )
                }
            }
        }
    }
}

@Composable
fun FloatingBubble(state: EmotionalState, desk: DeskSnapshot, onClick: () -> Unit) {
    val (regimeLabel, activeColor) = tiltRegime(state.active)
    val pnl = formatDeskPnl(desk.todayPnl, desk.currencySymbol, desk.zenMode)
    val pnlText = if (!desk.lastSyncOk) "—" else pnl.text
    val pnlColor = if (!desk.lastSyncOk) EmotionalThemeColors.PnlFlat else pnl.color

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(EmotionalMetrics.RadiusShell))
                .background(EmotionalThemeColors.DarkBackdrop)
                .border(1.dp, activeColor.copy(alpha = 0.45f), RoundedCornerShape(EmotionalMetrics.RadiusShell))
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(EmotionalMetrics.RadiusCard))
                    .clickable { onClick() }
                    .padding(8.dp)
            ) {
                Text(
                    text = regimeLabel,
                    fontFamily = FontFamily.Monospace,
                    fontSize = EmotionalMetrics.TextFloor,
                    fontWeight = FontWeight.Bold,
                    color = activeColor,
                    letterSpacing = 1.1.sp
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = String.format(Locale.US, "%.1f", state.active),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    style = TextStyle(fontFeatureSettings = "tnum")
                )
                Text(
                    text = pnlText,
                    fontFamily = FontFamily.Monospace,
                    fontSize = EmotionalMetrics.TextFloor,
                    fontWeight = FontWeight.Bold,
                    color = pnlColor,
                    style = TextStyle(fontFeatureSettings = "tnum")
                )
            }
        }
    }
}
