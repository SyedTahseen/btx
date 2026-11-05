package com.betterx.app.data

import com.betterx.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
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

object LocalCoach : CoachProvider {
    private val crisis = Regex("""suicid|kill myself|end my life|want to die|self.?harm|hurt myself|don'?t want to live""")
    private val abuse = Regex("""\brape(d)?\b|sexual(ly)? assault|assaulted|\babus(e|ed|ive)\b|coerc|forced me|forces me|without my consent|made me do|didn'?t consent|pressures? me into""")
    private val medical = Regex("""\bpain(ful)?\b|\bhurts?\b|bleed|\bblood\b|burning|discharge|\blump\b|swell|infection|\bstd\b|\bsti\b|\bnumb""")
    private val meds = Regex("""medication|medicine|antidepress|\bssri|prescri|\bmeds\b""")

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

    override suspend fun reply(history: List<ChatMsg>, ctx: CoachContext): CoachReply {
        delay(500)
        val last = history.lastOrNull { it.fromUser }?.text.orEmpty()
        return respond(last, ctx, history.count { it.fromUser })
    }

    fun respond(text: String, ctx: CoachContext, turn: Int): CoachReply {
        safetyReply(text)?.let { return it }
        val t = text.lowercase()
        fun has(vararg k: String) = k.any { t.contains(it) }
        val name = ctx.nickname.ifBlank { "there" }
        val v = turn % 2
        val partnerEx = if (ctx.partnered) "checkin_talk" else "needs_journal"

        if (medical.containsMatchIn(t)) {
            val med = if (meds.containsMatchIn(t)) " If a medication could be involved, please talk to your prescriber before changing anything, and don't stop it on your own." else ""
            return CoachReply(
                "Thanks for being open about that. Pain, bleeding, unusual discharge or a sudden change in how your body responds can have medical causes that are very treatable, " +
                    "and no app or exercise should replace a check-up. I'd suggest seeing a doctor or sexual-health clinician soon.$med " +
                    "In the meantime, keep things gentle and pressure-free, and stop anything that hurts. Want a calming practice that's safe to do while you arrange an appointment?",
                "body_scan",
            )
        }
        if (meds.containsMatchIn(t)) {
            return CoachReply(
                "Some medications can affect desire, arousal or climax, and it's common. Please don't stop or change anything on your own. " +
                    "Bring it up with your prescriber; there are often alternatives or adjustments. Meanwhile I can help with calm and communication skills that make things easier. " +
                    "When did you first notice a change?",
                "box_breath",
            )
        }

        return when {
            has("hello", "hi ", "hey") && t.length < 20 || t.trim() in listOf("hi", "hey", "hello") -> CoachReply(
                "Hi $name! I'm your BetterX coach. Tell me what's on your mind, whether it's anxiety, desire, communication or something else. Everything here stays private on your device.",
            )
            has("thank") -> CoachReply("Anytime. Small steps count. Want to do a quick check-in on how today felt?")
            has("today", "my plan", "what should i do", "where do i start", "where to start", "next step") -> {
                val ex = ctx.todayIds.firstOrNull()?.let { Content.exercise(it) }
                if (ex != null) CoachReply(
                    "Today's focus is ${Content.label(ctx.focus).lowercase()}. Start with \"${ex.title}\" (${ex.minutes} min). ${ex.why} How are you feeling about starting?",
                    ex.id,
                ) else CoachReply("Let's start with how you're feeling today. What's on your mind?")
            }
            has("progress", "how am i doing", "improv", "streak") -> {
                val a = ctx.anxietyAvg?.let { " Your recent pressure check-ins average ${"%.1f".format(it)} out of 5." }.orEmpty()
                CoachReply(
                    if (ctx.streak > 0) "You're on a ${ctx.streak}-day practice streak. That consistency is what creates change.$a What feels different, even slightly?"
                    else "Every plan starts with a first rep. Doing even 5 minutes today restarts your streak.$a What would make it easy to begin?",
                )
            }
            has("anxi", "nervous", "panic", "pressure", "perform", "worry", "stress", "choke", "freeze") -> {
                val calm = ctx.scores["Calm"]
                val note = if (calm != null && calm < 55) " Your Calm score from the assessment ($calm/100) points the same way, so we've made it a priority." else ""
                CoachReply(
                    if (v == 0)
                        "That pressure is really common, and it's a loop rather than a verdict on you: worry creates tension, tension changes how your body responds, and that feeds the worry.$note The fastest way to loosen it is to train your body's calm response outside the moment. Try the 5-minute breathing reset, then tell me: when does the worry show up first, before, during or after?"
                    else
                        "Thanks for naming that. Performance worry pulls your attention away from sensation, which makes everything harder.$note A short daily calm practice plus shifting focus from How am I doing? to What do I notice? helps a lot. When did you last feel relaxed and present?",
                    "box_breath",
                )
            }
            has("last longer", "too fast", "too quick", "early", "premature", "finish fast", "control") -> CoachReply(
                "Wanting more control over pace is one of the most common goals I hear, and it's very trainable. Two things matter most: noticing your arousal early so you have choices, and staying relaxed rather than braced. " +
                    "The arousal-scale practice builds exactly that, and pelvic floor awareness supports it. Small, patient reps beat willpower. How long has this been a concern, and does it happen in all situations or only some?",
                "arousal_scale",
            )
            has("erecti", "arousal", "aroused", "lubricat", "stay hard", "get hard", "getting hard", "climax", "orgasm") -> CoachReply(
                "Arousal and climax depend on a mix of mind, hormones, blood flow, nerves, sleep, alcohol and stress, and anxiety is one of the most common and most reversible factors. " +
                    "If this is new, long-lasting (about 3 months or more) or comes with other symptoms, it's worth a check-up. Meanwhile, calming your nervous system and lifestyle basics help. " +
                    "Has anything changed recently, like stress, sleep, alcohol or a new medication?",
                "lifestyle",
            )
            has("desire", "libido", "sex drive", "not in the mood", "no interest", "low drive", "don't want") -> CoachReply(
                "Desire shifts with stress, sleep, hormones, relationship dynamics, medications and routine. It isn't a switch that's broken. For many people it's responsive: it shows up after warmth or touch rather than before. " +
                    "Mapping what helps you feel open is a great first step. What was different the last time you felt interested?",
                "desire_map",
            )
            has("partner wants", "more often", "less often", "reject", "mismatch", "different drive", "wants more", "wants less") -> CoachReply(
                "A desire gap is one of the most common couple challenges, and nobody's the villain. The painful cycle is usually one person pursuing, one withdrawing, and both feeling rejected. " +
                    "Naming the cycle together and widening what counts as intimacy often eases it. A structured check-in gives you a safe way to talk. How do you usually react when the topic comes up?",
                partnerEx,
            )
            has("talk", "tell my", "communicat", "bring up", "awkward", "embarrass", "ashamed", "shy") -> CoachReply(
                "It's hard because there's often shame and a fear of rejection, and almost nobody is taught how. A good formula: When ___, I feel ___, and I'd love ___. " +
                    "Start with something you appreciate, pick a calm moment outside the bedroom, and keep it to one topic. Tell me the topic and I'll help you word your opening line.",
                "hard_convo",
            )
            has("body", "insecur", "confiden", "ugly", "size", "weight", "self-conscious", "self conscious") -> CoachReply(
                "Self-consciousness steals attention from sensation, which is where connection lives. Confidence usually grows from small proofs rather than pep talks: noticing the harsh commentary, questioning it, and treating your body with neutrality. " +
                    "Today's short practice is a body-neutral check-in. What's the thought that shows up loudest?",
                "body_kind",
            )
            has("distant", "disconnect", "lonely", "bored", "routine", "spark", "rut", "stuck") -> CoachReply(
                "Feeling distant is a signal, not a failure. Often connection has been crowded out by routine and stress. Small rituals beat grand gestures: a daily check-in, an appreciation exchange, a low-pressure date. " +
                    "What did closeness look like when it felt good?",
                if (ctx.partnered) "appreciation" else "solo_connect",
            )
            else -> CoachReply(
                when (v) {
                    0 -> "I want to understand properly so I can tailor this. Could you tell me when it tends to happen, what you notice in your body and thoughts, and how long it's been going on?"
                    else -> "Thanks for sharing that. To make sure I point you in the right direction: what's the one thing you'd most like to be different a month from now?"
                },
                ctx.todayIds.firstOrNull(),
            )
        }
    }
}

/** Calls YOUR backend, which should hold the LLM key and apply CoachPrompt.SYSTEM. Falls back to the on-device coach. */
class ProxyCoach(private val url: String) : CoachProvider {
    override suspend fun reply(history: List<ChatMsg>, ctx: CoachContext): CoachReply {
        // Safety replies never leave the device and always win.
        LocalCoach.safetyReply(history.lastOrNull { it.fromUser }?.text.orEmpty())?.let { return it }
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
                LocalCoach.reply(history, ctx)
            }
        }
    }
}

fun defaultCoach(): CoachProvider =
    if (BuildConfig.COACH_API_URL.isNotBlank()) ProxyCoach(BuildConfig.COACH_API_URL) else LocalCoach
