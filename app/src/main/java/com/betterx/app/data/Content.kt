package com.betterx.app.data

import kotlin.math.roundToInt

data class Concern(
    val id: String,
    val label: String,
    val domains: List<String>,
    val courseId: String,
    val medicalNote: Boolean = false,
)

data class Question(val id: String, val domain: String, val text: String)

data class Exercise(
    val id: String,
    val title: String,
    val domain: String,
    val minutes: Int,
    val level: Int,
    val partner: Boolean,
    val why: String,
    val steps: List<String>,
)

data class Lesson(val title: String, val body: String)

data class Course(
    val id: String,
    val title: String,
    val tagline: String,
    val domain: String,
    val lessons: List<Lesson>,
    val exerciseIds: List<String>,
)

object Content {
    val domains = listOf("Confidence", "Communication", "Calm", "Desire", "Body", "Connection")

    fun label(domain: String) = if (domain == "Body") "Body awareness" else domain

    // ---------------------------------------------------------------- concerns
    val concerns = listOf(
        Concern("anxiety", "Performance anxiety", listOf("Calm", "Confidence"), "calm"),
        Concern("control", "Lasting longer / more control", listOf("Body", "Calm"), "body"),
        Concern("arousal", "Arousal or erection concerns", listOf("Body", "Calm"), "body"),
        Concern("climax", "Difficulty reaching climax", listOf("Calm", "Body"), "body"),
        Concern("desire", "Low or fluctuating desire", listOf("Desire", "Body"), "desire"),
        Concern("mismatch", "Desire gap with my partner", listOf("Communication", "Desire", "Connection"), "desire"),
        Concern("talk", "Hard to talk about intimacy", listOf("Communication"), "talk"),
        Concern("confidence", "Body confidence", listOf("Confidence"), "confidence"),
        Concern("connection", "Feeling distant or stuck in a rut", listOf("Connection", "Communication"), "closer"),
        Concern("discomfort", "Discomfort or pain", listOf("Body", "Calm"), "body", medicalNote = true),
    )

    fun concern(id: String) = concerns.firstOrNull { it.id == id }

    // -------------------------------------------------------------- assessment
    val questions = listOf(
        Question("q1", "Confidence", "I feel comfortable and confident in my body when I'm intimate."),
        Question("q2", "Communication", "I can tell a partner what I like and don't like."),
        Question("q3", "Calm", "My mind stays in the moment instead of worrying about how I'm doing."),
        Question("q4", "Desire", "I feel interest in intimacy about as often as I'd like."),
        Question("q5", "Body", "I notice my arousal building and can tell what pace feels right."),
        Question("q6", "Connection", "I feel emotionally close and safe with the people I'm intimate with."),
    )

    /** 0..100 per domain (higher = stronger). */
    fun score(answers: Map<String, Int>): Map<String, Int> = domains.associateWith { dom ->
        val vals = questions.filter { it.domain == dom }.mapNotNull { answers[it.id] }
        if (vals.isEmpty()) 50 else (((vals.average() - 1.0) / 4.0) * 100).roundToInt()
    }

