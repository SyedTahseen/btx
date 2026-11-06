package com.betterx.app.data

import com.betterx.app.BuildConfig
import java.io.IOException
import java.net.URLEncoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put

sealed class AuthResult {
    /** [needsConfirm] is true when the account exists but the email has not been verified yet. */
    data class Ok(
        val uid: String = "",
        val email: String = "",
        val token: String = "",
        val refreshToken: String = "",
        val needsConfirm: Boolean = false,
    ) : AuthResult()

    data class Fail(val message: String, val code: String = "") : AuthResult()
}

/**
 * Accounts through the Firebase Authentication REST API (no SDK, so the app stays small):
 * email + password with email verification, password change and password reset.
 * Identity and the basic profile (see [ProfileSync]) live in Firebase. Plans, check-ins and chats stay on the device.
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

    /** Trades the stored refresh token for a fresh ID token (ID tokens only last an hour). */
    suspend fun refresh(refreshToken: String): AuthResult {
        return try {
            val body = "grant_type=refresh_token&refresh_token=" + URLEncoder.encode(refreshToken, "UTF-8")
            val r = Http.request(
                "POST",
                "https://securetoken.googleapis.com/v1/token?key=${BuildConfig.FIREBASE_API_KEY}",
                body,
                contentType = "application/x-www-form-urlencoded",
            )
            if (!r.ok) return failFrom(r)
            AuthResult.Ok(
                uid = r.json.str("user_id").orEmpty(),
                token = r.json.str("id_token").orEmpty(),
                refreshToken = r.json.str("refresh_token").orEmpty(),
            )
        } catch (e: IOException) {
            AuthResult.Fail("No internet connection. Please try again.", "network")
        } catch (e: Exception) {
            AuthResult.Fail(GENERIC)
        }
    }

    /**
     * Changes the password. The current password is checked first by signing in with it,
     * so a stolen unlocked phone can't change it silently. Returns the new session (Firebase
     * revokes the old refresh tokens when the password changes).
     */
    suspend fun changePassword(email: String, currentPassword: String, newPassword: String): AuthResult {
        val cur = signIn(email, currentPassword)
        if (cur is AuthResult.Fail) {
            return if (cur.code == "INVALID_LOGIN_CREDENTIALS" || cur.code == "INVALID_PASSWORD") {
                AuthResult.Fail("Your current password is incorrect.", cur.code)
            } else cur
        }
        cur as AuthResult.Ok
        val (json, fail) = call("update", buildJsonObject {
            put("idToken", cur.token)
            put("password", newPassword)
            put("returnSecureToken", true)
        })
        if (fail != null) return fail
        val next = session(json!!, email)
        return next.copy(uid = next.uid.ifBlank { cur.uid }, token = next.token.ifBlank { cur.token })
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
        refreshToken = j.str("refreshToken").orEmpty(),
    )

    private fun failFrom(r: HttpResp): AuthResult.Fail {
        val raw = (r.json["error"] as? JsonObject)?.str("message").orEmpty()
        val code = raw.substringBefore(" :").trim()
        return AuthResult.Fail(friendly(code), code)
    }

    private suspend fun call(endpoint: String, body: JsonObject): Pair<JsonObject?, AuthResult.Fail?> =
        try {
            val r = Http.request("POST", "$BASE:$endpoint?key=${BuildConfig.FIREBASE_API_KEY}", body.toString())
            if (r.ok) r.json to null else null to failFrom(r)
        } catch (e: IOException) {
            null to AuthResult.Fail("No internet connection. Please try again.", "network")
        } catch (e: Exception) {
            null to AuthResult.Fail(GENERIC)
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
        code == "INVALID_ID_TOKEN" || code == "TOKEN_EXPIRED" || code == "INVALID_REFRESH_TOKEN" ||
            code == "CREDENTIAL_TOO_OLD_LOGIN_AGAIN" || code == "USER_NOT_FOUND" ->
            "Your session expired. Please sign out and sign in again."
        else -> GENERIC
    }
}
