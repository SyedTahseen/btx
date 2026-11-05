package com.betterx.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.betterx.app.data.Completion
import com.betterx.app.data.Content
import com.betterx.app.data.Exercise
import com.betterx.app.data.today
import kotlinx.coroutines.delay

@Composable
fun LearnScreen(onBack: () -> Unit, onCourse: (String) -> Unit) {
    val d = LocalStore.current.data
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val recommended = d.concerns.mapNotNull { Content.concern(it)?.courseId }.distinct()
    val ordered = Content.courses.sortedBy { if (it.id in recommended) 0 else 1 }
    ScrollPage("Programs", onBack) {
        Text("Short lessons plus guided practice. The first ones are picked for your goals.", color = muted)
        ordered.forEach { c ->
            val done = c.lessons.indices.count { "${c.id}:$it" in d.lessonsDone }
            SoftCard(onClick = { onCourse(c.id) }) {
                if (c.id in recommended) {
                    Text("For you", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
                Text(c.title, style = MaterialTheme.typography.titleMedium)
                Text(c.tagline, style = MaterialTheme.typography.bodySmall, color = muted)
                Spacer(Modifier.height(10.dp))
                ThinBar(done.toFloat() / c.lessons.size)
                Text(
                    "$done/${c.lessons.size} lessons · ${c.exerciseIds.size} practices",
                    style = MaterialTheme.typography.labelSmall,
                    color = muted,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
        Disclaimer()
    }
}

@Composable
fun CourseScreen(id: String, onBack: () -> Unit, onExercise: (String) -> Unit, onPaywall: () -> Unit) {
    val store = LocalStore.current
    val d = store.data
    val course = Content.course(id)
    if (course == null) {
        ScrollPage("Program", onBack) { Text("This program isn't available.") }
        return
    }
    var expanded by remember { mutableIntStateOf(0) }
    ScrollPage(course.title, onBack) {
        Text(course.tagline, color = MaterialTheme.colorScheme.onSurfaceVariant)
        SectionTitle("Lessons")
        course.lessons.forEachIndexed { i, lesson ->
            val key = "${course.id}:$i"
            val locked = i >= 1 && !d.premium
            val read = key in d.lessonsDone
            SoftCard(onClick = { if (locked) onPaywall() else expanded = if (expanded == i) -1 else i }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${i + 1}. ${lesson.title}", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                    when {
                        locked -> Icon(Icons.Filled.Lock, contentDescription = "Locked", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        read -> Icon(Icons.Filled.CheckCircle, contentDescription = "Read", tint = MaterialTheme.colorScheme.primary)
                    }
                }
                if (expanded == i && !locked) {
                    Spacer(Modifier.height(8.dp))
                    Text(lesson.body, style = MaterialTheme.typography.bodyMedium)
                    if (!read) {
                        Button(
                            onClick = { store.update { it.copy(lessonsDone = it.lessonsDone + key) } },
                            modifier = Modifier.padding(top = 10.dp),
                        ) { Text("Mark as read") }
                    }
                }
            }
        }
        SectionTitle("Practices")
        course.exerciseIds.mapNotNull { Content.exercise(it) }.forEach { ex ->
            val doneToday = d.completions.any { it.day == today() && it.exerciseId == ex.id }
            ExerciseRow(ex, doneToday) { onExercise(ex.id) }
        }
        Disclaimer()
    }
}

@Composable
fun ExerciseScreen(id: String, onBack: () -> Unit) {
    val ex = Content.exercise(id)
    if (ex == null) {
        ScrollPage("Practice", onBack) { Text("This practice isn't available.") }
    } else {
        ExerciseContent(ex, onBack)
    }
}

@Composable
private fun ExerciseContent(ex: Exercise, onBack: () -> Unit) {
    val store = LocalStore.current
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    var remaining by remember { mutableIntStateOf(ex.minutes * 60) }
    var running by remember { mutableStateOf(false) }
    var rating by remember { mutableIntStateOf(0) }
    var done by remember { mutableStateOf(false) }

    LaunchedEffect(running) {
        while (running && remaining > 0) {
            delay(1000)
            remaining -= 1
        }
        if (remaining <= 0) running = false
    }

    ScrollPage(ex.title, onBack) {
        Text(
            "${ex.minutes} min · ${Content.label(ex.domain)}${if (ex.partner) " · with a partner" else ""}",
            color = muted,
        )
        SoftCard(container = MaterialTheme.colorScheme.primaryContainer) {
            Text("Why it helps", style = MaterialTheme.typography.titleSmall)
            Text(ex.why, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
        }
        SectionTitle("Steps")
        SoftCard {
            ex.steps.forEachIndexed { i, s ->
                Text("${i + 1}. $s", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 4.dp))
            }
        }
        if (ex.partner) {
            Text(
                "Best with a partner. Only do it if you both want to, and either of you can stop at any time.",
                style = MaterialTheme.typography.bodySmall,
                color = muted,
            )
        }
        if (!done) {
            SoftCard {
                Text(
                    "%d:%02d".format(remaining / 60, remaining % 60),
                    fontSize = 52.sp,
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                    Button(onClick = { running = !running }) {
                        Text(if (running) "Pause" else if (remaining < ex.minutes * 60) "Resume" else "Start timer")
                    }
                    OutlinedButton(onClick = { running = false; remaining = ex.minutes * 60 }) { Text("Reset") }
                }
            }
            SectionTitle("How did it feel?")
            RatingRow(rating.takeIf { it > 0 }, { rating = it }, "Hard", "Great")
            PrimaryButton(
                "Mark as done",
                enabled = rating > 0,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    store.update { it.copy(completions = it.completions + Completion(today(), ex.id, ex.minutes, rating)) }
                    done = true
                },
            )
        } else {
            SoftCard(container = MaterialTheme.colorScheme.secondaryContainer) {
                Text("Well done.", style = MaterialTheme.typography.titleMedium)
                Text("Every rep builds the skill. Your plan will adapt to how this felt.", style = MaterialTheme.typography.bodyMedium)
            }
            PrimaryButton("Back", onBack, arrows = false)
        }
    }
}
