package com.betterx.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.betterx.app.data.AppData
import com.betterx.app.data.Content
import com.betterx.app.data.Planner
import com.betterx.app.data.ScoreSnapshot
import com.betterx.app.data.today
import kotlin.math.cos
import kotlin.math.sin

/** Abstract hero artwork: orbs, a ring, a four-point star and a loose line. */
@Composable
fun HeroArt(modifier: Modifier = Modifier) {
    Canvas(modifier.fillMaxWidth().height(230.dp)) {
        val w = size.width
        val h = size.height

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF3A3A3A), Color(0xFF0B0B0B)),
                center = Offset(w * 0.12f, h * 0.62f),
                radius = 60.dp.toPx(),
            ),
            radius = 58.dp.toPx(),
            center = Offset(w * 0.12f, h * 0.62f),
        )

        val squiggle = Path().apply {
            moveTo(0f, h * 0.95f)
            cubicTo(w * 0.25f, h * 0.55f, w * 0.55f, h * 1.0f, w, h * 0.7f)
        }
        drawPath(squiggle, Color(0x662CF5CF), style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))

        drawCircle(
            color = Color(0xFF2CF5CF),
            radius = 44.dp.toPx(),
            center = Offset(w * 0.42f, h * 0.64f),
            style = Stroke(7.dp.toPx()),
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFFFF08A), BX.Yellow),
                center = Offset(w * 0.58f, h * 0.36f),
                radius = 60.dp.toPx(),
            ),
            radius = 56.dp.toPx(),
            center = Offset(w * 0.58f, h * 0.36f),
        )

        val cx = w * 0.27f
        val cy = h * 0.50f
        val ro = 34.dp.toPx()
        val ri = 8.dp.toPx()
        val star = Path()
        for (i in 0 until 8) {
            val ang = Math.PI / 4 * i - Math.PI / 2
            val r = if (i % 2 == 0) ro else ri
            val x = cx + (r * cos(ang)).toFloat()
            val y = cy + (r * sin(ang)).toFloat()
            if (i == 0) star.moveTo(x, y) else star.lineTo(x, y)
        }
        star.close()
        drawPath(star, BX.Blue)

        drawCircle(Color.White, 5.dp.toPx(), Offset(w * 0.84f, h * 0.10f))
        drawCircle(Color(0xFF2CF5CF), 8.dp.toPx(), Offset(w * 0.78f, h * 0.20f))
        drawCircle(Color.White, 4.dp.toPx(), Offset(w * 0.06f, h * 0.18f))
    }
}

