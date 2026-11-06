package com.betterx.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.betterx.app.BuildConfig
import com.betterx.app.data.Auth
import com.betterx.app.data.AuthResult
import kotlinx.coroutines.launch

private enum class Mode { SignIn, SignUp, Forgot, Verify }

@Composable
fun AuthScreen() {
    val store = LocalStore.current
    val scope = rememberCoroutineScope()
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    var mode by remember { mutableStateOf(Mode.SignUp) }
    var email by remember { mutableStateOf("") }
    var pw by remember { mutableStateOf("") }
    var pw2 by remember { mutableStateOf("") }
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

    /** Shared handling for sign-in style results. */
    fun handle(r: AuthResult) {
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
                    store.signIn(r.uid, r.email.ifBlank { cleanEmail })
                }
        }
    }

    val canSubmit = !busy && when (mode) {
        Mode.SignIn -> emailOk && pw.length >= 8
        Mode.SignUp -> emailOk && pw.length >= 8 && pw == pw2 && agree
        Mode.Forgot -> emailOk
        Mode.Verify -> true
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Spacer(Modifier.height(24.dp))
        Text("BetterX", style = MaterialTheme.typography.labelLarge, color = BX.Lime)
        Text(
            when (mode) {
                Mode.SignUp -> "Create your\naccount"
                Mode.SignIn -> "Welcome\nback"
                Mode.Forgot -> "Reset your\npassword"
                Mode.Verify -> "Verify your\nemail"
            },
            fontSize = 36.sp,
            lineHeight = 42.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            when (mode) {
                Mode.SignUp -> "Your email is only used to sign in. Your plan and progress stay private on this device."
                Mode.SignIn -> "Sign in to continue your plan."
                Mode.Forgot -> "Enter your email and we'll send you a link to choose a new password."
                Mode.Verify -> "We sent a verification link to $cleanEmail. Open it, then come back here and tap the button below."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = muted,
        )
        Spacer(Modifier.height(4.dp))

        if (mode != Mode.Verify) {
            OutlinedTextField(
                value = email,
                onValueChange = { email = it.take(100); error = null; info = null },
                label = { Text("Email") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (mode == Mode.SignIn || mode == Mode.SignUp) {
            OutlinedTextField(
                value = pw,
                onValueChange = { pw = it.take(64); error = null; info = null },
                label = { Text(if (mode == Mode.SignUp) "Password (8+ characters)" else "Password") },
                singleLine = true,
                visualTransformation = pwVisual,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = { TextButton(onClick = { show = !show }) { Text(if (show) "Hide" else "Show") } },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (mode == Mode.SignUp) {
            OutlinedTextField(
                value = pw2,
                onValueChange = { pw2 = it.take(64); error = null },
                label = { Text("Confirm password") },
                singleLine = true,
                visualTransformation = pwVisual,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                isError = pw2.isNotEmpty() && pw2 != pw,
                supportingText = { if (pw2.isNotEmpty() && pw2 != pw) Text("Passwords don't match") },
                modifier = Modifier.fillMaxWidth(),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = agree, onCheckedChange = { agree = it })
                Text("I agree to the Terms of Use and Privacy Policy.", style = MaterialTheme.typography.bodySmall)
            }
        }
        if (mode == Mode.SignIn) {
            TextButton(onClick = { go(Mode.Forgot) }) { Text("Forgot password?") }
        }

        error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
        info?.let { Text(it, color = BX.Lime, style = MaterialTheme.typography.bodyMedium) }

        PrimaryButton(
            text = when {
                busy -> "Please wait…"
                mode == Mode.SignUp -> "Create account"
                mode == Mode.SignIn -> "Sign in"
                mode == Mode.Forgot -> "Send reset link"
                else -> "I've verified my email"
            },
            enabled = canSubmit,
            onClick = {
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
                                info = "If an account exists for that email, we've sent a reset link. Open it, choose a new password, then sign in."
                            }
                        }
                        Mode.Verify -> handle(Auth.signIn(cleanEmail, pw))
                    }
                    busy = false
                }
            },
        )

        if (mode == Mode.Verify) {
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
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) { Text("Resend verification email") }
        }

        TextButton(
            onClick = { go(if (mode == Mode.SignUp) Mode.SignIn else if (mode == Mode.SignIn) Mode.SignUp else Mode.SignIn) },
            modifier = Modifier.align(Alignment.CenterHorizontally),
        ) {
            Text(
                when (mode) {
                    Mode.SignUp -> "Already have an account? Sign in"
                    Mode.SignIn -> "New here? Create an account"
                    else -> "Back to sign in"
                },
            )
        }

        if (!Auth.configured) {
            SoftCard(container = MaterialTheme.colorScheme.errorContainer) {
                Text("Accounts aren't set up in this build", style = MaterialTheme.typography.titleSmall)
                Text(
                    "Add your FIREBASE_API_KEY (see the README) and rebuild.",
                    style = MaterialTheme.typography.bodySmall,
                )
                if (BuildConfig.TEST_UNLOCK) {
                    OutlinedButton(
                        onClick = { store.signIn("test-user", "test@local") },
                        modifier = Modifier.padding(top = 8.dp),
                    ) { Text("Skip (test builds only)") }
                }
            }
        }
    }
}