    // --------------------------------------------------------------- exercises
    val exercises = listOf(
        Exercise(
            "box_breath", "Box breathing reset", "Calm", 5, 1, false,
            "Long, slow exhales tell your nervous system it's safe to relax - the opposite of the stress response that feeds performance worry.",
            listOf(
                "Sit comfortably with your feet on the floor and soften your jaw and shoulders.",
                "Breathe in through your nose for 4 counts.",
                "Hold gently for 4 counts.",
                "Breathe out slowly for 4 to 6 counts.",
                "Pause for 4 counts, then repeat for the full time.",
                "Finish by noticing where your body feels calmer.",
            ),
        ),
        Exercise(
            "body_scan", "Body scan: find the tension", "Calm", 8, 1, false,
            "Many of us carry tension in the belly, hips and jaw without noticing. Spotting it is the first step to releasing it.",
            listOf(
                "Lie down or sit back. Close your eyes if that feels okay.",
                "Starting at your feet, slowly move your attention up through your legs, hips and belly.",
                "At each area, ask: is this tight, neutral or relaxed? Don't try to fix it yet.",
                "Where you find tightness, breathe toward it and let the exhale soften it a little.",
                "Finish with the face, jaw and hands. Rate your overall tension from 0 to 10.",
            ),
        ),
        Exercise(
            "worry_parking", "Parking the worry loop", "Calm", 6, 2, false,
            "Worry gets louder when you fight it. Naming it and returning to your senses breaks the loop.",
            listOf(
                "Write down the 2 or 3 worries that show up most (for example: What if I lose focus?).",
                "Label each one: This is a worry thought, not a fact.",
                "Ground yourself with 5-4-3-2-1: five things you see, four you feel, three you hear, two you smell, one you taste.",
                "Decide on a 10-minute worry window for later today so the thoughts have somewhere to go.",
                "Take three slow breaths and rate how loud the worry feels now (0 to 10).",
            ),
        ),
        Exercise(
            "wind_down", "Evening wind-down breathing", "Calm", 4, 1, false,
            "A short, regular calming habit lowers baseline stress, which shows up everywhere - including intimacy.",
            listOf(
                "Dim the lights and put your phone down.",
                "Breathe in for 4 counts and out for 6 counts.",
                "With each exhale, relax one area: forehead, jaw, shoulders, hands, belly.",
                "Continue for the full time. Go to bed with a slower pace.",
            ),
        ),
        Exercise(
            "pelvic_floor", "Pelvic floor awareness", "Body", 6, 1, false,
            "The pelvic floor supports arousal, control and comfort. Learning to both engage and fully release it matters equally.",
            listOf(
                "Lie on your back with knees bent. Breathe low and slow.",
                "To find the muscles, imagine gently stopping gas: a light lift inward and up. (Don't practice this while urinating.)",
                "Lift gently for 3 seconds while breathing out, then release completely for 6 seconds.",
                "Do 8 repetitions. Keep your buttocks, thighs and belly relaxed and don't hold your breath.",
                "Stop if anything hurts. If you feel pain, tightness or can't relax the area, mention it to a clinician.",
            ),
        ),
        Exercise(
            "arousal_scale", "Know your arousal scale", "Body", 10, 2, false,
            "Pace is a skill built on awareness. When you can feel the build early, you have choices instead of surprises.",
            listOf(
                "In private, relaxed time, check in with a 0 to 10 scale: 0 is calm, 10 is the point of no return.",
                "Notice what 3, 5 and 7 each feel like in your body (breath, muscles, thoughts).",
                "When you reach about 7, pause. Breathe low and slow, relax your belly and legs, and let it settle toward 5.",
                "Resume at your own pace. Repeat the pause-and-settle cycle a few times.",
                "Afterward, note what helped. The goal is awareness and choice, not a stopwatch.",
            ),
        ),
        Exercise(
            "lifestyle", "Lifestyle levers check", "Body", 5, 1, false,
            "Sleep, alcohol, movement and stress all change how your body responds. Small shifts often beat big overhauls.",
            listOf(
                "Rate each from 1 to 5: sleep, movement, alcohol/other substances, stress load.",
                "Circle the lowest one.",
                "Choose one tiny change for the next 7 days (for example: lights-out 30 minutes earlier, or a 15-minute walk).",
                "Write it where you'll see it tomorrow morning.",
            ),
        ),
        Exercise(
            "needs_journal", "Name what you need", "Communication", 7, 1, false,
            "You can't ask for what you haven't put into words. Writing it first makes the conversation easier.",
            listOf(
                "Finish these sentences without editing: I feel closest when... I feel pressure when... I'd love more... I'd like less...",
                "Underline the one line that feels most important.",
                "Rewrite it as a kind, specific request.",
                "Decide when you might share it.",
            ),
        ),
        Exercise(
            "hard_convo", "Script a hard conversation", "Communication", 6, 2, false,
            "A clear opening line lowers defensiveness for both of you.",
            listOf(
                "Pick one topic only.",
                "Fill in the formula: When ___ happens, I feel ___, and I'd love ___.",
                "Start with one thing you appreciate about the person or the relationship.",
                "Choose a calm moment outside the bedroom, and ask: Is now a good time to talk about something important?",
                "Say your opening line out loud once, to hear how it lands.",
            ),
        ),
        Exercise(
            "checkin_talk", "The 10-minute check-in", "Communication", 10, 2, true,
            "A predictable, structured talk makes intimate topics feel safer and less loaded.",
            listOf(
                "Sit together, phones away. Decide who goes first.",
                "Speaker (3 minutes): one thing that felt good lately and one thing you'd like more of. No blame.",
                "Listener: reflect it back (What I heard is...) without defending or fixing.",
                "Switch roles.",
                "End by thanking each other and agreeing on one small thing to try this week.",
            ),
        ),
        Exercise(
            "sensate1", "Slow touch, no goals", "Connection", 20, 2, true,
            "Taking performance off the table lets attention return to sensation. This is a core step in many evidence-based approaches.",
            listOf(
                "Agree up front: this is non-sexual touch only, and either person can stop at any time.",
                "One person gives touch to arms, hands, shoulders and back for about 8 minutes. The other simply notices sensation.",
                "The receiver says what feels pleasant or what to change, using simple words.",
                "Switch roles.",
                "Afterwards, share one thing you noticed. No scoring, no expectations.",
            ),
        ),
        Exercise(
            "appreciation", "Appreciation exchange", "Connection", 5, 1, true,
            "Specific appreciation builds the safety that makes intimacy easier.",
            listOf(
                "Take turns sharing 3 specific things you appreciated about each other this week.",
                "Be concrete: When you ___, I felt ___.",
                "The listener says only: Thank you.",
            ),
        ),
        Exercise(
            "solo_connect", "What closeness means to me", "Connection", 6, 1, false,
            "Knowing what you want from closeness helps you find it, whether you're partnered or not.",
            listOf(
                "Write about a time you felt really connected to someone. What made it feel that way?",
                "List 3 ingredients (for example: humor, being listened to, safety, play).",
                "Choose one you can invite into your life this week.",
            ),
        ),
        Exercise(
            "dating_night", "Plan a low-pressure date", "Connection", 10, 2, true,
            "Novelty and play rebuild connection without any expectation of where the evening goes.",
            listOf(
                "Pick an activity you'd both enjoy that isn't dinner-and-a-screen.",
                "Agree: no sex expected tonight. Affection is welcome.",
                "Put it in the calendar.",
                "Afterwards, each share the best moment.",
            ),
        ),
        Exercise(
            "story_reframe", "Rewrite the performance story", "Confidence", 7, 2, false,
            "The story you tell yourself about how you're doing shapes how your body responds.",
            listOf(
                "Write the harshest thought you have in intimate moments (for example: I'm going to fail).",
                "Rate how much you believe it (0 to 100).",
                "Ask: What evidence do I have against it? What would I say to a friend?",
                "Write a fairer version (for example: I'm learning, and connection matters more than outcome).",
                "Re-rate your belief in the harsh thought.",
            ),
        ),
        Exercise(
            "body_kind", "Body-neutral check-in", "Confidence", 5, 1, false,
            "You don't have to love your body to treat it with respect. Neutral is a powerful starting point.",
            listOf(
                "Stand somewhere private. Look at yourself for 30 seconds without commentary.",
                "Describe what you see in neutral facts, as if for someone who can't see you.",
                "Name three things your body does for you that you value.",
                "Say one kind, honest sentence aloud.",
            ),
        ),
        Exercise(
            "affirm", "Confidence anchor", "Confidence", 4, 1, false,
            "A tiny daily proof of progress builds confidence faster than a pep talk.",
            listOf(
                "Write one small thing you did this week that moved you toward your goal.",
                "Name the skill it shows.",
                "Put a hand on your chest, breathe in for 4 and out for 6, and say: I'm building this.",
            ),
        ),
        Exercise(
            "desire_map", "Your desire map", "Desire", 10, 1, false,
            "Desire needs the right conditions. Mapping yours turns it from a mystery into something you can invite.",
            listOf(
                "List the moods, settings and circumstances in which you feel most open to intimacy (rested, playful, appreciated, away from home...).",
                "List what reliably switches it off (stress, resentment, rushing, tiredness).",
                "Pick one thing from each list to adjust this week.",
                "Notice how you feel at the end of the week.",
            ),
        ),
        Exercise(
            "desire_types", "Spontaneous vs responsive desire", "Desire", 6, 1, false,
            "Many people don't feel desire first. It builds in response to warmth, touch and context, and that is completely normal.",
            listOf(
                "Read: desire can arrive before touch (spontaneous) or after warmth begins (responsive). Both are healthy.",
                "Think of a time desire grew once things began. What helped?",
                "Plan a 5-minute low-pressure cuddle or massage this week, with no obligation to go further.",
                "Afterward, note any shift in how you feel.",
            ),
        ),
        Exercise(
            "yes_no_maybe", "Yes / No / Maybe list", "Desire", 12, 3, true,
            "A shared map of what each person is open to removes guesswork and pressure.",
            listOf(
                "Each person privately sorts a list of affectionate and intimate activities into Yes, Maybe and No (include non-sexual ones like massage, dates, new settings).",
                "Compare lists with curiosity. Overlap in Yes and Maybe is where to start.",
                "A No is respected, always, without explanation required.",
                "Choose one shared Yes or Maybe to try this month.",
            ),
        ),
    )

