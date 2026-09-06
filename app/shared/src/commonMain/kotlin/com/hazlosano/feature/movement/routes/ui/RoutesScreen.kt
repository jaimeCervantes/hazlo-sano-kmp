package com.hazlosano.feature.movement.routes.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.hazlosano.core.ui.components.atomic.HazloTopAppBar
import com.hazlosano.core.ui.components.atomic.LeafCard
import com.hazlosano.core.ui.model.palette
import com.hazlosano.core.ui.theme.HazloSpaces
import com.hazlosano.domain.feature.movement.model.Route
import com.hazlosano.domain.model.PillarType
import com.hazlosano.feature.movement.presentation.MovementFormat
import com.hazlosano.feature.movement.routes.presentation.RoutesViewModel
import hazlosano.app.shared.generated.resources.Res
import com.hazlosano.feature.movement.ui.text
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
import hazlosano.app.shared.generated.resources.top_app_bar_back
import org.jetbrains.compose.resources.stringResource

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
    modifier: Modifier = Modifier,
) {
    val routes by viewModel.allRoutes.collectAsState()
    val state by viewModel.uiState.collectAsState()
    var renaming by remember { mutableStateOf<Route?>(null) }

    val pickGpx = rememberGpxPicker { fileName, bytes -> viewModel.import(fileName, bytes) }
    val saveGpx = rememberGpxSaver()

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

        if (gpxFileAccessAvailable) {
            Button(
                onClick = pickGpx,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = HazloSpaces.gutter, vertical = HazloSpaces.sm),
            ) {
                Icon(Icons.Default.FileUpload, contentDescription = null)
                Text(
                    text = stringResource(Res.string.routes_import_gpx),
                    modifier = Modifier.padding(start = HazloSpaces.sm),
                )
            }
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (routes.isEmpty()) {
                EmptyRoutes()
            } else {
                RouteList(
                    routes = routes,
                    onOpen = { onOpenRoute(it.id) },
                    onRename = { renaming = it },
                    onExport = { viewModel.export(it.id) },
                    onDelete = { viewModel.delete(it.id) },
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
        Column(modifier = Modifier.padding(HazloSpaces.md)) {
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

@Composable
private fun EmptyRoutes() {
    Box(
        modifier = Modifier.fillMaxSize().padding(HazloSpaces.gutter),
        contentAlignment = Alignment.Center,
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
    }
}
