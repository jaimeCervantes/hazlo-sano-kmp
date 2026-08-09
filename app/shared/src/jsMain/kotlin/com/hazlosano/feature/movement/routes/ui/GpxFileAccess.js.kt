package com.hazlosano.feature.movement.routes.ui

import androidx.compose.runtime.Composable

/**
 * The browser has no database initialized in this app, so it has no routes to import into or export
 * from. The route screen hides both actions rather than offering a button that does nothing.
 */

@Composable
actual fun rememberGpxPicker(
    onPicked: (fileName: String, bytes: ByteArray) -> Unit,
): () -> Unit = {}

@Composable
actual fun rememberGpxSaver(): (fileName: String, gpx: String) -> Unit = { _, _ -> }

actual val gpxFileAccessAvailable: Boolean = false
