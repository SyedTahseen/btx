package com.betterx.app.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class Store(private val ctx: Context) {
    var data by mutableStateOf(Persistence.load(ctx))
        private set

    fun update(reschedule: Boolean = false, f: (AppData) -> AppData) {
        data = f(data)
        Persistence.save(ctx, data)
        if (reschedule) Reminders.schedule(ctx, data)
    }

    /**
     * Signing in as a different account than the one that owns the local data starts fresh.
     * [remote] is the profile saved in Firebase (if any); it wins over what is on this device.
     */
    fun signIn(uid: String, email: String, refreshToken: String, remote: RemoteProfile? = null) {
        update(reschedule = true) { cur ->
            val base = if (cur.ownerUid.isNotEmpty() && cur.ownerUid != uid) AppData.fresh() else cur
            val withProfile = if (remote == null) base else base.copy(
                nickname = remote.nickname.take(20),
                relationship = remote.relationship,
                about = remote.about.take(200),
                concerns = remote.concerns,
            )
            withProfile.copy(signedIn = true, ownerUid = uid, ownerEmail = email, refreshToken = refreshToken)
        }
    }

    fun signOut() {
        update(reschedule = true) { it.copy(signedIn = false, refreshToken = "") }
    }

    /** Erase everything including the sign-in (used after account deletion). */
    fun wipeAll() {
        Persistence.wipe(ctx)
        Reminders.cancel(ctx)
        data = AppData.fresh()
    }

    /**
     * Gets a fresh Firebase ID token using the stored refresh token (and keeps the rotated one).
     * Accounts that signed in before profile sync existed have no refresh token and need to sign in once more.
     */
    suspend fun freshSession(): AuthResult {
        val rt = data.refreshToken
        if (rt.isBlank()) {
            return AuthResult.Fail("Please sign out and sign back in once to turn on online profile saving.", "no_session")
        }
        val r = Auth.refresh(rt)
        if (r is AuthResult.Ok && r.refreshToken.isNotEmpty() && r.refreshToken != rt) {
            update { it.copy(refreshToken = r.refreshToken) }
        }
        return r
    }
}
