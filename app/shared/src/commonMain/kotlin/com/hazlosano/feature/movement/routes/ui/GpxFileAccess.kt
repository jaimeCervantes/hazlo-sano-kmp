package com.hazlosano.feature.movement.routes.ui

import androidx.compose.runtime.Composable

/**
 * Opening a GPX file and writing one back out, which no target does the same way: Android goes
 * through the Storage Access Framework, and the others have their own or none at all.
 *
 * Kept as small as it can be — hand me the bytes, take these bytes — so the route screens hold no
 * platform knowledge and can be read on any target.
 */

/**
 * @return a callback that asks the user for a GPX file; [onPicked] receives its bytes.
 *
 * [onAbandoned] corre cuando no va a llegar ningun archivo: se cerro el selector sin elegir, o lo
 * elegido no se pudo leer. Hace falta porque la pantalla empieza a esperar **al pedir** el archivo,
 * y sin esta señal cancelar la dejaria esperando para siempre.
 */
@Composable
expect fun rememberGpxPicker(
    onPicked: (fileName: String, bytes: ByteArray) -> Unit,
    onAbandoned: () -> Unit,
): () -> Unit

/** @return a callback that asks the user where to write a GPX file and writes it there. */
@Composable
expect fun rememberGpxSaver(): (fileName: String, gpx: String) -> Unit

/** Whether this target can open and write files at all; the screens hide the buttons when not. */
expect val gpxFileAccessAvailable: Boolean
