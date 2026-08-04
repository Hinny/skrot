package dev.hinny.skrot.ui.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.MutableState
import androidx.compose.ui.res.stringResource
import dev.hinny.skrot.R

/**
 * Whether a list is locked against reordering, starting from the user's default.
 *
 * Drag handles that are always live make it too easy to shuffle a program by
 * accident while scrolling, so lists start locked unless the setting says
 * otherwise — the same bargain sessions already make.
 */
@Composable
fun rememberReorderLock(lockedByDefault: Boolean): MutableState<Boolean> =
    remember(lockedByDefault) { mutableStateOf(lockedByDefault) }

/** Padlock that toggles [locked]; mirrors the one in the workout screen. */
@Composable
fun ReorderLockButton(locked: MutableState<Boolean>) {
    IconButton(onClick = { locked.value = !locked.value }) {
        Icon(
            if (locked.value) Icons.Filled.Lock else Icons.Filled.LockOpen,
            contentDescription = stringResource(
                if (locked.value) R.string.unlock_order else R.string.lock_order
            ),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
