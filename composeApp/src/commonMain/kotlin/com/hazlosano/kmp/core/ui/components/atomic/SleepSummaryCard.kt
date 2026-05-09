package com.hazlosano.kmp.core.ui.components.atomic

import androidx.compose.foundation.background
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
import androidx.compose.material3.Icon
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
import com.hazlosano.kmp.domain.model.SleepAnalysis

@Composable
fun SleepSummaryCard(
    analysis: SleepAnalysis,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    LeafCard(modifier = modifier.fillMaxWidth()) {
        Box {
            Column(modifier = Modifier.padding(HazloSpaces.md)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.12f)),
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
                        text = "Resumen de sueño",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
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

                Spacer(modifier = Modifier.height(HazloSpaces.sm))

                Text(
                    text = sleepQualityLabel(analysis.efficiency),
                    style = MaterialTheme.typography.labelLarge,
                    color = accentColor,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Icon(
                imageVector = Icons.Filled.Bedtime,
                contentDescription = null,
                tint = accentColor.copy(alpha = 0.08f),
                modifier = Modifier
                    .size(120.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 20.dp, y = (-20).dp),
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
            color = accentColor,
            fontSize = 28.sp,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun formatDuration(millis: Long): String {
    val totalMinutes = millis / 60_000
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return "${hours}h ${minutes}m"
}

private fun sleepQualityLabel(efficiency: Float): String = when {
    efficiency >= 0.85f -> "Excelente descanso"
    efficiency >= 0.70f -> "Buen descanso"
    efficiency >= 0.50f -> "Descanso regular"
    efficiency > 0f -> "Descanso insuficiente"
    else -> "Sin datos de sueño"
}
