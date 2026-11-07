package com.betterx.app.data

import android.util.Base64
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.io.IOException
import java.util.UUID
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/** A community post, as stored in Cloud Firestore at ghostPosts/{id}. */
data class GPost(
    val id: String,
    val uid: String,
    val alias: String,
    val topic: String,
    val title: String,
    val body: String,
    val ts: Long,
    val likes: Int = 0,
    val replies: Int = 0,
) {
    /** Tags the author added. Stored comma-separated in the existing `topic` field, so older posts and rules keep working. */
    val tags: List<String> get() = topic.split(",").map { it.trim() }.filter { it.isNotEmpty() }
}

/** A reply, stored at ghostPosts/{postId}/replies/{id}. */
data class GReply(
    val id: String,
    val uid: String,
    val alias: String,
    val text: String,
    val ts: Long,
)

/**
 * The shared Ghost community, through the Firestore REST API with the signed-in user's Firebase ID token.
 *
 * Layout:
 *   ghostPosts/{postId}                 uid, alias, topic (comma-separated tags), title, body, ts, likeCount, replyCount
 *   ghostPosts/{postId}/replies/{id}    uid, alias, text, ts
 *   ghostPosts/{postId}/likes/{uid}     ts            (one document per member, so a like can't be counted twice)
 *   ghostReports/{id}                   postId, replyId, reporter, ts
 *
 * The Firestore rules for these paths are in ghost-firestore-rules.txt (Firebase console > Firestore Database > Rules).
 */
object GhostApi {
    private const val OFFLINE = "No internet connection. Please try again."

