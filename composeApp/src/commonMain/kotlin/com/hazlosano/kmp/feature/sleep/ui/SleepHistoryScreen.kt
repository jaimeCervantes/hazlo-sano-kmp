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

@Composable
fun SleepHistoryScreen(
    viewModel: SleepHistoryViewModel,
    onBack: () -> Unit,
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

        is SleepHistoryUiState.Success -> HistoryContent(s.history, onBack)
    }
}

@Composable
private fun HistoryContent(history: SleepHistory, onBack: () -> Unit) {
    val bgUrl = sleepBackgroundUrl(history)
    Box(modifier = Modifier.fillMaxSize()) {
        // background image
        if (bgUrl != null) {
            AsyncImage(
                model = bgUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
        // dark overlay for readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.99f),
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
        // Header with back button + big moon
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = HazloSpaces.gutter),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(HazloSpaces.sm))
                Box(
                    modifier = Modifier.size(44.dp).clip(CircleShape)
                        .background(PillarSleep),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Bedtime, null, tint = Color.White, modifier = Modifier.size(28.dp))
                }
                Spacer(modifier = Modifier.width(HazloSpaces.sm))
                Column {
                    Text("Tu sueño", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(
                        encouragementText(history),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.8f),
                    )
                }
            }
            Spacer(modifier = Modifier.height(HazloSpaces.md))
        }

        // Summary metrics card
        item {
            LeafCard(
                modifier = Modifier.fillMaxWidth().padding(horizontal = HazloSpaces.gutter),
                containerColor = Color.Black.copy(alpha = 0.85f),
            ) {
                Column(modifier = Modifier.padding(HazloSpaces.md)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        MetricItem(Icons.Filled.Schedule, "Promedio", "${history.averageHours.toInt()}h ${((history.averageHours % 1) * 60).toInt()}m")
                        MetricItem(Icons.AutoMirrored.Filled.TrendingUp, "Eficiencia", "${(history.averageEfficiency * 100).toInt()}%")
                    }
                    Spacer(modifier = Modifier.height(HazloSpaces.sm))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        MetricItem(Icons.Filled.CrisisAlert, "Consistencia", "±${history.consistencyMinutes}m")
                        MetricItem(Icons.Filled.HourglassBottom, "Deuda sueño", "${history.sleepDebtMinutes / 60}h ${history.sleepDebtMinutes % 60}m")
                    }
                    Spacer(modifier = Modifier.height(HazloSpaces.sm))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Filled.TrendingUp, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "Tendencia: ${history.trendLabel}",
                            style = MaterialTheme.typography.labelLarge,
                            color = PillarSleep,
                            fontWeight = FontWeight.SemiBold,
                        )
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
private fun MetricItem(icon: ImageVector, label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(22.dp))
        Text(value, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
    }
}

private fun formatDuration(millis: Long): String {
    val totalMinutes = millis / 60_000
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return "${hours}h ${minutes}m"
}

private fun qualityLabel(efficiency: Float, segments: Int): String = when {
    efficiency >= 0.90f && segments <= 2 -> "🌟 Excelente"
    efficiency >= 0.80f -> "😊 Bueno"
    efficiency >= 0.65f -> "😕 Interrumpido"
    efficiency > 0f -> "😫 Deficiente"
    else -> "Sin datos"
}

private fun phaseEmoji(phaseLabel: String): String? = when (phaseLabel) {
    "Sueño ligero" -> "🌙"
    "Sueño profundo" -> "💤"
    "REM" -> "🧠"
    "Despierto" -> "👁️"
    "Dormido" -> "😴"
    else -> null
}

private fun encouragementText(history: SleepHistory): String {
    val nights = history.nights
    if (nights.isEmpty()) return "Esperando tus primeras noches"

    val lastNight = nights.first()
    val prevNight = nights.getOrNull(1)

    val improved = prevNight != null && lastNight.efficiency > prevNight.efficiency + 0.05f
    val worsened = prevNight != null && lastNight.efficiency < prevNight.efficiency - 0.05f
    val lastWasBad = lastNight.efficiency < 0.75f && lastNight.sessions.size >= 3

    return when {
        lastWasBad && improved -> "Anoche fue difícil, pero vas mejorando"
        lastWasBad -> "Tu sueño fue interrumpido, hoy intenta descansar"
        improved -> "Cada noche mejor, sigue así"
        worsened -> "Anoche descansaste menos, hoy recupera"
        history.averageEfficiency >= 0.85f && history.nights.size >= 4 -> "Semana excelente de descanso"
        history.averageEfficiency >= 0.80f -> "Vas por buen camino"
        history.sleepDebtMinutes > 120 -> "Llevas varias noches con déficit"
        else -> "La constancia construye el descanso"
    }
}

private fun sleepBackgroundUrl(history: SleepHistory): String? {
    if (history.nights.isEmpty()) return null
    return when {
        history.averageEfficiency >= 0.85f ->
            "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=800&q=80"
        history.averageEfficiency >= 0.75f ->
            "https://images.unsplash.com/photo-1540518614846-7eded433c457?w=800&q=80"
        history.averageEfficiency >= 0.60f ->
            "https://images.unsplash.com/photo-1532693322450-2cb5c511067d?w=800&q=80"
        else ->
            "https://images.unsplash.com/photo-1507400492013-162706c8c05e?w=800&q=80"
    }
}
