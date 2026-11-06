package com.betterx.app.data

import com.betterx.app.BuildConfig
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put

sealed class AuthResult {
    /** [needsConfirm] is true when the account exists but the email has not been verified yet. */
    data class Ok(
        val uid: String = "",
        val email: String = "",
        val token: String = "",
        val needsConfirm: Boolean = false,
    ) : AuthResult()

    data class Fail(val message: String, val code: String = "") : AuthResult()
}

/**
 * Accounts through the Firebase Authentication REST API (no SDK, so the app stays small):
 * email + password with email verification, and Google sign-in.
 * Only identity lives there. Health data never leaves the device.
 */
object Auth {
    private const val BASE = "https://identitytoolkit.googleapis.com/v1/accounts"

    /** Creates the account and sends the verification email. The user must verify before getting in. */
    suspend fun signUp(email: String, password: String): AuthResult {
        val (json, fail) = call("signUp", creds(email, password))
        if (fail != null) return fail
        val s = session(json!!, email)
        sendVerification(s.token) // if this fails, the Verify screen has a "Resend" button
        return s.copy(needsConfirm = true)
    }

    /** Signs in. The result has needsConfirm = true if the email isn't verified yet. */
    suspend fun signIn(email: String, password: String): AuthResult {
        val (json, fail) = call("signInWithPassword", creds(email, password))
        if (fail != null) return fail
        val s = session(json!!, email)
        val (info, lookupFail) = call("lookup", buildJsonObject { put("idToken", s.token) })
        if (lookupFail != null) return lookupFail
        val user = ((info!!["users"] as? JsonArray)?.firstOrNull()) as? JsonObject
        val verified = (user?.get("emailVerified") as? JsonPrimitive)?.booleanOrNull ?: false
        return s.copy(needsConfirm = !verified)
    }

    /** Exchanges a Google ID token (from the Google account picker) for a Firebase session. */
    suspend fun signInWithGoogle(googleIdToken: String): AuthResult {
        val (json, fail) = call("signInWithIdp", buildJsonObject {
            put("postBody", "id_token=$googleIdToken&providerId=google.com")
            put("requestUri", "http://localhost")
            put("returnIdpCredential", true)
            put("returnSecureToken", true)
        })
        return fail ?: session(json!!, "")
    }

    suspend fun sendVerification(token: String): AuthResult {
        val (_, fail) = call("sendOobCode", buildJsonObject {
            put("requestType", "VERIFY_EMAIL")
            put("idToken", token)
        })
        return fail ?: AuthResult.Ok()
    }

    /**
     * Sends a fresh verification email. If the email turns out to be verified already,
     * returns the signed-in session instead (token is not empty).
     */
    suspend fun resendVerification(email: String, password: String): AuthResult {
        return when (val s = signIn(email, password)) {
            is AuthResult.Fail -> s
            is AuthResult.Ok -> if (!s.needsConfirm) s else sendVerification(s.token)
        }
    }

    /** Looks successful for unknown emails so the form can't be used to discover accounts. */
    suspend fun sendReset(email: String): AuthResult {
        val (_, fail) = call("sendOobCode", buildJsonObject {
            put("requestType", "PASSWORD_RESET")
            put("email", email)
        })
        return if (fail != null && fail.code == "EMAIL_NOT_FOUND") AuthResult.Ok() else fail ?: AuthResult.Ok()
    }

    suspend fun deleteAccount(token: String): AuthResult {
        val (_, fail) = call("delete", buildJsonObject { put("idToken", token) })
        return fail ?: AuthResult.Ok()
    }

    // ----------------------------------------------------------------- internals

    private fun creds(email: String, password: String) = buildJsonObject {
        put("email", email)
        put("password", password)
        put("returnSecureToken", true)
    }

    private fun JsonObject.str(key: String): String? = (this[key] as? JsonPrimitive)?.contentOrNull

    private fun session(j: JsonObject, fallbackEmail: String) = AuthResult.Ok(
        uid = j.str("localId").orEmpty(),
        email = j.str("email") ?: fallbackEmail,
        token = j.str("idToken").orEmpty(),
    )

    private suspend fun call(endpoint: String, body: JsonObject): Pair<JsonObject?, AuthResult.Fail?> =
        withContext(Dispatchers.IO) {
            try {
                val conn = (URL("$BASE:$endpoint?key=${BuildConfig.FIREBASE_API_KEY}").openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 15_000
                    readTimeout = 20_000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                }
                conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
                val status = conn.responseCode
                val stream = if (status in 200..299) conn.inputStream else conn.errorStream
                val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
                val json = try {
                    if (text.isBlank()) JsonObject(emptyMap()) else Json.parseToJsonElement(text).jsonObject
                } catch (e: Exception) {
                    JsonObject(emptyMap())
                }
                if (status in 200..299) {
                    json to null
                } else {
                    val raw = (json["error"] as? JsonObject)?.str("message").orEmpty()
                    val code = raw.substringBefore(" :").trim()
                    null to AuthResult.Fail(friendly(code), code)
                }
            } catch (e: IOException) {
                null to AuthResult.Fail("No internet connection. Please try again.", "network")
            } catch (e: Exception) {
                null to AuthResult.Fail(GENERIC)
            }
        }

    private const val GENERIC = "Something went wrong. Please try again."

    private fun friendly(code: String): String = when {
        code == "EMAIL_EXISTS" -> "An account with this email already exists. Try signing in."
        code == "INVALID_LOGIN_CREDENTIALS" || code == "INVALID_PASSWORD" || code == "EMAIL_NOT_FOUND" ->
            "Incorrect email or password."
        code.startsWith("WEAK_PASSWORD") -> "Please choose a stronger password (at least 8 characters)."
        code.startsWith("TOO_MANY_ATTEMPTS") -> "Too many attempts. Please wait a bit and try again."
        code == "INVALID_EMAIL" -> "That email address doesn't look right."
        code == "USER_DISABLED" -> "This account has been disabled."
        code == "INVALID_ID_TOKEN" || code == "TOKEN_EXPIRED" -> "Your session expired. Please sign in again."
        else -> GENERIC
    }
}
