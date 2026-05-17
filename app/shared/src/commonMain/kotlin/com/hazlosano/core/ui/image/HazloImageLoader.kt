package com.hazlosano.core.ui.image

import androidx.compose.runtime.Composable
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.compose.LocalPlatformContext
import coil3.network.ktor3.KtorNetworkFetcherFactory

@Composable
fun HazloImageLoader(content: @Composable () -> Unit) {
    val context = LocalPlatformContext.current
    SingletonImageLoader.setSafe {
        ImageLoader.Builder(context)
            .components { add(KtorNetworkFetcherFactory()) }
            .build()
    }
    content()
}
