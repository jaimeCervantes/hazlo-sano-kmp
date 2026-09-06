package com.hazlosano.feature.sleep.ui

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
import com.hazlosano.core.ui.components.atomic.LeafCard
import com.hazlosano.core.ui.components.atomic.NightCard
import com.hazlosano.core.ui.theme.HazloSpaces
import com.hazlosano.core.ui.model.palette
import com.hazlosano.core.ui.model.pillarInkOnImage
import com.hazlosano.domain.model.PillarType
import com.hazlosano.domain.model.SleepHistory
import com.hazlosano.feature.sleep.presentation.SleepHistoryUiState
import com.hazlosano.feature.sleep.presentation.SleepHistoryViewModel
import hazlosano.app.shared.generated.resources.Res
import hazlosano.app.shared.generated.resources.encouragement_bad_improving
import hazlosano.app.shared.generated.resources.encouragement_bad_today
import hazlosano.app.shared.generated.resources.encouragement_debt
import hazlosano.app.shared.generated.resources.encouragement_default
import hazlosano.app.shared.generated.resources.encouragement_excellent_week
import hazlosano.app.shared.generated.resources.encouragement_good_path
import hazlosano.app.shared.generated.resources.encouragement_improving
import hazlosano.app.shared.generated.resources.encouragement_waiting
import hazlosano.app.shared.generated.resources.encouragement_worsening
import hazlosano.app.shared.generated.resources.history_avg_label
import hazlosano.app.shared.generated.resources.history_consistency_label
import hazlosano.app.shared.generated.resources.history_debt_label
import hazlosano.app.shared.generated.resources.history_trend_label
import hazlosano.app.shared.generated.resources.history_window_label
import hazlosano.app.shared.generated.resources.sleep_efficiency_label
import org.jetbrains.compose.resources.stringResource

@Composable
fun SleepHistoryScreen(viewModel: SleepHistoryViewModel) {
    val state by viewModel.state.collectAsState()

    when (val s = state) {
        is SleepHistoryUiState.Loading -> Box(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center,
        ) { CircularProgressIndicator(color = PillarType.SLEEP.palette().ink) }

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
                    modifier = Modifier.size(36.dp).clip(CircleShape).background(PillarType.SLEEP.palette().solid),
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
        Text(label, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = pillarInkOnImage(PillarType.SLEEP))
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
        eff >= 0.95f -> BG_PERFECT_OR_ALMOST
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
private const val BG_PERFECT_OR_ALMOST = "https://images.unsplash.com/photo-1608825252979-9b8dbef29c50?q=60&w=800"
private const val BG_IMPROVING_EXCELLENT = "https://images.unsplash.com/photo-1612181750970-b6c803178d80?w=800&q=60"
private const val BG_EXCELLENT = "https://images.unsplash.com/photo-1612294355484-3f5a3249d65d?q=60&w=800"
private const val BG_IMPROVING_GOOD = "https://plus.unsplash.com/premium_photo-1723709090773-0a9566c25afc?q=60&w=800"
private const val BG_GOOD = "https://images.unsplash.com/photo-1767884022240-b91fb555495a?w=800&q=60"
private const val BG_DECLINING = "https://images.unsplash.com/photo-1758600588872-3d340e69650f?w=800&q=60"
private const val BG_IMPROVING = "https://images.unsplash.com/photo-1719471497337-d140858e6ba7?q=60&w=800"
private const val BG_INTERRUPTED = "https://images.unsplash.com/photo-1712861712153-80a00199c83f?q=60&w=800"
private const val BG_POOR = "https://images.unsplash.com/photo-1694728598381-b31a9b129d75?q=60&w=800"
