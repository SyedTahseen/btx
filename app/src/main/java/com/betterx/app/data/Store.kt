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

    /** Signing in as a different account than the one that owns the local data starts fresh. */
    fun signIn(uid: String, email: String) {
        update(reschedule = true) { cur ->
            val base = if (cur.ownerUid.isNotEmpty() && cur.ownerUid != uid) AppData.fresh() else cur
            base.copy(signedIn = true, ownerUid = uid, ownerEmail = email)
        }
    }

    fun signOut() {
        update(reschedule = true) { it.copy(signedIn = false) }
    }

    /** Erase everything except the sign-in. */
    fun wipe() {
        Persistence.wipe(ctx)
        Reminders.cancel(ctx)
        val keep = data
        data = AppData.fresh().copy(signedIn = keep.signedIn, ownerUid = keep.ownerUid, ownerEmail = keep.ownerEmail)
        Persistence.save(ctx, data)
    }

    /** Erase everything including the sign-in (used after account deletion). */
    fun wipeAll() {
        Persistence.wipe(ctx)
        Reminders.cancel(ctx)
        data = AppData.fresh()
    }
}
