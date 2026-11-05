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

    fun wipe() {
        Persistence.wipe(ctx)
        Reminders.cancel(ctx)
        data = AppData.fresh()
    }
}
