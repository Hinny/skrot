package dev.hinny.skrot.domain

import dev.hinny.skrot.data.model.CoachFrequency

enum class CoachTrigger {
    WELCOME_BACK,
    PR_CHANCE,
    IDLE,
    LAST_EXERCISE,
    SESSION_DONE,
    STREAK,
}

/**
 * Rate limiter for coach comments: per-trigger cooldowns plus a max-comments-per-
 * session cap, both derived from the frequency setting. Message texts live in
 * localized string arrays keyed by (personality, trigger) in the UI layer.
 */
class CoachEngine(
    private val frequency: CoachFrequency,
    private val nowMs: () -> Long = System::currentTimeMillis,
) {
    private val maxPerSession = when (frequency) {
        CoachFrequency.LOW -> 2
        CoachFrequency.MEDIUM -> 4
        CoachFrequency.HIGH -> 8
    }
    private val cooldownMs = when (frequency) {
        CoachFrequency.LOW -> 15 * 60_000L
        CoachFrequency.MEDIUM -> 8 * 60_000L
        CoachFrequency.HIGH -> 3 * 60_000L
    }

    /** Triggers that fire at most once per session regardless of cooldown. */
    private val oncePerSession = setOf(
        CoachTrigger.WELCOME_BACK,
        CoachTrigger.LAST_EXERCISE,
        CoachTrigger.SESSION_DONE,
        CoachTrigger.STREAK,
    )

    private var shownCount = 0
    private val lastShownAt = mutableMapOf<CoachTrigger, Long>()
    private val firedOnce = mutableSetOf<CoachTrigger>()

    /** True if a comment for [trigger] may be shown now; records it as shown. */
    fun offer(trigger: CoachTrigger): Boolean {
        // The session-done message never counts against the cap.
        if (trigger != CoachTrigger.SESSION_DONE && shownCount >= maxPerSession) return false
        if (trigger in oncePerSession && trigger in firedOnce) return false
        val now = nowMs()
        val last = lastShownAt[trigger]
        if (last != null && now - last < cooldownMs) return false

        lastShownAt[trigger] = now
        firedOnce.add(trigger)
        shownCount++
        return true
    }
}
