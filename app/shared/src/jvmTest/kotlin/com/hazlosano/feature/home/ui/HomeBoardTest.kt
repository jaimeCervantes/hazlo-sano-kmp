package com.hazlosano.feature.home.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.hazlosano.core.ui.components.sections.PillarHighlightsTags
import com.hazlosano.domain.model.FeedPost
import com.hazlosano.domain.model.HazloChampion
import com.hazlosano.domain.model.HomeContent
import com.hazlosano.domain.model.PillarAction
import com.hazlosano.domain.model.PillarOverview
import com.hazlosano.domain.model.PillarType
import com.hazlosano.domain.model.SleepAnalysis
import com.hazlosano.feature.home.presentation.HomeUiState
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
                sleepAnalysis = analysis,
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
                sleepAnalysis = null,
                onNavigateToTracker = {},
                onRefreshSleep = null,
                onSleepCardClick = null,
            )
        }

        onNodeWithTag(HomeSkeletonTags.PILLARS).assertIsDisplayed()
        onNodeWithTag(PillarHighlightsTags.CHAMPIONS).assertDoesNotExist()
        onNodeWithTag(HomeTags.PILLARS).assertDoesNotExist()
    }

    @Test
    fun `what is ready leaves no gap behind`() = runComposeUiTest {
        mainClock.autoAdvance = false
        setContent {
            HomeBoard(
                state = HomeUiState.Success(content),
                sleepAnalysis = analysis,
                onNavigateToTracker = {},
                onRefreshSleep = null,
                onSleepCardClick = null,
            )
        }

        onNodeWithTag(HomeTags.PILLARS).assertIsDisplayed()
        onNodeWithTag(HomeTags.CHAMPIONS).assertIsDisplayed()
        onNodeWithTag(HomeSkeletonTags.PILLARS).assertDoesNotExist()
        onNodeWithTag(HomeSkeletonTags.FEED).assertDoesNotExist()
    }

    @Test
    fun `a home that failed says so without hiding last night`() = runComposeUiTest {
        mainClock.autoAdvance = false
        setContent {
            HomeBoard(
                state = HomeUiState.Failed,
                sleepAnalysis = analysis,
                onNavigateToTracker = {},
                onRefreshSleep = null,
                onSleepCardClick = null,
            )
        }

        onNodeWithTag(HomeTags.FAILED).assertIsDisplayed()
        onNodeWithText("Última noche").assertIsDisplayed()
        onNodeWithTag(HomeSkeletonTags.PILLARS).assertDoesNotExist()
    }

    @Test
    fun `a failed home offers a retry that asks again`() = runComposeUiTest {
        var retried = false

        setContent {
            HomeBoard(
                state = HomeUiState.Failed,
                sleepAnalysis = null,
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
