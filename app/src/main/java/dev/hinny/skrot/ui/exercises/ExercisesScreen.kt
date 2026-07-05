package dev.hinny.skrot.ui.exercises

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import dev.hinny.skrot.AppContainer
import dev.hinny.skrot.R
import dev.hinny.skrot.data.model.Equipment
import dev.hinny.skrot.data.model.Exercise
import dev.hinny.skrot.data.model.MuscleGroup
import dev.hinny.skrot.ui.Routes
import dev.hinny.skrot.ui.common.ExercisePickerDialog
import dev.hinny.skrot.ui.common.displayName
import dev.hinny.skrot.ui.common.muscleLabel
import dev.hinny.skrot.ui.containerViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class ExercisesViewModel(private val container: AppContainer) : ViewModel() {
    val exercises = MutableStateFlow<List<Exercise>>(emptyList())

    init {
        viewModelScope.launch {
            container.db.exerciseDao().observeAll().collect { exercises.value = it }
        }
    }

    fun create(name: String, muscle: MuscleGroup, onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            val id = container.db.exerciseDao().insert(
                Exercise(nameEn = name, nameSv = name, muscleGroup = muscle, isCustom = true)
            )
            onCreated(id)
        }
    }
}

@Composable
fun ExercisesScreen(container: AppContainer, nav: NavHostController) {
    val vm = containerViewModel(container) { c, _ -> ExercisesViewModel(c) }
    val exercises by vm.exercises.collectAsState()
    var query by remember { mutableStateOf("") }
    var muscleFilter by remember { mutableStateOf<MuscleGroup?>(null) }
    var equipmentFilter by remember { mutableStateOf<Equipment?>(null) }
    var showCreate by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = { showCreate = true }) {
                Icon(Icons.Filled.Add, null)
                Text(stringResource(R.string.new_exercise))
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
        ) {
            Text(
                stringResource(R.string.tab_exercises),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(vertical = 12.dp),
            )
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text(stringResource(R.string.search)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 6.dp),
            ) {
                MuscleGroup.entries.forEach { m ->
                    FilterChip(
                        selected = muscleFilter == m,
                        onClick = { muscleFilter = if (muscleFilter == m) null else m },
                        label = { Text(muscleLabel(m)) },
                    )
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = 6.dp),
            ) {
                Equipment.entries.forEach { eq ->
                    FilterChip(
                        selected = equipmentFilter == eq,
                        onClick = { equipmentFilter = if (equipmentFilter == eq) null else eq },
                        label = { Text(eq.name.lowercase()) },
                    )
                }
            }

            val filtered = exercises.filter { e ->
                (query.isBlank() || e.displayName().contains(query, ignoreCase = true)) &&
                    (muscleFilter == null || e.muscleGroup == muscleFilter) &&
                    (equipmentFilter == null || e.equipment == equipmentFilter)
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(filtered.size) { i ->
                    val e = filtered[i]
                    Card(
                        Modifier
                            .fillMaxWidth()
                            .clickable { nav.navigate(Routes.exercise(e.id)) },
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Row {
                                Text(
                                    e.displayName(),
                                    style = MaterialTheme.typography.titleSmall,
                                    modifier = Modifier.weight(1f),
                                )
                                if (e.isCustom) {
                                    Text(
                                        stringResource(R.string.custom_badge),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.tertiary,
                                    )
                                }
                            }
                            Text(
                                "${muscleLabel(e.muscleGroup)} · ${e.equipment.name.lowercase()}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }

    if (showCreate) {
        ExercisePickerDialog(
            exercises = emptyList(),
            onPick = {},
            onCreate = { name, muscle ->
                showCreate = false
                vm.create(name, muscle) { id -> nav.navigate(Routes.exercise(id)) }
            },
            onDismiss = { showCreate = false },
        )
    }
}
