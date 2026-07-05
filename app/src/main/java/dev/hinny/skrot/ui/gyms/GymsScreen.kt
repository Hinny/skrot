package dev.hinny.skrot.ui.gyms

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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.hinny.skrot.AppContainer
import dev.hinny.skrot.R
import dev.hinny.skrot.data.model.Equipment
import dev.hinny.skrot.data.model.Exercise
import dev.hinny.skrot.data.model.Gym
import dev.hinny.skrot.data.model.GymExercise
import dev.hinny.skrot.ui.common.displayName
import dev.hinny.skrot.ui.containerViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class GymsViewModel(private val container: AppContainer) : ViewModel() {
    private val db = container.db
    val gyms = MutableStateFlow<List<Gym>>(emptyList())
    val exercises = MutableStateFlow<List<Exercise>>(emptyList())
    val editingGym = MutableStateFlow<Gym?>(null)
    val editingAvailable = MutableStateFlow<Set<Long>>(emptySet())

    init {
        viewModelScope.launch { db.gymDao().observeAll().collect { gyms.value = it } }
        viewModelScope.launch { db.exerciseDao().observeAll().collect { exercises.value = it } }
        viewModelScope.launch {
            editingGym.flatMapLatest { gym ->
                if (gym == null) flowOf(emptyList())
                else db.gymDao().observeExerciseIdsAt(gym.id)
            }.collect { editingAvailable.value = it.toSet() }
        }
    }

    fun create(name: String) {
        viewModelScope.launch {
            val makeDefault = gyms.value.isEmpty()
            val id = db.gymDao().insert(Gym(name = name, isDefault = makeDefault))
            editingGym.value = db.gymDao().byId(id)
        }
    }

    fun rename(gym: Gym, name: String) {
        viewModelScope.launch { db.gymDao().update(gym.copy(name = name)) }
    }

    fun delete(gym: Gym) {
        viewModelScope.launch {
            db.gymDao().delete(gym)
            if (editingGym.value?.id == gym.id) editingGym.value = null
        }
    }

    fun setDefault(gym: Gym) {
        viewModelScope.launch { db.gymDao().setDefault(gym.id) }
    }

    fun toggleExercise(gymId: Long, exerciseId: Long, available: Boolean) {
        viewModelScope.launch {
            if (available) db.gymDao().addExercise(GymExercise(gymId, exerciseId))
            else db.gymDao().removeExercise(gymId, exerciseId)
        }
    }

    /** Bulk helper: mark every exercise with this equipment as available. */
    fun addAllWithEquipment(gymId: Long, equipment: Equipment) {
        viewModelScope.launch {
            db.gymDao().addExercises(
                exercises.value
                    .filter { it.equipment == equipment }
                    .map { GymExercise(gymId, it.id) }
            )
        }
    }

    fun addAll(gymId: Long) {
        viewModelScope.launch {
            db.gymDao().addExercises(exercises.value.map { GymExercise(gymId, it.id) })
        }
    }
}

@Composable
fun GymsScreen(container: AppContainer) {
    val vm = containerViewModel(container) { c, _ -> GymsViewModel(c) }
    val gyms by vm.gyms.collectAsState()
    val exercises by vm.exercises.collectAsState()
    val editing by vm.editingGym.collectAsState()
    val available by vm.editingAvailable.collectAsState()
    var showCreate by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }

    val gym = editing
    if (gym == null) {
        Scaffold(
            floatingActionButton = {
                ExtendedFloatingActionButton(onClick = { showCreate = true }) {
                    Icon(Icons.Filled.Add, null)
                    Text(stringResource(R.string.new_gym))
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
                        stringResource(R.string.gyms),
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(vertical = 12.dp),
                    )
                    Text(
                        stringResource(R.string.gyms_hint),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                items(gyms.size) { i ->
                    val g = gyms[i]
                    Card(
                        Modifier
                            .fillMaxWidth()
                            .clickable { vm.editingGym.value = g },
                    ) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                g.name,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.weight(1f),
                            )
                            IconButton(onClick = { vm.setDefault(g) }) {
                                if (g.isDefault) {
                                    Icon(
                                        Icons.Filled.Star,
                                        stringResource(R.string.default_gym),
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                } else {
                                    Icon(Icons.Outlined.StarOutline, stringResource(R.string.set_default))
                                }
                            }
                            IconButton(onClick = { vm.delete(g) }) {
                                Icon(Icons.Filled.Delete, stringResource(R.string.delete))
                            }
                        }
                    }
                }
            }
        }
    } else {
        // Gym editor: which exercises are available here
        var name by remember(gym.id) { mutableStateOf(gym.name) }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 12.dp),
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it; vm.rename(gym, it) },
                        label = { Text(stringResource(R.string.name)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { vm.editingGym.value = null }) {
                        Text(stringResource(R.string.done))
                    }
                }
            }
            item {
                Text(
                    stringResource(R.string.available_exercises, available.size),
                    style = MaterialTheme.typography.titleSmall,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                ) {
                    FilterChip(
                        selected = false,
                        onClick = { vm.addAll(gym.id) },
                        label = { Text(stringResource(R.string.select_all)) },
                    )
                    Equipment.entries.forEach { eq ->
                        FilterChip(
                            selected = false,
                            onClick = { vm.addAllWithEquipment(gym.id, eq) },
                            label = { Text(stringResource(R.string.all_of, eq.name.lowercase())) },
                        )
                    }
                }
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text(stringResource(R.string.search)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            val filtered = exercises.filter {
                query.isBlank() || it.displayName().contains(query, ignoreCase = true)
            }
            items(filtered.size) { i ->
                val e = filtered[i]
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { vm.toggleExercise(gym.id, e.id, e.id !in available) },
                ) {
                    Checkbox(
                        checked = e.id in available,
                        onCheckedChange = { vm.toggleExercise(gym.id, e.id, it) },
                    )
                    Spacer(Modifier.width(4.dp))
                    Column {
                        Text(e.displayName())
                        Text(
                            e.equipment.name.lowercase(),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
            item { Spacer(Modifier.height(40.dp)) }
        }
    }

    if (showCreate) {
        var newName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreate = false },
            title = { Text(stringResource(R.string.new_gym)) },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text(stringResource(R.string.name)) },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newName.isNotBlank()) {
                        vm.create(newName.trim())
                        showCreate = false
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
