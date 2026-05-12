package com.hazlosano.kmp.core.ui.components.atomic

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hazlosano.kmp.core.ui.theme.HazloSpaces
import com.hazlosano.kmp.core.ui.theme.PillarSleep
import com.hazlosano.kmp.core.ui.util.formatClockTime
import com.hazlosano.kmp.core.ui.util.formatWakeDate
import com.hazlosano.kmp.domain.model.SleepNight

@Composable
fun NightCard(night: SleepNight) {
    var expanded by remember { mutableStateOf(false) }

    LeafCard(
        modifier = Modifier.fillMaxWidth().padding(horizontal = HazloSpaces.gutter),
        containerColor = Color.Black.copy(alpha = CARD_ALPHA),
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
                        color = Color.White.copy(alpha = LABEL_ALPHA),
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
                        tint = Color.White.copy(alpha = ICON_ALPHA),
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
                            phaseEmoji(session.phase.label)?.let { emoji ->
                                Text(emoji, fontSize = 14.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text(
                                "${formatClockTime(session.startTime)} → ${formatClockTime(session.endTime)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = LABEL_ALPHA),
                            )
                        }
                        Text(
                            formatDuration(session.duration),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = LABEL_ALPHA),
                        )
                    }
                }
            }
        }
    }
}

private fun phaseEmoji(phaseLabel: String): String? = when (phaseLabel) {
    "Sueño ligero" -> "🌙"
    "Sueño profundo" -> "💤"
    "REM" -> "🧠"
    "Despierto" -> "👁️"
    "Dormido" -> "😴"
    else -> null
}

private fun qualityLabel(efficiency: Float, segments: Int): String = when {
    efficiency >= EXCELLENT_MIN && segments <= MAX_SEGMENTS_EXCELLENT -> "🌟 Excelente"
    efficiency >= GOOD_MIN -> "😊 Bueno"
    efficiency >= INTERRUPTED_MIN -> "😕 Interrumpido"
    efficiency > 0f -> "😫 Deficiente"
    else -> "Sin datos"
}

private fun formatDuration(millis: Long): String {
    val totalMinutes = millis / MS_PER_MIN
    val hours = totalMinutes / MINS_PER_HOUR
    val minutes = totalMinutes % MINS_PER_HOUR
    return "${hours}h ${minutes}m"
}

private const val CARD_ALPHA = 0.25f
private const val LABEL_ALPHA = 0.7f
private const val ICON_ALPHA = 0.6f
private const val MS_PER_MIN = 60_000L
private const val MINS_PER_HOUR = 60L
private const val EXCELLENT_MIN = 0.90f
private const val GOOD_MIN = 0.80f
private const val INTERRUPTED_MIN = 0.65f
private const val MAX_SEGMENTS_EXCELLENT = 2
