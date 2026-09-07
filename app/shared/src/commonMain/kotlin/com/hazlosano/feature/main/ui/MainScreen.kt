package com.hazlosano.feature.main.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.hazlosano.core.ui.components.atomic.HazloTopAppBar
import com.hazlosano.core.ui.model.palette
import com.hazlosano.core.ui.model.pillarIcon
import com.hazlosano.core.ui.model.pillarLabel
import com.hazlosano.core.ui.model.pillarShortLabel
import com.hazlosano.core.ui.theme.LocalHazloPalette
import com.hazlosano.core.ui.theme.PillarPalette
import com.hazlosano.domain.model.PillarType
import com.hazlosano.domain.model.SleepAnalysis
import com.hazlosano.domain.repository.SleepSessionRepository
import com.hazlosano.domain.usecase.GetSleepHistoryUseCase
import com.hazlosano.feature.catalog.presentation.rememberPillarCatalogViewModel
import com.hazlosano.feature.catalog.ui.PillarCatalogScreen
import com.hazlosano.feature.home.presentation.HomeViewModel
import com.hazlosano.feature.sleep.presentation.SleepHistoryViewModel
import com.hazlosano.feature.sleep.presentation.SleepViewModel
import com.hazlosano.feature.home.ui.HomeScreen
import com.hazlosano.feature.movement.detail.ui.SessionDetailScreen
import com.hazlosano.feature.movement.history.ui.MovementHistoryScreen
import com.hazlosano.feature.movement.presentation.MovementDestination
import com.hazlosano.feature.movement.presentation.MovementNavState
import com.hazlosano.feature.movement.routes.presentation.rememberRoutesViewModel
import com.hazlosano.feature.pillar.presentation.rememberPillarHighlightsViewModel
import com.hazlosano.feature.pillar.ui.PillarInfoScreen
import com.hazlosano.feature.settings.ui.SettingsScreen
import com.hazlosano.feature.movement.routes.ui.RouteDetailScreen
import com.hazlosano.feature.movement.routes.ui.RoutesScreen
import com.hazlosano.feature.movement.ui.MovementPillarActions
import com.hazlosano.feature.movement.tracker.ui.TrackerScreen
import com.hazlosano.feature.sleep.ui.SleepHistoryScreen
import com.hazlosano.feature.sleep.ui.SleepScreen
import hazlosano.app.shared.generated.resources.Res
import hazlosano.app.shared.generated.resources.app_name
import hazlosano.app.shared.generated.resources.bottom_tab_home
import hazlosano.app.shared.generated.resources.history_back
import hazlosano.app.shared.generated.resources.history_title
import hazlosano.app.shared.generated.resources.top_app_bar_back
import hazlosano.app.shared.generated.resources.top_app_bar_menu
import hazlosano.app.shared.generated.resources.top_app_bar_notifications
import hazlosano.app.shared.generated.resources.top_app_bar_profile
import org.jetbrains.compose.resources.stringResource

/**
 * Una pestaña de la barra inferior: cuatro llevan un pilar y una (Inicio) no.
 *
 * El nombre, el icono y el color de las cuatro de pilar salen de `PillarVisuals` — la misma fuente
 * que usa cada tablero — en vez de una segunda copia de los cuatro pilares.
 */
enum class BottomTab(val pillar: PillarType?) {
    Inicio(null),
    Sueno(PillarType.SLEEP),
    Nutricion(PillarType.NUTRITION),
    Movimiento(PillarType.MOVEMENT),
    Mente(PillarType.MIND),
}

/**
 * Lo que se lee en la pestaña, en su forma compacta: la barra reparte el ancho entre cinco, y
 * "Mente y Espíritu" entero partía en dos renglones y dejaba esa pestaña más alta que las otras.
 */
@Composable
internal fun BottomTab.label(): String =
    pillar?.let { pillarShortLabel(it) } ?: stringResource(Res.string.bottom_tab_home)

/**
 * El nombre entero del pilar, para quien no tiene el ancho por límite.
 *
 * Es lo que oye un lector de pantalla: acortar por falta de sitio es una decisión visual, y no hay
 * razón para que le llegue también a quien no está mirando la barra.
 */
