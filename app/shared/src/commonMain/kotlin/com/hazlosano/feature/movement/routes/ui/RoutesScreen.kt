package com.hazlosano.feature.movement.routes.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hazlosano.core.ui.components.AppSettingsMenuItem
import com.hazlosano.core.ui.components.atomic.HazloTopAppBar
import com.hazlosano.core.ui.components.atomic.LeafCard
import com.hazlosano.core.ui.model.palette
import com.hazlosano.core.ui.theme.HazloSpaces
import com.hazlosano.domain.feature.movement.model.Route
import com.hazlosano.domain.model.PillarType
import com.hazlosano.feature.movement.presentation.MovementFormat
import com.hazlosano.feature.movement.presentation.silhouetteOf
import com.hazlosano.feature.movement.routes.presentation.ImportingRoute
import com.hazlosano.feature.movement.routes.presentation.RoutesViewModel
import com.hazlosano.feature.movement.ui.TrackSilhouetteGap
import com.hazlosano.feature.movement.ui.TrackSilhouetteView
import com.hazlosano.feature.movement.ui.text
import hazlosano.app.shared.generated.resources.Res
import hazlosano.app.shared.generated.resources.action_cancel
import hazlosano.app.shared.generated.resources.action_save
import hazlosano.app.shared.generated.resources.movement_metric_distance
import hazlosano.app.shared.generated.resources.movement_metric_elevation
import hazlosano.app.shared.generated.resources.routes_delete
import hazlosano.app.shared.generated.resources.routes_duplicate_body
import hazlosano.app.shared.generated.resources.routes_duplicate_keep
import hazlosano.app.shared.generated.resources.routes_duplicate_replace
import hazlosano.app.shared.generated.resources.routes_duplicate_title
import hazlosano.app.shared.generated.resources.routes_empty_with_files
import hazlosano.app.shared.generated.resources.routes_empty_without_files
import hazlosano.app.shared.generated.resources.routes_export
import hazlosano.app.shared.generated.resources.routes_import_gpx
import hazlosano.app.shared.generated.resources.routes_name_dialog_title
import hazlosano.app.shared.generated.resources.routes_name_label
import hazlosano.app.shared.generated.resources.routes_rename
import hazlosano.app.shared.generated.resources.routes_title
import hazlosano.app.shared.generated.resources.routes_importing
import hazlosano.app.shared.generated.resources.routes_importing_unnamed
import hazlosano.app.shared.generated.resources.top_app_bar_back
import hazlosano.app.shared.generated.resources.top_app_bar_menu
import org.jetbrains.compose.resources.stringResource

/** Etiquetas de prueba: la pantalla se afirma por aqui y no por su redaccion. */
object RoutesTags {
    const val EMPTY_IMPORT: String = "routes_empty_import"
    const val IMPORTING: String = "routes_importing"
    const val IMPORT: String = "routes_import"

    fun silhouette(routeId: Long): String = "route_silhouette_$routeId"
}

/**
 * The routes you can follow: imported from a GPX file, or kept from an outing you recorded.
 *
 * Import and export are hidden where the platform cannot open files rather than shown as buttons
 * that do nothing — recording is Android-only for now, so the other targets have no routes of their
 * own to write out either.
 */
