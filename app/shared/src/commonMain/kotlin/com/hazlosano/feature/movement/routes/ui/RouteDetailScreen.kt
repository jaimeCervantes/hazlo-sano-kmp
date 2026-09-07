package com.hazlosano.feature.movement.routes.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
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
import com.hazlosano.core.ui.components.atomic.HazloTopAppBar
import com.hazlosano.core.ui.model.palette
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.ui.unit.dp
import com.hazlosano.core.ui.theme.HazloShapes
import com.hazlosano.core.ui.theme.HazloSpaces
import com.hazlosano.domain.model.PillarType
import com.hazlosano.feature.movement.presentation.MovementFormat
import com.hazlosano.feature.movement.routes.presentation.RouteDetailUiState
import com.hazlosano.feature.movement.routes.presentation.rememberRouteDetailViewModel
import com.hazlosano.feature.movement.ui.MovementMap
import hazlosano.app.shared.generated.resources.Res
import hazlosano.app.shared.generated.resources.route_detail_back
import hazlosano.app.shared.generated.resources.route_detail_distance
import hazlosano.app.shared.generated.resources.route_detail_elevation
import hazlosano.app.shared.generated.resources.route_detail_missing
import hazlosano.app.shared.generated.resources.route_detail_no_path
import hazlosano.app.shared.generated.resources.route_detail_points
import hazlosano.app.shared.generated.resources.routes_delete
import hazlosano.app.shared.generated.resources.routes_export
import hazlosano.app.shared.generated.resources.top_app_bar_back
import org.jetbrains.compose.resources.stringResource

/** Etiquetas de prueba: la estructura se afirma por aquí, no por la redacción. */
object RouteDetailTags {
    const val MAP: String = "route_detail_map"
    const val MISSING: String = "route_detail_missing"
    const val NO_PATH: String = "route_detail_no_path"
    const val METRICS: String = "route_detail_metrics"
    const val EXPORT: String = "route_detail_export"
    const val DELETE: String = "route_detail_delete"
}

@Composable
fun RouteDetailScreen(
    routeId: Long,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = rememberRouteDetailViewModel(routeId)
    val state by viewModel.state.collectAsState()
    val exported by viewModel.exported.collectAsState()
    val deleted by viewModel.deleted.collectAsState()
    val saveGpx = rememberGpxSaver()

    // Igual que en la lista: el GPX se entrega a la plataforma una vez y se limpia, para que volver
    // a esta pantalla no abra otra vez el selector de guardado.
    exported?.let { file ->
        LaunchedEffect(file) {
            saveGpx(file.fileName, file.gpx)
            viewModel.consumeExport()
        }
    }

    // Quedarse mirando el detalle de una ruta que se acaba de borrar no es una pantalla, es un
    // hueco. Se vuelve a la lista, que es donde esta lo que si existe.
    LaunchedEffect(deleted) {
        if (deleted) onBack()
    }

    RouteDetailContent(
        state = state,
        onBack = onBack,
        onExport = viewModel::export,
        onDelete = viewModel::delete,
        modifier = modifier,
    )
}

/** Sin ViewModel, para poder componerlo en un test con cualquier estado. */
@Composable
fun RouteDetailContent(
    state: RouteDetailUiState,
    onBack: () -> Unit,
    onExport: () -> Unit = {},
    onDelete: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var confirmingDelete by remember { mutableStateOf(false) }
    val detail = state as? RouteDetailUiState.Detail

    Column(modifier = modifier.fillMaxSize()) {
        HazloTopAppBar(
            title = detail?.name.orEmpty(),
            showBackButton = true,
            onBackClick = onBack,
            backContentDescription = stringResource(Res.string.top_app_bar_back),
        )

        // Las acciones solo existen mientras hay una ruta: sobre un detalle que se esta cargando o
        // que no encontro nada, borrar y descargar no significan nada.
        detail?.let { route ->
            RouteActions(
                canExport = gpxFileAccessAvailable,
                onExport = onExport,
                onDelete = { confirmingDelete = true },
            )

            if (confirmingDelete) {
                DeleteRouteDialog(
                    routeName = route.name,
                    onDismiss = { confirmingDelete = false },
                    onConfirm = {
                        confirmingDelete = false
                        onDelete()
                    },
                )
            }
        }

        when (state) {
            RouteDetailUiState.Loading -> Centered {
                CircularProgressIndicator(color = PillarType.MOVEMENT.palette().ink)
            }

            RouteDetailUiState.Missing -> Centered {
                Message(
                    text = stringResource(Res.string.route_detail_missing),
                    modifier = Modifier.testTag(RouteDetailTags.MISSING),
                )
            }

            is RouteDetailUiState.Detail -> RouteDetailBody(state)
        }
    }
}

