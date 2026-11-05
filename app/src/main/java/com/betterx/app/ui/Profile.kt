package com.betterx.app.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.betterx.app.BuildConfig
import com.betterx.app.data.Content
import com.betterx.app.data.ImportantDate
import com.betterx.app.data.Persistence
import com.betterx.app.data.Pin
import java.time.LocalDate
import java.util.UUID

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

    var showPin by remember { mutableStateOf(false) }
    var showWipe by remember { mutableStateOf(false) }
    var showDate by remember { mutableStateOf(false) }

    ScrollPage("Profile & settings", onBack) {
        SectionTitle("About you")
        SoftCard {
            OutlinedTextField(
                value = d.nickname,
                onValueChange = { v -> store.update { it.copy(nickname = v.take(20)) } },
                label = { Text("Nickname") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Text("Relationship", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 12.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Single", "Dating", "Partnered").forEach { r ->
                    FilterChip(selected = d.relationship == r, onClick = { store.update { it.copy(relationship = r) } }, label = { Text(r) })
                }
            }
            Text("Your goals", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 12.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Content.concerns.forEach { c ->
                    FilterChip(
                        selected = c.id in d.concerns,
                        onClick = {
                            store.update {
                                it.copy(concerns = if (c.id in it.concerns) it.concerns - c.id else it.concerns + c.id)
                            }
                        },
                        label = { Text(c.label) },
                    )
                }
            }
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

        SectionTitle("Important dates")
        SoftCard {
            if (d.dates.isEmpty()) {
                Text(
                    "Anniversaries, a partner's birthday, a date you want to plan for. We'll remind you the day before and on the day.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            d.dates.forEach { imp ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(imp.label, style = MaterialTheme.typography.titleSmall)
                        Text(
                            LocalDate.ofEpochDay(imp.day).toString() + if (imp.yearly) " · every year" else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = { store.update(reschedule = true) { it.copy(dates = it.dates.filter { x -> x.id != imp.id }) } }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Remove")
                    }
                }
            }
            OutlinedButton(onClick = { showDate = true }, modifier = Modifier.padding(top = 8.dp)) { Text("Add a date") }
        }

        SectionTitle("Privacy & security")
        SoftCard {
            Text(
                "Your data is encrypted on this device, screenshots are blocked, and nothing is backed up to the cloud.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SwitchRow("PIN lock", "Ask for a PIN whenever you open the app", d.pinHash.isNotEmpty()) { v ->
                if (v) showPin = true else store.update { it.copy(pinHash = "", pinSalt = "") }
            }
            OutlinedButton(onClick = {
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, Persistence.export(d))
                }
                ctx.startActivity(Intent.createChooser(send, "Export my data (unencrypted)"))
            }, modifier = Modifier.padding(top = 8.dp)) { Text("Export my data") }
            OutlinedButton(onClick = { showWipe = true }, modifier = Modifier.padding(top = 8.dp)) {
                Text("Delete all my data", color = MaterialTheme.colorScheme.error)
            }
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

    if (showPin) {
        var pin by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showPin = false },
            title = { Text("Set a PIN") },
            text = {
                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = it.filter(Char::isDigit).take(6) },
                    label = { Text("4 to 6 digits") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                )
            },
            confirmButton = {
                TextButton(enabled = pin.length >= 4, onClick = {
                    val salt = Pin.newSalt()
                    store.update { it.copy(pinSalt = salt, pinHash = Pin.hash(pin, salt)) }
                    store.unlocked = true
                    showPin = false
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showPin = false }) { Text("Cancel") } },
        )
    }

    if (showWipe) {
        AlertDialog(
            onDismissRequest = { showWipe = false },
            title = { Text("Delete everything?") },
            text = { Text("This permanently erases your profile, plan history, check-ins, chats and posts from this device. It can't be undone.") },
            confirmButton = {
                TextButton(onClick = { showWipe = false; store.wipe() }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { showWipe = false }) { Text("Cancel") } },
        )
    }

    if (showDate) {
        var label by remember { mutableStateOf("") }
        var day by remember { mutableStateOf<Long?>(null) }
        var yearly by remember { mutableStateOf(true) }
        AlertDialog(
            onDismissRequest = { showDate = false },
            title = { Text("Add an important date") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = label,
                        onValueChange = { label = it.take(30) },
                        label = { Text("Label (e.g. Anniversary)") },
                        singleLine = true,
                    )
                    OutlinedButton(onClick = {
                        val c = LocalDate.now()
                        DatePickerDialog(ctx, { _, y, m, dd -> day = LocalDate.of(y, m + 1, dd).toEpochDay() }, c.year, c.monthValue - 1, c.dayOfMonth).show()
                    }) { Text(day?.let { LocalDate.ofEpochDay(it).toString() } ?: "Pick a date") }
                    SwitchRow("Repeat every year", null, yearly) { yearly = it }
                }
            },
            confirmButton = {
                TextButton(enabled = label.isNotBlank() && day != null, onClick = {
                    val imp = ImportantDate(UUID.randomUUID().toString(), label.trim(), day!!, yearly)
                    store.update(reschedule = true) { it.copy(dates = it.dates + imp) }
                    askNotifications()
                    showDate = false
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showDate = false }) { Text("Cancel") } },
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
            if (BuildConfig.DEBUG) {
                OutlinedButton(onClick = { store.update { it.copy(premium = false) } }) { Text("(debug) Switch back to Free") }
            }
        } else {
            Button(
                onClick = {
                    if (BuildConfig.DEBUG) {
                        // Debug-only shortcut so you can test the Plus experience.
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
