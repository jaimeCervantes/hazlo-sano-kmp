package com.hazlosano.feature.movement.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.hazlosano.feature.movement.presentation.TrackSilhouette

/**
 * La forma de un recorrido, dibujada.
 *
 * **Un `Canvas` y no un mapa.** Veinte `MovementMap` en una lista es una lista inusable, y además
 * cada uno pediría tiles —o sea red— justo en el pilar que promete funcionar sin ella. La silueta
 * basta para reconocer una salida entre otras, que es lo que la lista necesita.
 *
 * Quien no tiene silueta no dibuja nada: el hueco lo pone quien lo llama, que es quien sabe qué
 * decir en su sitio.
 */
@Composable
fun TrackSilhouetteView(
    silhouette: TrackSilhouette,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(SILHOUETTE_CORNER))
            .background(color.copy(alpha = BACKGROUND_ALPHA)),
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(SILHOUETTE_PADDING)) {
            val points = silhouette.points
            if (points.size < 2) return@Canvas

            val path = Path()
            points.forEachIndexed { index, point ->
                val offset = Offset(x = point.x * size.width, y = point.y * size.height)
                if (index == 0) path.moveTo(offset.x, offset.y) else path.lineTo(offset.x, offset.y)
            }

            drawPath(
                path = path,
                color = color,
                style = Stroke(width = STROKE_WIDTH.toPx()),
            )
        }
    }
}

private val SILHOUETTE_CORNER = 8.dp

/**
 * El trazo no puede tocar el borde: con la silueta encuadrada al milímetro, media anchura de línea
 * se sale del recuadro y se recorta.
 */
private val SILHOUETTE_PADDING = 6.dp
private val STROKE_WIDTH = 2.dp
private const val BACKGROUND_ALPHA = 0.10f

/** El hueco de una salida que no guardó su silueta. Ver `TrackSilhouetteView`. */
@Composable
fun TrackSilhouetteGap(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(SILHOUETTE_CORNER))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    )
}
