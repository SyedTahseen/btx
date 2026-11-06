package com.betterx.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import com.betterx.app.data.Course
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

private val CARD_H = 168.dp
private val CARD_GAP = 14.dp

private fun courseColor(id: String): Color =
    BX.CardColors[Content.courses.indexOfFirst { it.id == id }.coerceAtLeast(0) % BX.CardColors.size]

/**
 * A colourful program card like the stacked cards in the reference. On first show the cards start
 * piled up and fan out one after another with a spring.
 */
@Composable
private fun ProgramCard(index: Int, course: Course, done: Int, forYou: Boolean, onClick: () -> Unit) {
    val color = courseColor(course.id)
    val ink = inkOn(color)
    val shape = RoundedCornerShape(28.dp)
    val travelPx = with(LocalDensity.current) { (CARD_H + CARD_GAP).toPx() }
    val p = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(index * 80L)
        p.animateTo(1f, spring(dampingRatio = 0.72f, stiffness = 180f))
    }
    val src = remember { MutableInteractionSource() }
    Box(
        Modifier
            .fillMaxWidth()
            .height(CARD_H)
            .graphicsLayer {
                translationY = -(1f - p.value) * travelPx * index * 0.9f
                val sc = 0.92f + 0.08f * p.value
                scaleX = sc
                scaleY = sc
                alpha = (p.value * 1.6f).coerceIn(0f, 1f)
            }
            .pressScale(src, 0.97f)
            .shadow(22.dp, shape, ambientColor = color, spotColor = color)
            .clip(shape)
            .background(color)
            .clickable(interactionSource = src, indication = null, role = Role.Button, onClick = onClick),
    ) {
        // Soft translucent circles, like the card art in the reference.
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(Color.White.copy(alpha = 0.16f), size.height * 0.85f, Offset(size.width * 0.18f, size.height * 1.05f))
            drawCircle(Color.White.copy(alpha = 0.14f), size.height * 0.5f, Offset(size.width * 0.98f, size.height * 0.92f))
        }
        Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(course.title, color = ink, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        course.tagline,
                        color = ink.copy(alpha = 0.72f),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                if (forYou) {
                    Text(
                        "For you",
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .clip(CircleShape)
                            .background(Color(0xCC141517))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }
            Column {
                ThinBar(done.toFloat() / course.lessons.size.coerceAtLeast(1), color = ink, track = ink.copy(alpha = 0.18f))
                Text(
                    "$done/${course.lessons.size} lessons · ${course.exerciseIds.size} practices",
                    color = ink.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

@Composable
fun LearnScreen(onBack: () -> Unit, onCourse: (String) -> Unit) {
    val d = LocalStore.current.data
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val recommended = d.concerns.mapNotNull { Content.concern(it)?.courseId }.distinct()
    val ordered = Content.courses.sortedBy { if (it.id in recommended) 0 else 1 }
    ScrollPage("Programs", onBack) {
        Text("Short lessons plus guided practice. The first ones are picked for your goals.", color = muted)
        ordered.forEachIndexed { i, c ->
            val done = c.lessons.indices.count { "${c.id}:$it" in d.lessonsDone }
            ProgramCard(i, c, done, c.id in recommended) { onCourse(c.id) }
        }
        Spacer(Modifier.height(4.dp))
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
        val banner = courseColor(course.id)
        val ink = inkOn(banner)
        val doneCount = course.lessons.indices.count { "${course.id}:$it" in d.lessonsDone }
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(banner)) {
            Canvas(Modifier.matchParentSize()) {
                drawCircle(Color.White.copy(alpha = 0.16f), size.height * 0.9f, Offset(size.width * 0.12f, size.height * 1.1f))
                drawCircle(Color.White.copy(alpha = 0.14f), size.height * 0.5f, Offset(size.width, size.height * 0.95f))
            }
            Column(Modifier.padding(20.dp)) {
                Text(course.tagline, color = ink, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(14.dp))
                ThinBar(doneCount.toFloat() / course.lessons.size.coerceAtLeast(1), color = ink, track = ink.copy(alpha = 0.18f))
                Text(
                    "$doneCount/${course.lessons.size} lessons",
                    color = ink.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
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

    Box(Modifier.fillMaxSize()) {
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
            SwipeToConfirm(
                label = if (rating > 0) "Swipe right to finish" else "Rate how it felt, then swipe",
                enabled = rating > 0,
                onConfirm = {
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
    if (done) SuccessOverlay("Well done", "${ex.title} · ${ex.minutes} min", onBack)
    }
}
