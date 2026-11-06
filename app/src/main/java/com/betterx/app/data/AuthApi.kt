package com.betterx.app.data

import com.betterx.app.BuildConfig
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put

sealed class AuthResult {
    /** [needsConfirm] is true when sign-up worked but the email must be confirmed with a code first. */
    data class Ok(
        val uid: String = "",
        val email: String = "",
        val token: String = "",
        val needsConfirm: Boolean = false,
    ) : AuthResult()

    data class Fail(val message: String, val code: String = "") : AuthResult()
}

/**
 * Email + password accounts through the Supabase Auth REST API (no SDK, so the app stays small).
 * Only identity lives in Supabase Auth. Health data never leaves the device.
 *
 * Password reset and email confirmation use a 6-digit code typed into the app, so no deep links are needed.
 * (Add {{ .Token }} to the Supabase email templates, see README.)
 */
object Auth {
    val configured: Boolean
        get() = BuildConfig.SUPABASE_URL.isNotBlank() && BuildConfig.SUPABASE_KEY.isNotBlank()

    suspend fun signUp(email: String, password: String): AuthResult =
        call("POST", "/auth/v1/signup", buildJsonObject {
            put("email", email)
            put("password", password)
        }).let { (json, fail) -> fail ?: toSession(json!!, email) }

    suspend fun signIn(email: String, password: String): AuthResult =
        call("POST", "/auth/v1/token?grant_type=password", buildJsonObject {
            put("email", email)
            put("password", password)
        }).let { (json, fail) -> fail ?: requireSession(json!!, email) }

    /** Sends a recovery code. Looks successful for unknown emails so it can't be used to discover accounts. */
    suspend fun sendRecovery(email: String): AuthResult {
        val (_, fail) = call("POST", "/auth/v1/recover", buildJsonObject { put("email", email) })
        return if (fail != null && fail.code != "over_email_send_rate_limit" && fail.code != "network") AuthResult.Ok() else fail ?: AuthResult.Ok()
    }

    suspend fun resendSignup(email: String): AuthResult {
        val (_, fail) = call("POST", "/auth/v1/resend", buildJsonObject {
            put("type", "signup")
            put("email", email)
        })
        return fail ?: AuthResult.Ok()
    }

    /** [type] is "signup" or "recovery". Returns a signed-in session when the code is right. */
    suspend fun verify(type: String, email: String, code: String): AuthResult =
        call("POST", "/auth/v1/verify", buildJsonObject {
            put("type", type)
            put("email", email)
            put("token", code)
        }).let { (json, fail) -> fail ?: requireSession(json!!, email) }

    suspend fun updatePassword(token: String, newPassword: String): AuthResult {
        val (_, fail) = call("PUT", "/auth/v1/user", buildJsonObject { put("password", newPassword) }, token)
        return fail ?: AuthResult.Ok()
    }

    /** Needs the "delete-account" Edge Function (supabase/functions/delete-account) to be deployed. */
    suspend fun deleteAccount(token: String): AuthResult {
        val (_, fail) = call("POST", "/functions/v1/delete-account", buildJsonObject {}, token)
        return fail ?: AuthResult.Ok()
    }

    // ----------------------------------------------------------------- internals

    private fun JsonObject.str(key: String): String? = (this[key] as? JsonPrimitive)?.contentOrNull

    private fun toSession(j: JsonObject, fallbackEmail: String): AuthResult.Ok {
        val user = j["user"] as? JsonObject
        val token = j.str("access_token").orEmpty()
        return AuthResult.Ok(
            uid = user?.str("id") ?: j.str("id").orEmpty(),
            email = user?.str("email") ?: j.str("email") ?: fallbackEmail,
            token = token,
            needsConfirm = token.isEmpty(),
        )
    }

    private fun requireSession(j: JsonObject, fallbackEmail: String): AuthResult {
        val s = toSession(j, fallbackEmail)
        return if (s.token.isEmpty()) AuthResult.Fail("Something went wrong. Please try again.") else s
    }

    private suspend fun call(
        method: String,
        path: String,
        body: JsonObject,
        bearer: String? = null,
    ): Pair<JsonObject?, AuthResult.Fail?> = withContext(Dispatchers.IO) {
        if (!configured) {
            return@withContext null to AuthResult.Fail("Accounts aren't set up in this build yet.", "config")
        }
        try {
            val base = BuildConfig.SUPABASE_URL.trimEnd('/')
            val conn = (URL(base + path).openConnection() as HttpURLConnection).apply {
                requestMethod = method
                connectTimeout = 15_000
                readTimeout = 20_000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("apikey", BuildConfig.SUPABASE_KEY)
                if (bearer != null) setRequestProperty("Authorization", "Bearer $bearer")
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
                val code = json.str("error_code") ?: json.str("error") ?: ""
                val msg = json.str("msg") ?: json.str("error_description") ?: json.str("message") ?: ""
                null to AuthResult.Fail(friendly(code, msg, status), code)
            }
        } catch (e: IOException) {
            null to AuthResult.Fail("No internet connection. Please try again.", "network")
        } catch (e: Exception) {
            null to AuthResult.Fail("Something went wrong. Please try again.")
        }
    }

    private fun friendly(code: String, msg: String, status: Int): String {
        val m = msg.lowercase()
        return when {
            code == "invalid_credentials" || (code == "invalid_grant" && "invalid login" in m) ->
                "Incorrect email or password."
            code == "user_already_exists" || code == "email_exists" ->
                "An account with this email already exists. Try signing in."
            code == "weak_password" -> "Please choose a stronger password (at least 8 characters)."
            code == "same_password" -> "Please choose a different password from your current one."
            code == "email_not_confirmed" -> "Please confirm your email first. Check your inbox for the code."
            code == "otp_expired" || "expired or is invalid" in m || "token has expired" in m ->
                "That code isn't right or has expired. Request a new one."
            code.startsWith("over_") || status == 429 ->
                "Too many attempts. Please wait a few minutes and try again."
            code == "validation_failed" || code == "email_address_invalid" || "invalid format" in m ->
                "That email address doesn't look right."
            code == "signup_disabled" -> "New sign-ups are turned off right now."
            "api key" in m || code == "no_api_key" -> "Accounts aren't configured correctly (API key)."
            status == 404 && "function" in m -> "Account deletion isn't set up on the server yet."
            else -> "Something went wrong. Please try again."
        }
    }
}