@Composable
fun RoutesScreen(
    viewModel: RoutesViewModel,
    onBack: () -> Unit,
    onOpenRoute: (Long) -> Unit,
    onOpenSettings: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val routes by viewModel.allRoutes.collectAsState()
    val state by viewModel.uiState.collectAsState()
    var renaming by remember { mutableStateOf<Route?>(null) }

    val pickGpx = rememberGpxPicker(
        onPicked = { fileName, bytes -> viewModel.import(fileName, bytes) },
        onAbandoned = viewModel::importAbandoned,
    )
    val saveGpx = rememberGpxSaver()

    // La espera empieza al pedir el archivo y no al recibirlo: entre las dos cosas esta el selector
    // del sistema, que baja el archivo si esta en la nube y es la parte que de verdad se sufre.
    // Parsear y guardar 20.000 puntos son 378 ms; el selector puede ser segundos.
    val askForGpx = {
        viewModel.importRequested()
        pickGpx()
    }
    var deleting by remember { mutableStateOf<Route?>(null) }

    // The export is handed to the platform once and then cleared, so returning to this screen does
    // not open the file picker again for a route already written out.
    state.exported?.let { exported ->
        LaunchedEffect(exported) {
            saveGpx(exported.fileName, exported.gpx)
            viewModel.consumeExport()
        }
    }

    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        HazloTopAppBar(
            title = stringResource(Res.string.routes_title),
            showBackButton = true,
            onBackClick = onBack,
            backContentDescription = stringResource(Res.string.top_app_bar_back),
            menuContentDescription = stringResource(Res.string.top_app_bar_menu),
            menuContent = { dismiss ->
                AppSettingsMenuItem(
                    onClick = {
                        dismiss()
                        onOpenSettings()
                    },
                )
            },
        )

        state.message?.let { message ->
            Text(
                text = message.text(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = HazloSpaces.gutter, vertical = HazloSpaces.sm),
            )
        }

        // Sin rutas no se pinta: el estado vacio ya ofrece importar, y dos botones identicos a
        // dos pulgadas uno de otro no dan una opcion mas, dan una duda.
        if (gpxFileAccessAvailable && routes.isNotEmpty()) {
            Button(
                onClick = askForGpx,
                // Dos importaciones a la vez dejarian dos rutas de un archivo o una carrera por
                // cual gana. Apagado ademas dice, sin texto, que ya hay una en marcha.
                enabled = !state.isImporting,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = HazloSpaces.gutter, vertical = HazloSpaces.sm)
                    .testTag(RoutesTags.IMPORT),
            ) {
                Icon(Icons.Default.FileUpload, contentDescription = null)
                Text(
                    text = stringResource(Res.string.routes_import_gpx),
                    modifier = Modifier.padding(start = HazloSpaces.sm),
                )
            }
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            // La primera importacion se pide desde el estado vacio, asi que ese estado tiene que
            // saber apartarse en cuanto hay algo entrando: si no, la ruta que llega no tendria
            // donde aparecer y seguiriamos diciendo que no hay ninguna.
            if (routes.isEmpty() && state.importing == null) {
                EmptyRoutes(onImport = askForGpx, enabled = !state.isImporting)
            } else {
                RouteList(
                    routes = routes,
                    importing = state.importing,
                    onOpen = { onOpenRoute(it.id) },
                    onRename = { renaming = it },
                    onExport = { viewModel.export(it.id) },
                    onDelete = { deleting = it },
                )
            }
        }
    }

    renaming?.let { route ->
        RenameRouteDialog(
            currentName = route.name,
            onDismiss = { renaming = null },
            onConfirm = { newName ->
                viewModel.rename(route.id, newName)
                renaming = null
            },
        )
    }

    deleting?.let { route ->
        DeleteRouteDialog(
            routeName = route.name,
            onDismiss = { deleting = null },
            onConfirm = {
                viewModel.delete(route.id)
                deleting = null
            },
        )
    }

    state.pendingImport?.let { pending ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissPendingImport() },
            title = { Text(stringResource(Res.string.routes_duplicate_title)) },
            text = {
                Text(
                    stringResource(
                        Res.string.routes_duplicate_body,
                        pending.existingName,
                        pending.incomingName,
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmReplace() }) {
                    Text(stringResource(Res.string.routes_duplicate_replace))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissPendingImport() }) {
                    Text(stringResource(Res.string.routes_duplicate_keep))
                }
            },
        )
    }
}

@Composable
private fun RouteList(
    routes: List<Route>,
    importing: ImportingRoute?,
    onOpen: (Route) -> Unit,
    onRename: (Route) -> Unit,
    onExport: (Route) -> Unit,
    onDelete: (Route) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = HazloSpaces.gutter,
            end = HazloSpaces.gutter,
            top = HazloSpaces.md,
            bottom = HazloSpaces.xl,
        ),
        verticalArrangement = Arrangement.spacedBy(HazloSpaces.sm),
    ) {
        // Arriba del todo: lo que acaba de pedirse es lo que se quiere ver aparecer, y ademas es
        // donde caera la ruta ya guardada, que se ordena por fecha de creacion.
        importing?.let { entering ->
            item(key = IMPORTING_ROW_KEY) { ImportingRouteRow(entering) }
        }

        items(routes, key = { it.id }) { route ->
            RouteRow(
                route = route,
                onOpen = { onOpen(route) },
                onRename = { onRename(route) },
                onExport = { onExport(route) },
                onDelete = { onDelete(route) },
            )
        }
    }
}

@Composable
private fun RouteRow(
    route: Route,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit,
) {
    // Toda la tarjeta abre la ruta. Los tres botones de dentro siguen haciendo lo suyo: en Compose
    // el hijo se queda el toque, así que no hace falta excluirlos a mano.
    LeafCard(onClick = onOpen, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(HazloSpaces.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RouteSilhouette(route)
            Spacer(modifier = Modifier.width(HazloSpaces.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = route.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = HazloSpaces.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RouteMetric(
                        label = stringResource(Res.string.movement_metric_distance),
                        value = MovementFormat.distance(route.distance),
                    )
                    Box(modifier = Modifier.padding(start = HazloSpaces.lg)) {
                        RouteMetric(
                            label = stringResource(Res.string.movement_metric_elevation),
                            value = MovementFormat.elevation(route.elevationGain),
                        )
                    }
                    Box(modifier = Modifier.weight(1f))
                    IconButton(onClick = onRename) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = stringResource(Res.string.routes_rename),
                        )
                    }
                    if (gpxFileAccessAvailable) {
                        IconButton(onClick = onExport) {
                            Icon(
                                Icons.Default.FileDownload,
                                contentDescription = stringResource(Res.string.routes_export),
                            )
                        }
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = stringResource(Res.string.routes_delete),
                        )
                    }
                }
            }
        }
    }
}

