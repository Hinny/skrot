package dev.hinny.skrot.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import dev.hinny.skrot.AppContainer
import dev.hinny.skrot.R
import dev.hinny.skrot.data.model.BodyMetric
import dev.hinny.skrot.data.model.Exercise
import dev.hinny.skrot.data.model.Gym
import dev.hinny.skrot.data.model.HomeSection
import dev.hinny.skrot.data.model.OneRepMaxRange
import dev.hinny.skrot.data.model.SetType
import dev.hinny.skrot.data.model.RoutineDay
import dev.hinny.skrot.data.model.RoutineWithDays
import dev.hinny.skrot.data.model.WorkoutSession
import dev.hinny.skrot.domain.CoachTrigger
import dev.hinny.skrot.domain.OneRepMax
import dev.hinny.skrot.domain.ScheduleEngine
import dev.hinny.skrot.domain.StreakCalculator
import dev.hinny.skrot.domain.Units
import dev.hinny.skrot.domain.VolumeCalculator
import dev.hinny.skrot.ui.Routes
import dev.hinny.skrot.ui.common.CoachMessages
import dev.hinny.skrot.ui.common.displayName
import dev.hinny.skrot.ui.common.lastPerformedText
import dev.hinny.skrot.ui.containerViewModel
import dev.hinny.skrot.ui.session.NextWorkoutCard
import dev.hinny.skrot.ui.session.NoActiveProgramCard
import dev.hinny.skrot.ui.session.OpenSessionCard
import dev.hinny.skrot.ui.session.RecoveryStartCard
import dev.hinny.skrot.ui.session.StartSessionViewModel
import dev.hinny.skrot.ui.session.StartFlowHost
import dev.hinny.skrot.ui.session.WorkoutPickerDialog
import dev.hinny.skrot.data.prefs.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.LocalDate

data class HomeUiState(
    val openSession: WorkoutSession? = null,
    val activeRoutine: RoutineWithDays? = null,
    val nextDay: RoutineDay? = null,
    val allRoutines: List<RoutineWithDays> = emptyList(),
    val lastByRoutine: Map<Long, Long> = emptyMap(),
    val lastByDay: Map<Long, Long> = emptyMap(),
    val gyms: List<Gym> = emptyList(),
    val daysSinceLastSession: Int? = null,
    val comebackRoutines: List<RoutineWithDays> = emptyList(),
    /** Every recovery program, regardless of how long it has been. */
    val recoveryRoutines: List<RoutineWithDays> = emptyList(),
    val backupOverdue: Boolean = false,
    /**
     * Set while the most recent finished workout was a recovery one: the same
     * program, and the day that follows the one just done. Clears itself as soon
     * as an ordinary session is finished.
     */
    val continueRecovery: Pair<RoutineWithDays, RoutineDay>? = null,
    val lastSession: LastSessionSummary? = null,
    val latestMetric: BodyMetric? = null,
    /** The entry before [latestMetric], for the change shown next to it. */
    val previousMetric: BodyMetric? = null,
    val weekStreak: Int = 0,
    val oneRepMaxes: List<OneRepMaxEntry> = emptyList(),
)

/** One tracked lift's best estimated 1RM within the configured window. */
data class OneRepMaxEntry(val exercise: Exercise, val estimateKg: Double?)

/** What the last finished workout amounted to, for the Home recap card. */
data class LastSessionSummary(
    val startedAt: Long,
    val title: String,
    val durationMs: Long,
    val exerciseCount: Int,
    val completedSets: Int,
    val volumeKg: Double,
    val sessionId: Long,
)

class HomeViewModel(private val container: AppContainer) : ViewModel() {
    private val db = container.db

    val comebackDismissed = MutableStateFlow(false)
    val backupReminderDismissed = MutableStateFlow(false)
    val uiState = MutableStateFlow(HomeUiState())

