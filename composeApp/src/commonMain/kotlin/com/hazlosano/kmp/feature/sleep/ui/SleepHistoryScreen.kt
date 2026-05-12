package com.hazlosano.kmp.feature.sleep.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.CrisisAlert
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material.icons.filled.SentimentDissatisfied
import androidx.compose.material.icons.filled.SentimentVeryDissatisfied
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.hazlosano.kmp.core.ui.components.atomic.LeafCard
import com.hazlosano.kmp.core.ui.theme.HazloSpaces
import com.hazlosano.kmp.core.ui.theme.PillarSleep
import com.hazlosano.kmp.core.ui.util.formatClockTime
import com.hazlosano.kmp.core.ui.util.formatWakeDate
import com.hazlosano.kmp.domain.model.SleepHistory
import com.hazlosano.kmp.domain.model.SleepNight
import com.hazlosano.kmp.feature.sleep.presentation.SleepHistoryUiState
import com.hazlosano.kmp.feature.sleep.presentation.SleepHistoryViewModel
import kmp.composeapp.generated.resources.Res
import kmp.composeapp.generated.resources.encouragement_bad_improving
import kmp.composeapp.generated.resources.encouragement_bad_today
import kmp.composeapp.generated.resources.encouragement_debt
import kmp.composeapp.generated.resources.encouragement_default
import kmp.composeapp.generated.resources.encouragement_excellent_week
import kmp.composeapp.generated.resources.encouragement_good_path
import kmp.composeapp.generated.resources.encouragement_improving
import kmp.composeapp.generated.resources.encouragement_waiting
import kmp.composeapp.generated.resources.encouragement_worsening
import kmp.composeapp.generated.resources.history_avg_label
import kmp.composeapp.generated.resources.history_back
import kmp.composeapp.generated.resources.history_consistency_label
import kmp.composeapp.generated.resources.history_debt_label
import kmp.composeapp.generated.resources.history_window_label
import kmp.composeapp.generated.resources.sleep_efficiency_label
import kmp.composeapp.generated.resources.history_title
import kmp.composeapp.generated.resources.history_trend_label
import kmp.composeapp.generated.resources.sleep_quality_excellent_short
import kmp.composeapp.generated.resources.sleep_quality_good_short
import kmp.composeapp.generated.resources.sleep_quality_interrupted_short
import kmp.composeapp.generated.resources.sleep_quality_no_data_short
import kmp.composeapp.generated.resources.sleep_quality_poor_short
import org.jetbrains.compose.resources.stringResource

@Composable
fun SleepHistoryScreen(
    viewModel: SleepHistoryViewModel,
) {
    val state by viewModel.state.collectAsState()

    when (val s = state) {
        is SleepHistoryUiState.Loading -> Box(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center,
        ) { CircularProgressIndicator(color = PillarSleep) }

        is SleepHistoryUiState.Error -> Box(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center,
        ) { Text(s.message, color = MaterialTheme.colorScheme.error) }

        is SleepHistoryUiState.Success -> HistoryContent(s.history)
    }
}

@Composable
private fun HistoryContent(history: SleepHistory) {
    val bgUrl = sleepBackgroundUrl(history)
    Box(modifier = Modifier.fillMaxSize()) {
        // background image
        if (bgUrl != null) {
            AsyncImage(
                model = bgUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alignment = Alignment.TopCenter,
            )
        }
        // dark overlay for readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.85f),
                            Color.Transparent
                        )
                    )
                ),
        )
        // content
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = HazloSpaces.md, bottom = HazloSpaces.xl),
        ) {
        // Summary metrics card with moon + encouragement
        item {
            LeafCard(
                modifier = Modifier.fillMaxWidth().padding(horizontal = HazloSpaces.gutter),
                containerColor = Color.Black.copy(alpha = 0.35f),
            ) {
                Column(modifier = Modifier.padding(HazloSpaces.md)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(36.dp).clip(CircleShape)
                                .background(PillarSleep),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Filled.Bedtime, null, tint = Color.White, modifier = Modifier.size(22.dp))
                        }
                        Spacer(modifier = Modifier.width(HazloSpaces.sm))
                        Text(
                            encouragementText(history),
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White.copy(alpha = 0.9f),
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Spacer(modifier = Modifier.height(HazloSpaces.md))
                    // Row 1: 3 columns
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        MetricItem(Icons.Filled.Schedule, stringResource(Res.string.history_avg_label), "${history.averageHours.toInt()}h ${((history.averageHours % 1) * 60).toInt()}m")
                        MetricItem(Icons.Filled.Bolt, stringResource(Res.string.sleep_efficiency_label), "${(history.averageEfficiency * 100).toInt()}%")
                        TrendItem(history.trendLabel)
                    }
                    Spacer(modifier = Modifier.height(HazloSpaces.sm))
                    // Row 2: 3 columns
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        MetricItem(Icons.Filled.CrisisAlert, stringResource(Res.string.history_consistency_label), "±${history.consistencyMinutes}m")
                        MetricItem(Icons.Filled.HourglassBottom, stringResource(Res.string.history_debt_label), "${history.sleepDebtMinutes / 60}h ${history.sleepDebtMinutes % 60}m")
                        MetricItem(Icons.Filled.Bedtime, stringResource(Res.string.history_window_label), "${history.sleepWindowHours.toInt()}h ${((history.sleepWindowHours % 1) * 60).toInt()}m")
                    }
                }
            }
            Spacer(modifier = Modifier.height(HazloSpaces.md))
        }

        // Daily list
        items(history.nights) { night ->
            NightCard(night)
            Spacer(modifier = Modifier.height(HazloSpaces.unit))
        }
    }
    }
}

