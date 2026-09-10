package com.hazlosano.feature.home.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.runComposeUiTest
import com.hazlosano.core.ui.components.atomic.SleepSummaryCardDefaults
import com.hazlosano.core.ui.components.sections.PillarFilterTags
import com.hazlosano.core.ui.components.sections.PillarHighlightsTags
import com.hazlosano.feature.search.ui.SearchTags
import com.hazlosano.domain.model.FeedPost
import com.hazlosano.domain.model.HazloChampion
import com.hazlosano.domain.model.HomeContent
import com.hazlosano.domain.model.PillarAction
import com.hazlosano.domain.model.PillarOverview
import com.hazlosano.domain.model.PillarType
import com.hazlosano.domain.model.SleepAnalysis
import com.hazlosano.feature.home.presentation.HomeUiState
import com.hazlosano.feature.sleep.presentation.SleepUiState
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Inicio carga por partes.
 *
 * El resumen de anoche no lo carga esta pantalla —llega ya calculado desde el pilar de sueño—, así
 * que lo que se comprueba aquí es que nunca queda escondido detrás de lo que sí está esperando.
 *
 * Los huecos laten, y una animación infinita nunca deja quieto al reloj de prueba: por eso cada
 * prueba que compone uno empieza con `mainClock.autoAdvance = false`.
 */
@OptIn(ExperimentalTestApi::class)
class HomeBoardTest {

    private val analysis = SleepAnalysis(
        totalSessions = 2,
        totalDurationMillis = 7 * 60 * 60 * 1000L,
        efficiency = 0.91f,
        periodStart = 1_788_534_000_000L,
        periodEnd = 1_788_570_000_000L,
    )

    private val content = HomeContent(
        headerTitle = "Tu Ecosistema",
        headerSubtitle = "Cultiva tus 4 pilares hoy",
        pillars = listOf(
            PillarOverview(
                pillarType = PillarType.MOVEMENT,
                title = "Movimiento",
                stat = "Recorrer Rutas",
                subtitle = "Inicia tu sesión de tracking",
                imageUrl = "",
                action = PillarAction.TRACKER,
            ),
            PillarOverview(
                pillarType = PillarType.NUTRITION,
                title = "Nutrición",
                stat = "",
                subtitle = "Receta del día",
                imageUrl = "",
            ),
        ),
        champions = listOf(
            HazloChampion(name = "Ana", title = "Sueño", stat = "42 hrs", imageUrl = ""),
        ),
        feedPosts = listOf(
            FeedPost(
                userName = "Ana M.",
                timeAgo = "Hace 2 horas",
                avatarSeed = "Ana",
                pillarType = PillarType.SLEEP,
                content = "Racha de 7 días durmiendo 8 horas seguidas.",
                imageUrl = null,
                likes = 24,
                comments = 5,
            ),
        ),
    )

    @Test
    fun `the sleep summary does not wait for the rest of the home screen`() = runComposeUiTest {
        mainClock.autoAdvance = false
        setContent {
            HomeBoard(
                state = HomeUiState.Loading,
                sleepState = SleepUiState.Success(analysis),
                onNavigateToTracker = {},
                onRefreshSleep = null,
                onSleepCardClick = null,
            )
        }

        // La tarjeta de anoche llega ya calculada del pilar de sueño: no tiene por qué esconderse
        // mientras Inicio carga lo suyo.
        onNodeWithText("Última noche").assertIsDisplayed()
        onNodeWithTag(HomeSkeletonTags.PILLARS).assertIsDisplayed()
    }

    @Test
    fun `every section of the home screen shows its own gap`() = runComposeUiTest {
        mainClock.autoAdvance = false
        setContent {
            HomeBoard(
                state = HomeUiState.Loading,
                sleepState = SleepUiState.Loading,
                onNavigateToTracker = {},
                onRefreshSleep = null,
                onSleepCardClick = null,
            )
        }

        onNodeWithTag(HomeSkeletonTags.SLEEP).assertIsDisplayed()
        onNodeWithTag(HomeSkeletonTags.PILLARS).assertIsDisplayed()
        onNodeWithTag(PillarHighlightsTags.CHAMPIONS).assertDoesNotExist()
        onNodeWithTag(HomeTags.PILLARS).assertDoesNotExist()
    }

    // ─────────── El resumen de anoche reserva su sitio ───────────

    /**
     * El brinco tal y como se reportó: se abre el app, el resumen de anoche no ocupa nada mientras
     * se lee de la base de datos, y al llegar empuja hacia abajo los cuatro pilares.
     *
     * Lo que se afirma es que **el hueco existe y mide lo que la tarjeta**, que es lo que hace que
     * al llegar nada se mueva.
     */
    @Test
    fun `the sleep summary keeps its place while it is being read`() = runComposeUiTest {
        mainClock.autoAdvance = false
        setContent {
            HomeBoard(
                state = HomeUiState.Success(content),
                sleepState = SleepUiState.Loading,
                onNavigateToTracker = {},
                onRefreshSleep = null,
                onSleepCardClick = null,
            )
        }

        onNodeWithTag(HomeSkeletonTags.SLEEP)
            .assertIsDisplayed()
            .assertHeightIsEqualTo(SleepSummaryCardDefaults.height)
    }

