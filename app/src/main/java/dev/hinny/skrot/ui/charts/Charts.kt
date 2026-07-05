package dev.hinny.skrot.ui.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.hinny.skrot.domain.Units
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.ceil

/**
 * Chart style follows one system: a single hue per chart (identity comes from
 * the title, not a palette), thin marks, recessive axes, direct min/max labels.
 */

@Composable
fun LineChart(
    points: List<Pair<Long, Double>>, // epoch ms -> value
    modifier: Modifier = Modifier,
    valueFormatter: (Double) -> String = { Units.formatValue(it) },
) {
    if (points.isEmpty()) {
        EmptyChartHint(modifier)
        return
    }
    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    val sorted = points.sortedBy { it.first }
    val minValue = sorted.minOf { it.second }
    val maxValue = sorted.maxOf { it.second }
    val dateFormat = DateTimeFormatter.ofPattern("d MMM")

    Column(modifier) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween) {
            Text(
                valueFormatter(maxValue),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                valueFormatter(minValue),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(160.dp)
                .padding(vertical = 4.dp),
        ) {
            val minX = sorted.first().first.toDouble()
            val maxX = sorted.last().first.toDouble()
            val spanX = (maxX - minX).coerceAtLeast(1.0)
            val spanY = (maxValue - minValue).coerceAtLeast(0.001)

            fun toOffset(p: Pair<Long, Double>): Offset {
                val x = ((p.first - minX) / spanX * size.width).toFloat()
                val y = (size.height - (p.second - minValue) / spanY * size.height).toFloat()
                return Offset(x, y.coerceIn(0f, size.height))
            }

            // recessive grid: three horizontal lines
            for (i in 0..2) {
                val y = size.height * i / 2f
                drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
            }

            val path = Path()
            sorted.forEachIndexed { i, p ->
                val offset = toOffset(p)
                if (i == 0) path.moveTo(offset.x, offset.y) else path.lineTo(offset.x, offset.y)
            }
            drawPath(path, lineColor, style = Stroke(width = 2.dp.toPx()))
            sorted.forEach { p ->
                drawCircle(lineColor, radius = 4.dp.toPx() / 2, center = toOffset(p))
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween) {
            Text(
                java.time.Instant.ofEpochMilli(sorted.first().first)
                    .atZone(java.time.ZoneId.systemDefault()).toLocalDate().format(dateFormat),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                java.time.Instant.ofEpochMilli(sorted.last().first)
                    .atZone(java.time.ZoneId.systemDefault()).toLocalDate().format(dateFormat),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun HorizontalBarChart(
    items: List<Pair<String, Int>>,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) {
        EmptyChartHint(modifier)
        return
    }
    val barColor = MaterialTheme.colorScheme.primary
    val max = items.maxOf { it.second }.coerceAtLeast(1)
    Column(modifier, verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp)) {
        items.forEach { (label, value) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.width(96.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Canvas(
                    Modifier
                        .weight(1f)
                        .height(16.dp),
                ) {
                    val width = size.width * value / max
                    drawRoundRect(
                        color = barColor,
                        size = Size(width, size.height),
                        cornerRadius = CornerRadius(4.dp.toPx()),
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(value.toString(), style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

/** GitHub-contribution-style calendar heatmap: sessions per day, sequential single hue. */
@Composable
fun CalendarHeatmap(
    countsByDay: Map<LocalDate, Int>,
    weeks: Int = 20,
    modifier: Modifier = Modifier,
) {
    val baseColor = MaterialTheme.colorScheme.primary
    val emptyColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    val maxCount = (countsByDay.values.maxOrNull() ?: 1).coerceAtLeast(1)
    val today = LocalDate.now()
    // Grid ends on the current week; columns = weeks, rows = Mon..Sun.
    val lastMonday = today.minusDays(((today.dayOfWeek.value + 6) % 7).toLong())
    val firstMonday = lastMonday.minusWeeks((weeks - 1).toLong())

    Canvas(
        modifier
            .fillMaxWidth()
            .height(120.dp),
    ) {
        val cell = minOf(size.width / weeks, size.height / 7f)
        val gap = cell * 0.15f
        for (week in 0 until weeks) {
            for (dow in 0 until 7) {
                val day = firstMonday.plusWeeks(week.toLong()).plusDays(dow.toLong())
                if (day.isAfter(today)) continue
                val count = countsByDay[day] ?: 0
                val alpha = if (count == 0) 0f else 0.25f + 0.75f * count / maxCount
                drawRoundRect(
                    color = if (count == 0) emptyColor else baseColor.copy(alpha = alpha),
                    topLeft = Offset(week * cell + gap / 2, dow * cell + gap / 2),
                    size = Size(cell - gap, cell - gap),
                    cornerRadius = CornerRadius(2.dp.toPx()),
                )
            }
        }
    }
}

@Composable
private fun EmptyChartHint(modifier: Modifier = Modifier) {
    Text(
        text = androidx.compose.ui.res.stringResource(dev.hinny.skrot.R.string.no_data_yet),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(vertical = 24.dp),
    )
}
