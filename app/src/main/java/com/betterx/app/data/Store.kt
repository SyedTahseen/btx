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
            val withProfile = if (remote == null) base else {
                val done = remote.onboarded && remote.scores.isNotEmpty()
                val scores = remote.scores.ifEmpty { base.scores }
                base.copy(
                    nickname = remote.nickname.take(20).ifBlank { base.nickname },
                    relationship = remote.relationship,
                    about = remote.about.take(200),
                    concerns = remote.concerns,
                    onboarded = done || base.onboarded,
                    answers = remote.answers.ifEmpty { base.answers },
                    scores = scores,
                    startDay = if (remote.startDay > 0L) remote.startDay else base.startDay,
                    remindersEnabled = if (done) remote.remindersEnabled else base.remindersEnabled,
                    // Re-assessments on this device are not uploaded, so start the history from the saved scores.
                    scoreHistory = if (base.scoreHistory.isNotEmpty()) base.scoreHistory
                    else if (done) listOf(ScoreSnapshot(if (remote.startDay > 0L) remote.startDay else base.startDay, scores))
                    else emptyList(),
                )
            }
            // No saved profile in Firebase yet (new account): upload what we have as soon as we're in.
            withProfile.copy(
                signedIn = true, ownerUid = uid, ownerEmail = email, refreshToken = refreshToken,
                profileDirty = remote == null || base.profileDirty,
            )
        }
    }

    /** Everything that is stored online, built from the device copy. */
    fun remoteProfile(d: AppData = data) = RemoteProfile(
        nickname = d.nickname,
        relationship = d.relationship,
        concerns = d.concerns,
        about = d.about,
        onboarded = d.onboarded,
        answers = d.answers,
        scores = d.scores,
        startDay = d.startDay,
        remindersEnabled = d.remindersEnabled,
    )

    /** Marks the profile as changed so it is uploaded (and retried later if it fails). */
    fun markProfileChanged() = update { it.copy(profileDirty = true) }

    /** Uploads the profile if there are unsynced changes. Safe to call repeatedly. */
    suspend fun pushProfileIfNeeded(): SyncResult<Unit>? {
        val d = data
        if (!d.signedIn || !d.profileDirty || d.ownerUid.isBlank()) return null
        val sent = remoteProfile(d)
        val s = freshSession()
        val res: SyncResult<Unit> = when (s) {
            is AuthResult.Ok -> ProfileSync.save(s.token, s.uid.ifBlank { d.ownerUid }, sent)
            is AuthResult.Fail -> SyncResult.Fail(s.message)
        }
        // Only clear the flag if nothing changed while we were uploading.
        if (res is SyncResult.Ok && remoteProfile() == sent) update { it.copy(profileDirty = false) }
        return res
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
