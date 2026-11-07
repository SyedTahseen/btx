package com.betterx.app.data

import java.time.LocalDate
import kotlinx.serialization.Serializable

fun today(): Long = LocalDate.now().toEpochDay()

@Serializable
data class CheckIn(
    val day: Long,
    val mood: Int,
    val confidence: Int,
    val anxiety: Int,
    val connection: Int,
    val note: String = "",
)

@Serializable
data class Completion(val day: Long, val exerciseId: String, val minutes: Int, val rating: Int = 0)

@Serializable
data class ChatMsg(val fromUser: Boolean, val text: String, val ts: Long, val exerciseId: String? = null)

@Serializable
data class GhostReply(val alias: String, val text: String, val ts: Long, val mine: Boolean = false)

@Serializable
data class GhostPost(
    val id: String,
    val alias: String,
    val topic: String,
    val title: String,
    val body: String,
    val ts: Long,
    val replies: List<GhostReply> = emptyList(),
    val hearts: Int = 0,
    val mine: Boolean = false,
)

@Serializable
data class ScoreSnapshot(val day: Long, val scores: Map<String, Int>)

@Serializable
data class AppData(
    val onboarded: Boolean = false,
    val nickname: String = "",
    val relationship: String = "Single",
    val about: String = "",
    val concerns: List<String> = emptyList(),
    val answers: Map<String, Int> = emptyMap(),
    val scores: Map<String, Int> = emptyMap(),
    val scoreHistory: List<ScoreSnapshot> = emptyList(),
    val startDay: Long = 0L,
    val checkIns: List<CheckIn> = emptyList(),
    val completions: List<Completion> = emptyList(),
    val lessonsDone: Set<String> = emptySet(),
    val chat: List<ChatMsg> = emptyList(),
    val coachUsageDay: Long = 0L,
    val coachUsageCount: Int = 0,
    val ghost: List<GhostPost> = emptyList(),
    val hearted: Set<String> = emptySet(),
    val hidden: Set<String> = emptySet(),
    val remindersEnabled: Boolean = true,
    val reminderHour: Int = 21,
    val reminderMinute: Int = 0,
    val weeklyCheckInEnabled: Boolean = false,
    val weeklyCheckInDow: Int = 7,
    val discreetNotifications: Boolean = true,
    val premium: Boolean = false,
    // Account: identity + a Firebase refresh token (this file is encrypted). Plans and check-ins stay on the device.
    val signedIn: Boolean = false,
    val ownerUid: String = "",
    val ownerEmail: String = "",
    val refreshToken: String = "",
) {
    companion object {
        fun fresh() = AppData(startDay = today())
    }
}