/**
 * El detalle es **mapa con hoja inferior**, no una ficha con un mapa dentro.
 *
 * Es el patrón que el proyecto hermano fijó para lo mismo (`034-panel-detalle-tienda-mapa`): la hoja
 * se superpone sin empujar el contenido, así que el trazado usa la pantalla entera y las cifras
 * quedan a mano encima. Es también a lo que han convergido las apps del ramo, y aquí importa más que
 * en ningún otro sitio: lo que se viene a ver es por dónde va la ruta.
 */
@Composable
private fun androidx.compose.foundation.layout.ColumnScope.RouteDetailBody(
    state: RouteDetailUiState.Detail,
) {
    Box(
        modifier = Modifier.weight(1f).fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        if (state.hasPath) {
            MovementMap(
                userLocation = null,
                path = state.path,
                // Encuadrar la ruta entera y no seguir una posición: esto no es una grabación en
                // curso, es un trazado que se mira antes de salir.
                fitPathInView = true,
                modifier = Modifier.fillMaxSize().testTag(RouteDetailTags.MAP),
            )
        } else {
            Message(
                text = stringResource(Res.string.route_detail_no_path),
                modifier = Modifier.testTag(RouteDetailTags.NO_PATH),
            )
        }

        Surface(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
            shape = RoundedCornerShape(topStart = HazloShapes.panel, topEnd = HazloShapes.panel),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = SHEET_ELEVATION,
        ) {
            RouteMetrics(state)
        }
    }
}

/** Lo justo para que la hoja se despegue del mapa sin taparlo con una sombra. */
private val SHEET_ELEVATION = 3.dp

@Composable
private fun RouteMetrics(state: RouteDetailUiState.Detail) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = HazloSpaces.gutter, vertical = HazloSpaces.md)
            .testTag(RouteDetailTags.METRICS),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        Metric(
            label = stringResource(Res.string.route_detail_distance),
            value = MovementFormat.distance(state.distanceMeters),
        )
        Metric(
            label = stringResource(Res.string.route_detail_elevation),
            // `elevation` ya devuelve "—" cuando no hay medida, que es la diferencia entre una
            // ruta llana y una que no dice nada de su desnivel.
            value = MovementFormat.elevation(state.elevationGainMeters),
        )
        Metric(
            label = stringResource(Res.string.route_detail_points),
            value = state.pointCount.toString(),
        )
    }
}

@Composable
private fun Metric(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.Centered(
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier.weight(1f).fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
private fun Message(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier.padding(HazloSpaces.gutter),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}

/**
 * Lo que se puede hacer con la ruta que se esta mirando.
 *
 * Descargar y borrar existian solo en las filas de la lista, asi que quien abria una ruta para verla
 * en el mapa —que es donde se decide si sirve— tenia que volver atras para hacer nada con ella.
 *
 * Descargar se esconde donde la plataforma no sabe escribir archivos, igual que en la lista: un
 * boton que no puede funcionar es peor que ninguno.
 */
@Composable
private fun RouteActions(
    canExport: Boolean,
    onExport: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = HazloSpaces.gutter),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (canExport) {
            IconButton(onClick = onExport, modifier = Modifier.testTag(RouteDetailTags.EXPORT)) {
                Icon(
                    Icons.Default.FileDownload,
                    contentDescription = stringResource(Res.string.routes_export),
                )
            }
        }
        IconButton(onClick = onDelete, modifier = Modifier.testTag(RouteDetailTags.DELETE)) {
            Icon(
                Icons.Default.Delete,
                contentDescription = stringResource(Res.string.routes_delete),
            )
        }
    }
}
