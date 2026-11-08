package com.betterx.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.betterx.app.data.Question
import com.betterx.app.data.Store

val LocalStore = staticCompositionLocalOf<Store> { error("Store not provided") }

// ------------------------------------------------------------------ layout

@Composable
fun ScreenHeader(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    if (onBack == null) {
        // Tab screens: big bold title on the left, actions on the right (like "Cards" + "Order a card" in the reference).
        Row(
            Modifier.fillMaxWidth().padding(start = 24.dp, end = 12.dp, top = 12.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                title,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Row(
                Modifier.heightIn(min = 48.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
                content = actions,
            )
        }
        return
    }
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(48.dp)) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        }
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Row(
            Modifier.widthIn(min = 48.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
            content = actions,
        )
    }
}

/** The app's toggle: mint track when on, quiet grey when off. Use everywhere instead of a bare Switch. */
@Composable
fun BxSwitch(checked: Boolean, onChange: ((Boolean) -> Unit)? = null) {
    Switch(
        checked = checked,
        onCheckedChange = onChange,
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
fun ScrollPage(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    imePadding: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(if (imePadding) Modifier.fillMaxSize().imePadding() else Modifier.fillMaxSize()) {
        ScreenHeader(title, onBack, actions)
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            content = content,
        )
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 6.dp))
}

@Composable
fun SoftCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    container: Color = BX.Card,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = CardDefaults.cardColors(containerColor = container, contentColor = contentColorFor(container))
    val inner: @Composable ColumnScope.() -> Unit = { Column(Modifier.padding(18.dp), content = content) }
    if (onClick != null) {
        val src = remember { MutableInteractionSource() }
        Card(
            onClick = onClick,
            modifier = modifier.fillMaxWidth().pressScale(src, 0.97f),
            shape = MaterialTheme.shapes.large,
            colors = colors,
            interactionSource = src,
            content = inner,
        )
    } else {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = colors,
            content = inner,
        )
    }
}

@Composable
fun Disclaimer() {
    Text(
        "BetterX offers education and coaching, not medical care. If you have pain, bleeding, sudden changes or symptoms lasting more than a few months, please see a clinician.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
fun StatTile(value: String, label: String, modifier: Modifier = Modifier) {
    SoftCard(modifier = modifier) {
        Text(value, style = MaterialTheme.typography.headlineSmall)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ---------------------------------------------------------------- controls

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    arrows: Boolean = false,
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
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text,
                fontWeight = FontWeight.SemiBold,
                fontSize = 17.sp,
                modifier = Modifier.weight(1f).padding(start = 8.dp),
                textAlign = if (arrows) TextAlign.Start else TextAlign.Center,
            )
            if (arrows) {
                Text("›››", fontSize = 18.sp, color = LocalContentColor.current.copy(alpha = 0.55f))
            }
        }
    }
}

