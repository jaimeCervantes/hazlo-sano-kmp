package com.hazlosano.core.ui.components.atomic

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import com.hazlosano.core.ui.theme.HazloShapes

/**
 * Un bloque que ocupa el sitio de algo que todavía no ha llegado.
 *
 * No sabe qué está esperando: recibe su tamaño y su forma por el `modifier`, igual que cualquier
 * caja. Eso es lo que le permite vivir en `atomic/` y servir para una tarjeta, un título o un
 * buscador sin conocer ninguno de los tres.
 *
 * Late en lugar de quedarse quieto porque un rectángulo gris inmóvil se lee como un hueco roto; el
 * pulso es lo que dice "esto viene en camino".
 */
@Composable
fun HazloSkeleton(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(HazloShapes.control),
) {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(
        initialValue = RESTING_ALPHA,
        targetValue = PEAK_ALPHA,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = PULSE_MILLIS, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "skeleton_alpha",
    )

    Box(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)),
    )
}

private const val RESTING_ALPHA = 0.06f
private const val PEAK_ALPHA = 0.16f
private const val PULSE_MILLIS = 900
