package com.hazlosano.core.ui.components.atomic

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale

@Composable
fun AsyncImageBackground(
    imageUrl: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    overlayColor: Color = Color.Black.copy(alpha = 0.4f),
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier = modifier) {
        HazloAsyncImage(
            model = imageUrl,
            contentDescription = contentDescription,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        Box(modifier = Modifier.fillMaxSize().background(overlayColor))
        content()
    }
}
