import androidx.compose.material.icons.filled.Add
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.*
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import com.example.widget.DeskSnapshot
import com.example.widget.EmotionalState
import com.example.widget.Windows11WidgetPanel
import com.example.widget.tiltRegime
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ── Brand Logo: Trade Metrics "broken frame + T" mark ──
@Composable
fun TradeMetricsBrandMark(
    size: Float = 36f,
    strokeColor: Color = Color.White,
    modifier: Modifier = Modifier
) {
    val s = size
    androidx.compose.foundation.Canvas(modifier = modifier.size(s.dp)) {
        val w = this.size.width
        val h = this.size.height
        val strokePx = (1.5f / 32f) * w
        val style = Stroke(width = strokePx, cap = androidx.compose.ui.graphics.StrokeCap.Square)
        val c = strokeColor

        // Broken frame: short stub top-left, full top, full right, bottom stops short
        drawLine(c, Offset(w * 4/32f, h * 4/32f), Offset(w * 4/32f, h * 9/32f), strokePx)      // left stub
        drawLine(c, Offset(w * 4/32f, h * 4/32f), Offset(w * 28/32f, h * 4/32f), strokePx)     // top edge
        drawLine(c, Offset(w * 28/32f, h * 4/32f), Offset(w * 28/32f, h * 28/32f), strokePx)   // right edge
        drawLine(c, Offset(w * 28/32f, h * 28/32f), Offset(w * 12/32f, h * 28/32f), strokePx)  // bottom partial

        // Serif T, centered
        drawLine(c, Offset(w * 12/32f, h * 12/32f), Offset(w * 20/32f, h * 12/32f), strokePx)  // T crossbar
        drawLine(c, Offset(w * 16/32f, h * 12/32f), Offset(w * 16/32f, h * 22/32f), strokePx)  // T stem
    }
}