@Composable
internal fun BottomTab.accessibleLabel(): String =
    pillar?.let { pillarLabel(it) } ?: stringResource(Res.string.bottom_tab_home)

internal fun BottomTab.icon(): ImageVector = pillar?.let { pillarIcon(it) } ?: Icons.Filled.Home

/**
 * Los papeles de la pestaña. Son dos y no uno: el indicador se **rellena** y la etiqueta se
 * **escribe**, así que antes el mismo tono hacía de fondo saturado y de tinta sobre el papel — y en
 * tres de los cuatro pilares no servía para lo segundo.
 *
 * Inicio no es un pilar, así que toma el verde de marca en los dos papeles.
 */
@Composable
internal fun BottomTab.palette(): PillarPalette {
    val brand = LocalHazloPalette.current
    return pillar?.palette() ?: PillarPalette(
        solid = brand.brandGreen,
        soft = brand.brandGreenSoft,
        ink = brand.brandGreen,
    )
}

@Composable
fun MainScreen(
    homeViewModel: HomeViewModel,
    sleepViewModel: SleepViewModel,
    sleepAnalysis: SleepAnalysis? = null,
    onRefreshSleep: (() -> Unit)? = null,
    sleepSessionRepository: SleepSessionRepository? = null,
) {
    var selectedTab by remember { mutableStateOf(BottomTab.Inicio) }
    var showSleepHistory by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    // Qué pilar se está leyendo, o ninguno. Se guarda el pilar y no un booleano porque la pantalla
    // es la misma para los cuatro: lo único que cambia es de cuál habla.
    var pillarInfo by remember { mutableStateOf<PillarType?>(null) }
    val movementNav = remember { MovementNavState() }

    val navigateToSleepHistory: () -> Unit = {
        selectedTab = BottomTab.Sueno
        showSleepHistory = true
    }

    // Vive aquí y no dentro de la rama de Rutas, que es donde estaba: allí el `remember` se olvidaba
    // al abrir el detalle de una ruta, así que una importación en curso perdía a quien la miraba y
    // al volver la pantalla decía que no pasaba nada. La importación sí terminaba —el trabajo no
    // depende de la pantalla— pero el aviso y el resultado se los llevaba la navegación.
    val routesViewModel = rememberRoutesViewModel()

    when (val movementDestination = movementNav.destination) {
        MovementDestination.Tracker -> {
            TrackerScreen(
                onBack = { movementNav.back() },
                onOpenHistory = { movementNav.openHistory() },
                modifier = Modifier.fillMaxSize(),
            )
            return
        }

        MovementDestination.History -> {
            MovementHistoryScreen(
                onBack = { movementNav.back() },
                onOpenSession = { sessionId -> movementNav.openSessionDetail(sessionId) },
                onOpenRoutes = { movementNav.openRoutes() },
                modifier = Modifier.fillMaxSize(),
            )
            return
        }

        MovementDestination.Routes -> {
            RoutesScreen(
                viewModel = routesViewModel,
                onBack = { movementNav.back() },
                onOpenRoute = { routeId -> movementNav.openRouteDetail(routeId) },
                modifier = Modifier.fillMaxSize(),
            )
            return
        }

        is MovementDestination.RouteDetail -> {
            RouteDetailScreen(
                routeId = movementDestination.routeId,
                onBack = { movementNav.back() },
                modifier = Modifier.fillMaxSize(),
            )
            return
        }

        is MovementDestination.SessionDetail -> {
            SessionDetailScreen(
                sessionId = movementDestination.sessionId,
                onBack = { movementNav.back() },
                modifier = Modifier.fillMaxSize(),
            )
            return
        }

        MovementDestination.Closed -> Unit
    }

    if (showSettings) {
        SettingsScreen(
            onBack = { showSettings = false },
            modifier = Modifier.fillMaxSize(),
        )
        return
    }

    pillarInfo?.let { pillar ->
        PillarInfoScreen(
            pillar = pillar,
            onBack = { pillarInfo = null },
            modifier = Modifier.fillMaxSize(),
        )
        return
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        HazloTopAppBar(
            title = stringResource(
                if (showSleepHistory) Res.string.history_title else Res.string.app_name,
            ),
            showBackButton = showSleepHistory,
            onBackClick = { showSleepHistory = false },
            // El icono de menú llevaba desde siempre sin hacer nada; ahora es la puerta a ajustes.
            onMenuClick = { showSettings = true },
            backContentDescription = stringResource(Res.string.top_app_bar_back),
            profileContentDescription = stringResource(Res.string.top_app_bar_profile),
            notificationsContentDescription = stringResource(Res.string.top_app_bar_notifications),
            menuContentDescription = stringResource(Res.string.top_app_bar_menu),
        )
        if (showSleepHistory && sleepSessionRepository != null) {
            val historyViewModel = remember {
                SleepHistoryViewModel(GetSleepHistoryUseCase(sleepSessionRepository))
            }
            SleepHistoryScreen(viewModel = historyViewModel)
        } else {
            Scaffold(
                modifier = Modifier.weight(1f),
                bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ) {
                    BottomTab.entries.forEach { tab ->
                        val selected = selectedTab == tab
                        val label = tab.label()
                        val palette = tab.palette()
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = tab.icon(),
                                    contentDescription = tab.accessibleLabel(),
                                )
                            },
                            label = {
                                // Un renglón siempre: la altura de la barra no puede depender de lo
                                // largo que sea el nombre de un pilar.
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1,
                                    softWrap = false,
                                    // Si una traducción futura no cabe, que se corte con puntos
                                    // suspensivos en vez de quedar cortada a hachazo.
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center,
                                )
                            },
                            selected = selected,
                            onClick = { selectedTab = tab },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.White,
                                selectedTextColor = palette.ink,
                                indicatorColor = palette.solid,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                        )
                    }
                }
            },
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                when (selectedTab) {
                    BottomTab.Inicio -> HomeScreen(
                        viewModel = homeViewModel,
                        sleepAnalysis = sleepAnalysis,
                        onNavigateToTracker = { movementNav.openTracker() },
                        onRefreshSleep = onRefreshSleep,
                        onSleepCardClick = navigateToSleepHistory,
                    )
                    // Sueño es la única pestaña de pilar que no delega entera en
                    // PillarCatalogScreen: ya tiene su propio encabezado —el resumen de la última
                    // noche— y sólo le faltaban las secciones del catálogo, que ahora comparte.
                    BottomTab.Sueno -> {
                        val sleepCatalog = rememberPillarCatalogViewModel(PillarType.SLEEP)
                        val sleepCatalogState by sleepCatalog.uiState.collectAsState()
                        val sleepHighlights = rememberPillarHighlightsViewModel(PillarType.SLEEP)
                        val sleepHighlightsState by sleepHighlights.uiState.collectAsState()
                        SleepScreen(
                            viewModel = sleepViewModel,
                            highlights = sleepHighlightsState,
                            catalogState = sleepCatalogState,
                            onOpenInfo = { pillarInfo = PillarType.SLEEP },
                            onRetryCatalog = sleepCatalog::refresh,
                            onRefresh = onRefreshSleep,
                            onCardClick = navigateToSleepHistory,
                        )
                    }
                    // Cada pestaña de pilar enseña su propio catálogo. La de Movimiento enseña
                    // además sus herramientas: era el único pilar cuya herramienta no se alcanzaba
                    // desde su propia pestaña.
                    BottomTab.Nutricion -> PillarCatalogScreen(
                        pillar = PillarType.NUTRITION,
                        onOpenInfo = { pillarInfo = PillarType.NUTRITION },
                    )

                    BottomTab.Movimiento -> PillarCatalogScreen(
                        pillar = PillarType.MOVEMENT,
                        onOpenInfo = { pillarInfo = PillarType.MOVEMENT },
                        pillarActions = {
                            MovementPillarActions(
                                onStartOuting = { movementNav.openTracker() },
                                onOpenRoutes = { movementNav.openRoutes() },
                                onOpenOutings = { movementNav.openHistory() },
                            )
                        },
                    )

                    BottomTab.Mente -> PillarCatalogScreen(
                        pillar = PillarType.MIND,
                        onOpenInfo = { pillarInfo = PillarType.MIND },
                    )
                }
            }
        }
        }
    }
}
