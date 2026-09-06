package com.hazlosano.feature.movement.history.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runComposeUiTest
import com.hazlosano.core.ui.theme.HazloSanoTheme
import com.hazlosano.domain.feature.movement.model.UserLocation
import com.hazlosano.feature.movement.history.presentation.SessionListItem
import com.hazlosano.feature.movement.presentation.TrackSilhouette
import com.hazlosano.feature.movement.presentation.toSilhouette
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Que cada salida de la lista tenga su sitio para la silueta, la conozca o no.
 *
 * Se afirma por `testTag` y no por lo que se ve dibujado: un `Canvas` no expone nodos que un test de
 * Compose pueda mirar. Lo que se puede comprobar aquí es que el hueco existe y que cada salida tiene
 * el suyo; que el trazo sea el correcto lo cuida `TrackSilhouetteTest`, sobre la geometría.
 */
@OptIn(ExperimentalTestApi::class)
class OutingSilhouetteTest {

    private fun at(latitude: Double, longitude: Double) =
        UserLocation(latitude = latitude, longitude = longitude, timestamp = 0)

    private val aRealTrack = listOf(
        at(19.4300, -99.1300),
        at(19.4310, -99.1320),
        at(19.4320, -99.1310),
    )

    @Test
    fun `a track that goes somewhere is drawable`() {
        assertTrue(aRealTrack.toSilhouette().isDrawable)
    }

    @Test
    fun `an outing recorded before the silhouette was stored is not drawable`() {
        assertFalse(TrackSilhouette(emptyList()).isDrawable)
    }

    @Test
    fun `every outing in the list gets its own place for a silhouette`() = runComposeUiTest {
        setContent {
            HazloSanoTheme {
                OutingList(
                    items = listOf(
                        outing(id = 1, silhouette = aRealTrack.toSilhouette()),
                        // La segunda no guardó silueta: enseña su hueco, no un recuadro vacío.
                        outing(id = 2, silhouette = TrackSilhouette(emptyList())),
                    ),
                    onOpenSession = {},
                )
            }
        }

        // Árbol sin fusionar: la tarjeta entera es clicable, así que Compose funde la semántica de
        // todo lo que hay dentro en un solo nodo y la etiqueta del hueco desaparece del árbol normal.
        onNodeWithTag(MovementHistoryTags.silhouette(1), useUnmergedTree = true).assertIsDisplayed()
        onNodeWithTag(MovementHistoryTags.silhouette(2), useUnmergedTree = true).assertIsDisplayed()
    }

    private fun outing(id: Long, silhouette: TrackSilhouette) = SessionListItem(
        id = id,
        name = "Salida $id",
        dateLabel = "24 jul 2026 · 07:15",
        distanceLabel = "390 m",
        durationLabel = "10:00",
        silhouette = silhouette,
    )
}
