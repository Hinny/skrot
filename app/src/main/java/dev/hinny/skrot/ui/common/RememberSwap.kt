package dev.hinny.skrot.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import dev.hinny.skrot.R
import dev.hinny.skrot.data.model.Exercise

/** The ways a swap can outlive the moment it was made. */
enum class SwapMemory {
    /** Mark the picked exercise as available at this gym. */
    ADD_TO_GYM,

    /** Rewrite the program day so the picked exercise is its default. */
    SAVE_TO_PLAN,

    /** Per-gym override: use the picked exercise here, leave the program alone. */
    ALWAYS_AT_GYM,

    /** Put the two in the same interchangeable-exercise group. */
    LINK_EQUIVALENT,
}

/**
 * "Remember this swap?" — asked after an exercise has been swapped, at start
 * or mid-session, with one checkbox per way of remembering it. Only the
 * [offered] ways are shown; the caller decides which apply (no gym, no
 * "add to gym"). One button, reading what it will actually do: untick
 * everything and it plainly says Skip rather than leaving two ways to say no.
 *
 * [picked] equal to [original] means the exercise was kept rather than
 * swapped, which can still be news about the gym.
 */
@Composable
fun RememberSwapDialog(
    original: Exercise,
    picked: Exercise,
    gymName: String?,
    offered: Set<SwapMemory>,
    defaults: Set<SwapMemory>,
    onDone: (chosen: Set<SwapMemory>) -> Unit,
    onDismiss: () -> Unit,
) {
    var chosen by remember(picked.id) { mutableStateOf(defaults intersect offered) }

    @Composable
    fun Option(memory: SwapMemory, text: String) {
        if (memory !in offered) return
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = memory in chosen,
                onCheckedChange = { chosen = if (it) chosen + memory else chosen - memory },
            )
            Text(text, style = MaterialTheme.typography.bodySmall)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.remember_swap_title)) },
        text = {
            Column {
                Option(
                    SwapMemory.ADD_TO_GYM,
                    stringResource(R.string.add_to_gym_body, picked.displayName(), gymName.orEmpty()),
                )
                Option(
                    SwapMemory.SAVE_TO_PLAN,
                    stringResource(R.string.save_to_plan_body, picked.displayName(), original.displayName()),
                )
                Option(
                    SwapMemory.ALWAYS_AT_GYM,
                    stringResource(
                        R.string.always_use_here_body,
                        picked.displayName(),
                        original.displayName(),
                        gymName.orEmpty(),
                    ),
                )
                Option(
                    SwapMemory.LINK_EQUIVALENT,
                    stringResource(R.string.flag_equivalent_body, picked.displayName(), original.displayName()),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onDone(chosen) }) {
                Text(stringResource(if (chosen.isEmpty()) R.string.skip else R.string.save))
            }
        },
    )
}