/**
 * La forma de la ruta, o su hueco.
 *
 * Una ruta guardada antes de que se guardara la silueta no tiene forma que dibujar. Ensena un hueco
 * en vez de un trazado inventado: no se sabe por donde va sin abrirla.
 */
@Composable
private fun RouteSilhouette(route: Route) {
    val silhouette = remember(route.id, route.previewPoints) {
        silhouetteOf(route.previewPoints.map { it.latitude to it.longitude })
    }
    val size = Modifier.size(SILHOUETTE_SIZE).testTag(RoutesTags.silhouette(route.id))

    if (silhouette.isDrawable) {
        TrackSilhouetteView(
            silhouette = silhouette,
            color = PillarType.MOVEMENT.palette().ink,
            modifier = size,
        )
    } else {
        TrackSilhouetteGap(modifier = size)
    }
}

private val SILHOUETTE_SIZE = 64.dp

@Composable
private fun RouteMetric(label: String, value: String) {
    Column {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = PillarType.MOVEMENT.palette().ink,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun RenameRouteDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by remember(currentName) { mutableStateOf(currentName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.routes_name_dialog_title)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                label = { Text(stringResource(Res.string.routes_name_label)) },
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }) {
                Text(stringResource(Res.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.action_cancel)) }
        },
    )
}

/**
 * Un estado vacio que **ofrece la accion**, no solo la explica.
 *
 * Donde se pueden abrir archivos, importar esta a un toque aqui mismo. Donde no, la explicacion es
 * lo unico honesto que se puede poner: un boton que no lleva a ninguna parte es peor que una frase.
 */
@Composable
private fun EmptyRoutes(onImport: () -> Unit, enabled: Boolean) {
    Column(
        modifier = Modifier.fillMaxSize().padding(HazloSpaces.gutter),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(
                if (gpxFileAccessAvailable) {
                    Res.string.routes_empty_with_files
                } else {
                    Res.string.routes_empty_without_files
                },
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (gpxFileAccessAvailable) {
            Spacer(modifier = Modifier.height(HazloSpaces.md))
            Button(
                onClick = onImport,
                enabled = enabled,
                modifier = Modifier.testTag(RoutesTags.EMPTY_IMPORT),
            ) {
                Icon(Icons.Default.FileUpload, contentDescription = null)
                Text(
                    text = stringResource(Res.string.routes_import_gpx),
                    modifier = Modifier.padding(start = HazloSpaces.sm),
                )
            }
        }
    }
}

/** La fila de la ruta que entra no tiene id todavia, asi que lleva la suya propia. */
private const val IMPORTING_ROW_KEY = "routes_importing_row"

/**
 * La ruta que esta entrando, con la forma de las que ya estan.
 *
 * Es una tarjeta y no un aviso encima de la lista porque lo que se espera al importar es **ver
 * aparecer la ruta**, no leer que algo esta pasando. Ocupa el sitio donde va a quedarse, asi que
 * cuando termina no hay salto: la fila deja de girar y se llena.
 *
 * **El cargador es indeterminado a proposito.** Se midio antes de decidirlo (`GpxImportBenchmark`):
 * parsear, medir, guardar y releer un GPX de 20.000 puntos son 378 ms. Con eso no hay trabajo largo
 * que repartir, y una barra que va del 0 al 100 en un parpadeo es un adorno que finge medir - la
 * misma clase de cifra inventada que el pilar lleva cuatro slices quitando de sus distancias y sus
 * desniveles.
 *
 * Lo que se enseña es el nombre del **archivo**, no el de la ruta: el nombre de verdad sale de
 * dentro del GPX y puede no parecerse. Antes de que el selector entregue el archivo no se sabe ni
 * eso, y entonces la fila lo dice en vez de inventarse un nombre.
 */
@Composable
private fun ImportingRouteRow(importing: ImportingRoute) {
    LeafCard(modifier = Modifier.fillMaxWidth().testTag(RoutesTags.IMPORTING)) {
        Row(
            modifier = Modifier.padding(HazloSpaces.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(SILHOUETTE_SIZE),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(HazloSpaces.lg),
                    strokeWidth = IMPORTING_STROKE,
                    color = PillarType.MOVEMENT.palette().ink,
                )
            }
            Spacer(modifier = Modifier.width(HazloSpaces.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = importing.fileName
                        ?: stringResource(Res.string.routes_importing_unnamed),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(Res.string.routes_importing),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = HazloSpaces.xs),
                )
            }
        }
    }
}

private val IMPORTING_STROKE = 2.dp
