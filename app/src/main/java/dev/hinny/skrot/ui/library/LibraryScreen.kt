package dev.hinny.skrot.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import dev.hinny.skrot.R
import dev.hinny.skrot.ui.Routes

/** Landing page for the Library tab: programs, exercises and gyms. */
@Composable
fun LibraryScreen(nav: NavHostController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Text(
            stringResource(R.string.tab_library),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 12.dp),
        )
        LibraryItem(
            icon = Icons.Filled.MenuBook,
            label = stringResource(R.string.tab_programs),
            hint = stringResource(R.string.library_programs_hint),
        ) { nav.navigate(Routes.PROGRAMS) }
        LibraryItem(
            icon = Icons.Filled.FitnessCenter,
            label = stringResource(R.string.tab_exercises),
            hint = stringResource(R.string.library_exercises_hint),
        ) { nav.navigate(Routes.EXERCISES) }
        LibraryItem(
            icon = Icons.Filled.Place,
            label = stringResource(R.string.gyms),
            hint = stringResource(R.string.library_gyms_hint),
        ) { nav.navigate(Routes.GYMS) }
    }
}

@Composable
private fun LibraryItem(icon: ImageVector, label: String, hint: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, null)
            Spacer(Modifier.width(16.dp))
            Column {
                Text(label, style = MaterialTheme.typography.titleMedium)
                Text(hint, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
