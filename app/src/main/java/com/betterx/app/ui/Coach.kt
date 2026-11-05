package com.betterx.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.betterx.app.data.ChatMsg
import com.betterx.app.data.Content
import com.betterx.app.data.LocalCoach
import com.betterx.app.data.buildCoachContext
import com.betterx.app.data.defaultCoach
import com.betterx.app.data.today
import kotlinx.coroutines.launch

private const val FREE_DAILY_LIMIT = 5

@Composable
fun CoachScreen(onExercise: (String) -> Unit, onPaywall: () -> Unit) {
    val store = LocalStore.current
    val d = store.data
    val scope = rememberCoroutineScope()
    val provider = remember { defaultCoach() }
    var input by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val day = today()
    val used = if (d.coachUsageDay == day) d.coachUsageCount else 0
    val remaining = if (d.premium) Int.MAX_VALUE else FREE_DAILY_LIMIT - used

    LaunchedEffect(d.chat.size, sending) {
        listState.animateScrollToItem(d.chat.size + if (sending) 1 else 0)
    }

    fun send(text: String) {
        val t = text.trim()
        if (t.isEmpty() || sending) return
        if (remaining <= 0) {
            onPaywall()
            return
        }
        input = ""
        store.update {
            it.copy(
                chat = (it.chat + ChatMsg(true, t, System.currentTimeMillis())).takeLast(200),
                coachUsageDay = day,
                coachUsageCount = (if (it.coachUsageDay == day) it.coachUsageCount else 0) + 1,
            )
        }
        sending = true
        scope.launch {
            val data = store.data
            val ctx = buildCoachContext(data, day)
            val r = try {
                provider.reply(data.chat, ctx)
            } catch (e: Exception) {
                LocalCoach.reply(data.chat, ctx)
            }
            store.update {
                it.copy(chat = (it.chat + ChatMsg(false, r.text, System.currentTimeMillis(), r.exerciseId)).takeLast(200))
            }
            sending = false
        }
    }

    Column(Modifier.fillMaxSize().imePadding()) {
        ScreenHeader("Coach", actions = {
            if (d.chat.isNotEmpty()) {
                IconButton(onClick = { store.update { it.copy(chat = emptyList()) } }) {
                    Icon(Icons.Filled.Delete, contentDescription = "Clear conversation")
                }
            }
        })
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Bubble(
                    fromUser = false,
                    text = "Hi${if (d.nickname.isBlank()) "" else " ${d.nickname}"}. I'm your private BetterX coach. Tell me what's on your mind and I'll tailor guidance to your plan. I offer education and coaching, not medical advice.",
                    exerciseId = null,
                    onExercise = onExercise,
                )
            }
            items(d.chat) { m -> Bubble(m.fromUser, m.text, m.exerciseId, onExercise) }
            if (sending) item { Bubble(false, "…", null, onExercise) }
        }

        if (d.chat.isEmpty()) {
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Where do I start?", "I get anxious", "Our desire doesn't match", "How am I doing?").forEach { s ->
                    AssistChip(onClick = { send(s) }, label = { Text(s) })
                }
            }
        }
        if (!d.premium) {
            Text(
                if (remaining > 0) "$remaining free messages left today" else "Daily free limit reached",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
        }
        if (!d.premium && remaining <= 0) {
            OutlinedButton(onClick = onPaywall, modifier = Modifier.fillMaxWidth().padding(16.dp)) { Text("Unlock unlimited coaching") }
        } else {
            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    placeholder = { Text("Message your coach") },
                    maxLines = 4,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { send(input) }, enabled = input.isNotBlank() && !sending) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun Bubble(fromUser: Boolean, text: String, exerciseId: String?, onExercise: (String) -> Unit) {
    Box(Modifier.fillMaxWidth(), contentAlignment = if (fromUser) Alignment.CenterEnd else Alignment.CenterStart) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = if (fromUser) MaterialTheme.colorScheme.primary else BX.Card,
            border = if (fromUser) null else BorderStroke(1.dp, BX.CardBorder),
            modifier = Modifier.widthIn(max = 320.dp),
        ) {
            Column(Modifier.padding(12.dp)) {
                Text(text, style = MaterialTheme.typography.bodyMedium)
                val ex = exerciseId?.let { Content.exercise(it) }
                if (ex != null && !fromUser) {
                    OutlinedButton(onClick = { onExercise(ex.id) }, modifier = Modifier.padding(top = 8.dp)) {
                        Text("Try: ${ex.title}")
                    }
                }
            }
        }
    }
}
