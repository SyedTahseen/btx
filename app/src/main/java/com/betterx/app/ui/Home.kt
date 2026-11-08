package com.betterx.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.betterx.app.data.Content
import com.betterx.app.data.Exercise
import com.betterx.app.data.Planner
import com.betterx.app.data.today
import java.time.LocalDate
import java.util.Locale

@Composable
fun ExerciseRow(ex: Exercise, done: Boolean, onClick: () -> Unit) {
    SoftCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(if (done) BX.Accent else BX.Chip),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (done) Icons.Filled.Check else Icons.Filled.PlayArrow,
                    contentDescription = if (done) "Done" else "Start",
                    tint = if (done) Color.Black else BX.Accent,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(ex.title, style = MaterialTheme.typography.titleSmall)
                Text(
                    "${ex.minutes} min · ${Content.label(ex.domain)}${if (ex.partner) " · with partner" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Colour for an exercise, taken from its domain so the same skill always looks the same. */
private fun domainColor(domain: String): Color =
    BX.CardColors[Content.domains.indexOf(domain).coerceAtLeast(0) % BX.CardColors.size]

internal fun inkOn(c: Color): Color = if (c == BX.Blue || c == BX.Coral || c == BX.Violet) Color.White else Color.Black

/** Yellow, mint and blue cards overlapping at the screen edge: the brand mark from the reference. */
@Composable
private fun CardStack(modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .width(60.dp)
            .height(140.dp)
            .clickable(role = Role.Button, onClickLabel = "Programs", onClick = onClick),
    ) {
        listOf(BX.Yellow to 0, BX.Mint to 18, BX.Blue to 36).forEach { (c, x) ->
            Box(
                Modifier
                    .offset(x = x.dp)
                    .fillMaxHeight()
                    .width(60.dp)
                    .clip(RoundedCornerShape(topStart = 18.dp, bottomStart = 18.dp))
                    .background(c),
            )
        }
    }
}

@Composable
private fun HeroPill(text: String, icon: ImageVector?, primary: Boolean, onClick: () -> Unit) {
    val src = remember { MutableInteractionSource() }
    Row(
        Modifier
            .pressScale(src)
            .height(48.dp)
            .clip(CircleShape)
            .background(if (primary) BX.Pill else BX.Card)
            .then(if (primary) Modifier else Modifier.border(1.dp, Color(0xFF34363A), CircleShape))
            .clickable(interactionSource = src, indication = null, role = Role.Button, onClick = onClick)
            .padding(start = if (icon != null) 9.dp else 18.dp, end = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Box(Modifier.size(30.dp).clip(CircleShape).background(Color(0xFF1B1C1F)), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.width(8.dp))
        }
        Text(text, color = if (primary) Color.Black else Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
    }
}

/** A row in the white sheet: coloured logo-style circle with a status dot, name, and minutes on the right. */
@Composable
private fun PlanRow(ex: Exercise, done: Boolean, index: Int, onClick: () -> Unit) {
    val c = domainColor(ex.domain)
    val src = remember { MutableInteractionSource() }
    Row(
        Modifier
            .fillMaxWidth()
            .pressScale(src, 0.98f)
            .clickable(interactionSource = src, indication = null, role = Role.Button, onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(48.dp).popIn(index)) {
            Box(Modifier.fillMaxSize().clip(CircleShape).background(c), contentAlignment = Alignment.Center) {
                Text(ex.title.take(1).uppercase(), color = inkOn(c), fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(if (done) BX.Mint else BX.Yellow)
                    .border(2.dp, Color.White, CircleShape),
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(ex.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, maxLines = 1)
            Text(
                Content.label(ex.domain) + if (ex.partner) " · partner" else "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            if (done) "Done" else "${ex.minutes} min",
            color = if (done) BX.Positive else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
        )
    }
}

@Composable
private fun SheetButton(text: String, onClick: () -> Unit) {
    val src = remember { MutableInteractionSource() }
    Button(
        onClick = onClick,
        interactionSource = src,
        modifier = Modifier.fillMaxWidth().height(52.dp).pressScale(src),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(containerColor = BX.SheetInk, contentColor = Color.White),
    ) { Text(text, fontWeight = FontWeight.SemiBold, fontSize = 16.sp) }
}

@Composable
private fun SheetChipButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(BX.SheetChip)
            .clickable(role = Role.Button, onClickLabel = description, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, contentDescription = description, tint = BX.SheetMuted, modifier = Modifier.size(20.dp)) }
}

@Composable
fun HomeScreen(
    onExercise: (String) -> Unit,
    onCheckIn: () -> Unit,
    onNotifications: () -> Unit,
    onReassess: () -> Unit,
    onPrograms: () -> Unit,
    onProfile: () -> Unit,
    onPaywall: () -> Unit,
) {
    val d = LocalStore.current.data
    val day = today()
    val plan = Planner.planFor(d, day)
    val week = Planner.week(d, day)
    val doneToday = d.completions.filter { it.day == day }.map { it.exerciseId }.toSet()
    val checkedIn = d.checkIns.any { it.day == day }
    val streak = Planner.streak(d, day)
    var showWeek by remember { mutableStateOf(false) }

    val perDay = (6 downTo 0).map { off -> d.completions.filter { it.day == day - off }.sumOf { it.minutes } }
    val labels = (6 downTo 0).map { off ->
        LocalDate.ofEpochDay(day - off).dayOfWeek.getDisplayName(java.time.format.TextStyle.NARROW, Locale.getDefault())
    }
    val next = plan.items.firstOrNull { it.id !in doneToday } ?: plan.items.first()
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val initial = d.nickname.trim().firstOrNull()?.uppercase() ?: "B"
    val count = animatedCount(streak)
    val hasNew = notificationsFor(d, day).any { it.action != null }

    // The whole screen is one scrolling page: the dark streak area scrolls away (fading and drifting up a little)
    // and the white plan sheet slides up over it.
    val scroll = rememberScrollState()
    val scope = rememberCoroutineScope()
    var topPx by remember { mutableIntStateOf(0) }

    BoxWithConstraints(Modifier.fillMaxSize()) {
    val viewport = maxHeight
    Column(Modifier.fillMaxSize().verticalScroll(scroll)) {
        // ---------------------------------------------------------- dark top area
        run {
          Box(
            Modifier
                .fillMaxWidth()
                .onSizeChanged { topPx = it.height }
                .graphicsLayer {
                    val p = if (topPx > 0) (scroll.value / topPx.toFloat()).coerceIn(0f, 1f) else 0f
                    alpha = 1f - p
                    translationY = scroll.value * 0.4f
                    scaleX = 1f - 0.06f * p
                    scaleY = 1f - 0.06f * p
                }
                // Soft teal-blue glow in the top-right corner, as in the reference.
                .drawBehind {
                    drawRect(
                        Brush.radialGradient(
                            listOf(Color(0x552A9DBA), Color.Transparent),
                            center = Offset(size.width * 0.92f, size.height * 0.2f),
                            radius = size.width * 0.9f,
                        ),
                    )
                },
          ) {
            // The three colour cards peeking in from the edge (tap = Programs).
            CardStack(Modifier.align(Alignment.TopEnd).padding(top = 72.dp), onPrograms)
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(BX.Mint, BX.Blue)))
                            .clickable(role = Role.Button, onClickLabel = "Profile and settings", onClick = onProfile),
                        contentAlignment = Alignment.Center,
                    ) { Text(initial, color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 18.sp) }
                    Spacer(Modifier.weight(1f))
                    Box(
                        Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .clickable(role = Role.Button, onClickLabel = "Notifications", onClick = onNotifications),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.Notifications, contentDescription = "Notifications", tint = Color(0xFFB4B5BA))
                        if (hasNew) {
                            Box(
                                Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(top = 9.dp, end = 10.dp)
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(BX.Yellow),
                            )
                        }
                    }
                }

                Spacer(Modifier.height(26.dp))
                Text(
                    if (d.nickname.isBlank()) "Day streak" else "${d.nickname}'s day streak",
                    style = MaterialTheme.typography.bodyLarge,
                    color = muted,
                )
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("$count", fontSize = 68.sp, fontWeight = FontWeight.SemiBold, lineHeight = 76.sp)
                    Text(
                        if (streak == 1) " day" else " days",
                        fontSize = 30.sp,
                        color = muted,
                        modifier = Modifier.padding(bottom = 12.dp),
                    )
                }
                Text(
                    Planner.insight(d, day),
                    style = MaterialTheme.typography.bodySmall,
                    color = muted,
                    modifier = Modifier.padding(top = 2.dp, end = 56.dp),
                )

                Row(
                    Modifier.padding(top = 18.dp).horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    HeroPill("Practice", Icons.Filled.PlayArrow, true) { onExercise(next.id) }
                    HeroPill("Programs", null, false, onPrograms)
                }

                Spacer(Modifier.height(20.dp))
            }
          }
        }

        // ---------------------------------------------------------- white sheet
        SheetTheme {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .heightIn(min = viewport)
                    .clip(RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp))
                    .background(BX.Sheet),
            ) {
                // Handle: tap to glide between the streak area and the plan.
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(28.dp)
                        .clickable(role = Role.Button, onClickLabel = "Scroll between streak and plan") {
                            scope.launch { scroll.animateScrollTo(if (scroll.value < topPx / 2) topPx else 0) }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Box(Modifier.width(40.dp).height(4.dp).clip(CircleShape).background(Color(0xFFDADADD)))
                }
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SheetChipButton(Icons.AutoMirrored.Filled.List, if (showWeek) "Hide your week" else "See your week") {
                        showWeek = !showWeek
                    }
                    Text(
                        "Today's plan",
                        style = MaterialTheme.typography.titleMedium,
                        color = BX.SheetMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                    Box(
                        Modifier.height(40.dp).clip(CircleShape).background(BX.SheetChip).padding(horizontal = 14.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("${plan.items.sumOf { it.minutes }} min", color = BX.SheetMuted, fontWeight = FontWeight.Medium)
                    }
                }

                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(top = 8.dp, bottom = 130.dp),
                ) {
                    plan.items.forEachIndexed { i, ex -> PlanRow(ex, ex.id in doneToday, i) { onExercise(ex.id) } }

                    if (!checkedIn) {
                        Column(
                            Modifier
                                .padding(top = 12.dp)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(22.dp))
                                .background(BX.SheetChip)
                                .padding(16.dp),
                        ) {
                            Text("How are you feeling today?", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "A one-minute check-in keeps your plan adapting.",
                                style = MaterialTheme.typography.bodySmall,
                                color = BX.SheetMuted,
                                modifier = Modifier.padding(top = 2.dp, bottom = 12.dp),
                            )
                            SheetButton("Check in", onCheckIn)
                        }
                    }

                    Text("Last 7 days · ${perDay.sum()} min", style = MaterialTheme.typography.titleSmall, color = BX.SheetMuted, modifier = Modifier.padding(top = 22.dp, bottom = 12.dp))
                    PracticeBars(
                        perDay, labels, highlight = 6,
                        track = BX.SheetChip, bar = BX.Blue, cap = Color.White,
                        labelColor = BX.SheetMuted, chipBg = BX.SheetInk, chipInk = Color.White,
                    )

                    // Skills (from the assessment) live here now, under the plan.
                    val firstScores = d.scoreHistory.firstOrNull()?.scores ?: d.scores
                    val latestScores = d.scoreHistory.lastOrNull()?.scores ?: d.scores
                    val sinceAssess = d.scoreHistory.lastOrNull()?.let { day - it.day } ?: 0L
                    Text("Your skills", style = MaterialTheme.typography.titleSmall, color = BX.SheetMuted, modifier = Modifier.padding(top = 22.dp))
                    Content.domains.forEach { dom ->
                        val now = latestScores[dom] ?: 50
                        val delta = now - (firstScores[dom] ?: now)
                        Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(Content.label(dom), style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "$now" + if (delta != 0) "  (${if (delta > 0) "+" else ""}$delta)" else "",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (delta >= 0) BX.Positive else MaterialTheme.colorScheme.error,
                            )
                        }
                        ThinBar(now / 100f, Modifier.padding(top = 6.dp), color = BX.Accent, track = BX.SheetChip)
                    }
                    Text(
                        if (sinceAssess >= 14) "It's been $sinceAssess days. Re-assess to see your progress and refresh your plan."
                        else "Re-assess every couple of weeks to see how your skills change.",
                        style = MaterialTheme.typography.bodySmall,
                        color = BX.SheetMuted,
                        modifier = Modifier.padding(top = 14.dp),
                    )
                    OutlinedButton(onClick = onReassess, modifier = Modifier.padding(top = 8.dp)) { Text("Re-assess") }

                    AnimatedVisibility(visible = showWeek) {
                        Column(Modifier.padding(top = 18.dp)) {
                            Text("Your week", style = MaterialTheme.typography.titleSmall, color = BX.SheetMuted)
                            week.forEachIndexed { i, p ->
                                val locked = i >= 2 && !d.premium
                                val dayName = LocalDate.ofEpochDay(p.day).dayOfWeek
                                    .getDisplayName(java.time.format.TextStyle.SHORT, Locale.getDefault())
                                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        if (i == 0) "Today" else dayName,
                                        style = MaterialTheme.typography.labelLarge,
                                        modifier = Modifier.width(56.dp),
                                    )
                                    if (locked) {
                                        Icon(Icons.Filled.Lock, contentDescription = "Locked", modifier = Modifier.size(16.dp), tint = BX.SheetMuted)
                                        Text(
                                            "  Unlock with Pro",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = BX.SheetMuted,
                                            modifier = Modifier.weight(1f),
                                        )
                                        TextButton(onClick = onPaywall) { Text("See") }
                                    } else {
                                        Text(
                                            p.items.joinToString(" + ") { it.title },
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.weight(1f),
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (d.concerns.any { Content.concern(it)?.medicalNote == true }) {
                        Column(
                            Modifier
                                .padding(top = 20.dp)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(22.dp))
                                .background(MaterialTheme.colorScheme.errorContainer)
                                .padding(16.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Warning, contentDescription = null)
                                Text("  Pain can have medical causes", style = MaterialTheme.typography.titleSmall)
                            }
                            Text(
                                "Please consider seeing a clinician. Stop any practice that hurts.",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
    }
}
