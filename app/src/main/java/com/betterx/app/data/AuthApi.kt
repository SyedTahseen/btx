package com.betterx.app.data

import com.betterx.app.BuildConfig
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

sealed class AuthResult {
    data class Ok(val uid: String = "", val email: String = "", val idToken: String = "") : AuthResult()
    data class Fail(val message: String, val code: String = "") : AuthResult()
}

/**
 * Email + password accounts through the Firebase Authentication REST API (no SDK, so the app stays small).
 * Only your identity lives there. Health data never leaves the device.
 */
object Auth {
    private const val BASE = "https://identitytoolkit.googleapis.com/v1/accounts"

    val configured: Boolean get() = BuildConfig.FIREBASE_API_KEY.isNotBlank()

    suspend fun signUp(email: String, password: String): AuthResult = credentials("signUp", email, password)

    suspend fun signIn(email: String, password: String): AuthResult = credentials("signInWithPassword", email, password)

    /** Always looks successful for unknown emails so the form can't be used to discover accounts. */
    suspend fun sendReset(email: String): AuthResult {
        val r = post("sendOobCode", buildJsonObject {
            put("requestType", "PASSWORD_RESET")
            put("email", email)
        })
        return if (r is AuthResult.Fail && r.code == "EMAIL_NOT_FOUND") AuthResult.Ok() else r
    }

    suspend fun delete(idToken: String): AuthResult = post("delete", buildJsonObject { put("idToken", idToken) })

    private suspend fun credentials(endpoint: String, email: String, password: String): AuthResult =
        post(endpoint, buildJsonObject {
            put("email", email)
            put("password", password)
            put("returnSecureToken", true)
        })

    private suspend fun post(endpoint: String, body: JsonObject): AuthResult = withContext(Dispatchers.IO) {
        if (!configured) return@withContext AuthResult.Fail("Accounts aren't set up in this build yet.")
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
            val json = if (text.isBlank()) JsonObject(emptyMap()) else Json.parseToJsonElement(text).jsonObject
            if (status in 200..299) {
                AuthResult.Ok(
                    uid = json["localId"]?.jsonPrimitive?.content.orEmpty(),
                    email = json["email"]?.jsonPrimitive?.content.orEmpty(),
                    idToken = json["idToken"]?.jsonPrimitive?.content.orEmpty(),
                )
            } else {
                val raw = json["error"]?.jsonObject?.get("message")?.jsonPrimitive?.content.orEmpty()
                val code = raw.substringBefore(" :").trim()
                AuthResult.Fail(friendly(code), code)
            }
        } catch (e: IOException) {
            AuthResult.Fail("No internet connection. Please try again.")
        } catch (e: Exception) {
            AuthResult.Fail("Something went wrong. Please try again.")
        }
    }

    private fun friendly(code: String): String = when {
        code == "EMAIL_EXISTS" -> "An account with this email already exists. Try signing in."
        code == "INVALID_LOGIN_CREDENTIALS" || code == "INVALID_PASSWORD" || code == "EMAIL_NOT_FOUND" ->
            "Incorrect email or password."
        code.startsWith("WEAK_PASSWORD") -> "Please choose a stronger password (at least 8 characters)."
        code.startsWith("TOO_MANY_ATTEMPTS") -> "Too many attempts. Please wait a bit and try again."
        code == "INVALID_EMAIL" -> "That email address doesn't look right."
        code == "USER_DISABLED" -> "This account has been disabled."
        code == "OPERATION_NOT_ALLOWED" -> "Email sign-in isn't enabled for this app yet."
        code.contains("API key", ignoreCase = true) || code.contains("API_KEY") ->
            "Accounts aren't configured correctly (API key)."
        else -> "Something went wrong. Please try again."
    }
}
