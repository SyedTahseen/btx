package com.betterx.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.betterx.app.data.AppData
import com.betterx.app.data.Content
import com.betterx.app.data.Planner
import com.betterx.app.data.ScoreSnapshot
import com.betterx.app.data.today

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen() {
    val store = LocalStore.current
    val askNotifications = rememberNotificationRequester()

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

    Column(Modifier.fillMaxSize().padding(20.dp)) {
        LinearProgressIndicator(progress = { (step + 1f) / total }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(16.dp))

        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            when {
                step == 0 -> {
                    Text("BetterX", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary)
                    Text(
                        "Tell us what's going on. We'll understand it, build a personal plan, help you practice, track your progress and adapt.",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text("Private by design", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "• No real name needed\n• Your data is encrypted on this device\n• Screenshots are blocked\n• Optional PIN lock and discreet notifications",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = adult, onCheckedChange = { adult = it })
                        Text(
                            "I'm 18 or older, and I understand BetterX provides education and coaching, not medical care.",
                            style = MaterialTheme.typography.bodyMedium,
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
                    Text("Relationship status", style = MaterialTheme.typography.titleMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Single", "Dating", "Partnered").forEach {
                            FilterChip(selected = rel == it, onClick = { rel = it }, label = { Text(it) })
                        }
                    }
                    Text(
                        "This only decides whether practices that involve a partner are included.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                step == 2 -> {
                    Text("What would you like help with?", style = MaterialTheme.typography.headlineSmall)
                    Text("Choose all that apply.", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                    Text("Quick assessment", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "How true is each statement for you right now? There are no right answers. (${step - 2} of ${pages.size})",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                    SoftCard(container = MaterialTheme.colorScheme.primaryContainer) {
                        Text("Your focus areas", style = MaterialTheme.typography.titleMedium)
                        prio.take(3).forEachIndexed { i, dom ->
                            Text("${i + 1}. ${Content.label(dom)}  (${scores[dom] ?: 50}/100 today)")
                        }
                    }
                    SoftCard {
                        Text("Day 1", style = MaterialTheme.typography.titleMedium)
                        first.items.forEach { Text("• ${it.title} (${it.minutes} min)") }
                        Text(
                            "Your plan adapts every day from your check-ins and progress.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (concerns.any { Content.concern(it)?.medicalNote == true }) {
                        SoftCard(container = MaterialTheme.colorScheme.errorContainer) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Warning, contentDescription = null)
                                Text("  About discomfort or pain", style = MaterialTheme.typography.titleSmall)
                            }
                            Text(
                                "Pain can have medical causes that are very treatable. Please consider seeing a clinician. Practices here are gentle and pressure-free; stop if anything hurts.",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Daily reminder at 9:00 pm", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "Discreet: nothing sensitive appears on your lock screen. Change anytime.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(checked = remindersOn, onCheckedChange = { remindersOn = it; if (it) askNotifications() })
                    }
                }
            }
        }

        Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (step > 0) TextButton(onClick = { step-- }) { Text("Back") }
            Spacer(Modifier.weight(1f))
            val canNext = when {
                step == 0 -> adult
                step == 2 -> concerns.isNotEmpty()
                step in 3 until lastStep -> pages[step - 3].all { answers.containsKey(it.id) }
                else -> true
            }
            Button(
                enabled = canNext,
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
            ) { Text(if (step < lastStep) "Continue" else "Start my plan") }
        }
    }
}