@OptIn(ExperimentalComposeUiApi::class)
fun main() = application {
    var subAxesExpanded by remember { mutableStateOf(true) }
    var isBubbleMode by remember { mutableStateOf(false) }

    // Auth state: null = not logged in, non-null = uid
    var userUid by remember { mutableStateOf<String?>(null) }
    var authToken by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    // ── On startup: attempt auto-login with saved credentials ──
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

    // ── Token refresh: silently re-authenticate every 50 minutes ──
    LaunchedEffect(userUid) {
        if (userUid != null) {
            while (true) {
                delay(50 * 60 * 1000L) // 50 minutes
                val saved = com.example.widget.SupabaseAuth.getSavedCredentials()
                if (saved != null) {
                    val result = com.example.widget.SupabaseAuth.login(saved.first, saved.second)
                    if (result != null) {
                        val (uid, token) = result
                        authToken = token
                        com.example.widget.SupabaseSync.bindUser(uid, token)
                        println("[Auth] Token silently refreshed.")
                    }
                }
            }
        }
    }

    // Dynamic window size
    val targetWidth = if (isBubbleMode) 128.dp else 400.dp
    val targetHeight = when {
        userUid == null -> 390.dp
        isBubbleMode -> 128.dp
        subAxesExpanded -> 632.dp
        else -> 496.dp
    }

    val animatedWidth by animateDpAsState(targetValue = targetWidth,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow))
    val animatedHeight by animateDpAsState(targetValue = targetHeight,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow))

    val windowState = remember { WindowState(size = DpSize(animatedWidth.coerceAtLeast(1.dp), animatedHeight.coerceAtLeast(1.dp))) }
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
            // ── PREMIUM LOGIN SCREEN ──────────────────────────────
            var email by remember { mutableStateOf("") }
            var password by remember { mutableStateOf("") }
            var isLoading by remember { mutableStateOf(false) }
            var showPassword by remember { mutableStateOf(false) }
            var errorMessage by remember { mutableStateOf<String?>(null) }

            val emailFocus = remember { FocusRequester() }
            val passwordFocus = remember { FocusRequester() }
            val focusManager = LocalFocusManager.current

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

            // Auto-focus email on load
            LaunchedEffect(Unit) { emailFocus.requestFocus() }

            WindowDraggableArea(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xF00D1117))
                        .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(20.dp))
                        // Rim light on top
                        .drawWithContent {
                            drawContent()
                            drawLine(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color(0x25FFFFFF),
                                        Color(0x25FFFFFF),
                                        Color.Transparent
                                    )
                                ),
                                start = Offset(24f, 0f),
                                end = Offset(size.width - 24f, 0f),
                                strokeWidth = 1.dp.toPx()
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(0.dp),
                        modifier = Modifier.padding(28.dp)
                    ) {
                        // ── Brand Mark ──────────────────────────
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF0F1923))
                                .border(1.dp, Color(0xFF1E3A5F), RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            TradeMetricsBrandMark(
                                size = 28f,
                                strokeColor = Color(0xFF60A5FA),
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Spacer(Modifier.height(16.dp))

                        // ── Title ──────────────────────────────
                        Text(
                            "TRADE METRICS",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF60A5FA),
                            letterSpacing = 2.sp
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Desk Terminal",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color.White.copy(alpha = 0.45f)
                        )

                        Spacer(Modifier.height(24.dp))

                        // ── Email Field ──────────────────────────
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it; errorMessage = null },
                            label = { Text("Email", fontSize = 12.sp) },
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
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF60A5FA),
                                unfocusedBorderColor = Color(0xFF1E293B),
                                focusedLabelColor = Color(0xFF60A5FA),
                                unfocusedLabelColor = Color.White.copy(alpha = 0.35f),
                                cursorColor = Color(0xFF60A5FA),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White.copy(alpha = 0.85f),
                                focusedContainerColor = Color(0xFF0D1117),
                                unfocusedContainerColor = Color(0xFF0D1117)
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )

                        Spacer(Modifier.height(10.dp))

                        // ── Password Field ────────────────────────
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it; errorMessage = null },
                            label = { Text("Password", fontSize = 12.sp) },
                            singleLine = true,
                            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(onDone = { doLogin() }),
                            trailingIcon = {
                                IconButton(onClick = { showPassword = !showPassword }, modifier = Modifier.size(36.dp)) {
                                    Icon(
                                        imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Toggle password",
                                        tint = Color.White.copy(alpha = 0.4f),
                                        modifier = Modifier.size(18.dp)
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
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF60A5FA),
                                unfocusedBorderColor = Color(0xFF1E293B),
                                focusedLabelColor = Color(0xFF60A5FA),
                                unfocusedLabelColor = Color.White.copy(alpha = 0.35f),
                                cursorColor = Color(0xFF60A5FA),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White.copy(alpha = 0.85f),
                                focusedContainerColor = Color(0xFF0D1117),
                                unfocusedContainerColor = Color(0xFF0D1117)
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )

                        // ── Error message ────────────────────────
                        AnimatedVisibility(visible = errorMessage != null) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = errorMessage ?: "",
                                fontSize = 11.sp,
                                color = Color(0xFFF87171),
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Spacer(Modifier.height(18.dp))

                        // ── Login Button ─────────────────────────
                        Button(
                            enabled = !isLoading && email.isNotBlank() && password.isNotBlank(),
                            onClick = doLogin,
                            modifier = Modifier.fillMaxWidth().height(44.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF60A5FA),
                                contentColor = Color(0xFF0D1117),
                                disabledContainerColor = Color(0xFF1E293B),
                                disabledContentColor = Color.White.copy(alpha = 0.25f)
                            )
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    color = Color(0xFF0D1117),
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
                            "Press Tab to switch fields · Enter to login",
                            fontSize = 9.sp,
                            color = Color.White.copy(alpha = 0.2f),
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "A product of Trade Metrics\nSince 2026",
                            fontSize = 8.sp,
                            color = Color.White.copy(alpha = 0.15f),
                            fontFamily = FontFamily.Monospace,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        } else {
            // ── THE WIDGET ────────────────────────────────────────
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


// ── The Beautiful Glowing Bubble UI ──────────────────────────────────────
@Composable
fun FloatingBubble(state: EmotionalState, desk: DeskSnapshot, onClick: () -> Unit) {
    val (regimeLabel, activeColor) = tiltRegime(state.active)
    val pnlColor = when {
        desk.todayPnl > 0 -> Color(0xFF34D399)
        desk.todayPnl < 0 -> Color(0xFFF87171)
        else -> Color.White.copy(alpha = 0.7f)
    }
    val pnlText = when {
        desk.zenMode -> "***"
        else -> String.format("%+.0f", desk.todayPnl)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xF20B1017))
                .border(1.dp, activeColor.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onClick() }
                    .padding(8.dp)
            ) {
                Text(
                    text = regimeLabel,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = activeColor,
                    letterSpacing = 1.1.sp
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = String.format("%.1f", state.active),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = pnlText,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = pnlColor
                )
            }
        }
    }
}
