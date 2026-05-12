package com.hazlosano.kmp.feature.sleep.ui

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CrisisAlert
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.hazlosano.kmp.core.ui.components.atomic.NightCard
import com.hazlosano.kmp.core.ui.theme.HazloSpaces
import com.hazlosano.kmp.core.ui.theme.PillarSleep
import com.hazlosano.kmp.domain.model.SleepHistory
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
import kmp.composeapp.generated.resources.history_consistency_label
import kmp.composeapp.generated.resources.history_debt_label
import kmp.composeapp.generated.resources.history_trend_label
import kmp.composeapp.generated.resources.history_window_label
import kmp.composeapp.generated.resources.sleep_efficiency_label
import org.jetbrains.compose.resources.stringResource

@Composable
fun SleepHistoryScreen(viewModel: SleepHistoryViewModel) {
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
    Box(modifier = Modifier.fillMaxSize()) {
        if (sleepBackgroundUrl(history) != null) {
            AsyncImage(
                model = sleepBackgroundUrl(history),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    colors = listOf(Color.Black.copy(alpha = 0.85f), Color.Transparent),
                ),
            ),
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = HazloSpaces.md, bottom = HazloSpaces.xl),
        ) {
            item { MetricsCard(history) }
            item { Spacer(modifier = Modifier.height(HazloSpaces.md)) }
            items(history.nights) { night ->
                NightCard(night)
                Spacer(modifier = Modifier.height(HazloSpaces.unit))
            }
        }
    }
}

@Composable
private fun MetricsCard(history: SleepHistory) {
    LeafCard(
        modifier = Modifier.fillMaxWidth().padding(horizontal = HazloSpaces.gutter),
        containerColor = Color.Black.copy(alpha = 0.35f),
    ) {
        Column(modifier = Modifier.padding(HazloSpaces.md)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(36.dp).clip(CircleShape).background(PillarSleep),
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                Metric(Icons.Filled.Schedule, stringResource(Res.string.history_avg_label),
                    "${history.averageHours.toInt()}h ${((history.averageHours % 1) * 60).toInt()}m")
                Metric(Icons.Filled.Bolt, stringResource(Res.string.sleep_efficiency_label),
                    "${(history.averageEfficiency * 100).toInt()}%")
                TrendMetric(history.trendLabel)
            }
            Spacer(modifier = Modifier.height(HazloSpaces.sm))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                Metric(Icons.Filled.CrisisAlert, stringResource(Res.string.history_consistency_label),
                    "±${history.consistencyMinutes}m")
                Metric(Icons.Filled.HourglassBottom, stringResource(Res.string.history_debt_label),
                    "${history.sleepDebtMinutes / 60}h ${history.sleepDebtMinutes % 60}m")
                Metric(Icons.Filled.Bedtime, stringResource(Res.string.history_window_label),
                    "${history.sleepWindowHours.toInt()}h ${((history.sleepWindowHours % 1) * 60).toInt()}m")
            }
        }
    }
}

@Composable
private fun Metric(icon: ImageVector, label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(22.dp))
        Text(value, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
    }
}

@Composable
private fun TrendMetric(label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.AutoMirrored.Filled.TrendingUp, null, tint = Color.White, modifier = Modifier.size(22.dp))
        Text(label, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PillarSleep)
        Text(stringResource(Res.string.history_trend_label), style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.6f))
    }
}

@Composable
private fun encouragementText(history: SleepHistory): String {
    val nights = history.nights
    if (nights.isEmpty()) return stringResource(Res.string.encouragement_waiting)
    val last = nights.first()
    val prev = nights.getOrNull(1)
    val improved = prev != null && last.efficiency > prev.efficiency + ENC_DELTA
    val worsened = prev != null && last.efficiency < prev.efficiency - ENC_DELTA
    val lastBad = last.efficiency < ENC_BAD_THRESHOLD && last.sessions.size >= ENC_BAD_SEGMENTS

    return when {
        lastBad && improved -> stringResource(Res.string.encouragement_bad_improving)
        lastBad -> stringResource(Res.string.encouragement_bad_today)
        improved -> stringResource(Res.string.encouragement_improving)
        worsened -> stringResource(Res.string.encouragement_worsening)
        history.averageEfficiency >= 0.85f && nights.size >= 4 ->
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
        eff >= 0.85f && trending -> BG_IMPROVING_EXCELLENT
        eff >= 0.85f -> BG_EXCELLENT
        eff >= 0.75f && trending -> BG_IMPROVING_GOOD
        eff >= 0.75f -> BG_GOOD
        declining -> BG_DECLINING
        trending -> BG_IMPROVING
        eff >= 0.60f -> BG_INTERRUPTED
        else -> BG_POOR
    }
}

private const val ENC_DELTA = 0.05f
private const val ENC_BAD_THRESHOLD = 0.75f
private const val ENC_BAD_SEGMENTS = 3
private const val BG_IMPROVING_EXCELLENT = "https://images.unsplash.com/photo-1728727267814-792db55ce678?w=800&q=80"
private const val BG_EXCELLENT = "https://images.unsplash.com/photo-1539336065911-c70206ee7aa5?w=800&q=80"
private const val BG_IMPROVING_GOOD = "https://images.unsplash.com/photo-1767884022240-b91fb555495a?w=800&q=80"
private const val BG_GOOD = "https://images.unsplash.com/photo-1615401796822-dedbfc0a4744?w=800&q=80"
private const val BG_DECLINING = "https://images.unsplash.com/photo-1758600588872-3d340e69650f?w=800&q=80"
private const val BG_IMPROVING = "https://images.unsplash.com/photo-1627361358783-164528683cfe?w=800&q=80"
private const val BG_INTERRUPTED = "https://images.unsplash.com/photo-1497491908353-c2624b242ecf?w=800&q=80"
private const val BG_POOR = "https://images.unsplash.com/photo-1583330618332-d1cc2fab8936?w=800&q=80"
