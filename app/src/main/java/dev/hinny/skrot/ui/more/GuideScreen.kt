package dev.hinny.skrot.ui.more

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.hinny.skrot.R

/** One expandable topic: title + explanation. */
private data class GuideTopic(val titleRes: Int, val bodyRes: Int)

private val basicTopics = listOf(
    GuideTopic(R.string.guide_programs_t, R.string.guide_programs_b),
    GuideTopic(R.string.guide_logging_t, R.string.guide_logging_b),
    GuideTopic(R.string.guide_library_t, R.string.guide_library_b),
    GuideTopic(R.string.guide_stats_t, R.string.guide_stats_b),
    GuideTopic(R.string.guide_backup_t, R.string.guide_backup_b),
)

private val advancedTopics = listOf(
    GuideTopic(R.string.guide_gyms_t, R.string.guide_gyms_b),
    GuideTopic(R.string.guide_temp_gym_t, R.string.guide_temp_gym_b),
    GuideTopic(R.string.guide_plan_edits_t, R.string.guide_plan_edits_b),
    GuideTopic(R.string.guide_supersets_t, R.string.guide_supersets_b),
    GuideTopic(R.string.guide_measurements_t, R.string.guide_measurements_b),
    GuideTopic(R.string.guide_prefill_t, R.string.guide_prefill_b),
    GuideTopic(R.string.guide_autofinish_t, R.string.guide_autofinish_b),
)

/** Pedagogical walkthrough of the app's features, from basics to fine print. */
@Composable
fun GuideScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Text(
            stringResource(R.string.guide_title),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 12.dp),
        )

        Text(
            stringResource(R.string.guide_basic),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(vertical = 8.dp),
        )
        basicTopics.forEach { TopicCard(it) }

        Text(
            stringResource(R.string.guide_advanced),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(vertical = 8.dp),
        )
        advancedTopics.forEach { TopicCard(it) }
    }
}

@Composable
private fun TopicCard(topic: GuideTopic) {
    var expanded by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { expanded = !expanded },
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(topic.titleRes),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = null,
                )
            }
            if (expanded) {
                Text(
                    stringResource(topic.bodyRes),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}
