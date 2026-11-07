package com.betterx.app.data

import com.betterx.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import java.net.HttpURLConnection
import java.net.URL

data class CoachContext(
    val nickname: String,
    val scores: Map<String, Int>,
    val concerns: List<String>,
    val partnered: Boolean,
    val todayIds: List<String>,
    val focus: String,
    val anxietyAvg: Double?,
    val streak: Int,
)

data class CoachReply(val text: String, val exerciseId: String? = null)

interface CoachProvider {
    suspend fun reply(history: List<ChatMsg>, ctx: CoachContext): CoachReply
}

fun buildCoachContext(d: AppData, day: Long): CoachContext {
    val plan = Planner.planFor(d, day)
    val r = d.checkIns.filter { it.day >= day - 7 }
    return CoachContext(
        nickname = d.nickname,
        scores = d.scores,
        concerns = d.concerns,
        partnered = d.relationship != "Single",
        todayIds = plan.items.map { it.id },
        focus = plan.focus,
        anxietyAvg = if (r.isEmpty()) null else r.map { it.anxiety }.average(),
        streak = Planner.streak(d, day),
    )
}

/** System prompt to use if you connect an LLM through your own backend. */
object CoachPrompt {
    const val SYSTEM = """You are the BetterX coach: a warm, discreet, non-judgmental sexual-wellness coach for adults.
Give practical, evidence-informed guidance on confidence, communication, anxiety, desire, body awareness and connection.
Keep replies short (3-6 sentences), personal, and end with one gentle question or one small action.
Never write sexually explicit content. Never diagnose. Never advise stopping or changing medication.
Always encourage seeing a clinician for pain, bleeding, sudden changes, or problems lasting more than about three months.
If the user mentions self-harm, suicide, abuse, coercion or assault, respond with care, put their safety first and point to emergency services and crisis resources.
Respect consent in every answer. Remind the user you offer education and coaching, not medical care, when relevant."""
}

/** Crisis and abuse replies. They are written by us, never sent to an AI, and always win. */
object CoachSafety {
    private val crisis = Regex("""suicid|kill myself|end my life|want to die|self.?harm|hurt myself|don'?t want to live""")
    private val abuse = Regex("""\brape(d)?\b|sexual(ly)? assault|assaulted|\babus(e|ed|ive)\b|coerc|forced me|forces me|without my consent|made me do|didn'?t consent|pressures? me into""")

    /** Safety replies that must come first, whatever coach (local or remote) is in use. */
    fun safetyReply(text: String): CoachReply? {
        val t = text.lowercase()
        return when {
            crisis.containsMatchIn(t) -> CoachReply(
                "I'm really glad you told me, and I'm concerned about your safety. You deserve support from a real person right now. " +
                    "If you might act on these thoughts, please contact your local emergency number or a crisis line. " +
                    "In the US you can call or text 988; findahelpline.com lists services in other countries. " +
                    "If you can, reach out to someone you trust and stay with them. I'm here to keep talking, too.",
            )
            abuse.containsMatchIn(t) -> CoachReply(
                "Thank you for trusting me with this. What you describe isn't your fault, and your consent matters, always. " +
                    "Nothing in BetterX should ever be used to push you into something you don't want. " +
                    "If you're in danger, please contact local emergency services. In the US, RAINN (1-800-656-4673) offers free, confidential support; " +
                    "findahelpline.com lists options elsewhere. Would you like to talk about what happened or how you're feeling?",
            )
            else -> null
        }
    }
}

/** Calls YOUR backend, which should hold the LLM key and apply CoachPrompt.SYSTEM. Shows a short retry message if it can't be reached. */
class ProxyCoach(private val url: String) : CoachProvider {
    override suspend fun reply(history: List<ChatMsg>, ctx: CoachContext): CoachReply {
        // Safety replies never leave the device and always win.
        CoachSafety.safetyReply(history.lastOrNull { it.fromUser }?.text.orEmpty())?.let { return it }
        return withContext(Dispatchers.IO) {
            try {
                val body = buildJsonObject {
                    put("system", CoachPrompt.SYSTEM)
                    putJsonObject("context") {
                        put("nickname", ctx.nickname)
                        put("focus", ctx.focus)
                        put("partnered", ctx.partnered)
                        put("streak", ctx.streak)
                        putJsonObject("scores") { ctx.scores.forEach { (k, v) -> put(k, v) } }
                    }
                    putJsonArray("messages") {
                        history.takeLast(20).forEach { m ->
                            addJsonObject {
                                put("role", if (m.fromUser) "user" else "assistant")
                                put("content", m.text)
                            }
                        }
                    }
                }.toString()
                val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 10_000
                    readTimeout = 30_000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                }
                conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                if (conn.responseCode !in 200..299) error("HTTP ${conn.responseCode}")
                val text = conn.inputStream.bufferedReader().use { it.readText() }
                CoachReply(Json.parseToJsonElement(text).jsonObject["reply"]!!.jsonPrimitive.content)
            } catch (e: Exception) {
                CoachReply("I couldn't reach the coach right now. Please check your internet connection and try again.")
            }
        }
    }
}

