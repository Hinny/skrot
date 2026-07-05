package dev.hinny.skrot.ui.more

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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
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
import dev.hinny.skrot.BuildConfig
import dev.hinny.skrot.R
import dev.hinny.skrot.ui.Routes

@Composable
fun MoreScreen(nav: NavHostController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Text(
            stringResource(R.string.tab_more),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 12.dp),
        )
        MoreItem(Icons.Filled.Place, stringResource(R.string.gyms)) { nav.navigate(Routes.GYMS) }
        MoreItem(Icons.Filled.MonitorWeight, stringResource(R.string.body_metrics)) {
            nav.navigate(Routes.BODY)
        }
        MoreItem(Icons.Filled.Save, stringResource(R.string.backup_and_import)) {
            nav.navigate(Routes.BACKUP)
        }
        MoreItem(Icons.Filled.Settings, stringResource(R.string.settings)) {
            nav.navigate(Routes.SETTINGS)
        }
        MoreItem(Icons.Filled.Info, stringResource(R.string.about)) { nav.navigate(Routes.ABOUT) }
    }
}

@Composable
private fun MoreItem(icon: ImageVector, label: String, onClick: () -> Unit) {
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
            Text(label, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
fun AboutScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Text(
            stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            stringResource(R.string.about_version, BuildConfig.VERSION_NAME),
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(
            stringResource(R.string.about_description),
            modifier = Modifier.padding(top = 16.dp),
        )
        Text(
            stringResource(R.string.about_license),
            modifier = Modifier.padding(top = 16.dp),
        )
        Text(
            "https://github.com/Hinny/skrot",
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(
            stringResource(R.string.about_offline),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 16.dp),
        )
    }
}
