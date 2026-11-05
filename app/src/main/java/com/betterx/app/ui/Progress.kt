package com.betterx.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.betterx.app.data.CheckIn
import com.betterx.app.data.Content
import com.betterx.app.data.Planner
import com.betterx.app.data.ScoreSnapshot
import com.betterx.app.data.today
import java.time.LocalDate

@Composable
private fun TrendChart(a: List<Int>, b: List<Int>, modifier: Modifier = Modifier) {
    val grid = BX.Chip
    Canvas(modifier.fillMaxWidth().height(140.dp)) {
        val pad = 8.dp.toPx()
        val w = size.width
        val h = size.height - 2 * pad
        for (g in 0..4) {
            val y = pad + h * g / 4f
            drawLine(grid, Offset(0f, y), Offset(w, y), strokeWidth = 1.dp.toPx())
        }
        fun series(vals: List<Int>, color: Color) {
            if (vals.isEmpty()) return
            val step = if (vals.size > 1) w / (vals.size - 1) else 0f
            val path = Path()
            vals.forEachIndexed { i, v ->
                val x = if (vals.size > 1) i * step else w / 2
                val y = pad + h - (v - 1) / 4f * h
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                drawCircle(color, 4.dp.toPx(), Offset(x, y))
            }
            if (vals.size > 1) drawPath(path, color, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
        }
        series(b, Color.White)
        series(a, BX.Lime)
    }
}

@Composable
fun ProgressScreen(onCheckIn: () -> Unit, onReassess: () -> Unit) {
    val d = LocalStore.current.data
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val day = today()
    val streak = Planner.streak(d, day)
    val minutes = d.completions.sumOf { it.minutes }
    val recent = d.checkIns.sortedBy { it.day }.takeLast(14)
    val first = d.scoreHistory.firstOrNull()?.scores ?: d.scores
    val latest = d.scoreHistory.lastOrNull()?.scores ?: d.scores
    val sinceAssess = d.scoreHistory.lastOrNull()?.let { day - it.day } ?: 0

    val perDay = (13 downTo 0).map { off -> d.completions.filter { it.day == day - off }.sumOf { it.minutes } }
    val labels = (13 downTo 0).map { off -> if (off % 3 == 0) LocalDate.ofEpochDay(day - off).dayOfMonth.toString() else "" }

    ScrollPage("Progress") {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatTile("$streak", "day streak", Modifier.weight(1f))
            StatTile("${d.completions.size}", "practices", Modifier.weight(1f))
            StatTile("$minutes", "minutes", Modifier.weight(1f))
        }

        SoftCard {
            Text("Practice, last 14 days", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(14.dp))
            PracticeBars(perDay, labels, highlight = 13)
        }

        SoftCard {
            Text("How you've been feeling", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(10.dp))
            if (recent.size < 2) {
                Text("Check in on a few days and your trends will show up here.", color = muted)
            } else {
                TrendChart(recent.map { it.confidence }, recent.map { it.anxiety })
                Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("● Confidence", color = BX.Lime, style = MaterialTheme.typography.labelMedium)
                    Text("● Pressure", color = Color.White, style = MaterialTheme.typography.labelMedium)
                }
            }
            Spacer(Modifier.height(12.dp))
            PrimaryButton("Check in now", onCheckIn, arrows = false)
        }

        SoftCard {
            Text("Your skills", style = MaterialTheme.typography.titleSmall)
            Content.domains.forEach { dom ->
                val now = latest[dom] ?: 50
                val delta = now - (first[dom] ?: now)
                Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(Content.label(dom), style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "$now" + if (delta != 0) "  (${if (delta > 0) "+" else ""}$delta)" else "",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (delta >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    )
                }
                ThinBar(now / 100f, Modifier.padding(top = 6.dp))
            }
            Text(
                if (sinceAssess >= 14) "It's been $sinceAssess days. Re-assess to see your progress and refresh your plan."
                else "Re-assess every couple of weeks to see how your skills change.",
                style = MaterialTheme.typography.bodySmall,
                color = muted,
                modifier = Modifier.padding(top = 14.dp),
            )
            OutlinedButton(onClick = onReassess, modifier = Modifier.padding(top = 8.dp)) { Text("Re-assess") }
        }

        if (recent.isNotEmpty()) {
            SectionTitle("Recent check-ins")
            recent.reversed().take(3).forEach { c ->
                SoftCard {
                    Text(LocalDate.ofEpochDay(c.day).toString(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Text(
                        "Mood ${c.mood}/5 · Confidence ${c.confidence}/5 · Pressure ${c.anxiety}/5 · Closeness ${c.connection}/5",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    if (c.note.isNotBlank()) Text(c.note, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
        Disclaimer()
    }
}

@Composable
fun CheckInScreen(onBack: () -> Unit) {
    val store = LocalStore.current
    var mood by remember { mutableStateOf<Int?>(null) }
    var confidence by remember { mutableStateOf<Int?>(null) }
    var anxiety by remember { mutableStateOf<Int?>(null) }
    var connection by remember { mutableStateOf<Int?>(null) }
    var note by remember { mutableStateOf("") }

    @Composable
    fun Scale(label: String, low: String, high: String, value: Int?, onChange: (Int) -> Unit) {
        SoftCard {
            Text(label, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(12.dp))
            RatingRow(value, onChange, low, high)
        }
    }

    ScrollPage("Check-in", onBack) {
        Text("Be honest. This is just for you and it shapes your plan.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Scale("Mood", "Low", "Great", mood) { mood = it }
        Scale("Confidence", "Low", "High", confidence) { confidence = it }
        Scale("Pressure or anxiety", "None", "A lot", anxiety) { anxiety = it }
        Scale("Closeness", "Distant", "Close", connection) { connection = it }
        OutlinedTextField(
            value = note,
            onValueChange = { note = it.take(300) },
            label = { Text("Anything to remember? (optional)") },
            modifier = Modifier.fillMaxWidth(),
        )
        PrimaryButton(
            "Save check-in",
            enabled = mood != null && confidence != null && anxiety != null && connection != null,
            onClick = {
                val day = today()
                val c = CheckIn(day, mood!!, confidence!!, anxiety!!, connection!!, note.trim())
                store.update { it.copy(checkIns = it.checkIns.filter { x -> x.day != day } + c) }
                onBack()
            },
        )
    }
}

@Composable
fun AssessScreen(onBack: () -> Unit) {
    val store = LocalStore.current
    val answers = remember { mutableStateMapOf<String, Int>().apply { putAll(store.data.answers) } }
    ScrollPage("Re-assessment", onBack) {
        Text("How true is each one for you right now?", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Content.questions.forEach { q -> QuestionCard(q, answers[q.id]) { answers[q.id] = it } }
        PrimaryButton(
            "Save and update my plan",
            enabled = Content.questions.all { answers.containsKey(it.id) },
            onClick = {
                val scores = Content.score(answers.toMap())
                store.update {
                    it.copy(
                        answers = answers.toMap(),
                        scores = scores,
                        scoreHistory = it.scoreHistory + ScoreSnapshot(today(), scores),
                    )
                }
                onBack()
            },
        )
    }
}
