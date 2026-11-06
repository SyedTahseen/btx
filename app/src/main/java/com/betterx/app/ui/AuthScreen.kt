package com.betterx.app.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.betterx.app.data.Auth
import com.betterx.app.data.AuthResult
import com.betterx.app.data.ProfileSync
import com.betterx.app.data.SyncResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class Mode { SignIn, SignUp, Forgot, Verify }

@Composable
fun AuthScreen() {
    val store = LocalStore.current
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    var mode by remember { mutableStateOf(Mode.SignIn) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var pw by remember { mutableStateOf("") }
    var show by remember { mutableStateOf(false) }
    var agree by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var info by remember { mutableStateOf<String?>(null) }

    val cleanEmail = email.trim()
    val emailOk = android.util.Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()
    val pwVisual = if (show) VisualTransformation.None else PasswordVisualTransformation()

    fun go(m: Mode) {
        mode = m
        error = null
        info = null
    }

    BackHandler(enabled = mode != Mode.SignIn) { go(Mode.SignIn) }

    fun comingSoon(provider: String) {
        Toast.makeText(ctx, "$provider sign-in is coming soon", Toast.LENGTH_SHORT).show()
    }

    /** Shared handling for sign-in style results. */
    suspend fun handle(r: AuthResult) {
        when (r) {
            is AuthResult.Fail -> error = r.message
            is AuthResult.Ok ->
                if (r.needsConfirm) {
                    if (mode == Mode.Verify) {
                        error = "Not verified yet. Open the link in the email we sent you (check your spam folder)."
                    } else {
                        mode = Mode.Verify
                        info = "Please verify your email to continue."
                    }
                } else {
                    // Restore the saved profile (nickname, relationship, goals). If it can't be loaded, sign in anyway.
                    val remote = (ProfileSync.load(r.token, r.uid) as? SyncResult.Ok)?.value
                    store.signIn(r.uid, r.email.ifBlank { cleanEmail }, r.refreshToken, remote)
                    // Brand new account with no saved profile yet: use the name typed on the sign-up screen.
                    if (name.isNotBlank() && store.data.nickname.isBlank()) {
                        store.update { it.copy(nickname = name.trim().take(20)) }
                    }
                }
        }
    }

    val canSubmit = !busy && when (mode) {
        Mode.SignIn -> emailOk && pw.length >= 8
        Mode.SignUp -> emailOk && pw.length >= 8 && agree
        Mode.Forgot -> emailOk
        Mode.Verify -> true
    }

    fun submit() {
        if (!canSubmit) return
        busy = true
        error = null
        info = null
        scope.launch {
            when (mode) {
                Mode.SignIn -> handle(Auth.signIn(cleanEmail, pw))
                Mode.SignUp -> when (val r = Auth.signUp(cleanEmail, pw)) {
                    is AuthResult.Fail -> error = r.message
                    is AuthResult.Ok -> {
                        mode = Mode.Verify
                        info = "Verification email sent."
                    }
                }
                Mode.Forgot -> when (val r = Auth.sendReset(cleanEmail)) {
                    is AuthResult.Fail -> error = r.message
                    is AuthResult.Ok -> {
                        mode = Mode.SignIn
                        pw = ""
                        info = "If an account exists for that email, we've sent a reset link. Open it, choose a new password, then log in."
                    }
                }
                Mode.Verify -> handle(Auth.signIn(cleanEmail, pw))
            }
            busy = false
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val minPageHeight = maxHeight

        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            AnimatedContent(
                targetState = mode,
                transitionSpec = {
                    (fadeIn(tween(260, delayMillis = 70)) + scaleIn(tween(260, delayMillis = 70), initialScale = 0.97f)) togetherWith
                        fadeOut(tween(110))
                },
                label = "authMode",
            ) { m ->
                Column(
                    Modifier.fillMaxWidth().heightIn(min = minPageHeight),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(Modifier.fillMaxWidth()) {
                        // ---------------------------------------------------------------- header
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (m == Mode.SignIn) {
                                Spacer(Modifier.size(44.dp))
                            } else {
                                RoundIconButton(onClick = { go(Mode.SignIn) }, description = "Back") {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color(0xFFB4B5BA), modifier = Modifier.size(22.dp))
                                }
                            }
                            Text(
                                when (m) {
                                    Mode.SignIn -> "Log in"
                                    Mode.SignUp -> "Sign up"
                                    Mode.Forgot -> "Reset password"
                                    Mode.Verify -> "Verify email"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f),
                            )
                            Spacer(Modifier.size(44.dp))
                        }

                        Column(
                            Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Spacer(Modifier.height(32.dp))

                            Text(
                                when (m) {
                                    Mode.SignIn -> "Welcome back!"
                                    Mode.SignUp -> "Create account"
                                    Mode.Forgot -> "Forgot password?"
                                    Mode.Verify -> "Check your inbox"
                                },
                                fontSize = 30.sp,
                                lineHeight = 36.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center,
                                                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                when (m) {
                                    Mode.SignIn -> "Log in and pick up where your plan left off."
                                    Mode.SignUp -> "Start your BetterX journey in a minute."
                                    Mode.Forgot -> "Enter your email and we'll send you a link to choose a new password."
                                    Mode.Verify -> "We sent a verification link to $cleanEmail. Open it, then come back here and tap the button below."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = muted,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 8.dp),
                            )
                            Spacer(Modifier.height(24.dp))

                            // ---------------------------------------------------------------- fields
                            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                if (m == Mode.SignUp) {
                                    AuthField(
                                        value = name,
                                        onValueChange = { name = it.take(20) },
                                        placeholder = "Your name (optional)",
                                        leading = { FieldIcon(Icons.Filled.Person) },
                                        keyboardType = KeyboardType.Text,
                                        imeAction = ImeAction.Next,
                                                                            )
                                }
                                if (m != Mode.Verify) {
                                    AuthField(
                                        value = email,
                                        onValueChange = { email = it.take(100); error = null; info = null },
                                        placeholder = "Email address",
                                        leading = { FieldIcon(Icons.Filled.Email) },
                                        trailing = {
                                            if (emailOk) {
                                                Icon(
                                                    Icons.Filled.CheckCircle,
                                                    contentDescription = "Email looks good",
                                                    tint = BX.Accent,
                                                    modifier = Modifier.size(22.dp),
                                                )
                                            }
                                        },
                                        keyboardType = KeyboardType.Email,
                                        imeAction = if (m == Mode.Forgot) ImeAction.Done else ImeAction.Next,
                                        onDone = { if (m == Mode.Forgot) submit() },
                                                                            )
                                }
                                if (m == Mode.SignIn || m == Mode.SignUp) {
                                    AuthField(
                                        value = pw,
                                        onValueChange = { pw = it.take(64); error = null; info = null },
                                        placeholder = "Password",
                                        leading = { FieldIcon(Icons.Filled.Lock) },
                                        trailing = {
                                            Box(
                                                Modifier
                                                    .size(36.dp)
                                                    .clip(CircleShape)
                                                    .clickable(role = Role.Button, onClickLabel = if (show) "Hide password" else "Show password") { show = !show },
                                                contentAlignment = Alignment.Center,
                                            ) { EyeIcon(open = show, tint = muted) }
                                        },
                                        visualTransformation = pwVisual,
                                        keyboardType = KeyboardType.Password,
                                        imeAction = ImeAction.Done,
                                        onDone = { submit() },
                                                                            )
                                }
                            }

                            // ---------------------------------------------------------------- mode specifics
                            if (m == Mode.SignUp) {
                                Spacer(Modifier.height(12.dp))
                                StrengthMeter(pw, Modifier)
                                Spacer(Modifier.height(14.dp))
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null,
                                            role = Role.Checkbox,
                                        ) { agree = !agree },
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    AuthCheckbox(agree)
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        buildAnnotatedString {
                                            append("I agree to the ")
                                            withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Color.White)) { append("Terms") }
                                            append(" & ")
                                            withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Color.White)) { append("Privacy Policy") }
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = muted,
                                    )
                                }
                            }
                            if (m == Mode.SignIn) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                    TextButton(onClick = { go(Mode.Forgot) }) {
                                        Text("Forgot password?", style = MaterialTheme.typography.labelLarge, color = muted)
                                    }
                                }
                            } else if (m != Mode.SignUp) {
                                Spacer(Modifier.height(8.dp))
                            }

                            error?.let {
                                Spacer(Modifier.height(8.dp))
                                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                            }
                            info?.let {
                                Spacer(Modifier.height(8.dp))
                                Text(it, color = BX.Accent, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                            }

                            Spacer(Modifier.height(if (m == Mode.SignUp) 20.dp else 10.dp))
                            AuthButton(
                                text = when {
                                    busy -> "Please wait…"
                                    m == Mode.SignUp -> "Create account"
                                    m == Mode.SignIn -> "Log in"
                                    m == Mode.Forgot -> "Send reset link"
                                    else -> "I've verified my email"
                                },
                                enabled = canSubmit,
                                showArrow = !busy,
                                onClick = { submit() },
                                                            )

                            if (m == Mode.Verify) {
                                TextButton(
                                    enabled = !busy,
                                    onClick = {
                                        busy = true
                                        error = null
                                        info = null
                                        scope.launch {
                                            when (val r = Auth.resendVerification(cleanEmail, pw)) {
                                                is AuthResult.Fail -> error = r.message
                                                is AuthResult.Ok ->
                                                    if (r.token.isNotEmpty()) handle(r) else info = "A new verification email is on its way."
                                            }
                                            busy = false
                                        }
                                    },
                                ) { Text("Resend verification email") }
                            }

                            // ---------------------------------------------------------------- social
                            if (m == Mode.SignIn || m == Mode.SignUp) {
                                Spacer(Modifier.height(22.dp))
                                Row(
                                    Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    Box(Modifier.weight(1f).height(1.dp).background(BX.CardBorder))
                                    Text("or continue with", style = MaterialTheme.typography.labelMedium, color = muted)
                                    Box(Modifier.weight(1f).height(1.dp).background(BX.CardBorder))
                                }
                                Spacer(Modifier.height(16.dp))
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    SocialButton("Google", onClick = { comingSoon("Google") }) { GoogleIcon(Modifier.size(20.dp)) }
                                    SocialButton("GitHub", onClick = { comingSoon("GitHub") }) { GitHubIcon(Modifier.size(20.dp)) }
                                }
                            }
                        }
                    }

                    // -------------------------------------------------------------------- footer
                    Row(
                        Modifier.fillMaxWidth().padding(top = 28.dp, bottom = 20.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            when (m) {
                                Mode.SignIn -> "Don't have an account? "
                                Mode.SignUp -> "Already have an account? "
                                else -> "Remembered it? "
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = muted,
                        )
                        Text(
                            when (m) {
                                Mode.SignIn -> "Sign up"
                                Mode.SignUp -> "Log in"
                                else -> "Log in"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            textDecoration = TextDecoration.Underline,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable(role = Role.Button) { go(if (m == Mode.SignIn) Mode.SignUp else Mode.SignIn) }
                                .padding(vertical = 6.dp),
                        )
                    }
                }
            }
        }
    }
}

