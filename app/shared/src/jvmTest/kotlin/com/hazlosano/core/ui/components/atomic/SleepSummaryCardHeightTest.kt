package com.hazlosano.core.ui.components.atomic

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import com.hazlosano.domain.model.SleepAnalysis
import com.hazlosano.domain.model.SleepPhase
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * La tarjeta de anoche mide siempre lo mismo.
 *
 * Spec: `features/el_inicio_no_brinca.feature`, slice 1.
 *
 * Tres de sus filas son opcionales, y `App.kt` relee el análisis a los 3 y a los 8 segundos de abrir
 * porque el detector de Android escribe sus segmentos con retraso. Sin esta prueba, cualquiera de
 * esas relecturas puede hacer crecer la tarjeta y empujar la pantalla entera hacia abajo.
 *
 * **Esta prueba es la que fija el número** de `SleepSummaryCardDefaults.height`. No se eligió a ojo:
 * se compuso la variante más rica y se leyó lo que medía.
 */
@OptIn(ExperimentalTestApi::class)
class SleepSummaryCardHeightTest {

    private val periodStart = 1_788_534_000_000L
    private val periodEnd = 1_788_570_000_000L

    /** Una noche que nadie registró: el caso normal la primera vez que se abre el app. */
    private val empty = SleepAnalysis(
        totalSessions = 0,
        totalDurationMillis = 0L,
        efficiency = 0f,
        periodStart = periodStart,
        periodEnd = periodEnd,
    )

    private val withTimes = empty.copy(
        totalSessions = 2,
        totalDurationMillis = 7 * 60 * 60 * 1000L,
        efficiency = 0.91f,
        firstSleepStart = periodStart,
        lastSleepEnd = periodEnd,
    )

    private val withAccuracy = withTimes.copy(averageConfidence = 0.82f)

    private val withEverything = withAccuracy.copy(
        phaseBreakdown = mapOf(
            SleepPhase.LIGHT to 4 * 60 * 60 * 1000L,
            SleepPhase.DEEP to 2 * 60 * 60 * 1000L,
            SleepPhase.REM to 60 * 60 * 1000L,
        ),
    )

    /**
     * Lo que el app produce hoy: una noche sin registrar, y una noche registrada.
     *
     * Las otras dos variantes existen en el código pero **no pueden llegar** — ver
     * [the rows that cannot happen today would make it grow].
     */
    private val variants = listOf(
        "vacia" to empty,
        "con_horas" to withTimes,
    )

    @Test
    fun `it measures the same whatever the analysis brings`() = runComposeUiTest {
        setContent {
            // Con scroll, y no un `Column` a secas: sin él la columna reparte el alto disponible y
            // a las últimas tarjetas no les queda sitio, así que la prueba mediría el recorte del
            // contenedor en vez de la tarjeta.
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
            ) {
                variants.forEach { (tag, analysis) ->
                    SleepSummaryCard(
                        analysis = analysis,
                        accentColor = Color.White,
                        modifier = Modifier.testTag(tag),
                    )
                }
            }
        }

        // Se miden las cuatro y se comparan a la vez, en vez de afirmar una por una: así el fallo
        // dice **qué variante** se salió y cuánto mide, que es lo que hace falta para poner el
        // número. Con `assertHeightIsEqualTo` en un bucle, el mensaje no dice de cuál hablaba.
        val measured = variants.associate { (tag, _) ->
            tag to onNodeWithTag(tag).getUnclippedBoundsInRoot().height
        }

        assertEquals(
            variants.associate { (tag, _) -> tag to SleepSummaryCardDefaults.height },
            measured,
            "la tarjeta no mide lo mismo con todos los análisis",
        )
    }

    /**
     * Las dos filas que hoy no pueden llegar, y lo que costaría reservarles sitio.
     *
     * **La línea de precisión** sólo se pinta con `averageConfidence < 1`, y `SleepReceiver` guarda
     * `confidence = 1.0f` fijo. **El desglose de fases** sólo se pinta con LIGHT, DEEP o REM, que
     * salen de unos `STATUS_` (3, 4 y 5) que el Sleep API de Android no devuelve: no reporta etapas
     * de sueño. Reservarles sitio dejaba la tarjeta con un tercio de aire debajo, probado en un
     * teléfono.
     *
     * Se afirman sus alturas igualmente para que **el día que alguien arregle ese mapeo, esta prueba
     * le diga que la tarjeta crece** y pueda decidirlo a sabiendas en vez de descubrirlo mirando.
     */
    @Test
    fun `the rows that cannot happen today would make it grow`() = runComposeUiTest {
        setContent {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
            ) {
                SleepSummaryCard(
                    analysis = withAccuracy,
                    accentColor = Color.White,
                    modifier = Modifier.testTag("con_precision"),
                )
                SleepSummaryCard(
                    analysis = withEverything,
                    accentColor = Color.White,
                    modifier = Modifier.testTag("con_todo"),
                )
            }
        }

        assertEquals(
            mapOf("con_precision" to 260.dp, "con_todo" to 308.dp),
            mapOf(
                "con_precision" to onNodeWithTag("con_precision").getUnclippedBoundsInRoot().height,
                "con_todo" to onNodeWithTag("con_todo").getUnclippedBoundsInRoot().height,
            ),
            "cambió lo que costaría reservar sitio para la precisión o las fases",
        )
    }

    /**
     * Y el hueco que espera por ella mide exactamente eso.
     *
     * Es la misma constante leída dos veces, así que la prueba no puede fallar por desincronización
     * — puede fallar porque alguien le ponga otro alto al hueco, que es justo lo que hay que impedir.
     */
    @Test
    fun `the gap that waits for it measures the same`() = runComposeUiTest {
        mainClock.autoAdvance = false
        setContent {
            HazloSkeleton(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(SleepSummaryCardDefaults.height)
                    .testTag("hueco"),
            )
        }

        onNodeWithTag("hueco").assertHeightIsEqualTo(SleepSummaryCardDefaults.height)
    }
}