    init {
        viewModelScope.launch {
            val base = combine(
                db.sessionDao().observeOpenSession(),
                db.routineDao().observeAllWithDays(),
                db.routineDao().observeLastPerformedByRoutine(),
                db.routineDao().observeLastPerformedByDay(),
                db.gymDao().observeAll(),
            ) { open, routines, lastRoutine, lastDay, gyms ->
                HomeUiState(
                    openSession = open,
                    allRoutines = routines,
                    lastByRoutine = lastRoutine.associate { it.routineId to it.last },
                    lastByDay = lastDay.associate { it.dayId to it.last },
                    gyms = gyms,
                )
            }
            val withHistory = combine(
                base,
                db.sessionDao().observeFinishedSessions(),
                db.bodyMetricDao().observeAll(),
            ) { state, finished, metrics -> Triple(state, finished, metrics) }

            combine(
                withHistory,
                container.settings.settings,
                comebackDismissed,
                backupReminderDismissed,
            ) { (state, finished, metrics), settings, dismissed, backupDismissed ->
                val active = state.allRoutines.find { it.routine.isActive }
                val nextDay = active?.let {
                    ScheduleEngine.nextDay(it.routine, it.days, LocalDate.now())
                }
                val lastSession = finished.maxOfOrNull { it.startedAt }
                val daysSince = lastSession?.let {
                    ((System.currentTimeMillis() - it) / 86_400_000L).toInt()
                }
                val comeback =
                    if (!dismissed && (daysSince == null || daysSince >= settings.comebackDays)) {
                        state.allRoutines.filter { it.routine.isRecovery }
                    } else emptyList()

                // Carry on recovering: look at the workout most recently finished
                // and, if it was a recovery one, propose the next day of the same
                // program. Finishing a normal session makes this fall away.
                val lastFinished = finished.maxByOrNull { it.startedAt }
                val recoveryProgram = lastFinished?.routineId
                    ?.let { id -> state.allRoutines.find { it.routine.id == id } }
                    ?.takeIf { it.routine.isRecovery }
                val continueRecovery = recoveryProgram?.let { program ->
                    val days = program.sortedDays
                    if (days.isEmpty()) return@let null
                    val lastIndex = days.indexOfFirst { it.id == lastFinished.routineDayId }
                    val next = days[(lastIndex + 1).mod(days.size)]
                    program to next
                }
                // Backup reminder: counts from the last backup, or from the oldest
                // logged session if no backup was ever made.
                val backupBasis = settings.lastBackupAt.takeIf { it > 0 }
                    ?: finished.minOfOrNull { it.startedAt }
                val backupOverdue = !backupDismissed &&
                    settings.backupReminderDays > 0 &&
                    backupBasis != null &&
                    System.currentTimeMillis() - backupBasis >
                    settings.backupReminderDays * 86_400_000L
                val sortedMetrics = metrics.sortedByDescending { it.date }
                val streak =
                    if (HomeSection.WEEK_STREAK in settings.homeSections) {
                        StreakCalculator.weeks(
                            finished.map { it.startedAt },
                            settings.streakMinPerWeek,
                        )
                    } else 0
                val oneRepMaxes =
                    if (HomeSection.ONE_REP_MAX in settings.homeSections) {
                        oneRepMaxes(settings.oneRepMaxExerciseIds, settings.oneRepMaxRange)
                    } else emptyList()
                state.copy(
                    activeRoutine = active,
                    nextDay = nextDay,
                    daysSinceLastSession = daysSince,
                    comebackRoutines = if (daysSince == null) emptyList() else comeback,
                    recoveryRoutines = state.allRoutines.filter { it.routine.isRecovery },
                    backupOverdue = backupOverdue,
                    continueRecovery = continueRecovery,
                    lastSession = lastFinished?.let { summarize(it, settings.bodyweightFallbackKg) },
                    latestMetric = sortedMetrics.firstOrNull(),
                    previousMetric = sortedMetrics.getOrNull(1),
                    weekStreak = streak,
                    oneRepMaxes = oneRepMaxes,
                )
            }.collect { uiState.value = it }
        }
    }

