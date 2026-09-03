package com.hazlosano.feature.movement.routes.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.hazlosano.core.ui.components.atomic.HazloTopAppBar
import com.hazlosano.core.ui.theme.HazloSpaces
import com.hazlosano.core.ui.theme.PillarMovement
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
import hazlosano.app.shared.generated.resources.top_app_bar_back
import org.jetbrains.compose.resources.stringResource

/** Etiquetas de prueba: la estructura se afirma por aquí, no por la redacción. */
object RouteDetailTags {
    const val MAP: String = "route_detail_map"
    const val MISSING: String = "route_detail_missing"
    const val NO_PATH: String = "route_detail_no_path"
    const val METRICS: String = "route_detail_metrics"
}

@Composable
fun RouteDetailScreen(
    routeId: Long,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by rememberRouteDetailViewModel(routeId).state.collectAsState()

    RouteDetailContent(state = state, onBack = onBack, modifier = modifier)
}

/** Sin ViewModel, para poder componerlo en un test con cualquier estado. */
@Composable
fun RouteDetailContent(
    state: RouteDetailUiState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        HazloTopAppBar(
            title = (state as? RouteDetailUiState.Detail)?.name.orEmpty(),
            showBackButton = true,
            onBackClick = onBack,
            backContentDescription = stringResource(Res.string.top_app_bar_back),
        )

        when (state) {
            RouteDetailUiState.Loading -> Centered {
                CircularProgressIndicator(color = PillarMovement)
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

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.RouteDetailBody(
    state: RouteDetailUiState.Detail,
) {
    RouteMetrics(state)

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
    }
}

@Composable
private fun RouteMetrics(state: RouteDetailUiState.Detail) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = HazloSpaces.gutter, vertical = HazloSpaces.sm)
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