@Composable
fun ActionTile(icon: ImageVector, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val src = remember { MutableInteractionSource() }
    Column(
        modifier
            .pressScale(src)
            .clickable(interactionSource = src, indication = null, role = Role.Button, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(64.dp).clip(RoundedCornerShape(22.dp)).background(BX.Card),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = Color.White)
        }
        Spacer(Modifier.height(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

/** Big round 1-5 buttons. Replaces sliders and chip rows. */
@Composable
fun RatingRow(value: Int?, onChange: (Int) -> Unit, low: String? = null, high: String? = null) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            (1..5).forEach { n ->
                val sel = value == n
                val bg by animateColorAsState(if (sel) BX.Pill else BX.Chip, tween(160), label = "ratingBg")
                val scale by animateFloatAsState(if (sel) 1.12f else 1f, spring(dampingRatio = 0.55f, stiffness = 400f), label = "ratingScale")
                Box(
                    Modifier
                        .size(48.dp)
                        .graphicsLayer { scaleX = scale; scaleY = scale }
                        .clip(CircleShape)
                        .background(bg)
                        .clickable(role = Role.RadioButton, onClick = { onChange(n) }),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "$n",
                        color = if (sel) Color.Black else Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                    )
                }
            }
        }
        if (low != null && high != null) {
            Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(low, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(high, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun QuestionCard(q: Question, value: Int?, onChange: (Int) -> Unit) {
    SoftCard {
        Text(q.text, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(14.dp))
        RatingRow(value, onChange, "Not true", "Very true")
    }
}

@Composable
fun ThinBar(
    progress: Float,
    modifier: Modifier = Modifier,
    height: Dp = 6.dp,
    color: Color = BX.Accent,
    track: Color = BX.Chip,
) {
    val p by animateFloatAsState(progress.coerceIn(0f, 1f), tween(600), label = "bar")
    Box(modifier.fillMaxWidth().height(height).clip(CircleShape).background(track)) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(p).clip(CircleShape).background(color))
    }
}

@Composable
fun SegmentedProgress(total: Int, current: Int, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(total) { i ->
            Box(
                Modifier
                    .weight(1f)
                    .height(3.dp)
                    .clip(CircleShape)
                    .background(if (i <= current) BX.Pill else Color(0x33FFFFFF)),
            )
        }
    }
}

// --------------------------------------------------------------- navigation

data class NavItem(val route: String, val label: String, val icon: ImageVector)

/** Floating dark pill. The selected tab grows into a white pill (animated), like the reference. */
@Composable
fun PillNav(items: List<NavItem>, selected: String, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 14.dp), contentAlignment = Alignment.Center) {
        Row(
            Modifier
                .widthIn(max = 320.dp)
                .fillMaxWidth(0.82f)
                .height(64.dp)
                .shadow(18.dp, RoundedCornerShape(32.dp), ambientColor = Color.Black, spotColor = Color.Black)
                .clip(RoundedCornerShape(32.dp))
                .background(BX.Nav)
                .padding(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items.forEach { item ->
                val sel = item.route == selected
                val weight by animateFloatAsState(if (sel) 1.9f else 1f, spring(dampingRatio = 0.8f, stiffness = 380f), label = "navW")
                val bg by animateColorAsState(if (sel) BX.Pill else Color.Transparent, tween(220), label = "navBg")
                val tint by animateColorAsState(if (sel) Color.Black else Color(0xFFB4B5BA), tween(220), label = "navTint")
                val src = remember { MutableInteractionSource() }
                Row(
                    Modifier
                        .weight(weight)
                        .fillMaxHeight()
                        .pressScale(src, 0.92f)
                        .clip(CircleShape)
                        .background(bg)
                        .clickable(interactionSource = src, indication = null, role = Role.Tab, onClickLabel = item.label) {
                            onSelect(item.route)
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(item.icon, contentDescription = item.label, tint = tint, modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}

// ------------------------------------------------------------------- charts

/** Rounded bars on a dark track with a white cap, like a tiny equalizer. */
@Composable
fun PracticeBars(
    values: List<Int>,
    labels: List<String>,
    highlight: Int,
    modifier: Modifier = Modifier,
    track: Color = BX.Track,
    bar: Color = BX.Accent,
    cap: Color = Color.White,
    labelColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    chipBg: Color = Color.White,
    chipInk: Color = Color.Black,
) {
    val max = (values.maxOrNull() ?: 0).coerceAtLeast(1)
    Column(modifier.fillMaxWidth()) {
        Canvas(Modifier.fillMaxWidth().height(110.dp)) {
            val n = values.size.coerceAtLeast(1)
            val gap = if (n > 8) 6.dp.toPx() else 12.dp.toPx()
            val bw = (size.width - gap * (n - 1)) / n
            values.forEachIndexed { i, v ->
                val x = i * (bw + gap)
                drawRoundRect(track, Offset(x, 0f), Size(bw, size.height), CornerRadius(bw / 2))
                if (v > 0) {
                    val h = (size.height * v / max).coerceAtLeast(bw)
                    drawRoundRect(bar, Offset(x, size.height - h), Size(bw, h), CornerRadius(bw / 2))
                    drawCircle(cap, bw / 2 * 0.55f, Offset(x + bw / 2, size.height - h + bw / 2))
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 10.dp)) {
            labels.forEachIndexed { i, l ->
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    val hl = i == highlight
                    Text(
                        l,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (hl) chipInk else labelColor,
                        modifier = if (hl) {
                            Modifier.background(chipBg, CircleShape).padding(horizontal = 8.dp, vertical = 3.dp)
                        } else {
                            Modifier.padding(vertical = 3.dp)
                        },
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------------- misc

fun relTime(ts: Long): String {
    val m = (System.currentTimeMillis() - ts) / 60000
    return when {
        m < 1 -> "just now"
        m < 60 -> "${m}m ago"
        m < 1440 -> "${m / 60}h ago"
        else -> "${m / 1440}d ago"
    }
}

@Composable
fun rememberNotificationRequester(): () -> Unit {
    val ctx = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    return remember {
        val f: () -> Unit = {
            if (Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) {
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        f
    }
}