    /**
     * Best estimated 1RM for each tracked lift within [range]. Warmups are
     * excluded, as they are everywhere else 1RM is estimated.
     */
    private suspend fun oneRepMaxes(
        exerciseIds: List<Long>,
        range: OneRepMaxRange,
    ): List<OneRepMaxEntry> {
        val all = db.exerciseDao().getAll()
        val tracked = resolveOneRepMaxExercises(exerciseIds, all)
        val from = when (range) {
            OneRepMaxRange.CURRENT, OneRepMaxRange.ALL_TIME -> 0L
            OneRepMaxRange.PAST_YEAR -> System.currentTimeMillis() - 365L * 86_400_000L
            OneRepMaxRange.PAST_3_YEARS -> System.currentTimeMillis() - 3 * 365L * 86_400_000L
        }
        return tracked.map { exercise ->
            val sets = db.sessionDao().setsForExercise(exercise.id)
                .filter { it.set.setType != SetType.WARMUP && it.set.completed }
                .filter { it.sessionDate >= from }
            val estimate = if (range == OneRepMaxRange.CURRENT) {
                // "Latest" means the best set of the most recent session that
                // included this lift, not the best ever.
                val latestSession = sets.maxByOrNull { it.sessionDate }?.sessionId
                sets.filter { it.sessionId == latestSession }
            } else {
                sets
            }.mapNotNull { OneRepMax.epley(it.set.load, it.set.reps) }.maxOrNull()
            OneRepMaxEntry(exercise, estimate)
        }
    }

    /** Totals for the last finished workout, for the Home recap card. */
    private suspend fun summarize(
        session: WorkoutSession,
        bodyweightFallbackKg: Double,
    ): LastSessionSummary? {
        val content = db.sessionDao().sessionWithContent(session.id) ?: return null
        val bodyweight = db.bodyMetricDao().latestWeightAtOrBefore(session.startedAt)
            ?.weightKg ?: bodyweightFallbackKg
        val dayName = session.routineDayId?.let { db.routineDao().dayWithContent(it)?.day?.name }
        return LastSessionSummary(
            startedAt = session.startedAt,
            title = dayName.orEmpty(),
            durationMs = (session.endedAt ?: session.startedAt) - session.startedAt,
            exerciseCount = content.exercises.size,
            completedSets = VolumeCalculator.completedSetCount(content),
            volumeKg = VolumeCalculator.sessionVolumeKg(content, bodyweight),
            sessionId = session.id,
        )
    }

}

/**
 * The recovery offers: carrying on with a recovery program just used, and the
 * comeback nudge after a long gap. Shared by Home and the Session tab so both
 * can start a recovery workout on a chosen day.
 */
@Composable
fun RecoverySection(
    state: HomeUiState,
    onDismissComeback: () -> Unit,
    onStart: (RoutineWithDays, RoutineDay) -> Unit,
    alwaysOffer: Boolean = false,
) {
    state.continueRecovery?.let { (program, nextDay) ->
        RecoveryStartCard(
            title = stringResource(R.string.continue_recovery_title),
            body = stringResource(R.string.continue_recovery_body),
            routines = listOf(program),
            suggestedDay = { nextDay },
            onStart = onStart,
        )
    }
    when {
        state.comebackRoutines.isNotEmpty() -> RecoveryStartCard(
            title = stringResource(R.string.comeback_title, state.daysSinceLastSession ?: 0),
            body = stringResource(R.string.comeback_body),
            routines = state.comebackRoutines,
            suggestedDay = { it.sortedDays.firstOrNull() },
            onStart = onStart,
            onDismiss = onDismissComeback,
        )

        // Wanting an easier day isn't only a thing after a long break, so the
        // Session tab can keep the option permanently on hand.
        alwaysOffer && state.continueRecovery == null &&
            state.recoveryRoutines.isNotEmpty() -> RecoveryStartCard(
            title = stringResource(R.string.recovery_program),
            body = stringResource(R.string.recovery_offer_body),
            routines = state.recoveryRoutines,
            suggestedDay = { it.sortedDays.firstOrNull() },
            onStart = onStart,
        )
    }
}

