package com.betterx.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
                Modifier.size(40.dp).clip(CircleShape).background(if (done) BX.Lime else BX.Chip),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (done) Icons.Filled.Check else Icons.Filled.PlayArrow,
                    contentDescription = if (done) "Done" else "Start",
                    tint = if (done) Color.Black else BX.Lime,
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

@Composable
fun HomeScreen(
    onExercise: (String) -> Unit,
    onCheckIn: () -> Unit,
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

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        // Header
        Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (d.nickname.isBlank()) "Hello!" else "Hello ${d.nickname}!",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
            )
            Box(Modifier.size(44.dp)) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(Brush.verticalGradient(listOf(BX.Olive1, BX.Olive2)))
                        .clickable(role = Role.Button, onClickLabel = "Profile and settings", onClick = onProfile),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(initial, color = BX.OliveInk, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
                if (!checkedIn) {
                    Box(
                        Modifier
                            .align(Alignment.TopEnd)
                            .size(13.dp)
                            .clip(CircleShape)
                            .background(BX.Lime)
                            .border(2.dp, Color.Black, CircleShape),
                    )
                }
            }
        }

        // Hero number
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Day streak", style = MaterialTheme.typography.titleSmall, color = muted)
            Text("$streak", fontSize = 68.sp, fontWeight = FontWeight.Bold)
            Text(
                Planner.insight(d, day),
                style = MaterialTheme.typography.bodySmall,
                color = muted,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
        }

        // Quick actions
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            ActionTile(Icons.Filled.PlayArrow, "Practice", { onExercise(next.id) }, Modifier.weight(1f))
            ActionTile(Icons.Filled.Check, "Check in", onCheckIn, Modifier.weight(1f))
            ActionTile(Icons.AutoMirrored.Filled.List, "Programs", onPrograms, Modifier.weight(1f))
            ActionTile(Icons.Filled.Settings, "More", onProfile, Modifier.weight(1f))
        }

        // Weekly practice chart
        SoftCard {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text("Practice", style = MaterialTheme.typography.labelLarge)
                    Text("${perDay.sum()} min", style = MaterialTheme.typography.headlineSmall)
                }
                Text("Last 7 days", style = MaterialTheme.typography.labelMedium, color = muted)
            }
            Spacer(Modifier.height(14.dp))
            PracticeBars(perDay, labels, highlight = 6)
        }

        // Today's plan
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Today's plan", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text("${plan.items.sumOf { it.minutes }} min", style = MaterialTheme.typography.labelMedium, color = muted)
            }
            plan.items.forEach { ex -> ExerciseRow(ex, ex.id in doneToday) { onExercise(ex.id) } }
        }

        if (!checkedIn) {
            SoftCard {
                Text("How are you feeling today?", style = MaterialTheme.typography.titleSmall)
                Text(
                    "A one-minute check-in keeps your plan adapting.",
                    style = MaterialTheme.typography.bodySmall,
                    color = muted,
                    modifier = Modifier.padding(top = 2.dp, bottom = 12.dp),
                )
                PrimaryButton("Check in", onCheckIn, arrows = false)
            }
        }

        // Week ahead (collapsed by default to keep things calm)
        Column {
            TextButton(onClick = { showWeek = !showWeek }) { Text(if (showWeek) "Hide your week" else "See your week") }
            if (showWeek) {
                SoftCard {
                    week.forEachIndexed { i, p ->
                        val locked = i >= 2 && !d.premium
                        val dayName = LocalDate.ofEpochDay(p.day).dayOfWeek
                            .getDisplayName(java.time.format.TextStyle.SHORT, Locale.getDefault())
                        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                if (i == 0) "Today" else dayName,
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.width(56.dp),
                            )
                            if (locked) {
                                Icon(Icons.Filled.Lock, contentDescription = "Locked", modifier = Modifier.size(16.dp), tint = muted)
                                Text(
                                    "  Unlock with Plus",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = muted,
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
        }

        if (d.concerns.any { Content.concern(it)?.medicalNote == true }) {
            SoftCard(container = MaterialTheme.colorScheme.errorContainer) {
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
        Disclaimer()
    }
}
