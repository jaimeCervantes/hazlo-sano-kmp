package com.hazlosano.feature.movement.tracker.presentation

import com.hazlosano.domain.feature.movement.model.RecordingState
import com.hazlosano.domain.feature.movement.model.UserLocation
import com.hazlosano.domain.feature.movement.repository.LocationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Las cifras de la salida **en curso**.
 *
 * El tracker enseñaba dos —distancia y tiempo— mientras la salida ya terminada enseñaba ocho, y las
 * otras seis ya estaban calculadas. Lo que se afirma aquí es que el directo las saca del **mismo**
 * cálculo que el detalle, y que hereda con él la regla que este pilar lleva cuatro slices
 * defendiendo: **una cifra que no se midió no se enseña, y no se enseña un cero en su lugar.**
 *
 * Spec: `features/pulido_de_movimiento.feature`, slice 6.
 */
class LiveStatsTest {

    private val locations = MutableSharedFlow<UserLocation>(replay = 1)

    private val locationRepository = object : LocationRepository {
        override fun getLocationUpdates(): Flow<UserLocation> = locations
    }

    private lateinit var controller: FakeRecordingController

    private fun tracker(): TrackerViewModel {
        controller = FakeRecordingController()
        return TrackerViewModel(
            locationRepository = locationRepository,
            recordingController = controller,
            routes = FakeRoutes(emptyList()),
        )
    }

    /** Un tramo que avanza de verdad: ~11 m cada dos segundos son unos 20 km/h. */
    private fun leg(count: Int, withAltitude: Boolean = false, stuckAltitude: Boolean = false) =
        List(count) { index ->
            UserLocation(
                latitude = 19.4300 + index * 0.0001,
                longitude = -99.1300,
                altitude = when {
                    stuckAltitude -> 2_200.0
                    withAltitude -> 2_200.0 + index
                    else -> null
                },
                accuracy = 4f,
                verticalAccuracy = 2f,
                timestamp = index * 2_000L,
            )
        }

    private fun FakeRecordingController.recording(points: List<UserLocation>, elapsed: Long) {
        publish(
            RecordingState(
                isRecording = true,
                startedAtMillis = 0,
                traveledPoints = points,
                elapsedSeconds = elapsed,
                lastReadingAtMillis = points.lastOrNull()?.timestamp,
            ),
        )
    }

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /**
     * Una salida que acaba de empezar no ha medido nada todavía, y lo dice callándose.
     *
     * Es el caso que más tienta a poner ceros: el ritmo «0:00 /km» y el tiempo en movimiento
     * «00:00» se leen como cifras y son afirmaciones que la grabación no ha hecho.
     */
    @Test
    fun `a recording with nothing measured yet reports nothing`() = runTest {
        val model = tracker()
        model.liveStats.first()

        controller.recording(points = emptyList(), elapsed = 0)

        val stats = model.liveStats.value
        assertNull(stats.avgPace, "el ritmo salió de la nada")
        assertNull(stats.movingTime, "el tiempo en movimiento salió de la nada")
        assertNull(stats.totalAscent)
    }

    @Test
    fun `a recording that is going somewhere reports pace and moving time`() = runTest {
        val model = tracker()
        model.liveStats.first()

        controller.recording(points = leg(count = 60), elapsed = 120)

        val stats = model.liveStats.value
        assertNotNull(stats.avgPace, "no hubo ritmo con 650 m recorridos")
        val movingTime = assertNotNull(stats.movingTime)
        assertTrue(movingTime > 0, "no contó nada de tiempo en movimiento")
    }

    /**
     * Sin altitud en las lecturas no hay desnivel, ni siquiera cero.
     *
     * Un cero afirmaría terreno llano, que es exactamente lo que una grabación sin altímetro no
     * puede decir.
     */
    @Test
    fun `a recording with no altitude has no climb rather than a flat one`() = runTest {
        val model = tracker()
        model.liveStats.first()

        controller.recording(points = leg(count = 60, withAltitude = false), elapsed = 120)

        assertNull(model.liveStats.value.totalAscent)
        assertNull(model.liveStats.value.maxAltitude)
    }

    /**
     * Y la regla de B4 vale igual en marcha que al terminar: **una altitud que se quedó pegada calla
     * el desnivel entero de la salida.**
     *
     * Es la razón de que el directo use el mismo cálculo en vez de uno propio. El tracker no puede
     * enseñar un ascenso que el detalle luego se niegue a dar: sería la misma salida contada de dos
     * maneras, y la de la pantalla grande es la que el usuario recuerda.
     */
    @Test
    fun `an altitude that got stuck silences the climb while still recording`() = runTest {
        val model = tracker()
        model.liveStats.first()

        // 60 lecturas cada 2 s con el mismo valor exacto: 118 s pegada, muy por encima del umbral.
        controller.recording(points = leg(count = 60, stuckAltitude = true), elapsed = 120)

        val stats = model.liveStats.value
        assertNull(stats.totalAscent, "enseñó un desnivel que el sensor no midió")
        assertNull(stats.maxAltitude)
        // Lo que no depende de la altitud sigue estando: callarse una cosa no es callarse todo.
        assertNotNull(stats.movingTime)
    }

    /**
     * El caso de control, y sin él las dos pruebas de arriba no valdrían nada.
     *
     * Con altitud que de verdad cambia, el desnivel **sí** sale. Es lo que demuestra que los `null`
     * de las otras dos vienen de la regla —sin altímetro, y altitud pegada— y no de que el cálculo
     * en directo devuelva nulo siempre, que las habría dejado pasando por el motivo equivocado.
     */
    @Test
    fun `a recording with real altitude does report a climb`() = runTest {
        val model = tracker()
        model.liveStats.first()

        controller.recording(points = leg(count = 60, withAltitude = true), elapsed = 120)

        val stats = model.liveStats.value
        val ascent = assertNotNull(stats.totalAscent, "no midió el desnivel de una subida real")
        assertTrue(ascent > 0.0, "la subida salió en $ascent m")
        assertNotNull(stats.maxAltitude)
    }

    /** Las cifras del directo son las de la salida entera, no las del último tramo. */
    @Test
    fun `the figures describe the whole outing so far`() = runTest {
        val model = tracker()
        model.liveStats.first()
        controller.recording(points = leg(count = 30), elapsed = 60)
        val early = assertNotNull(model.liveStats.value.movingTime)

        controller.recording(points = leg(count = 60), elapsed = 120)

        val later = assertNotNull(model.liveStats.value.movingTime)
        assertTrue(later > early, "el tiempo en movimiento no creció con la salida")
        assertEquals(true, model.liveStats.value.avgPace != null)
    }
}