    fun exercise(id: String) = exercises.firstOrNull { it.id == id }

    // ----------------------------------------------------------------- courses
    val courses = listOf(
        Course(
            "calm", "Calm Under Pressure", "Break the worry-tension loop", "Calm",
            listOf(
                Lesson(
                    "Why pressure backfires",
                    "Worry switches on your stress response, which pulls attention and resources away from pleasure. It becomes a loop: worry, tension, an outcome you didn't want, more worry. Seeing the loop removes shame. It isn't a flaw in you; it's a pattern, and patterns can be changed.",
                ),
                Lesson(
                    "Your calming toolkit",
                    "Slow exhales, a soft jaw, relaxed shoulders and belly, and grounding in your senses are portable tools. Practice them when you're not in the moment so they're available when you are. Short daily reps beat occasional long ones.",
                ),
                Lesson(
                    "From performing to experiencing",
                    "Shift attention from How am I doing? to What do I notice? Sensation-focused attention is the heart of many evidence-based approaches, including slow-touch exercises. Pleasure grows where attention goes.",
                ),
            ),
            listOf("box_breath", "body_scan", "worry_parking", "wind_down", "story_reframe"),
        ),
        Course(
            "body", "Know Your Body", "Awareness, pace and comfort", "Body",
            listOf(
                Lesson(
                    "How arousal works",
                    "Arousal is shaped by thoughts, hormones, blood flow, nerves, sleep, alcohol, stress and some medications. That's good news: many of these levers are adjustable. Anxiety is among the most common and most reversible factors.",
                ),
                Lesson(
                    "Pace and awareness",
                    "Learning to notice arousal early gives you choices. Clinicians often teach pause-and-settle approaches that build awareness over weeks. Be patient: it's a skill, not a test.",
                ),
                Lesson(
                    "Pelvic floor: strength and release",
                    "Both engaging and fully releasing these muscles matter. Overly tight muscles can cause problems just as weak ones can. Gentle, regular practice with full relaxation is the aim; stop if there is pain.",
                ),
                Lesson(
                    "When to see a clinician",
                    "See a doctor or sexual-health clinician if you notice pain, bleeding, sudden changes, changes lasting more than about three months, or concerns about medication side effects. Never stop a prescribed medication without talking to your prescriber. Getting checked is a strength, not a failure.",
                ),
            ),
            listOf("pelvic_floor", "arousal_scale", "lifestyle", "body_scan"),
        ),
        Course(
            "desire", "Desire Reset", "Understand and rebuild desire", "Desire",
            listOf(
                Lesson(
                    "Desire isn't one thing",
                    "Some people feel desire first (spontaneous); many feel it only after warmth or touch begins (responsive). Neither is broken. Knowing your pattern helps you set up the right conditions.",
                ),
                Lesson(
                    "What helps and what blocks",
                    "Stress, poor sleep, resentment, routine and some medications lower desire. Novelty, play, feeling appreciated and unhurried time raise it. Small changes add up.",
                ),
                Lesson(
                    "Bridging a desire gap",
                    "In many couples one person pursues and the other withdraws, and both end up feeling rejected. Name the cycle together, widen what counts as intimacy, and agree on low-pressure ways to connect. Nobody is the villain.",
                ),
            ),
            listOf("desire_map", "desire_types", "yes_no_maybe", "checkin_talk", "lifestyle"),
        ),
        Course(
            "talk", "Talk About It", "Say what you need, kindly", "Communication",
            listOf(
                Lesson(
                    "Why it's hard",
                    "Shame, fear of rejection and a lack of role models make intimate topics tough. Almost nobody is taught how. Needing practice is normal.",
                ),
                Lesson(
                    "A simple formula",
                    "Try: When ___, I feel ___, and I'd love ___. Start with appreciation, pick a calm time outside the bedroom, and stick to one topic.",
                ),
                Lesson(
                    "Listening that heals",
                    "Reflect back what you heard before responding. Don't fix, defend or debate. Feeling understood often lowers the temperature more than any solution.",
                ),
            ),
            listOf("needs_journal", "hard_convo", "checkin_talk", "appreciation"),
        ),
        Course(
            "closer", "Closer Together", "Rebuild connection and play", "Connection",
            listOf(
                Lesson(
                    "Connection is the foundation",
                    "Emotional safety and goodwill make physical intimacy easier. When connection fades, desire and ease often follow it down.",
                ),
                Lesson(
                    "Rituals of connection",
                    "Small repeated rituals matter: a daily check-in, a long hug, a weekly date. Consistency beats grand gestures.",
                ),
                Lesson(
                    "Touch without a goal",
                    "Slow, non-sexual touch removes pressure and reconnects you with sensation and each other. Agree on rules first, and either person can pause at any time.",
                ),
            ),
            listOf("appreciation", "sensate1", "dating_night", "solo_connect", "checkin_talk"),
        ),
        Course(
            "confidence", "Body Confidence", "Quiet the inner critic", "Confidence",
            listOf(
                Lesson(
                    "Your inner critic",
                    "Self-judgment steals attention from sensation, which is where connection lives. Noticing the critic's voice is the first step to loosening it.",
                ),
                Lesson(
                    "Body neutrality",
                    "You don't have to love how you look to respect your body. Neutral, factual, appreciative thinking is a realistic, sturdy middle ground.",
                ),
                Lesson(
                    "Confidence is built by small proofs",
                    "Confidence follows evidence. Collect small wins weekly and name the skill each one shows.",
                ),
            ),
            listOf("story_reframe", "body_kind", "affirm", "needs_journal"),
        ),
    )

