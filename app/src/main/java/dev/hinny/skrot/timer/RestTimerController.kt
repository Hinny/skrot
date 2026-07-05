package dev.hinny.skrot.timer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat
import dev.hinny.skrot.R
import dev.hinny.skrot.data.prefs.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class RestTimerState(
    val totalSec: Int,
    val remainingSec: Int,
    val exerciseName: String,
)

/**
 * App-scoped rest timer. Counts down in-app and mirrors the countdown as a
 * notification; alerts with sound/vibration (per settings) when done.
 */
class RestTimerController(
    private val context: Context,
    private val scope: CoroutineScope,
    private val settings: SettingsRepository,
) {
    private val _state = MutableStateFlow<RestTimerState?>(null)
    val state: StateFlow<RestTimerState?> = _state

    private var job: Job? = null
    private var endAtMs = 0L

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.rest_timer),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            setSound(null, null)
            enableVibration(false)
        }
        notificationManager.createNotificationChannel(channel)
    }

    /** Starts the timer; 0 seconds means no timer. */
    fun start(seconds: Int, exerciseName: String) {
        job?.cancel()
        if (seconds <= 0) {
            clear()
            return
        }
        endAtMs = System.currentTimeMillis() + seconds * 1000L
        _state.value = RestTimerState(seconds, seconds, exerciseName)
        job = scope.launch { runCountdown(exerciseName) }
    }

    fun adjust(deltaSec: Int) {
        val current = _state.value ?: return
        endAtMs += deltaSec * 1000L
        val remaining = remainingSec()
        if (remaining <= 0) {
            skip()
        } else {
            _state.value = current.copy(
                remainingSec = remaining,
                totalSec = maxOf(current.totalSec + deltaSec, remaining),
            )
        }
    }

    fun skip() {
        job?.cancel()
        clear()
    }

    private fun remainingSec(): Int =
        ((endAtMs - System.currentTimeMillis() + 999) / 1000).toInt()

    private suspend fun runCountdown(exerciseName: String) {
        while (true) {
            val remaining = remainingSec()
            if (remaining <= 0) break
            _state.value = _state.value?.copy(remainingSec = remaining)
            postCountdownNotification(exerciseName)
            delay(500)
        }
        onFinished(exerciseName)
    }

    private suspend fun onFinished(exerciseName: String) {
        clear()
        val prefs = settings.settings.first()
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.rest_over))
            .setContentText(context.getString(R.string.rest_over_body, exerciseName))
            .setAutoCancel(true)
            .setSilent(true)
        runCatching { notificationManager.notify(NOTIFICATION_ID, builder.build()) }

        if (prefs.timerSound) {
            runCatching {
                RingtoneManager.getRingtone(
                    context,
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                )?.play()
            }
        }
        if (prefs.timerVibrate) {
            runCatching {
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 300, 150, 300), -1))
            }
        }
    }

    private fun postCountdownNotification(exerciseName: String) {
        val state = _state.value ?: return
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.rest_timer))
            .setContentText(
                context.getString(
                    R.string.rest_remaining,
                    state.remainingSec / 60,
                    state.remainingSec % 60,
                    exerciseName,
                )
            )
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setSilent(true)
        runCatching { notificationManager.notify(NOTIFICATION_ID, builder.build()) }
    }

    private fun clear() {
        _state.value = null
        runCatching { notificationManager.cancel(NOTIFICATION_ID) }
    }

    companion object {
        private const val CHANNEL_ID = "rest_timer"
        private const val NOTIFICATION_ID = 1
    }
}
