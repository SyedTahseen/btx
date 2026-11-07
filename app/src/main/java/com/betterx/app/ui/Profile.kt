package com.betterx.app.ui

import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.betterx.app.BuildConfig
import com.betterx.app.data.Auth
import com.betterx.app.data.AuthResult
import com.betterx.app.data.Content
import com.betterx.app.data.ProfileSync
import com.betterx.app.data.RemoteProfile
import com.betterx.app.data.SyncResult
import kotlinx.coroutines.launch

@Composable
private fun SwitchRow(title: String, subtitle: String? = null, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

private fun fmtTime(h: Int, m: Int): String {
    val hh = if (h % 12 == 0) 12 else h % 12
    return "%d:%02d %s".format(hh, m, if (h < 12) "am" else "pm")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(onBack: () -> Unit, onPaywall: () -> Unit) {
    val store = LocalStore.current
    val d = store.data
    val ctx = LocalContext.current
    val askNotifications = rememberNotificationRequester()

    var showPassword by remember { mutableStateOf(false) }
    var showSignOut by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    ScrollPage("Profile & settings", onBack) {
        SectionTitle("Account")
        SoftCard {
            Text(d.ownerEmail.ifBlank { "Signed in" }, style = MaterialTheme.typography.titleSmall)
            Text(
                "Your email and basic profile are saved with your account. Your plan, check-ins and chats stay on this device.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
            OutlinedButton(onClick = { showPassword = true }, modifier = Modifier.padding(top = 10.dp)) {
                Text("Change password")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { showSignOut = true }) { Text("Sign out") }
                OutlinedButton(onClick = { showDelete = true }) {
                    Text("Delete account", color = MaterialTheme.colorScheme.error)
                }
            }
        }

        SectionTitle("About you")
        // Edit a draft, then Save: it is stored on this device and in your online profile.
        var nick by remember { mutableStateOf(d.nickname) }
        var rel by remember { mutableStateOf(d.relationship) }
        var about by remember { mutableStateOf(d.about) }
        val goals = remember { mutableStateListOf<String>().apply { addAll(d.concerns) } }
        var saving by remember { mutableStateOf(false) }
        var saveMsg by remember { mutableStateOf<String?>(null) }
        var saveFailed by remember { mutableStateOf(false) }
        val dirty = nick.trim() != d.nickname || about.trim() != d.about || rel != d.relationship || goals.toSet() != d.concerns.toSet()
        SoftCard {
            OutlinedTextField(
                value = nick,
                onValueChange = { nick = it.take(20); saveMsg = null },
                label = { Text("Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = about,
                onValueChange = { about = it.take(200); saveMsg = null },
                label = { Text("About you (optional)") },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            )
            Text("Relationship", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 12.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Single", "Dating", "Partnered").forEach { r ->
                    FilterChip(selected = rel == r, onClick = { rel = r; saveMsg = null }, label = { Text(r) })
                }
            }
            Text("Your goals", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 12.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Content.concerns.forEach { c ->
                    FilterChip(
                        selected = c.id in goals,
                        onClick = { if (c.id in goals) goals.remove(c.id) else goals.add(c.id); saveMsg = null },
                        label = { Text(c.label) },
                    )
                }
            }
            saveMsg?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (saveFailed) MaterialTheme.colorScheme.error else BX.Accent,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }
            Button(
                enabled = dirty && !saving,
                onClick = {
                    val profile = RemoteProfile(nick.trim(), rel, goals.toList(), about.trim())
                    saving = true
                    saveMsg = null
                    scope.launch {
                        // Always keep it on the device first, then sync online.
                        store.update(reschedule = true) {
                            it.copy(nickname = profile.nickname, about = profile.about, relationship = profile.relationship, concerns = profile.concerns)
                        }
                        nick = profile.nickname
                        about = profile.about
                        val session = store.freshSession()
                        val res: SyncResult<Unit> = when (session) {
                            is AuthResult.Ok -> ProfileSync.save(session.token, session.uid.ifBlank { d.ownerUid }, profile)
                            is AuthResult.Fail -> SyncResult.Fail(session.message)
                        }
                        when (res) {
                            is SyncResult.Ok -> { saveFailed = false; saveMsg = "Saved." }
                            is SyncResult.Fail -> {
                                saveFailed = true
                                saveMsg = "Saved on this device, but not online yet: ${res.message}"
                            }
                        }
                        saving = false
                    }
                },
                modifier = Modifier.padding(top = 10.dp),
            ) { Text(if (saving) "Saving…" else "Save changes") }
        }

        SectionTitle("Reminders")
        SoftCard {
            SwitchRow("Daily practice reminder", "At ${fmtTime(d.reminderHour, d.reminderMinute)}", d.remindersEnabled) { v ->
                store.update(reschedule = true) { it.copy(remindersEnabled = v) }
                if (v) askNotifications()
            }
            OutlinedButton(onClick = {
                TimePickerDialog(ctx, { _, h, m ->
                    store.update(reschedule = true) { it.copy(reminderHour = h, reminderMinute = m) }
                }, d.reminderHour, d.reminderMinute, false).show()
            }) { Text("Change time") }

            SwitchRow("Weekly relationship check-in", "A nudge to talk with your partner", d.weeklyCheckInEnabled) { v ->
                store.update(reschedule = true) { it.copy(weeklyCheckInEnabled = v) }
                if (v) askNotifications()
            }
            if (d.weeklyCheckInEnabled) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEachIndexed { i, n ->
                        FilterChip(
                            selected = d.weeklyCheckInDow == i + 1,
                            onClick = { store.update { it.copy(weeklyCheckInDow = i + 1) } },
                            label = { Text(n) },
                        )
                    }
                }
            }
            SwitchRow(
                "Discreet notifications",
                "Hide details so nothing sensitive shows on your lock screen",
                d.discreetNotifications,
            ) { v -> store.update { it.copy(discreetNotifications = v) } }
        }

        SectionTitle("Subscription")
        SoftCard {
            Text(if (d.premium) "BetterX Plus is active" else "Free plan", style = MaterialTheme.typography.titleSmall)
            Text(
                if (d.premium) "Unlimited coaching, all lessons and your full week plan."
                else "Upgrade for unlimited coaching, all lessons and your full week plan.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = onPaywall, modifier = Modifier.padding(top = 8.dp)) {
                Text(if (d.premium) "Manage" else "See BetterX Plus")
            }
        }

        Text(
            "BetterX ${BuildConfig.VERSION_NAME}. Education and coaching only, not medical care.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    if (showSignOut) {
        AlertDialog(
            onDismissRequest = { showSignOut = false },
            title = { Text("Sign out?") },
            text = { Text("Your plan and progress stay on this device and will be here when you sign back in.") },
            confirmButton = { TextButton(onClick = { showSignOut = false; store.signOut() }) { Text("Sign out") } },
            dismissButton = { TextButton(onClick = { showSignOut = false }) { Text("Cancel") } },
        )
    }

    if (showDelete) {
        var pw by remember { mutableStateOf("") }
        var busy by remember { mutableStateOf(false) }
        var err by remember { mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = { if (!busy) showDelete = false },
            title = { Text("Delete your account?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("This permanently deletes your account, your online profile and everything on this device. Enter your password to confirm.")
                    OutlinedTextField(
                        value = pw,
                        onValueChange = { pw = it.take(64); err = null },
                        label = { Text("Password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        isError = err != null,
                        supportingText = { err?.let { Text(it) } },
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = pw.isNotEmpty() && !busy,
                    onClick = {
                        busy = true
                        err = null
                        scope.launch {
                            when (val auth = Auth.signIn(d.ownerEmail, pw)) {
                                is AuthResult.Fail -> { err = auth.message; busy = false }
                                is AuthResult.Ok -> {
                                    // Remove the online profile first (best effort), then the account itself.
                                    ProfileSync.delete(auth.token, auth.uid.ifBlank { d.ownerUid })
                                    val del = Auth.deleteAccount(auth.token)
                                    if (del is AuthResult.Fail) {
                                        err = del.message
                                        busy = false
                                    } else {
                                        showDelete = false
                                        store.wipeAll()
                                    }
                                }
                            }
                        }
                    },
                ) { Text(if (busy) "Deleting…" else "Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(enabled = !busy, onClick = { showDelete = false }) { Text("Cancel") } },
        )
    }

    if (showPassword) {
        var cur by remember { mutableStateOf("") }
        var next by remember { mutableStateOf("") }
        var again by remember { mutableStateOf("") }
        var busy by remember { mutableStateOf(false) }
        var err by remember { mutableStateOf<String?>(null) }
        val mismatch = again.isNotEmpty() && again != next
        AlertDialog(
            onDismissRequest = { if (!busy) showPassword = false },
            title = { Text("Change password") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = cur,
                        onValueChange = { cur = it.take(64); err = null },
                        label = { Text("Current password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    )
                    OutlinedTextField(
                        value = next,
                        onValueChange = { next = it.take(64); err = null },
                        label = { Text("New password (8+ characters)") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    )
                    OutlinedTextField(
                        value = again,
                        onValueChange = { again = it.take(64); err = null },
                        label = { Text("Confirm new password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        isError = mismatch,
                        supportingText = { if (mismatch) Text("Passwords don't match") },
                    )
                    err?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !busy && cur.isNotEmpty() && next.length >= 8 && next == again && next != cur,
                    onClick = {
                        busy = true
                        err = null
                        scope.launch {
                            when (val r = Auth.changePassword(d.ownerEmail, cur, next)) {
                                is AuthResult.Fail -> { err = r.message; busy = false }
                                is AuthResult.Ok -> {
                                    // The old refresh token stops working after a password change; keep the new one.
                                    if (r.refreshToken.isNotEmpty()) store.update { it.copy(refreshToken = r.refreshToken) }
                                    showPassword = false
                                    Toast.makeText(ctx, "Password updated.", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    },
                ) { Text(if (busy) "Saving…" else "Update") }
            },
            dismissButton = { TextButton(enabled = !busy, onClick = { showPassword = false }) { Text("Cancel") } },
        )
    }
}

@Composable
fun PaywallScreen(onBack: () -> Unit) {
    val store = LocalStore.current
    val d = store.data
    val ctx = LocalContext.current
    ScrollPage("BetterX Plus", onBack) {
        SoftCard(container = MaterialTheme.colorScheme.primaryContainer) {
            Text("Go deeper, faster", style = MaterialTheme.typography.titleLarge)
            Text("Everything in Free, plus:", style = MaterialTheme.typography.bodyMedium)
        }
        SoftCard {
            listOf(
                "Unlimited private coaching",
                "Every lesson in every course",
                "Your full week-ahead plan",
                "Priority access to new programs",
            ).forEach { Text("✓  $it", modifier = Modifier.padding(vertical = 4.dp)) }
        }
        Text(
            "Free includes your daily plan, check-ins, progress tracking, the Ghost community, reminders, and 5 coach messages a day.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (d.premium) {
            Text("You're on BetterX Plus. Thank you.", style = MaterialTheme.typography.titleSmall)
            if (BuildConfig.TEST_UNLOCK) {
                OutlinedButton(onClick = { store.update { it.copy(premium = false) } }) { Text("(test) Switch back to Free") }
            }
        } else {
            Button(
                onClick = {
                    if (BuildConfig.TEST_UNLOCK) {
                        // Test-only shortcut. Set TEST_UNLOCK to "false" in app/build.gradle.kts before shipping.
                        store.update { it.copy(premium = true) }
                        onBack()
                    } else {
                        Toast.makeText(ctx, "Purchases aren't connected yet.", Toast.LENGTH_LONG).show()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Start BetterX Plus") }
            TextButton(onClick = {
                Toast.makeText(ctx, "Restore will check Google Play once billing is connected.", Toast.LENGTH_LONG).show()
            }) { Text("Restore purchases") }
        }
        Text(
            "Subscriptions renew automatically until cancelled in Google Play. Pricing is set in Play Console.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
