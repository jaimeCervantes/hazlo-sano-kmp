package com.hazlosano.feature.catalog.ui

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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hazlosano.core.ui.components.atomic.LeafCard
import com.hazlosano.core.ui.theme.HazloSpaces

/**
 * El encabezado del tablero de un pilar.
 *
 * Es deliberadamente el mismo objeto visual que `SleepSummaryCard` —`LeafCard` sobre un velo negro,
 * insignia circular con el color del pilar, cifras grandes y la marca de agua enorme en la esquina—
 * porque la queja era justo esa: que tres pestañas no se parecían a la que sí estaba bien. Copiar
 * el lenguaje es el objetivo, no un atajo.
 *
 * A diferencia de aquella, **no conoce ningún tipo del dominio**: recibe cifras y textos ya
 * resueltos. `SleepSummaryCard` toma un `SleepAnalysis` y por eso `AGENTS.md` la señala como deuda;
 * esto no repite el error.
 */
/** Etiquetas de prueba de la tarjeta de resumen. */
object PillarSummaryCardTags {
    const val INFO: String = "pillar_summary_info"
}

@Composable
fun PillarSummaryCard(
    title: String,
    metrics: List<PillarMetric>,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    refreshContentDescription: String? = null,
    onRefresh: (() -> Unit)? = null,
    infoContentDescription: String? = null,
    onInfo: (() -> Unit)? = null,
) {
    val darkOverlay = Color.Black.copy(alpha = 0.6f)

    LeafCard(modifier = modifier.fillMaxWidth(), containerColor = darkOverlay) {
        Box(modifier = Modifier.background(darkOverlay)) {
            Column(modifier = Modifier.padding(HazloSpaces.md)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = BADGE_ALPHA)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(HazloSpaces.sm))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.weight(1f),
                    )
                    // Qué es este pilar va junto a actualizar, y no escondido en el título: es la
                    // pregunta que se hace quien abre la pestaña por primera vez.
                    if (onInfo != null) {
                        IconButton(
                            onClick = onInfo,
                            modifier = Modifier.size(32.dp).testTag(PillarSummaryCardTags.INFO),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = infoContentDescription,
                                tint = accentColor,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                    if (onRefresh != null) {
                        IconButton(onClick = onRefresh, modifier = Modifier.size(32.dp)) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = refreshContentDescription,
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
                    metrics.forEach { metric ->
                        PillarMetricColumn(metric)
                    }
                }
            }

            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor.copy(alpha = WATERMARK_ALPHA),
                modifier = Modifier
                    .size(130.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 25.dp, y = (-25).dp),
            )
        }
    }
}

/** Una cifra del resumen: el número y qué es. Ya redactado por quien lo pinta. */
data class PillarMetric(val value: String, val label: String)

@Composable
private fun PillarMetricColumn(metric: PillarMetric, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = metric.value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            fontSize = 28.sp,
        )
        Text(
            text = metric.label,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = LABEL_ALPHA),
        )
    }
}

private const val BADGE_ALPHA = 0.25f
private const val WATERMARK_ALPHA = 0.10f
private const val LABEL_ALPHA = 0.7f
