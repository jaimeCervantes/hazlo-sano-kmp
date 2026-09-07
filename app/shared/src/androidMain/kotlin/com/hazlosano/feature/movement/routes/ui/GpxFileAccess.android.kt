package com.hazlosano.feature.movement.routes.ui

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The Storage Access Framework, so importing and exporting need no storage permission at all: the
 * user picks the file themselves and the grant travels with the URI they chose.
 */

@Composable
actual fun rememberGpxPicker(
    onPicked: (fileName: String, bytes: ByteArray) -> Unit,
    onAbandoned: () -> Unit,
): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        // Cerrar el selector sin elegir llega como una URI nula. Es el caso normal —cambiar de
        // idea— y quien espera tiene que enterarse, o se queda esperando un archivo que nadie
        // mandó.
        if (uri == null) {
            onAbandoned()
            return@rememberLauncherForActivityResult
        }
        scope.launch {
            val bytes = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                }.getOrNull()
            }
            if (bytes != null) onPicked(context.displayNameOf(uri), bytes) else onAbandoned()
        }
    }
    // GPX is XML, and plenty of file providers report it as text/xml or as nothing at all.
    // Filtering strictly on application/gpx+xml hides the very files this exists to open, so the
    // filter is wide and the parser is what decides whether the file is a route.
    return {
        launcher.launch(arrayOf("application/gpx+xml", "application/xml", "text/xml", "*/*"))
    }
}

@Composable
actual fun rememberGpxSaver(): (fileName: String, gpx: String) -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // CreateDocument only carries a file name to the picker, so the content to write waits here
    // until the user has chosen where it goes.
    val pending = remember { mutableStateOfNullableString() }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/gpx+xml"),
    ) { uri: Uri? ->
        val gpx = pending.value
        pending.value = null
        if (uri == null || gpx == null) return@rememberLauncherForActivityResult
        scope.launch {
            withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openOutputStream(uri)?.use {
                        it.write(gpx.encodeToByteArray())
                    }
                }
            }
        }
    }
    return { fileName, gpx ->
        pending.value = gpx
        launcher.launch(fileName)
    }
}

actual val gpxFileAccessAvailable: Boolean = true

private fun mutableStateOfNullableString() =
    androidx.compose.runtime.mutableStateOf<String?>(null)

/** The name the picked file has for the user, so an imported route can be called after it. */
private fun Context.displayNameOf(uri: Uri): String {
    val fromProvider = runCatching {
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
        }
    }.getOrNull()
    return fromProvider ?: uri.lastPathSegment.orEmpty()
}
