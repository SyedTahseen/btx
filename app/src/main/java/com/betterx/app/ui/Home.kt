package com.betterx.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
            Icon(
                if (done) Icons.Filled.CheckCircle else Icons.Filled.PlayArrow,
                contentDescription = if (done) "Done" else "Start",
                tint = if (done) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(ex.title, style = MaterialTheme.typography.titleSmall)
                Text(
                    "${ex.minutes} min · ${Content.label(ex.domain)}${if (ex.partner) " · with partner" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun HomeScreen(
    onExercise: (String) -> Unit,
    onCheckIn: () -> Unit,
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
    val last7 = d.completions.filter { it.day > day - 7 }.map { it.day }.distinct().size
    val totalMin = d.completions.sumOf { it.minutes }

    ScrollPage(
        title = if (d.nickname.isBlank()) "Today" else "Hi, ${d.nickname}",
        actions = {
            IconButton(onClick = onProfile) { Icon(Icons.Filled.Settings, contentDescription = "Profile and settings") }
        },
    ) {
        SoftCard(container = MaterialTheme.colorScheme.primaryContainer) {
            Text("Your plan, adapted", style = MaterialTheme.typography.titleMedium)
            Text(Planner.insight(d, day), style = MaterialTheme.typography.bodyMedium)
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

        SectionTitle("Today's practice · ${plan.items.sumOf { it.minutes }} min")
        plan.items.forEach { ex -> ExerciseRow(ex, ex.id in doneToday) { onExercise(ex.id) } }

        SectionTitle("Check-in")
        SoftCard {
            if (checkedIn) {
                Text("You've checked in today. Nice.", style = MaterialTheme.typography.bodyMedium)
            } else {
                Text(
                    "One minute to tell us how you're feeling. It's how your plan adapts.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Button(onClick = onCheckIn, modifier = Modifier.padding(top = 8.dp)) { Text("Check in") }
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatTile("$streak", "day streak", Modifier.weight(1f))
            StatTile("$last7/7", "days this week", Modifier.weight(1f))
            StatTile("$totalMin", "minutes total", Modifier.weight(1f))
        }

        SectionTitle("This week")
        SoftCard {
            week.forEachIndexed { i, p ->
                val locked = i >= 2 && !d.premium
                val name = LocalDate.ofEpochDay(p.day).dayOfWeek
                    .getDisplayName(java.time.format.TextStyle.SHORT, Locale.getDefault())
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        if (i == 0) "Today" else name,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.width(56.dp),
                    )
                    if (locked) {
                        Icon(Icons.Filled.Lock, contentDescription = "Locked", modifier = Modifier.padding(end = 6.dp))
                        Text(
                            "Unlock with Plus",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f),
                        )
                        androidx.compose.material3.TextButton(onClick = onPaywall) { Text("See") }
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
        Disclaimer()
    }
}
