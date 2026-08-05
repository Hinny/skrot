package dev.hinny.skrot.ui.routines

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Card
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import dev.hinny.skrot.AppContainer
import dev.hinny.skrot.R
import dev.hinny.skrot.data.model.Routine
import dev.hinny.skrot.data.model.RoutineWithDays
import dev.hinny.skrot.ui.Routes
import dev.hinny.skrot.data.prefs.Settings
import dev.hinny.skrot.ui.common.ReorderHandle
import dev.hinny.skrot.ui.common.ReorderLockButton
import dev.hinny.skrot.ui.common.rememberReorderLock
import dev.hinny.skrot.ui.common.rememberReorderState
import dev.hinny.skrot.ui.common.reorderableRow
import dev.hinny.skrot.ui.common.lastPerformedText
import dev.hinny.skrot.ui.common.vector
import dev.hinny.skrot.ui.common.vectorOrNull
import dev.hinny.skrot.ui.containerViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class ProgramsViewModel(private val container: AppContainer) : ViewModel() {
    val routines = MutableStateFlow<List<RoutineWithDays>>(emptyList())

    /** Reorders programs; the list is presented in [Routine.position] order. */
    fun move(from: Int, to: Int) {
        val current = routines.value
        if (from !in current.indices || to !in current.indices || from == to) return
        viewModelScope.launch {
            val reordered = current.toMutableList()
            reordered.add(to, reordered.removeAt(from))
            reordered.forEachIndexed { index, r ->
                if (r.routine.position != index) {
                    container.db.routineDao().update(r.routine.copy(position = index))
                }
            }
        }
    }
    val lastByRoutine = MutableStateFlow<Map<Long, Long>>(emptyMap())

    init {
        viewModelScope.launch {
            combine(
                container.db.routineDao().observeAllWithDays(),
                container.db.routineDao().observeLastPerformedByRoutine(),
            ) { all, last -> all to last.associate { it.routineId to it.last } }
                .collect { (all, last) ->
                    routines.value = all
                    lastByRoutine.value = last
                }
        }
    }

    fun create(name: String, onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            val position = (routines.value.maxOfOrNull { it.routine.position } ?: -1) + 1
            val id = container.db.routineDao().insert(Routine(name = name, position = position))
            onCreated(id)
        }
    }

    fun setActive(id: Long, active: Boolean) {
        viewModelScope.launch {
            container.db.routineDao().setActive(if (active) id else null)
        }
    }
}

@Composable
fun ProgramsScreen(container: AppContainer, settings: Settings, nav: NavHostController) {
    val vm = containerViewModel(container) { c, _ -> ProgramsViewModel(c) }
    val routines by vm.routines.collectAsState()
    val last by vm.lastByRoutine.collectAsState()
    val reorder = rememberReorderState { from, to -> vm.move(from, to) }
    val locked = rememberReorderLock(settings.listsLockedByDefault)
    var showCreate by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = { showCreate = true }) {
                Icon(Icons.Filled.Add, null)
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.new_program))
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.tab_programs),
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 12.dp),
                    )
                    ReorderLockButton(locked)
                }
            }
            items(routines.size) { i ->
                val r = routines[i]
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (locked.value) Modifier
                            else Modifier.reorderableRow(reorder, i, routines.size)
                        )
                        .clickable { nav.navigate(Routes.program(r.routine.id)) },
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (!locked.value) {
                            ReorderHandle(reorder, i, routines.size)
                            Spacer(Modifier.width(8.dp))
                        }
                        r.routine.icon.vectorOrNull()?.let { Icon(it, null) }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(r.routine.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                stringResource(R.string.day_count, r.days.size) + " · " +
                                    lastPerformedText(last[r.routine.id]),
                                style = MaterialTheme.typography.bodySmall,
                            )
                            if (r.routine.tags.isNotEmpty()) {
                                Text(
                                    r.routine.tags.joinToString(" ") { "#$it" },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.tertiary,
                                )
                            }
                        }
                        IconButton(onClick = { vm.setActive(r.routine.id, !r.routine.isActive) }) {
                            if (r.routine.isActive) {
                                Icon(
                                    Icons.Filled.Star,
                                    stringResource(R.string.active_badge),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            } else {
                                Icon(Icons.Outlined.StarOutline, stringResource(R.string.set_active))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreate) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreate = false },
            title = { Text(stringResource(R.string.new_program)) },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.name)) },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (name.isNotBlank()) {
                        showCreate = false
                        vm.create(name.trim()) { id -> nav.navigate(Routes.program(id)) }
                    }
                }) { Text(stringResource(R.string.create)) }
            },
            dismissButton = {
                TextButton(onClick = { showCreate = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}
