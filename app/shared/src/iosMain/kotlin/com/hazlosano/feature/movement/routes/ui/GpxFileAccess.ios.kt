package com.hazlosano.feature.movement.routes.ui

import androidx.compose.runtime.Composable

/**
 * iOS has no file access here yet: it needs UIDocumentPickerViewController wired to the hosting
 * controller, which is a slice of its own. The route screen hides both actions rather than offering
 * a button that does nothing.
 */

@Composable
actual fun rememberGpxPicker(
    onPicked: (fileName: String, bytes: ByteArray) -> Unit,
): () -> Unit = {}

@Composable
actual fun rememberGpxSaver(): (fileName: String, gpx: String) -> Unit = { _, _ -> }

actual val gpxFileAccessAvailable: Boolean = false
