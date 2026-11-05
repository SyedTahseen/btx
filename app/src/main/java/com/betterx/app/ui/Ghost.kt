package com.betterx.app.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.betterx.app.data.Content
import com.betterx.app.data.GhostPost
import com.betterx.app.data.GhostReply
import java.util.UUID

private val piiRegex = Regex("""(https?://|www\.)\S+|\S+@\S+\.\S+|\+?\d[\d\s\-().]{7,}\d|@\w{3,}""")

private fun hasPii(text: String) = piiRegex.containsMatchIn(text)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GhostScreen(onOpen: (String) -> Unit, onNew: () -> Unit) {
    val d = LocalStore.current.data
    var topic by remember { mutableStateOf("All") }
    val posts = d.ghost
        .filter { it.id !in d.hidden && (topic == "All" || it.topic == topic) }
        .sortedByDescending { it.ts }

    ScrollPage(
        "Ghost",
        actions = {
            IconButton(onClick = onNew) { Icon(Icons.Filled.Add, contentDescription = "Share anonymously") }
        },
    ) {
        Text(
            "Anonymous, real experiences. You get a random name for each post; it's never linked to your profile.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            (listOf("All") + Content.ghostTopics).forEach {
                FilterChip(selected = topic == it, onClick = { topic = it }, label = { Text(it) })
            }
        }
        Button(onClick = onNew, modifier = Modifier.fillMaxWidth()) { Text("Share anonymously") }
        if (posts.isEmpty()) Text("Nothing here yet. Be the first.")
        posts.forEach { p ->
            SoftCard(onClick = { onOpen(p.id) }) {
                Text("${p.alias} · ${p.topic} · ${relTime(p.ts)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                Text(p.title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 4.dp))
                Text(p.body, style = MaterialTheme.typography.bodySmall, maxLines = 3, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    "♥ ${p.hearts}   💬 ${p.replies.size}",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
        Text(
            "Community guidelines: be kind, no names or contact details, no explicit content, no medical advice. Peer experiences are not a substitute for care.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun PostScreen(id: String, onBack: () -> Unit) {
    val store = LocalStore.current
    val d = store.data
    val ctx = LocalContext.current
    val post = d.ghost.firstOrNull { it.id == id }
    var reply by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    if (post == null) {
        ScrollPage("Post", onBack) { Text("This post is no longer available.") }
        return
    }
    val hearted = id in d.hearted

    ScrollPage("Ghost", onBack) {
        SoftCard {
            Text("${post.alias} · ${post.topic} · ${relTime(post.ts)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
            Text(post.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 4.dp))
            Text(post.body, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                IconButton(onClick = {
                    store.update {
                        it.copy(
                            hearted = if (hearted) it.hearted - id else it.hearted + id,
                            ghost = it.ghost.map { g -> if (g.id == id) g.copy(hearts = g.hearts + if (hearted) -1 else 1) else g },
                        )
                    }
                }) {
                    Icon(
                        if (hearted) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = "Support",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                Text("${post.hearts}")
                Row(Modifier.weight(1f)) {}
                if (post.mine) {
                    TextButton(onClick = {
                        store.update { it.copy(ghost = it.ghost.filter { g -> g.id != id }) }
                        onBack()
                    }) { Text("Delete") }
                } else {
                    TextButton(onClick = {
                        store.update { it.copy(hidden = it.hidden + id) }
                        Toast.makeText(ctx, "Hidden for you. Reports are reviewed once the community server is connected.", Toast.LENGTH_LONG).show()
                        onBack()
                    }) { Text("Report / hide") }
                }
            }
        }
        SectionTitle("${post.replies.size} replies")
        post.replies.forEach { r ->
            SoftCard {
                Text("${if (r.mine) "You (anonymous)" else r.alias} · ${relTime(r.ts)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                Text(r.text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
            }
        }
        OutlinedTextField(
            value = reply,
            onValueChange = { reply = it.take(500); error = null },
            label = { Text("Reply anonymously") },
            isError = error != null,
            supportingText = { error?.let { Text(it) } },
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            enabled = reply.isNotBlank(),
            onClick = {
                if (hasPii(reply)) {
                    error = "Please remove links, emails or phone numbers."
                } else {
                    val r = GhostReply(Content.newAlias(), reply.trim(), System.currentTimeMillis(), mine = true)
                    store.update { it.copy(ghost = it.ghost.map { g -> if (g.id == id) g.copy(replies = g.replies + r) else g }) }
                    reply = ""
                }
            },
        ) { Text("Reply") }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NewPostScreen(onBack: () -> Unit) {
    val store = LocalStore.current
    var topic by remember { mutableStateOf(Content.ghostTopics.first()) }
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    ScrollPage("Share anonymously", onBack) {
        Text(
            "Your post gets a random name and isn't linked to your profile. Don't include names, contact details or links.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Content.ghostTopics.forEach { FilterChip(selected = topic == it, onClick = { topic = it }, label = { Text(it) }) }
        }
        OutlinedTextField(
            value = title,
            onValueChange = { title = it.take(80); error = null },
            label = { Text("Title") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = body,
            onValueChange = { body = it.take(1000); error = null },
            label = { Text("What's on your mind?") },
            minLines = 5,
            isError = error != null,
            supportingText = { error?.let { Text(it) } },
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                enabled = title.isNotBlank() && body.length >= 10,
                onClick = {
                    if (hasPii("$title $body")) {
                        error = "Please remove links, emails or phone numbers."
                    } else {
                        val post = GhostPost(
                            id = UUID.randomUUID().toString(),
                            alias = Content.newAlias(),
                            topic = topic,
                            title = title.trim(),
                            body = body.trim(),
                            ts = System.currentTimeMillis(),
                            mine = true,
                        )
                        store.update { it.copy(ghost = it.ghost + post) }
                        onBack()
                    }
                },
            ) { Text("Post") }
            OutlinedButton(onClick = onBack) { Text("Cancel") }
        }
    }
}