/**
 * The coach outside a workout. Uses the welcome-back lines when you have been
 * away long enough for them to mean something, and the home greeting otherwise.
 * Held in [remember] so it doesn't reshuffle on every recomposition.
 */
@Composable
private fun CoachCard(settings: Settings, daysSince: Int?) {
    val context = LocalContext.current
    val trigger = if (daysSince != null && daysSince >= settings.comebackDays) {
        CoachTrigger.WELCOME_BACK
    } else {
        CoachTrigger.HOME
    }
    val message = remember(settings.coachPersonality, trigger) {
        CoachMessages.random(context, settings.coachPersonality, trigger)
    } ?: return

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(16.dp),
        )
    }
}

/** Days since the last workout and the run of training weeks behind it. */
@Composable
private fun StreakCard(daysSince: Int?, streak: Int?) {
    val lines = buildList {
        if (daysSince != null) {
            add(
                if (daysSince <= 0) stringResource(R.string.trained_today)
                else stringResource(R.string.days_since_last_workout, daysSince)
            )
        }
        if (streak != null) {
            add(
                if (streak <= 0) stringResource(R.string.week_streak_none)
                else stringResource(R.string.week_streak, streak)
            )
        }
    }
    if (lines.isEmpty()) return
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            lines.forEachIndexed { index, line ->
                Text(
                    line,
                    style = if (index == 0) MaterialTheme.typography.titleMedium
                    else MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

/** Estimated 1RM for the tracked lifts. */
@Composable
private fun OneRepMaxCard(
    entries: List<OneRepMaxEntry>,
    range: OneRepMaxRange,
    settings: Settings,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "${stringResource(R.string.home_section_one_rep_max)} · ${rangeLabel(range)}",
                style = MaterialTheme.typography.labelMedium,
            )
            entries.forEach { entry ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(entry.exercise.displayName(), modifier = Modifier.weight(1f))
                    val estimate = entry.estimateKg
                    Text(
                        estimate?.let { Units.formatWeight(it, settings.unit) } ?: "—",
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        }
    }
}

/** Recap of the workout most recently finished. */
@Composable
private fun LastSessionCard(
    summary: LastSessionSummary,
    settings: Settings,
    onOpen: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                stringResource(R.string.last_session),
                style = MaterialTheme.typography.labelMedium,
            )
            Text(
                summary.title.ifBlank { stringResource(R.string.workout) },
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                listOf(
                    lastPerformedText(summary.startedAt),
                    stringResource(R.string.minutes_short, summary.durationMs / 60_000),
                    stringResource(R.string.sets_count, summary.completedSets),
                    Units.formatWeight(summary.volumeKg, settings.unit),
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

/** Most recent body measurement, with the change since the one before it. */
@Composable
private fun LastMetricCard(
    metric: BodyMetric,
    previous: BodyMetric?,
    settings: Settings,
    onOpen: () -> Unit,
) {
    val weightKg = metric.weightKg ?: return
    val deltaKg = previous?.weightKg?.let { weightKg - it }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                stringResource(R.string.body_metrics),
                style = MaterialTheme.typography.labelMedium,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    Units.formatWeight(weightKg, settings.unit),
                    style = MaterialTheme.typography.titleMedium,
                )
                if (deltaKg != null && deltaKg != 0.0) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        (if (deltaKg > 0) "+" else "") +
                            Units.formatWeight(deltaKg, settings.unit),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                lastPerformedText(metric.date),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
fun HomeScreen(container: AppContainer, settings: Settings, nav: NavHostController) {
    val vm = containerViewModel(container) { c, _ -> HomeViewModel(c) }
    val startVm = containerViewModel(container) { c, _ -> StartSessionViewModel(c) }
    val state by vm.uiState.collectAsState()
    var startTarget by remember { mutableStateOf<Pair<RoutineWithDays?, RoutineDay?>?>(null) }
    var showPicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium)

        val shows = { section: HomeSection -> section in settings.homeSections }

        if (settings.coachEnabled && shows(HomeSection.COACH)) {
            CoachCard(settings = settings, daysSince = state.daysSinceLastSession)
        }

        state.openSession?.let { open ->
            OpenSessionCard(onResume = { nav.navigate(Routes.workout(open.id)) })
        }

        if (state.backupOverdue && shows(HomeSection.BACKUP_REMINDER)) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            stringResource(R.string.backup_reminder_title),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = { vm.backupReminderDismissed.value = true }) {
                            Icon(Icons.Filled.Close, stringResource(R.string.dismiss))
                        }
                    }
                    Text(
                        stringResource(R.string.backup_reminder_body),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    TextButton(onClick = { nav.navigate(Routes.BACKUP) }) {
                        Text(stringResource(R.string.backup_now))
                    }
                }
            }
        }

        if (state.openSession == null && shows(HomeSection.RECOVERY)) {
            RecoverySection(
                state = state,
                onDismissComeback = { vm.comebackDismissed.value = true },
                onStart = { r, day -> startTarget = r to day },
            )
        }

        val active = state.activeRoutine
        if (shows(HomeSection.NEXT_WORKOUT) && state.openSession == null) {
            if (active != null) {
                NextWorkoutCard(
                    routine = active,
                    nextDay = state.nextDay,
                    lastPerformed = state.nextDay?.let { state.lastByDay[it.id] },
                    onStart = { r, day -> startTarget = r to day },
                    onChooseOther = { showPicker = true },
                )
            } else {
                NoActiveProgramCard(
                    onGoToPrograms = { nav.navigate(Routes.PROGRAMS) },
                    onChooseWorkout = { showPicker = true },
                )
            }
        }

        if (shows(HomeSection.DAYS_SINCE_LAST) || shows(HomeSection.WEEK_STREAK)) {
            StreakCard(
                daysSince = state.daysSinceLastSession.takeIf { shows(HomeSection.DAYS_SINCE_LAST) },
                streak = state.weekStreak.takeIf { shows(HomeSection.WEEK_STREAK) },
            )
        }
        if (shows(HomeSection.ONE_REP_MAX) && state.oneRepMaxes.isNotEmpty()) {
            OneRepMaxCard(
                entries = state.oneRepMaxes,
                range = settings.oneRepMaxRange,
                settings = settings,
            )
        }
        if (shows(HomeSection.LAST_SESSION)) {
            state.lastSession?.let { last ->
                LastSessionCard(
                    summary = last,
                    settings = settings,
                    onOpen = { nav.navigate(Routes.historySession(last.sessionId)) },
                )
            }
        }
        if (shows(HomeSection.BODY_METRIC)) {
            state.latestMetric?.let { metric ->
                LastMetricCard(
                    metric = metric,
                    previous = state.previousMetric,
                    settings = settings,
                    onOpen = { nav.navigate(Routes.BODY) },
                )
            }
        }

        Spacer(Modifier.height(24.dp))
    }

    // Workout picker (swap in a different day, from this or another program)
    if (showPicker) {
        WorkoutPickerDialog(
            routines = state.allRoutines,
            lastByDay = state.lastByDay,
            onDismiss = { showPicker = false },
            onPick = { r, day ->
                showPicker = false
                startTarget = r to day
            },
        )
    }

    // Gym + temporary-visit selection, then resolution
    StartFlowHost(
        vm = startVm,
        nav = nav,
        settings = settings,
        gyms = state.gyms,
        startTarget = startTarget,
        onClearTarget = { startTarget = null },
    )
}
