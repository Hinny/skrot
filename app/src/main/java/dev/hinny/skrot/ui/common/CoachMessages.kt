package dev.hinny.skrot.ui.common

import android.content.Context
import dev.hinny.skrot.R
import dev.hinny.skrot.data.model.CoachPersonality
import dev.hinny.skrot.domain.CoachTrigger

/**
 * Coach comments are entirely local, rule-based, pre-written string templates —
 * no AI, no network. One localized string-array per (personality, trigger).
 */
object CoachMessages {

    private val arrays: Map<Pair<CoachPersonality, CoachTrigger>, Int> = mapOf(
        (CoachPersonality.CHEERLEADER to CoachTrigger.WELCOME_BACK) to R.array.coach_cheerleader_welcome_back,
        (CoachPersonality.CHEERLEADER to CoachTrigger.PR_CHANCE) to R.array.coach_cheerleader_pr_chance,
        (CoachPersonality.CHEERLEADER to CoachTrigger.IDLE) to R.array.coach_cheerleader_idle,
        (CoachPersonality.CHEERLEADER to CoachTrigger.LAST_EXERCISE) to R.array.coach_cheerleader_last_exercise,
        (CoachPersonality.CHEERLEADER to CoachTrigger.SESSION_DONE) to R.array.coach_cheerleader_session_done,
        (CoachPersonality.CHEERLEADER to CoachTrigger.STREAK) to R.array.coach_cheerleader_streak,
        (CoachPersonality.BRO to CoachTrigger.WELCOME_BACK) to R.array.coach_bro_welcome_back,
        (CoachPersonality.BRO to CoachTrigger.PR_CHANCE) to R.array.coach_bro_pr_chance,
        (CoachPersonality.BRO to CoachTrigger.IDLE) to R.array.coach_bro_idle,
        (CoachPersonality.BRO to CoachTrigger.LAST_EXERCISE) to R.array.coach_bro_last_exercise,
        (CoachPersonality.BRO to CoachTrigger.SESSION_DONE) to R.array.coach_bro_session_done,
        (CoachPersonality.BRO to CoachTrigger.STREAK) to R.array.coach_bro_streak,
        (CoachPersonality.PT to CoachTrigger.WELCOME_BACK) to R.array.coach_pt_welcome_back,
        (CoachPersonality.PT to CoachTrigger.PR_CHANCE) to R.array.coach_pt_pr_chance,
        (CoachPersonality.PT to CoachTrigger.IDLE) to R.array.coach_pt_idle,
        (CoachPersonality.PT to CoachTrigger.LAST_EXERCISE) to R.array.coach_pt_last_exercise,
        (CoachPersonality.PT to CoachTrigger.SESSION_DONE) to R.array.coach_pt_session_done,
        (CoachPersonality.PT to CoachTrigger.STREAK) to R.array.coach_pt_streak,
        (CoachPersonality.MINIMAL to CoachTrigger.WELCOME_BACK) to R.array.coach_minimal_welcome_back,
        (CoachPersonality.MINIMAL to CoachTrigger.PR_CHANCE) to R.array.coach_minimal_pr_chance,
        (CoachPersonality.MINIMAL to CoachTrigger.IDLE) to R.array.coach_minimal_idle,
        (CoachPersonality.MINIMAL to CoachTrigger.LAST_EXERCISE) to R.array.coach_minimal_last_exercise,
        (CoachPersonality.MINIMAL to CoachTrigger.SESSION_DONE) to R.array.coach_minimal_session_done,
        (CoachPersonality.MINIMAL to CoachTrigger.STREAK) to R.array.coach_minimal_streak,
    )

    fun random(context: Context, personality: CoachPersonality, trigger: CoachTrigger): String? {
        val arrayRes = arrays[personality to trigger] ?: return null
        val options = context.resources.getStringArray(arrayRes)
        return options.randomOrNull()
    }
}
