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
import dev.hinny.skrot.ui.common.lastPerformedText
import dev.hinny.skrot.ui.common.vector
import dev.hinny.skrot.ui.containerViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class ProgramsViewModel(private val container: AppContainer) : ViewModel() {
    val routines = MutableStateFlow<List<RoutineWithDays>>(emptyList())
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
fun ProgramsScreen(container: AppContainer, nav: NavHostController) {
    val vm = containerViewModel(container) { c, _ -> ProgramsViewModel(c) }
    val routines by vm.routines.collectAsState()
    val last by vm.lastByRoutine.collectAsState()
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
                Text(
                    stringResource(R.string.tab_programs),
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(vertical = 12.dp),
                )
            }
            items(routines.size) { i ->
                val r = routines[i]
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { nav.navigate(Routes.program(r.routine.id)) },
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(r.routine.icon.vector(), null)
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
