package com.hazlosano.core.ui.components.atomic

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage

@Composable
fun HazloAsyncImage(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    alpha: Float = 1f,
    colorFilter: ColorFilter? = null,
) {
    val resolvedModel = remember(model) { resolveHazloAsyncImageModel(model) }

    if (resolvedModel != null) {
        AsyncImage(
            model = resolvedModel,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale,
            alpha = alpha,
            colorFilter = colorFilter,
        )
    }
}

internal fun resolveHazloAsyncImageModel(model: Any?): Any? {
    return when (model) {
        is String -> model.takeIf(String::isNotBlank)
        else -> model
    }
}