    private fun projectId(idToken: String): String? = try {
        val payload = idToken.split(".")[1]
        val bytes = Base64.decode(payload, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
        (Json.parseToJsonElement(String(bytes, Charsets.UTF_8)) as? JsonObject)
            ?.get("aud")?.let { (it as? JsonPrimitive)?.contentOrNull }
    } catch (e: Exception) {
        null
    }

    private fun root(pid: String) = "projects/$pid/databases/(default)/documents"
    private fun postPath(pid: String, id: String) = "${root(pid)}/ghostPosts/$id"
    private fun api(pid: String) = "https://firestore.googleapis.com/v1/${root(pid)}"

    private fun failMessage(r: HttpResp): String = when (r.status) {
        401 -> "Your session expired. Please sign out and sign in again."
        403 -> "The community isn't switched on yet. Add the Ghost rules in Firebase (Firestore Database > Rules)."
        404 -> "The community isn't set up yet."
        429 -> "Too many attempts. Please wait a bit and try again."
        else -> "Something went wrong. Please try again."
    }

    private suspend fun <T> guarded(token: String, block: suspend (pid: String) -> SyncResult<T>): SyncResult<T> {
        val pid = projectId(token) ?: return SyncResult.Fail("Couldn't read your session. Please sign in again.")
        return try {
            block(pid)
        } catch (e: IOException) {
            SyncResult.Fail(OFFLINE)
        } catch (e: Exception) {
            SyncResult.Fail("Something went wrong. Please try again.")
        }
    }

    // ------------------------------------------------------------------ json helpers

    private fun JsonObjectBuilder.sv(k: String, v: String) = putJsonObject(k) { put("stringValue", v) }
    private fun JsonObjectBuilder.iv(k: String, v: Long) = putJsonObject(k) { put("integerValue", v.toString()) }

    private fun fieldsOf(doc: JsonObject): JsonObject? = doc["fields"] as? JsonObject
    private fun JsonObject?.str(k: String): String =
        (((this?.get(k) as? JsonObject)?.get("stringValue")) as? JsonPrimitive)?.contentOrNull.orEmpty()

    private fun JsonObject?.lng(k: String): Long =
        (((this?.get(k) as? JsonObject)?.get("integerValue")) as? JsonPrimitive)?.contentOrNull?.toLongOrNull() ?: 0L

    private fun docId(doc: JsonObject): String =
        (doc["name"] as? JsonPrimitive)?.contentOrNull.orEmpty().substringAfterLast('/')

    private fun createWrite(path: String, fields: JsonObjectBuilder.() -> Unit) = buildJsonObject {
        putJsonObject("update") {
            put("name", path)
            putJsonObject("fields", fields)
        }
        putJsonObject("currentDocument") { put("exists", false) }
    }

    /** Adds [by] to a counter on an existing document without touching anything else. */
    private fun incrementWrite(path: String, field: String, by: Long) = buildJsonObject {
        putJsonObject("update") { put("name", path) }
        putJsonObject("updateMask") { putJsonArray("fieldPaths") {} }
        putJsonArray("updateTransforms") {
            add(
                buildJsonObject {
                    put("fieldPath", field)
                    putJsonObject("increment") { put("integerValue", by.toString()) }
                },
            )
        }
        putJsonObject("currentDocument") { put("exists", true) }
    }

    private fun deleteWrite(path: String, mustExist: Boolean) = buildJsonObject {
        put("delete", path)
        if (mustExist) putJsonObject("currentDocument") { put("exists", true) }
    }

    private suspend fun commit(token: String, pid: String, writes: List<JsonObject>): HttpResp {
        val body = buildJsonObject { put("writes", buildJsonArray { writes.forEach { add(it) } }) }
        return Http.request("POST", "https://firestore.googleapis.com/v1/projects/$pid/databases/(default)/documents:commit", body.toString(), bearer = token)
    }

    // ------------------------------------------------------------------ reading

    suspend fun listPosts(token: String): SyncResult<List<GPost>> = guarded(token) { pid ->
        val r = Http.request("GET", "${api(pid)}/ghostPosts?orderBy=ts%20desc&pageSize=50", bearer = token)
        if (!r.ok) return@guarded SyncResult.Fail(failMessage(r))
        val docs = (r.json["documents"] as? JsonArray).orEmpty()
        SyncResult.Ok(
            docs.mapNotNull { el ->
                val doc = el as? JsonObject ?: return@mapNotNull null
                val f = fieldsOf(doc) ?: return@mapNotNull null
                GPost(
                    id = docId(doc),
                    uid = f.str("uid"),
                    alias = f.str("alias"),
                    topic = f.str("topic"),
                    title = f.str("title"),
                    body = f.str("body"),
                    ts = f.lng("ts"),
                    likes = f.lng("likeCount").toInt().coerceAtLeast(0),
                    replies = f.lng("replyCount").toInt().coerceAtLeast(0),
                )
            },
        )
    }

    suspend fun listReplies(token: String, postId: String): SyncResult<List<GReply>> = guarded(token) { pid ->
        val r = Http.request("GET", "${api(pid)}/ghostPosts/$postId/replies?orderBy=ts&pageSize=100", bearer = token)
        if (!r.ok) return@guarded SyncResult.Fail(failMessage(r))
        val docs = (r.json["documents"] as? JsonArray).orEmpty()
        SyncResult.Ok(
            docs.mapNotNull { el ->
                val doc = el as? JsonObject ?: return@mapNotNull null
                val f = fieldsOf(doc) ?: return@mapNotNull null
                GReply(docId(doc), f.str("uid"), f.str("alias"), f.str("text"), f.lng("ts"))
            },
        )
    }

    // ------------------------------------------------------------------ writing

    suspend fun createPost(token: String, p: GPost): SyncResult<Unit> = guarded(token) { pid ->
        val r = commit(
            token, pid,
            listOf(
                createWrite(postPath(pid, p.id)) {
                    sv("uid", p.uid)
                    sv("alias", p.alias)
                    sv("topic", p.topic)
                    sv("title", p.title)
                    sv("body", p.body)
                    iv("ts", p.ts)
                    iv("likeCount", 0)
                    iv("replyCount", 0)
                },
            ),
        )
        if (r.ok) SyncResult.Ok(Unit) else SyncResult.Fail(failMessage(r))
    }

    suspend fun createReply(token: String, postId: String, rep: GReply): SyncResult<Unit> = guarded(token) { pid ->
        val post = postPath(pid, postId)
        val r = commit(
            token, pid,
            listOf(
                createWrite("$post/replies/${rep.id}") {
                    sv("uid", rep.uid)
                    sv("alias", rep.alias)
                    sv("text", rep.text)
                    iv("ts", rep.ts)
                },
                incrementWrite(post, "replyCount", 1),
            ),
        )
        if (r.ok) SyncResult.Ok(Unit) else SyncResult.Fail(failMessage(r))
    }

    /**
     * Likes or un-likes a post. Returns true when the count changed, false when the server already
     * had the same state (for example it was already liked from another device).
     */
    suspend fun setLike(token: String, uid: String, postId: String, like: Boolean): SyncResult<Boolean> = guarded(token) { pid ->
        val post = postPath(pid, postId)
        val likeDoc = "$post/likes/$uid"
        val writes = if (like) {
            listOf(createWrite(likeDoc) { iv("ts", System.currentTimeMillis()) }, incrementWrite(post, "likeCount", 1))
        } else {
            listOf(deleteWrite(likeDoc, mustExist = true), incrementWrite(post, "likeCount", -1))
        }
        val r = commit(token, pid, writes)
        when {
            r.ok -> SyncResult.Ok(true)
            like && r.status == 409 -> SyncResult.Ok(false)
            !like && r.status == 404 -> SyncResult.Ok(false)
            else -> SyncResult.Fail(failMessage(r))
        }
    }

    suspend fun deletePost(token: String, postId: String): SyncResult<Unit> = guarded(token) { pid ->
        val r = commit(token, pid, listOf(deleteWrite(postPath(pid, postId), mustExist = false)))
        if (r.ok) SyncResult.Ok(Unit) else SyncResult.Fail(failMessage(r))
    }

    suspend fun deleteReply(token: String, postId: String, replyId: String): SyncResult<Unit> = guarded(token) { pid ->
        val post = postPath(pid, postId)
        val r = commit(token, pid, listOf(deleteWrite("$post/replies/$replyId", mustExist = true), incrementWrite(post, "replyCount", -1)))
        if (r.ok || r.status == 404) SyncResult.Ok(Unit) else SyncResult.Fail(failMessage(r))
    }

    suspend fun report(token: String, uid: String, postId: String, replyId: String?): SyncResult<Unit> = guarded(token) { pid ->
        val r = commit(
            token, pid,
            listOf(
                createWrite("${root(pid)}/ghostReports/${UUID.randomUUID()}") {
                    sv("postId", postId)
                    sv("replyId", replyId.orEmpty())
                    sv("reporter", uid)
                    iv("ts", System.currentTimeMillis())
                },
            ),
        )
        if (r.ok) SyncResult.Ok(Unit) else SyncResult.Fail(failMessage(r))
    }

    /** Removes every post this member wrote (used when they delete their account). */
    suspend fun deleteMyPosts(token: String, uid: String): SyncResult<Unit> = guarded(token) { pid ->
        val query = buildJsonObject {
            putJsonObject("structuredQuery") {
                putJsonArray("from") { add(buildJsonObject { put("collectionId", "ghostPosts") }) }
                putJsonObject("where") {
                    putJsonObject("fieldFilter") {
                        putJsonObject("field") { put("fieldPath", "uid") }
                        put("op", "EQUAL")
                        putJsonObject("value") { put("stringValue", uid) }
                    }
                }
                put("limit", 300)
            }
        }
        val q = Http.request("POST", "${api(pid)}:runQuery", query.toString(), bearer = token)
        if (!q.ok) return@guarded SyncResult.Fail(failMessage(q))
        val paths = q.array.mapNotNull { el ->
            ((el as? JsonObject)?.get("document") as? JsonObject)?.get("name")?.let { (it as? JsonPrimitive)?.contentOrNull }
        }
        if (paths.isEmpty()) return@guarded SyncResult.Ok(Unit)
        val r = commit(token, pid, paths.map { deleteWrite(it, mustExist = false) })
        if (r.ok) SyncResult.Ok(Unit) else SyncResult.Fail(failMessage(r))
    }
}

/** What the Ghost screens show. Kept in memory for this session; the community itself lives in Firestore. */
object GhostRepo {
    var posts by mutableStateOf<List<GPost>>(emptyList())
        private set
    var replies by mutableStateOf<Map<String, List<GReply>>>(emptyMap())
        private set
    var loading by mutableStateOf(false)
        private set
    var loaded by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    private var lastPostAt = 0L
    private var lastReplyAt = 0L

