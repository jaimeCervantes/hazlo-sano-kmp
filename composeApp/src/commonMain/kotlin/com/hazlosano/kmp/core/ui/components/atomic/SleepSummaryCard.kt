package com.hazlosano.kmp.core.ui.components.atomic

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hazlosano.kmp.core.ui.theme.HazloSpaces
import com.hazlosano.kmp.core.ui.util.formatClockTime
import com.hazlosano.kmp.domain.model.SleepAnalysis
import com.hazlosano.kmp.domain.model.SleepPhase

@Composable
fun SleepSummaryCard(
    analysis: SleepAnalysis,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onRefresh: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val darkOverlay = Color.Black.copy(alpha = 0.6f)

    LeafCard(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        containerColor = darkOverlay,
    ) {
        Box(modifier = Modifier.background(darkOverlay)) {
            Column(modifier = Modifier.padding(HazloSpaces.md)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Bedtime,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(HazloSpaces.sm))
                    Text(
                        text = Última noche",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.weight(1f),
                    )
                    if (onRefresh != null) {
                        IconButton(onClick = onRefresh, modifier = Modifier.size(32.dp)) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = "Actualizar",
                                tint = accentColor,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(HazloSpaces.md))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    SummaryMetric(
                        value = formatDuration(analysis.totalDurationMillis),
                        label = "Dormido",
                        accentColor = accentColor,
                    )
                    SummaryMetric(
                        value = "${(analysis.efficiency * 100).toInt()}%",
                        label = "Eficiencia",
                        accentColor = accentColor,
                    )
                    SummaryMetric(
                        value = analysis.totalSessions.toString(),
                        label = "Segmentos",
                        accentColor = accentColor,
                    )
                }

                val sleepStart = analysis.firstSleepStart
                val sleepEnd = analysis.lastSleepEnd
                if (sleepStart != null && sleepEnd != null) {
                    Spacer(modifier = Modifier.height(HazloSpaces.sm))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        TimeLabel("Te dormiste", sleepStart, accentColor)
                        TimeLabel("Despertaste", sleepEnd, accentColor)
                    }
                }

                Spacer(modifier = Modifier.height(HazloSpaces.sm))

                Text(
                    text = sleepQualityLabel(analysis.efficiency, analysis.totalSessions),
                    style = MaterialTheme.typography.labelLarge,
                    color = accentColor,
                    fontWeight = FontWeight.SemiBold,
                )

                if (analysis.averageConfidence < 1.0f) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Precisión: ${(analysis.averageConfidence * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.5f),
                    )
                }

                val phases = analysis.phaseBreakdown
                if (phases.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(HazloSpaces.sm))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        PhaseLabel(SleepPhase.LIGHT, phases[SleepPhase.LIGHT], accentColor)
                        PhaseLabel(SleepPhase.DEEP, phases[SleepPhase.DEEP], accentColor)
                        PhaseLabel(SleepPhase.REM, phases[SleepPhase.REM], accentColor)
                    }
                }
            }

            Icon(
                imageVector = Icons.Filled.Bedtime,
                contentDescription = null,
                tint = accentColor.copy(alpha = 0.10f),
                modifier = Modifier
                    .size(130.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 25.dp, y = (-25).dp),
            )
        }
    }
}

@Composable
private fun SummaryMetric(
    value: String,
    label: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            fontSize = 28.sp,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.7f),
        )
    }
}

@Composable
private fun PhaseLabel(phase: SleepPhase, duration: Long?, accentColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = phase.label,
            style = MaterialTheme.typography.labelSmall,
            color = accentColor.copy(alpha = 0.9f),
        )
        Text(
            text = if (duration != null) formatDuration(duration) else "—",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
    }
}

private fun formatDuration(millis: Long): String {
    val totalMinutes = millis / 60_000
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return "${hours}h ${minutes}m"
}

@Composable
private fun TimeLabel(label: String, epochMillis: Long, accentColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = formatClockTime(epochMillis),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.7f),
        )
    }
}

private fun sleepQualityLabel(efficiency: Float, segments: Int): String = when {
    efficiency >= 0.90f && segments <= 2 -> "Excelente descanso"
    efficiency >= 0.80f -> "Buen descanso"
    efficiency >= 0.65f -> "Sueño interrumpido"
    efficiency > 0f -> "Descanso deficiente"
    else -> "Aún sin datos"
}
