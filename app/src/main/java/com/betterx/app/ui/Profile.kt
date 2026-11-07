package com.betterx.app.ui

import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.betterx.app.BuildConfig
import com.betterx.app.data.Auth
import com.betterx.app.data.AuthResult
import com.betterx.app.data.Content
import com.betterx.app.data.ProfileSync
import com.betterx.app.data.RemoteProfile
import com.betterx.app.data.SyncResult
import kotlinx.coroutines.launch

private enum class Page { Main, Edit }

// Fill these in when the pages are ready. While a link is blank the row just says "coming soon".
private const val FAQ_URL = ""
private const val TERMS_URL = ""
private const val PRIVACY_URL = ""

private val Danger = Color(0xFFD64535)
private val IconInk = Color(0xFFD9D9DC)
private val dayNames = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

private fun fmtTime(h: Int, m: Int): String {
    val hh = if (h % 12 == 0) 12 else h % 12
    return "%d:%02d %s".format(hh, m, if (h < 12) "am" else "pm")
}

// ============================================================================ building blocks

/** One rounded card holding several rows, like the grouped lists in the reference. */
@Composable
private fun GroupCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(BX.Card)
            .padding(vertical = 4.dp),
        content = content,
    )
}

@Composable
private fun RowDivider() {
    Box(Modifier.fillMaxWidth().padding(start = 64.dp, end = 16.dp).height(1.dp).background(BX.CardBorder))
}

@Composable
private fun GlyphIcon(icon: ImageVector) {
    Icon(icon, contentDescription = null, tint = IconInk, modifier = Modifier.size(18.dp))
}

@Composable
private fun ClockGlyph() {
    Canvas(Modifier.size(18.dp)) {
        val sw = 1.8.dp.toPx()
        val c = center
        drawCircle(IconInk, radius = size.minDimension / 2f - sw / 2f, style = Stroke(sw))
        drawLine(IconInk, c, Offset(c.x, c.y - size.height * 0.28f), strokeWidth = sw, cap = StrokeCap.Round)
        drawLine(IconInk, c, Offset(c.x + size.width * 0.2f, c.y + size.height * 0.1f), strokeWidth = sw, cap = StrokeCap.Round)
    }
}

@Composable
private fun Chevron() {
    Icon(
        Icons.AutoMirrored.Filled.KeyboardArrowRight,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(22.dp),
    )
}

@Composable
private fun TrailingValue(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(2.dp))
        Chevron()
    }
}

@Composable
private fun BxSwitch(checked: Boolean) {
    Switch(
        checked = checked,
        onCheckedChange = null,
        colors = SwitchDefaults.colors(
            checkedThumbColor = BX.Bg,
            checkedTrackColor = BX.Accent,
            checkedBorderColor = Color.Transparent,
            uncheckedThumbColor = Color(0xFFA0A1A6),
            uncheckedTrackColor = BX.Chip,
            uncheckedBorderColor = Color.Transparent,
        ),
    )
}

@Composable
private fun SettingsRow(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String? = null,
    role: Role = Role.Button,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    val base = Modifier.fillMaxWidth().heightIn(min = 60.dp)
    val mod = if (onClick != null) base.clickable(role = role, onClick = onClick) else base
    Row(mod.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(36.dp).clip(CircleShape).background(BX.Chip), contentAlignment = Alignment.Center) { icon() }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(8.dp))
            trailing()
        }
    }
}

@Composable
private fun ProfileCard(initial: String, name: String, email: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(BX.Card)
            .clickable(role = Role.Button, onClickLabel = "Edit profile", onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(52.dp).clip(CircleShape).background(Brush.linearGradient(listOf(BX.Mint, BX.Blue))),
            contentAlignment = Alignment.Center,
        ) { Text(initial, color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 20.sp) }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                email,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Chevron()
    }
}

@Composable
private fun PlanCard(premium: Boolean, onClick: () -> Unit) {
    val src = remember { MutableInteractionSource() }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(BX.Yellow)
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                if (premium) "BetterX Pro is active" else "Upgrade to BetterX Pro",
                color = Color.Black,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                if (premium) "Thank you for supporting BetterX."
                else "Unlimited coaching, all lessons and your full week plan.",
                color = Color.Black.copy(alpha = 0.65f),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Box(
            Modifier
                .pressScale(src)
                .height(40.dp)
                .clip(CircleShape)
                .background(Color(0xFF141517))
                .clickable(interactionSource = src, indication = null, role = Role.Button, onClick = onClick)
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(if (premium) "Manage" else "Upgrade", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        }
    }
}

