package com.betterx.app.data

import kotlin.math.abs

/**
 * Builds the personalized, adaptive plan.
 * Inputs: onboarding scores + goals, recent check-ins, recent adherence, relationship status.
 * Output is deterministic for a given day (it only looks at data from before that day).
 */
object Planner {
    data class DayPlan(val day: Long, val items: List<Exercise>, val focus: String)

    private fun recent(d: AppData, day: Long) = d.checkIns.filter { it.day in (day - 7) until day }

    private fun anxious(d: AppData, day: Long): Boolean {
        val r = recent(d, day)
        return r.size >= 2 && r.map { it.anxiety }.average() >= 3.5
    }

    private fun adherence(d: AppData, day: Long): Double =
        d.completions.filter { it.day in (day - 7) until day }.map { it.day }.distinct().size / 7.0

    /** Domains ordered from highest priority to lowest. */
    fun priorities(d: AppData, day: Long): List<String> {
        val w = Content.domains.associateWith { (d.scores[it] ?: 50).toDouble() }.toMutableMap()
        d.concerns.forEach { cid ->
            Content.concern(cid)?.domains?.forEachIndexed { i, dom ->
                w[dom] = (w[dom] ?: 50.0) - maxOf(14 - 4 * i, 4)
            }
        }
        val r = recent(d, day)
        if (r.size >= 2) {
            if (r.map { it.anxiety }.average() >= 3.5) w["Calm"] = (w["Calm"] ?: 50.0) - 15
            if (r.map { it.confidence }.average() <= 2.5) w["Confidence"] = (w["Confidence"] ?: 50.0) - 12
            if (r.map { it.connection }.average() <= 2.5) w["Connection"] = (w["Connection"] ?: 50.0) - 12
        }
        return Content.domains.sortedBy { w[it] ?: 50.0 }
    }

    private fun pick(domain: String, d: AppData, maxMin: Int, exclude: Set<String>): Exercise? {
        val partnered = d.relationship != "Single"
        val pool = Content.exercises.filter {
            it.domain == domain && (!it.partner || partnered) && it.minutes <= maxMin && it.id !in exclude
        }
        if (pool.isEmpty()) return null
        val last = d.completions.groupBy { it.exerciseId }.mapValues { e -> e.value.maxOf { it.day } }
        val doneInDomain = d.completions.count { Content.exercise(it.exerciseId)?.domain == domain }
        val targetLevel = (1 + doneInDomain / 4).coerceAtMost(3)
        return pool.sortedWith(
            compareBy<Exercise>(
                { last[it.id] ?: Long.MIN_VALUE },
                { abs(it.level - targetLevel) },
                { it.id },
            ),
        ).first()
    }

    fun planFor(d0: AppData, day: Long, exclude: Set<String> = emptySet()): DayPlan {
        // Only use history from before this day so today's plan is stable while you practice.
        val d = d0.copy(
            completions = d0.completions.filter { it.day < day },
            checkIns = d0.checkIns.filter { it.day < day },
        )
        val prio = priorities(d, day)
        val idx = (day - d.startDay).toInt().coerceAtLeast(0)
        val pattern = intArrayOf(0, 1, 0, 2, 1, 0, 3)
        val slot = pattern[idx % 7]
        val primaryDomain = prio[slot]
        val lowAdherence = idx >= 7 && adherence(d, day) < 0.4
        val maxMin = if (lowAdherence) 8 else 20

        val items = mutableListOf<Exercise>()
        val main = pick(primaryDomain, d, maxMin, exclude)
            ?: pick(primaryDomain, d, maxMin, emptySet())
            ?: pick(prio[0], d, maxMin, emptySet())
        if (main != null) items += main

        if (!lowAdherence) {
            val quickDomain = if (anxious(d, day)) "Calm" else prio[(slot + 1) % prio.size]
            val ex = items.map { it.id }.toSet() + exclude
            val quick = pick(quickDomain, d, 6, ex) ?: pick(quickDomain, d, 6, items.map { it.id }.toSet())
            if (quick != null) items += quick
        }
        if (items.isEmpty()) items += Content.exercises.first()
        return DayPlan(day, items, primaryDomain)
    }

    fun week(d: AppData, from: Long): List<DayPlan> {
        val used = mutableSetOf<String>()
        return (0 until 7).map { i ->
            planFor(d, from + i, used).also { p -> used += p.items.map { it.id } }
        }
    }

    fun insight(d: AppData, day: Long): String {
        val idx = (day - d.startDay).toInt().coerceAtLeast(0)
        return when {
            anxious(d, day) ->
                "Your recent check-ins show a lot of pressure, so we're leaning on calming practices this week."
            idx >= 7 && adherence(d, day) < 0.4 ->
                "Life gets busy. We've shortened your plan so it's easier to keep going. Small and steady wins."
            else -> {
                val p = priorities(d, day).take(2).joinToString(" and ") { Content.label(it).lowercase() }
                "Built from your assessment and goals: right now we're focusing on $p. It adapts as you check in."
            }
        }
    }

    fun streak(d: AppData, today: Long): Int {
        val days = d.completions.map { it.day }.toSet()
        var cursor = if (today in days) today else today - 1
        var n = 0
        while (cursor in days) {
            n++
            cursor--
        }
        return n
    }
}
