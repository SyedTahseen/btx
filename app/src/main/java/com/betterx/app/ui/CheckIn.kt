package com.betterx.app.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.betterx.app.data.CheckIn
import com.betterx.app.data.Content
import com.betterx.app.data.ScoreSnapshot
import com.betterx.app.data.today

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
        FieldLabel("Anything to remember? (optional)")
        ProfileField(
            value = note,
            onValueChange = { note = it.take(300) },
            placeholder = "A note for yourself",
            singleLine = false,
            minHeight = 96.dp,
            shape = RoundedCornerShape(24.dp),
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
                        profileDirty = true,
                    )
                }
                onBack()
            },
        )
    }
}
