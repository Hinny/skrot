package dev.hinny.skrot.ui.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.hinny.skrot.domain.Units
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

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

/**
 * Calendar heatmap where every row is one week: seven day cells, Monday first,
 * oldest week at the top. Cell size shrinks for longer ranges so half a year
 * still fits on screen without scrolling.
 */
@Composable
fun WeekCalendarHeatmap(
    countsByDay: Map<LocalDate, Int>,
    weeks: Int,
    modifier: Modifier = Modifier,
) {
    val today = LocalDate.now()
    val maxCount = (countsByDay.values.maxOrNull() ?: 1).coerceAtLeast(1)
    val thisMonday = today.minusDays(((today.dayOfWeek.value + 6) % 7).toLong())
    val rows = weeks.coerceAtLeast(1)
    val firstMonday = thisMonday.minusWeeks((rows - 1).toLong())
    val locale = Locale.getDefault()
    val cell = when {
        rows <= 14 -> 22.dp
        rows <= 30 -> 16.dp
        else -> 12.dp
    }
    val labelWidth = 48.dp
    val monthFormat = remember(locale) { DateTimeFormatter.ofPattern("d MMM", locale) }

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            Spacer(Modifier.width(labelWidth))
            for (dow in 1..7) {
                HeatAxisLabel(
                    text = DayOfWeek.of(dow).getDisplayName(TextStyle.NARROW, locale)
                        .uppercase(locale),
                    modifier = Modifier.width(cell),
                )
            }
        }
        for (week in 0 until rows) {
            val monday = firstMonday.plusWeeks(week.toLong())
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                HeatAxisLabel(
                    text = monday.format(monthFormat),
                    modifier = Modifier.width(labelWidth),
                    align = TextAlign.End,
                )
                for (dow in 0 until 7) {
                    val day = monday.plusDays(dow.toLong())
                    HeatCell(
                        count = if (day.isAfter(today)) null else countsByDay[day] ?: 0,
                        maxCount = maxCount,
                        modifier = Modifier.width(cell),
                    )
                }
            }
        }
    }
}

/**
 * Calendar heatmap where every row is one month: up to 31 day cells. Used for
 * the long ranges, where one row per week would be an unreadably tall grid.
 */
@Composable
fun MonthCalendarHeatmap(
    countsByDay: Map<LocalDate, Int>,
    months: Int,
    modifier: Modifier = Modifier,
) {
    val today = LocalDate.now()
    val maxCount = (countsByDay.values.maxOrNull() ?: 1).coerceAtLeast(1)
    val rows = months.coerceAtLeast(1)
    val firstMonth = YearMonth.from(today).minusMonths((rows - 1).toLong())
    val locale = Locale.getDefault()
    val labelWidth = 48.dp
    val monthFormat = remember(locale) { DateTimeFormatter.ofPattern("MMM yy", locale) }

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
            Spacer(Modifier.width(labelWidth))
            for (dayOfMonth in 1..31) {
                // Only every fifth day is numbered; the rest would not fit.
                HeatAxisLabel(
                    text = if (dayOfMonth % 5 == 0) dayOfMonth.toString() else "",
                    modifier = Modifier.weight(1f),
                )
            }
        }
        for (row in 0 until rows) {
            val month = firstMonth.plusMonths(row.toLong())
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                HeatAxisLabel(
                    text = month.atDay(1).format(monthFormat),
                    modifier = Modifier.width(labelWidth),
                    align = TextAlign.End,
                )
                for (dayOfMonth in 1..31) {
                    val day =
                        if (dayOfMonth <= month.lengthOfMonth()) month.atDay(dayOfMonth) else null
                    HeatCell(
                        count = when {
                            day == null || day.isAfter(today) -> null
                            else -> countsByDay[day] ?: 0
                        },
                        maxCount = maxCount,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

/** One day in a heatmap; [count] null means "outside the calendar" and stays blank. */
@Composable
private fun HeatCell(count: Int?, maxCount: Int, modifier: Modifier = Modifier) {
    val baseColor = MaterialTheme.colorScheme.primary
    val emptyColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    val color = when {
        count == null -> Color.Transparent
        count == 0 -> emptyColor
        else -> baseColor.copy(alpha = 0.25f + 0.75f * count / maxCount)
    }
    Box(
        modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(2.dp))
            .background(color),
    )
}

@Composable
private fun HeatAxisLabel(
    text: String,
    modifier: Modifier = Modifier,
    align: TextAlign = TextAlign.Center,
) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = align,
        maxLines = 1,
        overflow = TextOverflow.Clip,
        modifier = modifier.padding(end = 2.dp),
    )
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
