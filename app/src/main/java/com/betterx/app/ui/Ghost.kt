package com.betterx.app.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.betterx.app.data.Content
import com.betterx.app.data.GhostRepo
import kotlinx.coroutines.launch

private val piiRegex = Regex("""(https?://|www\.)\S+|\S+@\S+\.\S+|\+?\d[\d\s\-().]{7,}\d|@\w{3,}""")

private fun hasPii(text: String) = piiRegex.containsMatchIn(text)

private const val PII_MESSAGE = "Please remove links, emails or phone numbers."

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GhostScreen(onOpen: (String) -> Unit, onNew: () -> Unit) {
    val store = LocalStore.current
    val d = store.data
    val scope = rememberCoroutineScope()
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    var topic by remember { mutableStateOf("All") }

    LaunchedEffect(Unit) { GhostRepo.refresh(store) }

    val posts = GhostRepo.posts.filter { it.id !in d.hidden && (topic == "All" || it.topic == topic) }

    ScrollPage(
        "Ghost",
        actions = {
            IconButton(onClick = { scope.launch { GhostRepo.refresh(store) } }) {
                Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
            }
            IconButton(onClick = onNew) { Icon(Icons.Filled.Add, contentDescription = "Share anonymously") }
        },
    ) {
        Text(
            "Anonymous, real experiences. Every post and reply gets a random name; it's never shown with your profile.",
            color = muted,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            (listOf("All") + Content.ghostTopics).forEach {
                FilterChip(selected = topic == it, onClick = { topic = it }, label = { Text(it) })
            }
        }
        PrimaryButton("Share anonymously", onClick = onNew)

        val err = GhostRepo.error
        when {
            err != null -> {
                Text(err, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = { scope.launch { GhostRepo.refresh(store) } }) { Text("Try again") }
            }
            GhostRepo.loading && !GhostRepo.loaded -> Text("Loading…", color = muted)
            posts.isEmpty() -> Text("Nothing here yet. Be the first to share.", color = muted)
        }

        posts.forEach { p ->
            SoftCard(onClick = { onOpen(p.id) }) {
                Text("${p.alias} · ${p.topic} · ${relTime(p.ts)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                Text(p.title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 4.dp))
                Text(p.body, style = MaterialTheme.typography.bodySmall, maxLines = 3, color = muted)
                Text(
                    "♥ ${p.likes}   ·   ${p.replies} ${if (p.replies == 1) "reply" else "replies"}",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
        Text(
            "Community guidelines: be kind, no names or contact details, no explicit content, no medical advice. Peer experiences are not a substitute for care.",
            style = MaterialTheme.typography.bodySmall,
            color = muted,
        )
    }
}

@Composable
fun PostScreen(id: String, onBack: () -> Unit) {
    val store = LocalStore.current
    val d = store.data
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val post = GhostRepo.posts.firstOrNull { it.id == id }
    var reply by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var sending by remember { mutableStateOf(false) }
    var liking by remember { mutableStateOf(false) }

    LaunchedEffect(id) {
        GhostRepo.loadReplies(store, id)?.let { Toast.makeText(ctx, it, Toast.LENGTH_SHORT).show() }
    }

    if (post == null) {
        ScrollPage("Post", onBack) { Text("This post is no longer available.", color = muted) }
        return
    }
    val hearted = id in d.hearted
    val mine = post.uid == d.ownerUid
    val replies = GhostRepo.replies[id].orEmpty().filter { it.id !in d.hidden }

    ScrollPage("Ghost", onBack) {
        SoftCard {
            Text("${post.alias} · ${post.topic} · ${relTime(post.ts)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
            Text(post.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 4.dp))
            Text(post.body, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                IconButton(onClick = {
                    if (!liking) {
                        liking = true
                        val like = !hearted
                        store.update { it.copy(hearted = if (like) it.hearted + id else it.hearted - id) }
                        scope.launch {
                            val err = GhostRepo.setLike(store, id, like)
                            if (err != null) {
                                store.update { it.copy(hearted = if (like) it.hearted - id else it.hearted + id) }
                                Toast.makeText(ctx, err, Toast.LENGTH_SHORT).show()
                            }
                            liking = false
                        }
                    }
                }) {
                    Icon(
                        if (hearted) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = if (hearted) "Remove support" else "Support",
                        tint = if (hearted) BX.Coral else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text("${post.likes}")
                Row(Modifier.weight(1f)) {}
                if (mine) {
                    TextButton(onClick = {
                        scope.launch {
                            val err = GhostRepo.deletePost(store, id)
                            if (err == null) onBack() else Toast.makeText(ctx, err, Toast.LENGTH_SHORT).show()
                        }
                    }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                } else {
                    TextButton(onClick = {
                        store.update { it.copy(hidden = it.hidden + id) }
                        scope.launch { GhostRepo.report(store, id, null) }
                        Toast.makeText(ctx, "Thanks. We'll review it, and it's hidden for you.", Toast.LENGTH_LONG).show()
                        onBack()
                    }) { Text("Report", color = muted) }
                }
            }
        }

        SectionTitle("${post.replies} ${if (post.replies == 1) "reply" else "replies"}")
        replies.forEach { r ->
            val mineReply = r.uid == d.ownerUid
            SoftCard {
                Text(
                    "${if (mineReply) "You (anonymous)" else r.alias} · ${relTime(r.ts)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
                Text(r.text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    if (mineReply) {
                        TextButton(onClick = {
                            scope.launch {
                                GhostRepo.deleteReply(store, id, r.id)?.let { Toast.makeText(ctx, it, Toast.LENGTH_SHORT).show() }
                            }
                        }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                    } else {
                        TextButton(onClick = {
                            store.update { it.copy(hidden = it.hidden + r.id) }
                            scope.launch { GhostRepo.report(store, id, r.id) }
                            Toast.makeText(ctx, "Thanks. We'll review it, and it's hidden for you.", Toast.LENGTH_LONG).show()
                        }) { Text("Report", color = muted) }
                    }
                }
            }
        }

        FieldLabel("Reply anonymously")
        ProfileField(
            value = reply,
            onValueChange = { reply = it.take(500); error = null },
            placeholder = "Share something kind or helpful",
            singleLine = false,
            minHeight = 56.dp,
            shape = RoundedCornerShape(24.dp),
        )
        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
        PrimaryButton(
            text = if (sending) "Sending…" else "Reply",
            onClick = {
                when {
                    sending -> {}
                    reply.isBlank() -> error = "Write something first."
                    hasPii(reply) -> error = PII_MESSAGE
                    else -> {
                        sending = true
                        scope.launch {
                            val err = GhostRepo.createReply(store, id, reply.trim())
                            if (err == null) reply = "" else error = err
                            sending = false
                        }
                    }
                }
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NewPostScreen(onBack: () -> Unit) {
    val store = LocalStore.current
    val scope = rememberCoroutineScope()
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    var topic by remember { mutableStateOf(Content.ghostTopics.first()) }
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var sending by remember { mutableStateOf(false) }

    ScrollPage("Share anonymously", onBack) {
        Text(
            "Your post gets a random name and isn't shown with your profile. Don't include names, contact details or links.",
            color = muted,
        )
        FieldLabel("Topic")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Content.ghostTopics.forEach { FilterChip(selected = topic == it, onClick = { topic = it }, label = { Text(it) }) }
        }
        FieldLabel("Title")
        ProfileField(title, { title = it.take(80); error = null }, "A short headline")
        FieldLabel("Your experience")
        ProfileField(
            value = body,
            onValueChange = { body = it.take(1000); error = null },
            placeholder = "What's on your mind?",
            singleLine = false,
            minHeight = 150.dp,
            shape = RoundedCornerShape(24.dp),
        )
        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
        PrimaryButton(
            text = if (sending) "Posting…" else "Post",
            onClick = {
                when {
                    sending -> {}
                    title.isBlank() -> error = "Add a title."
                    body.trim().length < 10 -> error = "Write a little more (at least 10 characters)."
                    hasPii("$title $body") -> error = PII_MESSAGE
                    else -> {
                        sending = true
                        scope.launch {
                            val err = GhostRepo.createPost(store, topic, title.trim(), body.trim())
                            sending = false
                            if (err == null) onBack() else error = err
                        }
                    }
                }
            },
        )
    }
}