@Composable
private fun NightCard(night: SleepNight) {
    var expanded by remember { mutableStateOf(false) }

    LeafCard(
        modifier = Modifier.fillMaxWidth().padding(horizontal = HazloSpaces.gutter),
        containerColor = Color.Black.copy(alpha = 0.85f),
    ) {
        Column(modifier = Modifier.padding(HazloSpaces.sm)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        formatWakeDate(night.nightKey),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                    Text(
                        "${formatDuration(night.totalDurationMillis)} · ${(night.efficiency * 100).toInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.7f),
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        qualityLabel(night.efficiency, night.sessions.size),
                        style = MaterialTheme.typography.labelSmall,
                        color = PillarSleep,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = Color.White.copy(alpha = 0.6f),
                    )
                }
            }

            if (expanded && night.sessions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(HazloSpaces.xs))
                night.sessions.forEach { session ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val emoji = phaseEmoji(session.phase.label)
                            if (emoji != null) {
                                Text(emoji, fontSize = 14.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text(
                                "${formatClockTime(session.startTime)} → ${formatClockTime(session.endTime)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.7f),
                            )
                        }
                        Text(
                            "${session.duration / 60_000}m",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TrendItem(label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.AutoMirrored.Filled.TrendingUp, null, tint = Color.White, modifier = Modifier.size(22.dp))
        Text(label, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PillarSleep)
        Text(stringResource(Res.string.history_trend_label), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
    }
}

@Composable
private fun MetricItem(icon: ImageVector, label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(22.dp))
        Text(value, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
    }
}

private fun formatDuration(millis: Long): String {
    val totalMinutes = millis / MS_PER_MIN
    val hours = totalMinutes / MINS_PER_HOUR
    val minutes = totalMinutes % MINS_PER_HOUR
    return "${hours}h ${minutes}m"
}

@Composable
private fun qualityLabel(efficiency: Float, segments: Int): String = when {
    efficiency >= EXCELLENT_MIN && segments <= MAX_SEGMENTS_EXCELLENT ->
        stringResource(Res.string.sleep_quality_excellent_short)
    efficiency >= GOOD_MIN ->
        stringResource(Res.string.sleep_quality_good_short)
    efficiency >= INTERRUPTED_MIN ->
        stringResource(Res.string.sleep_quality_interrupted_short)
    efficiency > 0f ->
        stringResource(Res.string.sleep_quality_poor_short)
    else ->
        stringResource(Res.string.sleep_quality_no_data_short)
}

private const val MS_PER_MIN = 60_000L
private const val MINS_PER_HOUR = 60L
private const val EXCELLENT_MIN = 0.90f
private const val GOOD_MIN = 0.80f
private const val INTERRUPTED_MIN = 0.65f
private const val MAX_SEGMENTS_EXCELLENT = 2
private const val NO_DATA = "Sin datos"

private fun phaseEmoji(phaseLabel: String): String? = when (phaseLabel) {
    "Sueño ligero" -> "🌙"
    "Sueño profundo" -> "💤"
    "REM" -> "🧠"
    "Despierto" -> "👁️"
    "Dormido" -> "😴"
    else -> null
}

@Composable
private fun encouragementText(history: SleepHistory): String {
    val nights = history.nights
    if (nights.isEmpty()) return stringResource(Res.string.encouragement_waiting)

    val lastNight = nights.first()
    val prevNight = nights.getOrNull(1)

    val improved = prevNight != null && lastNight.efficiency > prevNight.efficiency + 0.05f
    val worsened = prevNight != null && lastNight.efficiency < prevNight.efficiency - 0.05f
    val lastWasBad = lastNight.efficiency < 0.75f && lastNight.sessions.size >= 3

    return when {
        lastWasBad && improved -> stringResource(Res.string.encouragement_bad_improving)
        lastWasBad -> stringResource(Res.string.encouragement_bad_today)
        improved -> stringResource(Res.string.encouragement_improving)
        worsened -> stringResource(Res.string.encouragement_worsening)
        history.averageEfficiency >= 0.85f && history.nights.size >= 4 ->
            stringResource(Res.string.encouragement_excellent_week)
        history.averageEfficiency >= 0.80f -> stringResource(Res.string.encouragement_good_path)
        history.sleepDebtMinutes > 120 -> stringResource(Res.string.encouragement_debt)
        else -> stringResource(Res.string.encouragement_default)
    }
}

private fun sleepBackgroundUrl(history: SleepHistory): String? {
    if (history.nights.isEmpty()) return null
    val eff = history.averageEfficiency
    val trending = history.trendLabel.contains("Mejorando")
    val declining = history.trendLabel.contains("Empeorando")

    return when {
        eff >= 0.85f && trending ->
            "https://images.unsplash.com/photo-1728727267814-792db55ce678?w=800&q=80"
        eff >= 0.85f ->
            "https://images.unsplash.com/photo-1539336065911-c70206ee7aa5?w=800&q=80"
        eff >= 0.75f && trending ->
            "https://images.unsplash.com/photo-1767884022240-b91fb555495a?w=800&q=80"
        eff >= 0.75f ->
            "https://images.unsplash.com/photo-1615401796822-dedbfc0a4744?w=800&q=80"
        declining ->
            "https://images.unsplash.com/photo-1758600588872-3d340e69650f?w=800&q=80"
        trending ->
            "https://images.unsplash.com/photo-1627361358783-164528683cfe?w=800&q=80"
        eff >= 0.60f ->
            "https://images.unsplash.com/photo-1497491908353-c2624b242ecf?w=800&q=80"
        else ->
            "https://images.unsplash.com/photo-1583330618332-d1cc2fab8936?w=800&q=80"
    }
}