/** Answers with Google Gemini. Safety replies stay on-device; if Gemini can't be reached a short retry message is shown. */
class GeminiCoach(private val apiKey: String, private val model: String) : CoachProvider {
    override suspend fun reply(history: List<ChatMsg>, ctx: CoachContext): CoachReply {
        CoachSafety.safetyReply(history.lastOrNull { it.fromUser }?.text.orEmpty())?.let { return it }
        return withContext(Dispatchers.IO) {
            try {
                CoachReply(ask(history, ctx))
            } catch (e: Exception) {
                android.util.Log.w("GeminiCoach", "Gemini failed: ${e.message}")
                CoachReply("I couldn't reach the coach right now. Please check your internet connection and try again.")
            }
        }
    }

    private fun systemText(ctx: CoachContext): String = buildString {
        append(CoachPrompt.SYSTEM)
        append("\n\nAbout this user (private, use only to personalise):")
        if (ctx.nickname.isNotBlank()) append("\n- Nickname: ${ctx.nickname}")
        append("\n- In a relationship: ${if (ctx.partnered) "yes" else "no"}")
        if (ctx.concerns.isNotEmpty()) append("\n- Main concerns: ${ctx.concerns.joinToString()}")
        if (ctx.scores.isNotEmpty()) append("\n- Assessment scores (0-100): ${ctx.scores.entries.joinToString { "${it.key} ${it.value}" }}")
        append("\n- Current focus: ${ctx.focus}")
        append("\n- Practice streak: ${ctx.streak} days")
        append("\nReply in plain text, no markdown, no lists, 3-6 short sentences.")
    }

    private fun ask(history: List<ChatMsg>, ctx: CoachContext): String {
        // Gemini wants alternating turns that start with the user.
        val turns = ArrayList<Pair<String, String>>()
        history.takeLast(20).forEach { m ->
            val role = if (m.fromUser) "user" else "model"
            if (turns.isEmpty() && role == "model") return@forEach
            if (turns.isNotEmpty() && turns.last().first == role) {
                turns[turns.size - 1] = role to (turns.last().second + "\n" + m.text)
            } else turns.add(role to m.text)
        }
        if (turns.isEmpty() || turns.last().first != "user") error("no user turn")

        val body = buildJsonObject {
            putJsonObject("systemInstruction") { putJsonArray("parts") { addJsonObject { put("text", systemText(ctx)) } } }
            putJsonArray("contents") {
                turns.forEach { (role, text) ->
                    addJsonObject {
                        put("role", role)
                        putJsonArray("parts") { addJsonObject { put("text", text) } }
                    }
                }
            }
            putJsonObject("generationConfig") {
                put("temperature", 0.7)
                put("maxOutputTokens", 600)
            }
            // Sexual-wellness talk is the point of the app; only the highest-risk explicit content is blocked.
            putJsonArray("safetySettings") {
                addJsonObject {
                    put("category", "HARM_CATEGORY_SEXUALLY_EXPLICIT")
                    put("threshold", "BLOCK_ONLY_HIGH")
                }
            }
        }.toString()

        val conn = (URL("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 10_000
            readTimeout = 30_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("x-goog-api-key", apiKey)
        }
        try {
            conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            val text = (if (code in 200..299) conn.inputStream else conn.errorStream)?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code !in 200..299) error("HTTP $code ${text.take(200)}")
            val parts = Json.parseToJsonElement(text).jsonObject["candidates"]?.jsonArray?.firstOrNull()
                ?.jsonObject?.get("content")?.jsonObject?.get("parts")?.jsonArray
            val reply = parts?.joinToString("") { it.jsonObject["text"]?.jsonPrimitive?.content.orEmpty() }?.trim().orEmpty()
            if (reply.isEmpty()) error("empty reply (blocked?)")
            return reply
        } finally {
            conn.disconnect()
        }
    }
}

fun defaultCoach(): CoachProvider = when {
    BuildConfig.GEMINI_API_KEY.isNotBlank() -> GeminiCoach(BuildConfig.GEMINI_API_KEY, BuildConfig.GEMINI_MODEL)
    BuildConfig.COACH_API_URL.isNotBlank() -> ProxyCoach(BuildConfig.COACH_API_URL)
    else -> object : CoachProvider {
        override suspend fun reply(history: List<ChatMsg>, ctx: CoachContext): CoachReply =
            CoachSafety.safetyReply(history.lastOrNull { it.fromUser }?.text.orEmpty())
                ?: CoachReply("The coach isn't set up yet. Please try again later.")
    }
}
