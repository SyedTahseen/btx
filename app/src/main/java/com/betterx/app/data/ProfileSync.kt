package com.betterx.app.data

import android.util.Base64
import java.io.IOException
import java.time.Instant
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/** The part of the profile that is stored in Firebase. Plans, check-ins and chats are NOT included. */
data class RemoteProfile(
    val nickname: String,
    val relationship: String,
    val concerns: List<String>,
)

sealed class SyncResult<out T> {
    data class Ok<T>(val value: T) : SyncResult<T>()
    data class Fail(val message: String) : SyncResult<Nothing>()
}

/**
 * Saves the profile in Cloud Firestore at users/{uid}, through the REST API with the user's Firebase ID token.
 * The Firebase project is read from the token itself, so there is nothing extra to configure in the app.
 *
 * Firestore rules needed (Firebase console > Firestore Database > Rules):
 *   match /users/{uid} { allow read, write: if request.auth != null && request.auth.uid == uid; }
 */
object ProfileSync {
    private const val OFFLINE = "No internet connection. Please try again."

    /** Firebase ID tokens carry the project ID in the "aud" claim. */
    private fun projectId(idToken: String): String? = try {
        val payload = idToken.split(".")[1]
        val bytes = Base64.decode(payload, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
        (Json.parseToJsonElement(String(bytes, Charsets.UTF_8)) as? JsonObject)
            ?.get("aud")?.let { (it as? JsonPrimitive)?.contentOrNull }
    } catch (e: Exception) {
        null
    }

    private fun docPath(pid: String, uid: String) = "projects/$pid/databases/(default)/documents/users/$uid"

    private fun failMessage(r: HttpResp): String {
        val msg = ((r.json["error"] as? JsonObject)?.get("message") as? JsonPrimitive)?.contentOrNull.orEmpty()
        return when (r.status) {
            401 -> "Your session expired. Please sign out and sign in again."
            403 -> "Online profile storage refused access. Check the Firestore rules for users/{uid}."
            404 -> if (msg.contains("database", ignoreCase = true)) "Firestore isn't enabled for this Firebase project yet."
            else "Profile not found."
            429 -> "Too many attempts. Please wait a bit and try again."
            else -> "Couldn't reach your online profile. Please try again."
        }
    }

    /** Returns null inside Ok when the user has no saved profile yet. */
    suspend fun load(idToken: String, uid: String): SyncResult<RemoteProfile?> {
        val pid = projectId(idToken) ?: return SyncResult.Fail("Couldn't read your session. Please sign in again.")
        return try {
            val r = Http.request("GET", "https://firestore.googleapis.com/v1/${docPath(pid, uid)}", bearer = idToken)
            when {
                r.ok -> SyncResult.Ok(parse(r.json))
                r.status == 404 && !failMessage(r).startsWith("Firestore") -> SyncResult.Ok(null)
                else -> SyncResult.Fail(failMessage(r))
            }
        } catch (e: IOException) {
            SyncResult.Fail(OFFLINE)
        } catch (e: Exception) {
            SyncResult.Fail("Couldn't load your online profile.")
        }
    }

    suspend fun save(idToken: String, uid: String, p: RemoteProfile): SyncResult<Unit> {
        val pid = projectId(idToken) ?: return SyncResult.Fail("Couldn't read your session. Please sign in again.")
        val body = buildJsonObject {
            putJsonArray("writes") {
                add(buildJsonObject {
                    putJsonObject("update") {
                        put("name", docPath(pid, uid))
                        putJsonObject("fields") {
                            putJsonObject("nickname") { put("stringValue", p.nickname) }
                            putJsonObject("relationship") { put("stringValue", p.relationship) }
                            putJsonObject("concerns") {
                                putJsonObject("arrayValue") {
                                    put("values", buildJsonArray {
                                        p.concerns.forEach { c -> add(buildJsonObject { put("stringValue", c) }) }
                                    })
                                }
                            }
                            putJsonObject("updatedAt") { put("timestampValue", Instant.now().toString()) }
                        }
                    }
                })
            }
        }
        return commit(idToken, pid, body.toString())
    }

    /** Removes the saved profile (called before the account itself is deleted). */
    suspend fun delete(idToken: String, uid: String): SyncResult<Unit> {
        val pid = projectId(idToken) ?: return SyncResult.Fail("Couldn't read your session.")
        val body = buildJsonObject {
            putJsonArray("writes") { add(buildJsonObject { put("delete", docPath(pid, uid)) }) }
        }
        return commit(idToken, pid, body.toString())
    }

    private suspend fun commit(idToken: String, pid: String, body: String): SyncResult<Unit> = try {
        val r = Http.request(
            "POST",
            "https://firestore.googleapis.com/v1/projects/$pid/databases/(default)/documents:commit",
            body,
            bearer = idToken,
        )
        if (r.ok) SyncResult.Ok(Unit) else SyncResult.Fail(failMessage(r))
    } catch (e: IOException) {
        SyncResult.Fail(OFFLINE)
    } catch (e: Exception) {
        SyncResult.Fail("Couldn't save your online profile.")
    }

    private fun parse(doc: JsonObject): RemoteProfile? {
        val f = doc["fields"] as? JsonObject ?: return null
        fun str(k: String) = (((f[k] as? JsonObject)?.get("stringValue")) as? JsonPrimitive)?.contentOrNull
        val concerns = (((f["concerns"] as? JsonObject)?.get("arrayValue") as? JsonObject)?.get("values") as? JsonArray)
            ?.mapNotNull { ((it as? JsonObject)?.get("stringValue") as? JsonPrimitive)?.contentOrNull }
            .orEmpty()
        return RemoteProfile(str("nickname").orEmpty(), str("relationship") ?: "Single", concerns)
    }
}