    private suspend fun <T> withSession(store: Store, block: suspend (token: String, uid: String) -> SyncResult<T>): SyncResult<T> =
        when (val s = store.freshSession()) {
            is AuthResult.Ok -> block(s.token, s.uid.ifBlank { store.data.ownerUid })
            is AuthResult.Fail -> SyncResult.Fail(s.message)
        }

    suspend fun refresh(store: Store) {
        if (loading) return
        loading = true
        error = null
        when (val r = withSession(store) { token, _ -> GhostApi.listPosts(token) }) {
            is SyncResult.Ok -> { posts = r.value; loaded = true }
            is SyncResult.Fail -> error = r.message
        }
        loading = false
    }

    suspend fun loadReplies(store: Store, postId: String): String? =
        when (val r = withSession(store) { token, _ -> GhostApi.listReplies(token, postId) }) {
            is SyncResult.Ok -> { replies = replies + (postId to r.value); null }
            is SyncResult.Fail -> r.message
        }

    /** Returns an error message, or null when the post was shared. */
    suspend fun createPost(store: Store, tags: List<String>, title: String, body: String): String? {
        if (System.currentTimeMillis() - lastPostAt < 20_000) return "Please wait a moment before posting again."
        return when (
            val r = withSession(store) { token, uid ->
                val post = GPost(UUID.randomUUID().toString(), uid, Content.newAlias(), tags.joinToString(","), title, body, System.currentTimeMillis())
                val res = GhostApi.createPost(token, post)
                if (res is SyncResult.Ok) {
                    posts = listOf(post) + posts
                    lastPostAt = System.currentTimeMillis()
                }
                res
            }
        ) {
            is SyncResult.Ok -> null
            is SyncResult.Fail -> r.message
        }
    }