/** Pill / rounded input on a plain card surface, a bit lighter while focused. */
@Composable
private fun ProfileField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    singleLine: Boolean = true,
    minHeight: Dp = 56.dp,
    shape: Shape = CircleShape,
) {
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val fill by animateColorAsState(if (focused) BX.Chip else BX.Card, tween(160), label = "profileFieldFill")
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = singleLine,
        maxLines = if (singleLine) 1 else 5,
        interactionSource = source,
        textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
        cursorBrush = SolidColor(BX.Mint),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        modifier = Modifier.fillMaxWidth(),
        decorationBox = { inner ->
            Box(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = minHeight)
                    .clip(shape)
                    .background(fill)
                    .padding(horizontal = 20.dp, vertical = if (singleLine) 0.dp else 16.dp),
                contentAlignment = if (singleLine) Alignment.CenterStart else Alignment.TopStart,
            ) {
                if (value.isEmpty()) {
                    Text(placeholder, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 16.sp)
                }
                inner()
            }
        },
    )
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 6.dp, top = 4.dp),
    )
}

// ============================================================================ screen

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(onBack: () -> Unit, onPaywall: () -> Unit) {
    val store = LocalStore.current
    val d = store.data
    val ctx = LocalContext.current
    val askNotifications = rememberNotificationRequester()
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    var page by remember { mutableStateOf(Page.Main) }
    var showPassword by remember { mutableStateOf(false) }
    var showSignOut by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    var showDay by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Edit-profile draft: saved on this device and in your online profile when you tap Save.
    var nick by remember { mutableStateOf(d.nickname) }
    var rel by remember { mutableStateOf(d.relationship) }
    var about by remember { mutableStateOf(d.about) }
    val goals = remember { mutableStateListOf<String>().apply { addAll(d.concerns) } }
    var saving by remember { mutableStateOf(false) }
    var saveMsg by remember { mutableStateOf<String?>(null) }
    var saveFailed by remember { mutableStateOf(false) }
    val dirty = nick.trim() != d.nickname || about.trim() != d.about || rel != d.relationship || goals.toSet() != d.concerns.toSet()

    fun openEdit() {
        nick = d.nickname
        rel = d.relationship
        about = d.about
        goals.clear()
        goals.addAll(d.concerns)
        saveMsg = null
        page = Page.Edit
    }

    fun save() {
        if (saving) return
        if (nick.isBlank()) {
            saveFailed = true
            saveMsg = "Enter your name."
            return
        }
        if (!dirty) {
            saveFailed = false
            saveMsg = "No changes to save."
            return
        }
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
    }

    fun openLink(url: String) {
        if (url.isBlank()) {
            Toast.makeText(ctx, "Coming soon", Toast.LENGTH_SHORT).show()
        } else {
            try {
                ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            } catch (e: Exception) {
                Toast.makeText(ctx, "Couldn't open the link", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun setDaily(v: Boolean) {
        store.update(reschedule = true) { it.copy(remindersEnabled = v) }
        if (v) askNotifications()
    }

    fun setWeekly(v: Boolean) {
        store.update(reschedule = true) { it.copy(weeklyCheckInEnabled = v) }
        if (v) askNotifications()
    }

    BackHandler(enabled = page != Page.Main) { page = Page.Main }

    AnimatedContent(
        targetState = page,
        transitionSpec = {
            (fadeIn(tween(260, delayMillis = 70)) + scaleIn(tween(260, delayMillis = 70), initialScale = 0.97f)) togetherWith
                fadeOut(tween(110))
        },
        label = "profilePage",
    ) { p ->
        when (p) {
            Page.Main -> ScrollPage("Settings", onBack) {
                val name = d.nickname.trim().ifBlank { "Add your name" }
                ProfileCard(
                    initial = d.nickname.trim().firstOrNull()?.uppercase() ?: "B",
                    name = name,
                    email = d.ownerEmail.ifBlank { "Signed in" },
                    onClick = { openEdit() },
                )

                PlanCard(premium = d.premium, onClick = onPaywall)

                GroupCard {
                    SettingsRow(
                        icon = { GlyphIcon(Icons.Filled.Notifications) },
                        title = "Daily practice reminder",
                        subtitle = "At ${fmtTime(d.reminderHour, d.reminderMinute)}",
                        role = Role.Switch,
                        onClick = { setDaily(!d.remindersEnabled) },
                        trailing = { BxSwitch(d.remindersEnabled) },
                    )
                    RowDivider()
                    SettingsRow(
                        icon = { ClockGlyph() },
                        title = "Reminder time",
                        onClick = {
                            TimePickerDialog(ctx, { _, h, m ->
                                store.update(reschedule = true) { it.copy(reminderHour = h, reminderMinute = m) }
                            }, d.reminderHour, d.reminderMinute, false).show()
                        },
                        trailing = { TrailingValue(fmtTime(d.reminderHour, d.reminderMinute)) },
                    )
                    RowDivider()
                    SettingsRow(
                        icon = { GlyphIcon(Icons.Filled.Favorite) },
                        title = "Weekly relationship check-in",
                        subtitle = "A nudge to talk with your partner",
                        role = Role.Switch,
                        onClick = { setWeekly(!d.weeklyCheckInEnabled) },
                        trailing = { BxSwitch(d.weeklyCheckInEnabled) },
                    )
                    if (d.weeklyCheckInEnabled) {
                        RowDivider()
                        SettingsRow(
                            icon = { GlyphIcon(Icons.Filled.DateRange) },
                            title = "Check-in day",
                            onClick = { showDay = true },
                            trailing = { TrailingValue(dayNames.getOrElse(d.weeklyCheckInDow - 1) { "Monday" }) },
                        )
                    }
                    RowDivider()
                    SettingsRow(
                        icon = { GlyphIcon(Icons.Filled.Lock) },
                        title = "Discreet notifications",
                        subtitle = "Hide details on your lock screen",
                        role = Role.Switch,
                        onClick = { store.update { it.copy(discreetNotifications = !d.discreetNotifications) } },
                        trailing = { BxSwitch(d.discreetNotifications) },
                    )
                }

                GroupCard {
                    SettingsRow(
                        icon = { GlyphIcon(Icons.Filled.Star) },
                        title = "Subscription",
                        onClick = onPaywall,
                        trailing = { TrailingValue(if (d.premium) "Pro" else "Free") },
                    )
                    RowDivider()
                    SettingsRow(
                        icon = { GlyphIcon(Icons.Filled.Settings) },
                        title = "Change password",
                        onClick = { showPassword = true },
                        trailing = { Chevron() },
                    )
                }

                GroupCard {
                    SettingsRow(
                        icon = { GlyphIcon(Icons.Filled.Info) },
                        title = "FAQ",
                        onClick = { openLink(FAQ_URL) },
                        trailing = { Chevron() },
                    )
                    RowDivider()
                    SettingsRow(
                        icon = { GlyphIcon(Icons.Filled.List) },
                        title = "Terms of Service",
                        onClick = { openLink(TERMS_URL) },
                        trailing = { Chevron() },
                    )
                    RowDivider()
                    SettingsRow(
                        icon = { GlyphIcon(Icons.Filled.Lock) },
                        title = "Privacy Policy",
                        onClick = { openLink(PRIVACY_URL) },
                        trailing = { Chevron() },
                    )
                }

                val logoutSrc = remember { MutableInteractionSource() }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .pressScale(logoutSrc)
                        .clip(CircleShape)
                        .background(BX.Pill)
                        .clickable(interactionSource = logoutSrc, indication = null, role = Role.Button) { showSignOut = true },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, tint = Danger, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Log out", color = Danger, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                }

                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TextButton(onClick = { showDelete = true }) {
                        Text("Delete account", color = MaterialTheme.colorScheme.error)
                    }
                }

                Text(
                    "BetterX ${BuildConfig.VERSION_NAME}. Education and coaching only, not medical care.",
                    style = MaterialTheme.typography.bodySmall,
                    color = muted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Page.Edit -> ScrollPage("Edit profile", { page = Page.Main }) {
                FieldLabel("Name")
                ProfileField(nick, { nick = it.take(20); saveMsg = null }, "Your name")

                FieldLabel("About you (optional)")
                ProfileField(
                    about,
                    { about = it.take(200); saveMsg = null },
                    "Anything you'd like to remember about yourself",
                    singleLine = false,
                    minHeight = 110.dp,
                    shape = RoundedCornerShape(24.dp),
                )

                FieldLabel("Relationship")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Single", "Dating", "Partnered").forEach { r ->
                        FilterChip(selected = rel == r, onClick = { rel = r; saveMsg = null }, label = { Text(r) })
                    }
                }

                FieldLabel("Your goals")
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
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (saveFailed) MaterialTheme.colorScheme.error else BX.Accent,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                PrimaryButton(
                    text = if (saving) "Saving…" else "Save changes",
                    onClick = { save() },
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }

    if (showDay) {
        AlertDialog(
            onDismissRequest = { showDay = false },
            title = { Text("Check-in day") },
            text = {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    dayNames.forEachIndexed { i, n ->
                        FilterChip(
                            selected = d.weeklyCheckInDow == i + 1,
                            onClick = { store.update { it.copy(weeklyCheckInDow = i + 1) }; showDay = false },
                            label = { Text(n.take(3)) },
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showDay = false }) { Text("Close") } },
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
private fun PlanFeature(text: String, tint: Color, textColor: Color) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.Top) {
        Icon(Icons.Filled.Check, contentDescription = null, tint = tint, modifier = Modifier.padding(top = 2.dp).size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, color = textColor, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun CurrentTag(onDark: Boolean) {
    Box(
        Modifier
            .clip(CircleShape)
            .background(if (onDark) BX.Chip else Color.Black.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            "Current plan",
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = if (onDark) Color.White else Color.Black,
        )
    }
}

/** Subscription screen: the Free and Pro plans side by side as cards. Payments will be connected later. */
@Composable
fun PaywallScreen(onBack: () -> Unit) {
    val store = LocalStore.current
    val d = store.data
    val ctx = LocalContext.current
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val upgradeSrc = remember { MutableInteractionSource() }

    ScrollPage("Subscription", onBack) {
        // ---------------------------------------------------------------- Free
        Column(
            Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.large)
                .background(BX.Card)
                .padding(20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Free", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                if (!d.premium) CurrentTag(onDark = true)
            }
            Text("Everything you need to get started.", style = MaterialTheme.typography.bodyMedium, color = muted, modifier = Modifier.padding(top = 2.dp, bottom = 10.dp))
            listOf(
                "Your daily plan and check-ins",
                "Progress tracking",
                "The Ghost community",
                "Reminders",
                "5 coach messages a day",
                "The first lesson of every course",
            ).forEach { PlanFeature(it, BX.Accent, Color.White) }
        }

        // ---------------------------------------------------------------- Pro
        Column(
            Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.large)
                .background(BX.Yellow)
                .padding(20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Pro", style = MaterialTheme.typography.titleLarge, color = Color.Black, modifier = Modifier.weight(1f))
                if (d.premium) CurrentTag(onDark = false)
            }
            Text(
                "Go deeper, faster. Everything in Free, plus:",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Black.copy(alpha = 0.65f),
                modifier = Modifier.padding(top = 2.dp, bottom = 10.dp),
            )
            listOf(
                "Unlimited private coaching",
                "Every lesson in every course",
                "Your full week-ahead plan",
                "Priority access to new programs",
            ).forEach { PlanFeature(it, Color.Black, Color.Black) }

            if (!d.premium) {
                Box(
                    Modifier
                        .padding(top = 16.dp)
                        .fillMaxWidth()
                        .height(52.dp)
                        .pressScale(upgradeSrc)
                        .clip(CircleShape)
                        .background(Color(0xFF141517))
                        .clickable(interactionSource = upgradeSrc, indication = null, role = Role.Button) {
                            Toast.makeText(ctx, "Pro purchases are coming soon.", Toast.LENGTH_SHORT).show()
                        },
                    contentAlignment = Alignment.Center,
                ) { Text("Upgrade to Pro", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp) }
            }
        }

        Text(
            "Subscriptions will renew automatically until cancelled in Google Play.",
            style = MaterialTheme.typography.bodySmall,
            color = muted,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