    /** Y cuando llega, ocupa exactamente ese hueco. */
    @Test
    fun `and when it arrives it fills exactly that place`() = runComposeUiTest {
        mainClock.autoAdvance = false
        setContent {
            HomeBoard(
                state = HomeUiState.Success(content),
                sleepState = SleepUiState.Success(analysis),
                onNavigateToTracker = {},
                onRefreshSleep = null,
                onSleepCardClick = null,
            )
        }

        onNodeWithTag(HomeSkeletonTags.SLEEP).assertDoesNotExist()
        onNodeWithTag(HomeTags.SLEEP).assertHeightIsEqualTo(SleepSummaryCardDefaults.height)
    }

    /**
     * Si la lectura falló no queda un hueco latiendo.
     *
     * Un esqueleto perpetuo promete algo que ya se sabe que no va a llegar, y eso es peor que la
     * ausencia: con `SleepAnalysis?` aplanado las dos situaciones eran el mismo nulo y no se podían
     * distinguir.
     */
    @Test
    fun `a reading that failed leaves no gap pulsing forever`() = runComposeUiTest {
        setContent {
            HomeBoard(
                state = HomeUiState.Success(content),
                sleepState = SleepUiState.Failed,
                onNavigateToTracker = {},
                onRefreshSleep = null,
                onSleepCardClick = null,
            )
        }

        onNodeWithTag(HomeSkeletonTags.SLEEP).assertDoesNotExist()
        onNodeWithTag(HomeTags.SLEEP).assertDoesNotExist()
        onNodeWithTag(HomeTags.PILLARS).assertIsDisplayed()
    }

    @Test
    fun `what is ready leaves no gap behind`() = runComposeUiTest {
        // Esta sí deja correr el reloj: no compone ningún hueco —todo está listo— y en cambio
        // necesita bajar hasta los campeones, que con el reloj parado no llega a asentarse.
        setContent {
            HomeBoard(
                state = HomeUiState.Success(content),
                sleepState = SleepUiState.Success(analysis),
                onNavigateToTracker = {},
                onRefreshSleep = null,
                onSleepCardClick = null,
            )
        }

        onNodeWithTag(HomeTags.PILLARS).assertIsDisplayed()
        onNodeWithTag(HomeSkeletonTags.PILLARS).assertDoesNotExist()
        onNodeWithTag(HomeSkeletonTags.FEED).assertDoesNotExist()

        // Los campeones caen por debajo del pliegue: con el resumen de anoche reservando su sitio,
        // la primera pantalla ya no llega hasta ellos. Se baja hasta ahí en vez de aflojar la
        // afirmación, que sería dejar de comprobar que la sección existe.
        onNodeWithTag(HomeTags.BOARD).performScrollToNode(hasTestTag(HomeTags.CHAMPIONS))
        onNodeWithTag(HomeTags.CHAMPIONS).assertIsDisplayed()
    }

    @Test
    fun `a home that failed says so without hiding last night`() = runComposeUiTest {
        mainClock.autoAdvance = false
        setContent {
            HomeBoard(
                state = HomeUiState.Failed,
                sleepState = SleepUiState.Success(analysis),
                onNavigateToTracker = {},
                onRefreshSleep = null,
                onSleepCardClick = null,
            )
        }

        onNodeWithTag(HomeTags.FAILED).assertIsDisplayed()
        onNodeWithText("Última noche").assertIsDisplayed()
        onNodeWithTag(HomeSkeletonTags.PILLARS).assertDoesNotExist()
    }

    /**
     * El Inicio conserva sus chips y ya no tiene campo.
     *
     * Acotar el feed por pilar no es buscar: es la lista que se recorre sin preguntar nada, y sigue
     * siendo suya. Lo que se fue a la barra de arriba es la pregunta, que nunca fue del Inicio
     * porque entra a los cuatro pilares.
     */
    @Test
    fun `the home keeps its chips and no longer has a search field`() = runComposeUiTest {
        setContent {
            HomeBoard(
                state = HomeUiState.Success(content),
                sleepState = SleepUiState.Failed,
                onNavigateToTracker = {},
                onRefreshSleep = null,
                onSleepCardClick = null,
            )
        }

        onNodeWithTag(HomeTags.BOARD).performScrollToNode(hasTestTag(PillarFilterTags.ROW))
        onNodeWithTag(PillarFilterTags.ROW).assertIsDisplayed()
        onNodeWithTag(SearchTags.FIELD).assertDoesNotExist()
    }

    @Test
    fun `a failed home offers a retry that asks again`() = runComposeUiTest {
        var retried = false

        setContent {
            HomeBoard(
                state = HomeUiState.Failed,
                // Nada que late en esta prueba, que no para el reloj: con el resumen de anoche
                // esperando, su hueco animaría para siempre y el test no llegaría a asentarse.
                sleepState = SleepUiState.Failed,
                onNavigateToTracker = {},
                onRefreshSleep = null,
                onSleepCardClick = null,
                onRetry = { retried = true },
            )
        }

        onNodeWithTag(HomeTags.RETRY).performClick()

        assertTrue(retried, "el botón de reintentar de Inicio vuelve a pedir el contenido")
    }
}
