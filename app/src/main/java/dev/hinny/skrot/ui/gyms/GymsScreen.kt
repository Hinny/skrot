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
import dev.hinny.skrot.ui.common.PendingChangesBar
import dev.hinny.skrot.ui.common.displayName
import dev.hinny.skrot.ui.common.equipmentLabel
import dev.hinny.skrot.ui.containerViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class GymsViewModel(private val container: AppContainer) : ViewModel() {
    private val db = container.db
    val gyms = MutableStateFlow<List<Gym>>(emptyList())
    val exercises = MutableStateFlow<List<Exercise>>(emptyList())

    private val editingGymId = MutableStateFlow<Long?>(null)
    val editingGym = MutableStateFlow<Gym?>(null)
    val editingAvailable = MutableStateFlow<Set<Long>>(emptySet())
    val confirmEdits = MutableStateFlow(true)
    val hasPendingChanges = MutableStateFlow(false)

    /** Last-confirmed state for the gym being edited; only meaningful while [confirmEdits] is on. */
    private var baselineGym: Gym? = null
    private var baselineAvailable: Set<Long> = emptySet()

    init {
        viewModelScope.launch { db.gymDao().observeAll().collect { gyms.value = it } }
        viewModelScope.launch { db.exerciseDao().observeAll().collect { exercises.value = it } }
        viewModelScope.launch {
            editingGymId.flatMapLatest { id ->
                if (id == null) {
                    flowOf<Pair<Gym?, Set<Long>>>(null to emptySet())
                } else {
                    combine(
                        db.gymDao().observeById(id),
                        db.gymDao().observeExerciseIdsAt(id),
                    ) { gym, ids -> gym to ids.toSet() }
                }
            }.collect { (gym, available) ->
                val switchedGym = gym != null && baselineGym?.id != gym.id
                editingGym.value = gym
                editingAvailable.value = available
                if (gym == null) {
                    baselineGym = null
                    baselineAvailable = emptySet()
                } else if (switchedGym) {
                    baselineGym = gym
                    baselineAvailable = available
                }
                recomputePending()
            }
        }
        viewModelScope.launch {
            container.settings.settings.collect {
                confirmEdits.value = it.confirmLibraryEdits
                recomputePending()
            }
        }
    }

    /**
     * Every mutation below (rename, toggle an exercise, bulk-add, ...) writes
     * straight to the database as before; this just tracks whether the live
     * state has drifted from the last-confirmed baseline so the UI can show
     * an Apply/Cancel bar. In auto-apply mode every change re-baselines
     * immediately, so no bar ever appears.
     */
    private fun recomputePending() {
        val gym = editingGym.value
        if (gym == null) {
            hasPendingChanges.value = false
            return
        }
        if (!confirmEdits.value) {
            baselineGym = gym
            baselineAvailable = editingAvailable.value
            hasPendingChanges.value = false
        } else {
            hasPendingChanges.value = gym != baselineGym || editingAvailable.value != baselineAvailable
        }
    }

    fun applyChanges() {
        baselineGym = editingGym.value
        baselineAvailable = editingAvailable.value
        hasPendingChanges.value = false
    }

    /** Reverts the edited gym's name and available-exercise list to the last Apply point. */
    fun cancelChanges() {
        viewModelScope.launch {
            val gym = baselineGym ?: return@launch
            db.gymDao().update(gym)
            val current = db.gymDao().exerciseIdsAt(gym.id).toSet()
            for (id in current - baselineAvailable) db.gymDao().removeExercise(gym.id, id)
            val toAdd = baselineAvailable - current
            if (toAdd.isNotEmpty()) db.gymDao().addExercises(toAdd.map { GymExercise(gym.id, it) })
        }
    }

    fun enterEditing(gymId: Long) {
        editingGymId.value = gymId
    }

    fun exitEditing() {
        editingGymId.value = null
    }

    fun create(name: String) {
        viewModelScope.launch {
            val makeDefault = gyms.value.isEmpty()
            val id = db.gymDao().insert(Gym(name = name, isDefault = makeDefault))
            editingGymId.value = id
        }
    }

    fun rename(gym: Gym, name: String) {
        viewModelScope.launch { db.gymDao().update(gym.copy(name = name)) }
    }

    fun delete(gym: Gym) {
        viewModelScope.launch {
            db.gymDao().delete(gym)
            if (editingGymId.value == gym.id) editingGymId.value = null
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
                    .filter { equipment in it.equipment }
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
    val hasPendingChanges by vm.hasPendingChanges.collectAsState()
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
                            .clickable { vm.enterEditing(g.id) },
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
        Column(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
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
                    TextButton(onClick = { vm.exitEditing() }) {
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
                            label = { Text(stringResource(R.string.all_of, equipmentLabel(eq).lowercase())) },
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
                            e.equipment.map { equipmentLabel(it) }.joinToString(" + "),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
            item { Spacer(Modifier.height(40.dp)) }
        }
        if (hasPendingChanges) {
            PendingChangesBar(onApply = { vm.applyChanges() }, onCancel = { vm.cancelChanges() })
        }
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