@Composable
private fun NumDot(n: Int) {
    Box(Modifier.size(28.dp).clip(CircleShape).background(BX.Accent), contentAlignment = Alignment.Center) {
        Text("$n", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen() {
    val store = LocalStore.current
    val askNotifications = rememberNotificationRequester()
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    var step by remember { mutableIntStateOf(0) }
    var adult by remember { mutableStateOf(false) }
    var nick by remember { mutableStateOf("") }
    var rel by remember { mutableStateOf("Single") }
    var remindersOn by remember { mutableStateOf(true) }
    val concerns = remember { mutableStateListOf<String>() }
    val answers = remember { mutableStateMapOf<String, Int>() }

    val pages = remember { Content.questions.chunked(3) }
    val lastStep = 3 + pages.size
    val total = lastStep + 1

    LaunchedEffect(step) { if (step == lastStep) askNotifications() }

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp)) {
                if (step > 0) {
                    IconButton(onClick = { step-- }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            }
            SegmentedProgress(total, step, Modifier.weight(1f))
            Spacer(Modifier.width(40.dp))
        }
        Spacer(Modifier.height(16.dp))

        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when {
                step == 0 -> Column(
                    Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Text("BetterX · sexual wellness", style = MaterialTheme.typography.labelLarge, color = muted)
                    Text(
                        "Understand it.\nPractice it.\nImprove it.",
                        fontSize = 34.sp,
                        lineHeight = 40.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                    HeroArt()
                    Text(
                        "Private by design: no real name, encrypted on this device, screenshots blocked.",
                        style = MaterialTheme.typography.bodySmall,
                        color = muted,
                        textAlign = TextAlign.Center,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = adult, onCheckedChange = { adult = it })
                        Text(
                            "I'm 18 or older, and I understand BetterX offers education and coaching, not medical care.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                step == 1 -> {
                    Text("A little about you", style = MaterialTheme.typography.headlineSmall)
                    OutlinedTextField(
                        value = nick,
                        onValueChange = { nick = it.take(20) },
                        label = { Text("What should we call you? (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text("Relationship", style = MaterialTheme.typography.titleMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Single", "Dating", "Partnered").forEach {
                            FilterChip(selected = rel == it, onClick = { rel = it }, label = { Text(it) })
                        }
                    }
                    Text(
                        "This only decides whether practices with a partner are included.",
                        style = MaterialTheme.typography.bodySmall,
                        color = muted,
                    )
                }
                step == 2 -> {
                    Text("What would you like help with?", style = MaterialTheme.typography.headlineSmall)
                    Text("Choose all that apply.", color = muted)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Content.concerns.forEach { c ->
                            FilterChip(
                                selected = c.id in concerns,
                                onClick = { if (c.id in concerns) concerns.remove(c.id) else concerns.add(c.id) },
                                label = { Text(c.label) },
                            )
                        }
                    }
                }
                step < lastStep -> {
                    val page = pages[step - 3]
                    Text("Quick check", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "How true is each one for you right now? No right answers. (${step - 2} of ${pages.size})",
                        color = muted,
                    )
                    page.forEach { q -> QuestionCard(q, answers[q.id]) { answers[q.id] = it } }
                }
                else -> {
                    val scores = Content.score(answers.toMap())
                    val preview = AppData(
                        onboarded = true, relationship = rel, concerns = concerns.toList(),
                        scores = scores, startDay = today(),
                    )
                    val prio = Planner.priorities(preview, today())
                    val first = Planner.planFor(preview, today())
                    Text("Your plan is ready", style = MaterialTheme.typography.headlineSmall)
                    SoftCard {
                        Text("Your focus", style = MaterialTheme.typography.titleMedium)
                        prio.take(3).forEachIndexed { i, dom ->
                            Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                                NumDot(i + 1)
                                Text("  ${Content.label(dom)}", style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                    SoftCard {
                        Text("Day 1", style = MaterialTheme.typography.titleMedium)
                        first.items.forEach {
                            Text("• ${it.title} (${it.minutes} min)", modifier = Modifier.padding(top = 6.dp))
                        }
                        Text(
                            "Your plan adapts every day from your check-ins.",
                            style = MaterialTheme.typography.bodySmall,
                            color = muted,
                            modifier = Modifier.padding(top = 10.dp),
                        )
                    }
                    if (concerns.any { Content.concern(it)?.medicalNote == true }) {
                        SoftCard(container = MaterialTheme.colorScheme.errorContainer) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Warning, contentDescription = null)
                                Text("  About discomfort or pain", style = MaterialTheme.typography.titleSmall)
                            }
                            Text(
                                "Pain can have medical causes that are very treatable. Please consider seeing a clinician. Stop anything that hurts.",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Daily reminder at 9:00 pm", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "Discreet: nothing sensitive on your lock screen.",
                                style = MaterialTheme.typography.bodySmall,
                                color = muted,
                            )
                        }
                        Switch(checked = remindersOn, onCheckedChange = { remindersOn = it; if (it) askNotifications() })
                    }
                }
            }
        }

        val canNext = when {
            step == 0 -> adult
            step == 2 -> concerns.isNotEmpty()
            step in 3 until lastStep -> pages[step - 3].all { answers.containsKey(it.id) }
            else -> true
        }
        PrimaryButton(
            text = when {
                step == 0 -> "Get started"
                step < lastStep -> "Continue"
                else -> "Start my plan"
            },
            enabled = canNext,
            modifier = Modifier.padding(top = 12.dp),
            onClick = {
                if (step < lastStep) {
                    step++
                } else {
                    val day = today()
                    val scores = Content.score(answers.toMap())
                    store.update(reschedule = true) {
                        it.copy(
                            onboarded = true,
                            nickname = nick.trim(),
                            relationship = rel,
                            concerns = concerns.toList(),
                            answers = answers.toMap(),
                            scores = scores,
                            scoreHistory = listOf(ScoreSnapshot(day, scores)),
                            startDay = day,
                            remindersEnabled = remindersOn,
                        )
                    }
                }
            },
        )
    }
}
