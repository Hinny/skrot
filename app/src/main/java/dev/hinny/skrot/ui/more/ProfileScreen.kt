package dev.hinny.skrot.ui.more

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.hinny.skrot.AppContainer
import dev.hinny.skrot.R
import dev.hinny.skrot.data.model.Sex
import dev.hinny.skrot.data.prefs.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Offline profile: every field optional, nothing ever leaves the device. */
@Composable
fun ProfileScreen(container: AppContainer, settings: Settings) {
    val scope = remember { CoroutineScope(Dispatchers.Main) }
    val repo = container.settings
    var name by remember { mutableStateOf(settings.profileName) }
    var birthYear by remember {
        mutableStateOf(if (settings.profileBirthYear > 0) settings.profileBirthYear.toString() else "")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(stringResource(R.string.profile), style = MaterialTheme.typography.headlineMedium)
        Text(
            stringResource(R.string.profile_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        OutlinedTextField(
            value = name,
            onValueChange = {
                name = it
                scope.launch { repo.setProfileName(it.trim()) }
            },
            label = { Text(stringResource(R.string.profile_name)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = birthYear,
            onValueChange = { input ->
                birthYear = input.filter(Char::isDigit).take(4)
                val year = birthYear.toIntOrNull() ?: 0
                // Only persist plausible years (or clearing the field).
                if (birthYear.isEmpty() || year in 1900..LocalDate.now().year) {
                    scope.launch { repo.setProfileBirthYear(if (birthYear.isEmpty()) 0 else year) }
                }
            },
            label = { Text(stringResource(R.string.profile_birth_year)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Text(stringResource(R.string.profile_sex), style = MaterialTheme.typography.titleMedium)
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState()),
        ) {
            listOf(
                Sex.UNSPECIFIED to stringResource(R.string.sex_unspecified),
                Sex.FEMALE to stringResource(R.string.sex_female),
                Sex.MALE to stringResource(R.string.sex_male),
                Sex.OTHER to stringResource(R.string.sex_other),
            ).forEach { (value, label) ->
                FilterChip(
                    selected = settings.profileSex == value,
                    onClick = { scope.launch { repo.setProfileSex(value) } },
                    label = { Text(label) },
                )
            }
        }
    }
}