// ============================================================================ pieces

@Composable
private fun RoundIconButton(
    onClick: () -> Unit,
    description: String,
    content: @Composable () -> Unit,
) {
    Box(
        Modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClickLabel = description, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun FieldIcon(icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
}

/** Pill-shaped input on a plain card surface (slightly lighter while focused). */
@Composable
private fun AuthField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leading: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    onDone: () -> Unit = {},
) {
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val fill by animateColorAsState(if (focused) BX.Chip else BX.Card, tween(160), label = "fieldFill")

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        interactionSource = source,
        visualTransformation = visualTransformation,
        textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
        cursorBrush = SolidColor(BX.Mint),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { onDone() }),
        modifier = modifier.fillMaxWidth(),
        decorationBox = { inner ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(CircleShape)
                    .background(fill)
                    .padding(start = 18.dp, end = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                leading()
                Spacer(Modifier.width(12.dp))
                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) {
                        Text(placeholder, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 16.sp, maxLines = 1)
                    }
                    inner()
                }
                if (trailing != null) {
                    Spacer(Modifier.width(8.dp))
                    trailing()
                }
            }
        },
    )
}

@Composable
private fun AuthButton(
    text: String,
    enabled: Boolean,
    showArrow: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val src = remember { MutableInteractionSource() }
    Button(
        onClick = onClick,
        enabled = enabled,
        interactionSource = src,
        modifier = modifier.fillMaxWidth().height(56.dp).pressScale(src),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = BX.Pill,
            contentColor = Color.Black,
            disabledContainerColor = BX.Chip,
            disabledContentColor = Color(0xFF77787D),
        ),
    ) {
        Text(text, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
        if (showArrow) {
            Spacer(Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun RowScope.SocialButton(
    label: String,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
) {
    val src = remember { MutableInteractionSource() }
    Row(
        Modifier
            .weight(1f)
            .height(52.dp)
            .pressScale(src)
            .clip(CircleShape)
            .background(BX.Card)
            .clickable(interactionSource = src, indication = null, role = Role.Button, onClickLabel = "Continue with $label", onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        icon()
        Spacer(Modifier.width(10.dp))
        Text(label, fontWeight = FontWeight.Medium, fontSize = 15.sp)
    }
}

@Composable
private fun AuthCheckbox(checked: Boolean) {
    val bg by animateColorAsState(if (checked) BX.Pill else Color.Transparent, tween(160), label = "cbBg")
    val ring by animateColorAsState(if (checked) BX.Pill else Color(0xFF5A5B60), tween(160), label = "cbRing")
    Box(
        Modifier
            .size(22.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(bg)
            .border(1.5.dp, ring, RoundedCornerShape(7.dp)),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) Icon(Icons.Filled.Check, contentDescription = "Agreed", tint = Color.Black, modifier = Modifier.size(15.dp))
    }
}

// ------------------------------------------------------------------ password strength

private fun passwordScore(p: String): Int {
    if (p.isEmpty()) return 0
    var s = 0
    if (p.length >= 8) s++
    if (p.length >= 12) s++
    if (p.any { it.isLowerCase() } && p.any { it.isUpperCase() }) s++
    if (p.any { it.isDigit() }) s++
    if (p.any { !it.isLetterOrDigit() }) s++
    val capped = s.coerceAtMost(4)
    return if (p.length < 8) capped.coerceAtMost(1) else capped.coerceAtLeast(1)
}

@Composable
private fun StrengthMeter(password: String, modifier: Modifier = Modifier) {
    val score = passwordScore(password)
    val colors = listOf(BX.Coral, BX.Yellow, BX.Blue, BX.Mint)
    val labels = listOf("", "Weak", "Fair", "Good", "Strong")
    val activeColor = if (score == 0) MaterialTheme.colorScheme.onSurfaceVariant else colors[score - 1]
    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(4) { i ->
                val c by animateColorAsState(if (i < score) colors[i] else BX.Chip, tween(250), label = "strength$i")
                Box(Modifier.weight(1f).height(4.dp).clip(CircleShape).background(c))
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.End) {
            Text(
                if (score == 0) "Use 8+ characters" else labels[score],
                style = MaterialTheme.typography.labelSmall,
                color = activeColor,
            )
        }
    }
}

// ------------------------------------------------------------------ icons (drawn, so no icon library is needed)

@Composable
private fun EyeIcon(open: Boolean, tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.size(22.dp)) {
        val w = size.width
        val h = size.height
        val sw = 1.8.dp.toPx()
        val lens = Path().apply {
            moveTo(w * 0.06f, h * 0.5f)
            quadraticBezierTo(w * 0.5f, h * 0.02f, w * 0.94f, h * 0.5f)
            quadraticBezierTo(w * 0.5f, h * 0.98f, w * 0.06f, h * 0.5f)
            close()
        }
        drawPath(lens, tint, style = Stroke(sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawCircle(tint, w * 0.14f, Offset(w * 0.5f, h * 0.5f), style = Stroke(sw))
        if (!open) {
            drawLine(tint, Offset(w * 0.18f, h * 0.86f), Offset(w * 0.82f, h * 0.14f), strokeWidth = sw, cap = StrokeCap.Round)
        }
    }
}

@Composable
private fun GoogleIcon(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val s = size.width
        val stroke = s * 0.21f
        val r = s * 0.5f - stroke / 2f
        val tl = Offset(stroke / 2f, stroke / 2f)
        val box = Size(r * 2f, r * 2f)
        val st = Stroke(stroke)
        drawArc(Color(0xFFEA4335), 200f, 115f, false, tl, box, style = st)
        drawArc(Color(0xFFFBBC05), 140f, 60f, false, tl, box, style = st)
        drawArc(Color(0xFF34A853), 45f, 95f, false, tl, box, style = st)
        drawArc(Color(0xFF4285F4), 0f, 45f, false, tl, box, style = st)
        drawRect(
            Color(0xFF4285F4),
            topLeft = Offset(s * 0.5f, s * 0.5f - stroke / 2f),
            size = Size(s * 0.5f, stroke),
        )
    }
}

private const val GITHUB_MARK =
    "M8 0C3.58 0 0 3.58 0 8c0 3.54 2.29 6.53 5.47 7.59.4.07.55-.17.55-.38 0-.19-.01-.82-.01-1.49" +
        "-2.01.37-2.53-.49-2.69-.94-.09-.23-.48-.94-.82-1.13-.28-.15-.68-.52-.01-.53.63-.01 1.08.58 1.23.82" +
        ".72 1.21 1.87.87 2.33.66.07-.52.28-.87.51-1.07-1.78-.2-3.64-.89-3.64-3.95 0-.87.31-1.59.82-2.15" +
        "-.08-.2-.36-1.02.08-2.12 0 0 .67-.21 2.2.82.64-.18 1.32-.27 2-.27.68 0 1.36.09 2 .27 1.53-1.04 2.2-.82 2.2-.82" +
        ".44 1.1.16 1.92.08 2.12.51.56.82 1.27.82 2.15 0 3.07-1.87 3.75-3.65 3.95.29.25.54.73.54 1.48 0 1.07-.01 1.93-.01 2.2" +
        " 0 .21.15.46.55.38A8.013 8.013 0 0 0 16 8c0-4.42-3.58-8-8-8z"

@Composable
private fun GitHubIcon(modifier: Modifier = Modifier) {
    val path = remember { PathParser().parsePathString(GITHUB_MARK).toPath() }
    Canvas(modifier) {
        val k = size.width / 16f
        withTransform({ scale(k, k, pivot = Offset.Zero) }) {
            drawPath(path, Color.White)
        }
    }
}