    suspend fun createReply(store: Store, postId: String, text: String): String? {
        if (System.currentTimeMillis() - lastReplyAt < 5_000) return "Please wait a moment before replying again."
        return when (
            val r = withSession(store) { token, uid ->
                val rep = GReply(UUID.randomUUID().toString(), uid, Content.newAlias(), text, System.currentTimeMillis())
                val res = GhostApi.createReply(token, postId, rep)
                if (res is SyncResult.Ok) {
                    replies = replies + (postId to (replies[postId].orEmpty() + rep))
                    posts = posts.map { if (it.id == postId) it.copy(replies = it.replies + 1) else it }
                    lastReplyAt = System.currentTimeMillis()
                }
                res
            }
        ) {
            is SyncResult.Ok -> null
            is SyncResult.Fail -> r.message
        }
    }

    /** Updates the count right away, then confirms with the server. Returns an error message when it failed. */
    suspend fun setLike(store: Store, postId: String, like: Boolean): String? {
        val delta = if (like) 1 else -1
        posts = posts.map { if (it.id == postId) it.copy(likes = (it.likes + delta).coerceAtLeast(0)) else it }
        val r = withSession(store) { token, uid -> GhostApi.setLike(token, uid, postId, like) }
        return when {
            r is SyncResult.Ok && r.value -> null
            r is SyncResult.Ok -> {
                // The server already had this state, so the count was not changed there.
                posts = posts.map { if (it.id == postId) it.copy(likes = (it.likes - delta).coerceAtLeast(0)) else it }
                null
            }
            else -> {
                posts = posts.map { if (it.id == postId) it.copy(likes = (it.likes - delta).coerceAtLeast(0)) else it }
                (r as SyncResult.Fail).message
            }
        }
    }

    suspend fun deletePost(store: Store, postId: String): String? =
        when (val r = withSession(store) { token, _ -> GhostApi.deletePost(token, postId) }) {
            is SyncResult.Ok -> { posts = posts.filter { it.id != postId }; null }
            is SyncResult.Fail -> r.message
        }

    suspend fun deleteReply(store: Store, postId: String, replyId: String): String? =
        when (val r = withSession(store) { token, _ -> GhostApi.deleteReply(token, postId, replyId) }) {
            is SyncResult.Ok -> {
                replies = replies + (postId to replies[postId].orEmpty().filter { it.id != replyId })
                posts = posts.map { if (it.id == postId) it.copy(replies = (it.replies - 1).coerceAtLeast(0)) else it }
                null
            }
            is SyncResult.Fail -> r.message
        }

    suspend fun report(store: Store, postId: String, replyId: String?): String? =
        when (val r = withSession(store) { token, uid -> GhostApi.report(token, uid, postId, replyId) }) {
            is SyncResult.Ok -> null
            is SyncResult.Fail -> r.message
        }
}