    fun course(id: String) = courses.firstOrNull { it.id == id }

    // --------------------------------------------------------------- community
    val ghostTopics = listOf("Anxiety", "Desire", "Communication", "Body", "Connection", "Wins")

    val aliasAdjectives = listOf("Quiet", "Kind", "Steady", "Brave", "Gentle", "Calm", "Bright", "Honest")
    val aliasAnimals = listOf("Otter", "Heron", "Lynx", "Fox", "Wren", "Finch", "Panda", "Seal")

    fun newAlias() = "${aliasAdjectives.random()} ${aliasAnimals.random()} ${(10..99).random()}"

    fun seedGhost(): List<GhostPost> {
        val now = System.currentTimeMillis()
        val h = 3_600_000L
        return listOf(
            GhostPost(
                "seed1", "Quiet Otter 21", "Anxiety",
                "My brain starts grading me before we even begin",
                "Every time things get close, I start monitoring myself and it makes everything worse. I know it's a loop but I can't stop it. Has anything actually helped you?",
                now - 5 * h,
                listOf(
                    GhostReply("Steady Heron 08", "Slow exhales plus grounding in my senses helped more than I expected. It's a skill, and it improves with practice.", now - 4 * h),
                    GhostReply("Kind Lynx 55", "Telling my partner out loud took half the pressure away.", now - 3 * h),
                ),
                14,
            ),
            GhostPost(
                "seed2", "Brave Wren 63", "Desire",
                "Different drives and the guilt that comes with it",
                "My desire is lower than my partner's and I feel guilty every time I say no. We both end up feeling rejected. How do you handle this without it becoming a fight?",
                now - 20 * h,
                listOf(
                    GhostReply("Gentle Seal 17", "What helped us was widening what counts as intimacy. Cuddling and massage with no expectation changed the mood.", now - 18 * h),
                ),
                22,
            ),
            GhostPost(
                "seed3", "Honest Fox 42", "Wins",
                "Finally had the conversation",
                "I avoided talking about this for two years. Used the 'when X, I feel Y, I'd love Z' line and it went better than I imagined. Just wanted to say it's possible.",
                now - 30 * h,
                listOf(
                    GhostReply("Calm Finch 29", "Congrats! That takes courage.", now - 29 * h),
                ),
                41,
            ),
            GhostPost(
                "seed4", "Calm Panda 36", "Body",
                "Three weeks of pelvic floor awareness",
                "Started with the gentle awareness practice, mostly learning to fully relax. I feel more in tune with my body already. Anyone else notice the relaxing part is the harder part?",
                now - 50 * h,
                listOf(
                    GhostReply("Bright Otter 74", "Yes! Releasing was the surprise for me too.", now - 48 * h),
                ),
                9,
            ),
            GhostPost(
                "seed5", "Gentle Heron 15", "Connection",
                "Dating my partner again",
                "We started a weekly low-pressure date with no expectations. Weirdly, closeness came back before anything else did.",
                now - 70 * h,
                emptyList(),
                17,
            ),
            GhostPost(
                "seed6", "Steady Lynx 90", "Desire",
                "Wanting less after a stressful year. Normal?",
                "Work and life have been intense and my interest has dropped a lot. Is this just stress, and does it come back?",
                now - 90 * h,
                listOf(
                    GhostReply("Kind Seal 31", "Stress and sleep are huge. Mine returned once I fixed those basics.", now - 85 * h),
                ),
                12,
            ),
        )
    }
}
