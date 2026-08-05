package dev.hinny.skrot.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import dev.hinny.skrot.data.db.SessionCounts
import dev.hinny.skrot.data.model.SessionWithContent
import dev.hinny.skrot.data.model.WorkoutSession
import dev.hinny.skrot.ui.Routes
import dev.hinny.skrot.ui.common.SearchField
import dev.hinny.skrot.ui.containerViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class SessionHistoryViewModel(private val container: AppContainer) : ViewModel() {
    val sessions = MutableStateFlow<List<WorkoutSession>>(emptyList())
    val dayNames = MutableStateFlow<Map<Long, String>>(emptyMap())
    val gyms = MutableStateFlow<Map<Long, String>>(emptyMap())
    val counts = MutableStateFlow<Map<Long, SessionCounts>>(emptyMap())

    init {
        viewModelScope.launch {
            container.db.sessionDao().observeFinishedSessions()
                .collect { all -> sessions.value = all.sortedByDescending { it.startedAt } }
        }
        viewModelScope.launch {
            container.db.routineDao().observeAllWithDays().collect { routines ->
                dayNames.value = routines.flatMap { r -> r.days.map { it.id to it.name } }.toMap()
            }
        }
        viewModelScope.launch {
            container.db.gymDao().observeAll()
                .collect { all -> gyms.value = all.associate { it.id to it.name } }
        }
        viewModelScope.launch {
            container.db.sessionDao().observeSessionCounts()
                .collect { all -> counts.value = all.associateBy { it.sessionId } }
        }
    }

    /** The last workout deleted, held in memory so the snackbar can put it back. */
    private var lastDeleted: SessionWithContent? = null

    suspend fun delete(id: Long) {
        val dao = container.db.sessionDao()
        lastDeleted = dao.sessionWithContent(id)
        dao.deleteSession(id)
    }

    /**
     * Re-inserts the snapshot taken before the delete. Row ids are reassigned on
     * the way back in — nothing the user can see depends on them, and the
     * alternative is fighting the autoincrement for no gain.
     */
    suspend fun undoDelete() {
        val snapshot = lastDeleted ?: return
        lastDeleted = null
        val dao = container.db.sessionDao()
        val sessionId = dao.insertSession(snapshot.session.copy(id = 0))
        for (se in snapshot.exercises) {
            val seId = dao.insertSessionExercise(
                se.sessionExercise.copy(id = 0, sessionId = sessionId)
            )
            for (set in se.sortedSets) {
                dao.insertLoggedSet(set.copy(id = 0, sessionExerciseId = seId))
            }
        }
    }
}

/** Every logged workout, newest first; opens the history editor on tap. */
@Composable
fun SessionHistoryScreen(container: AppContainer, nav: NavHostController) {
    val vm = containerViewModel(container) { c, _ -> SessionHistoryViewModel(c) }
    val sessions by vm.sessions.collectAsState()
    val dayNames by vm.dayNames.collectAsState()
    val gyms by vm.gyms.collectAsState()
    val counts by vm.counts.collectAsState()
    var query by remember { mutableStateOf("") }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val deletedMsg = stringResource(R.string.session_deleted)
    val undoLabel = stringResource(R.string.undo)

    val zone = ZoneId.systemDefault()
    val dateFormat = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd") }
    val freestyle = stringResource(R.string.freestyle_session)

    fun title(s: WorkoutSession) = s.routineDayId?.let { dayNames[it] } ?: freestyle
    fun dateOf(s: WorkoutSession) =
        Instant.ofEpochMilli(s.startedAt).atZone(zone).toLocalDate().format(dateFormat)

    val filtered = sessions.filter { s ->
        query.isBlank() || listOfNotNull(
            title(s),
            dateOf(s),
            s.gymId?.let { gyms[it] },
        ).any { it.contains(query, ignoreCase = true) }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(horizontal = 16.dp),
    ) {
        Text(
            stringResource(R.string.session_history),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(vertical = 12.dp),
        )
        SearchField(
            value = query,
            onValueChange = { query = it },
            placeholder = stringResource(R.string.search_history),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        if (filtered.isEmpty()) {
            Text(
                stringResource(R.string.no_data_yet),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(filtered.size) { i ->
                val session = filtered[i]
                Card(
                    Modifier
                        .fillMaxWidth()
                        .clickable { nav.navigate(Routes.historySession(session.id)) },
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(title(session), style = MaterialTheme.typography.titleMedium)
                            Text(
                                dateOf(session) +
                                    (session.gymId?.let { g -> gyms[g]?.let { " · $it" } } ?: ""),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            counts[session.id]?.let { c ->
                                Text(
                                    stringResource(
                                        R.string.history_counts,
                                        c.exerciseCount,
                                        c.setCount,
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        // Deleting is undoable rather than confirmed: a dialog
                        // on every delete is friction on the common case, and
                        // the whole workout comes back from the snackbar.
                        IconButton(onClick = {
                            scope.launch {
                                vm.delete(session.id)
                                val result = snackbar.showSnackbar(
                                    message = deletedMsg,
                                    actionLabel = undoLabel,
                                    duration = SnackbarDuration.Long,
                                )
                                if (result == SnackbarResult.ActionPerformed) vm.undoDelete()
                            }
                        }) {
                            Icon(Icons.Filled.Delete, stringResource(R.string.delete))
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
    }
}
