package com.betterx.app.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.betterx.app.data.AppData
import com.betterx.app.data.Planner
import com.betterx.app.data.today
import java.util.Locale

/** What a notice asks the user to do. Notices with an action count as "new" (the dot on the bell). */
enum class NoticeAction(val label: String) { CheckIn("Check in"), Practice("Start"), Reassess("Re-assess"), Settings("Open settings") }

data class AppNotice(val id: String, val title: String, val body: String, val action: NoticeAction? = null)

private fun clock(h: Int, m: Int): String {
    val hour12 = if (h % 12 == 0) 12 else h % 12
    return String.format(Locale.US, "%d:%02d %s", hour12, m, if (h < 12) "am" else "pm")
}

/** Builds the in-app notification list from what's on the device (no server needed). Most important first. */
fun notificationsFor(d: AppData, day: Long = today()): List<AppNotice> {
    val out = mutableListOf<AppNotice>()
    val plan = Planner.planFor(d, day)
    val done = d.completions.filter { it.day == day }.map { it.exerciseId }.toSet()
    val left = plan.items.filter { it.id !in done }
    val streak = Planner.streak(d, day)
    val sinceAssess = d.scoreHistory.lastOrNull()?.let { day - it.day } ?: 0L

    if (d.checkIns.none { it.day == day }) {
        out += AppNotice("checkin", "How are you feeling today?", "A one-minute check-in keeps your plan adapting.", NoticeAction.CheckIn)
    }
    if (left.isNotEmpty()) {
        val n = left.size
        out += AppNotice(
            "practice",
            if (n == 1) "1 practice left today" else "$n practices left today",
            "Up next: ${left.first().title} (${left.first().minutes} min).",
            NoticeAction.Practice,
        )
    } else if (plan.items.isNotEmpty()) {
        out += AppNotice("alldone", "Today's plan is done", "Nice work. See you tomorrow.")
    }
    if (sinceAssess >= 14) {
        out += AppNotice("reassess", "Time to re-assess", "It's been $sinceAssess days. Re-assess to see your progress and refresh your plan.", NoticeAction.Reassess)
    }
    if (streak >= 3) {
        out += AppNotice("streak", "$streak-day streak", "You've practiced $streak days in a row. Keep it going.")
    }
    if (!d.remindersEnabled) {
        out += AppNotice("remindersoff", "Daily reminder is off", "Turn it on to get a gentle nudge each day.", NoticeAction.Settings)
    } else {
        out += AppNotice("reminderon", "Daily reminder is on", "We'll remind you at ${clock(d.reminderHour, d.reminderMinute)}.")
    }
    return out
}

@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    onCheckIn: () -> Unit,
    onExercise: (String) -> Unit,
    onReassess: () -> Unit,
    onProfile: () -> Unit,
) {
    val d = LocalStore.current.data
    val day = today()
    val notices = notificationsFor(d, day)
    val next = Planner.planFor(d, day).items.firstOrNull { e -> d.completions.none { it.day == day && it.exerciseId == e.id } }

    ScrollPage("Notifications", onBack) {
        notices.forEach { n ->
            SoftCard {
                Text(n.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    n.body,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
                val a = n.action
                if (a != null) {
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = {
                            when (a) {
                                NoticeAction.CheckIn -> onCheckIn()
                                NoticeAction.Practice -> next?.let { onExercise(it.id) }
                                NoticeAction.Reassess -> onReassess()
                                NoticeAction.Settings -> onProfile()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(a.label) }
                }
            }
        }
    }
}
