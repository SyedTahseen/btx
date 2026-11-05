package com.betterx.app.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.betterx.app.MainActivity
import com.betterx.app.R
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

object Reminders {
    const val CHANNEL = "betterx_reminders"
    private const val WORK = "betterx_daily"

    fun ensureChannel(ctx: Context) {
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, "Reminders", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Practice, check-in and important-date reminders"
                lockscreenVisibility = android.app.Notification.VISIBILITY_PRIVATE
            },
        )
    }

    fun schedule(ctx: Context, d: AppData) {
        val wm = WorkManager.getInstance(ctx)
        val anything = d.onboarded && (d.remindersEnabled || d.weeklyCheckInEnabled || d.dates.isNotEmpty())
        if (!anything) {
            wm.cancelUniqueWork(WORK)
            return
        }
        val now = LocalDateTime.now()
        var next = now.withHour(d.reminderHour).withMinute(d.reminderMinute).withSecond(0).withNano(0)
        if (!next.isAfter(now)) next = next.plusDays(1)
        val delay = Duration.between(now, next).toMillis()
        val req = PeriodicWorkRequestBuilder<ReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .build()
        wm.enqueueUniquePeriodicWork(WORK, ExistingPeriodicWorkPolicy.UPDATE, req)
    }

    fun cancel(ctx: Context) {
        WorkManager.getInstance(ctx).cancelUniqueWork(WORK)
    }
}

class ReminderWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        val d = Persistence.load(applicationContext)
        if (!d.onboarded) return Result.success()
        if (!NotificationManagerCompat.from(applicationContext).areNotificationsEnabled()) return Result.success()

        val today = LocalDate.now()
        val ep = today.toEpochDay()
        val discreet = d.discreetNotifications

        if (d.remindersEnabled && d.completions.none { it.day == ep }) {
            val plan = Planner.planFor(d, ep)
            val mins = plan.items.sumOf { it.minutes }
            if (discreet) {
                post(1, "Daily check-in", "Your $mins minutes are ready.")
            } else {
                post(1, "Today's practice", "${plan.items.first().title} · $mins min")
            }
        }

        if (d.weeklyCheckInEnabled && today.dayOfWeek.value == d.weeklyCheckInDow) {
            post(2, if (discreet) "Weekly reminder" else "Relationship check-in", "A good time for your weekly check-in.")
        }

        d.dates.forEachIndexed { i, imp ->
            val base = LocalDate.ofEpochDay(imp.day)
            val candidates = if (imp.yearly) listOf(base.withYear(today.year), base.withYear(today.year + 1)) else listOf(base)
            val when_ = when {
                candidates.any { it == today } -> "today"
                candidates.any { it == today.plusDays(1) } -> "tomorrow"
                else -> null
            }
            if (when_ != null) {
                post(10 + i, if (discreet) "Reminder" else imp.label, if (discreet) "Something you set is coming up $when_." else "${imp.label} is $when_.")
            }
        }
        return Result.success()
    }

    private fun post(id: Int, title: String, text: String) {
        val ctx = applicationContext
        val open = PendingIntent.getActivity(
            ctx, 0, Intent(ctx, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val n = NotificationCompat.Builder(ctx, Reminders.CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setAutoCancel(true)
            .setContentIntent(open)
            .build()
        try {
            NotificationManagerCompat.from(ctx).notify(id, n)
        } catch (e: SecurityException) {
            // Permission revoked between the check and the call; nothing to do.
        }
    }
}
