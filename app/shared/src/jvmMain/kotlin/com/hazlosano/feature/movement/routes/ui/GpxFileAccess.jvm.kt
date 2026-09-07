package com.hazlosano.feature.movement.routes.ui

import androidx.compose.runtime.Composable

/**
 * Desktop has no file access here yet. Recording is Android-only for now, so a desktop build has no
 * routes of its own to export and nothing to import them into; the route screen hides both actions
 * rather than offering a button that does nothing.
 */

@Composable
actual fun rememberGpxPicker(
    onPicked: (fileName: String, bytes: ByteArray) -> Unit,
    onAbandoned: () -> Unit,
): () -> Unit = {}

@Composable
actual fun rememberGpxSaver(): (fileName: String, gpx: String) -> Unit = { _, _ -> }

actual val gpxFileAccessAvailable: Boolean = false
