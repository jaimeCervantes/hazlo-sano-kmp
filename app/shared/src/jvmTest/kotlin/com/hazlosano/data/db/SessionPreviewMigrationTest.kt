package com.hazlosano.data.db

import com.hazlosano.data.movement.SqlDelightMovementSessionRepository
import com.hazlosano.domain.feature.movement.model.MovementSession
import com.hazlosano.domain.feature.movement.model.UserLocation
import com.hazlosano.domain.feature.movement.repository.MovementSessionRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * La silueta de una salida, que llevaba desde el slice 11 calculándose y muriendo en memoria.
 *
 * `SaveSessionUseCase` muestreaba la traza a 200 puntos y la ponía en `MovementSession`, pero no
 * había columna donde guardarla y `toDomain` la devolvía siempre vacía. Lo que estos tests cuidan es
 * que el viaje entero —calcular, guardar, releer— llegue hasta la lista.
 */
class SessionPreviewMigrationTest {

    private fun at(latitude: Double, longitude: Double) =
        UserLocation(latitude = latitude, longitude = longitude, timestamp = 0)

    private val recordedPath = listOf(
        at(19.4300, -99.1300),
        at(19.4310, -99.1320),
        at(19.4320, -99.1310),
    )

    private suspend fun save(
        repository: MovementSessionRepository,
        preview: List<UserLocation>,
        points: List<UserLocation> = recordedPath,
    ) {
        repository.saveSession(
            MovementSession(
                routeId = null,
                name = "Salida de prueba",
                date = 1_784_877_300_000L,
                elapsedTime = 600,
                distanceTraveled = 390.0,
                previewPoints = preview,
            ),
            points,
        )
    }

    @Test
    fun `an install that came from version one gets the preview column`() {
        val driver = inMemoryDriver()

        driver.migrateFromVersionOne()

        assertTrue(
            "previewPoints" in driver.columnsOf("MovementSessionEntity"),
            "una instalación que se actualizó no tiene dónde guardar la silueta",
        )
    }

    @Test
    fun `a fresh install and an upgraded one agree on the session table`() {
        val fresh = inMemoryDriver().also { HazloSanoDatabase.Schema.create(it) }
        val upgraded = inMemoryDriver().also { it.migrateFromVersionOne() }

        assertEquals(
            fresh.columnsOf("MovementSessionEntity"),
            upgraded.columnsOf("MovementSessionEntity"),
        )
    }

    @Test
    fun `the silhouette survives being stored and read back`() = runTest {
        val repository: MovementSessionRepository =
            SqlDelightMovementSessionRepository(inMemoryHazloSanoDatabase())

        save(repository, preview = recordedPath)

        val stored = repository.getAllSessions().first().single()
        assertEquals(
            recordedPath.map { it.latitude to it.longitude },
            stored.previewPoints.map { it.latitude to it.longitude },
        )
    }

    /**
     * Las salidas grabadas antes de que existiera la columna vuelven sin silueta, y eso es lo
     * correcto: sus puntos están en `MovementPointEntity`, pero muestrearlos aquí sería justamente el
     * «leer todos los puntos de todas las salidas» que la columna viene a evitar. La lista enseña su
     * hueco.
     */
    @Test
    fun `a session recorded before the column came back has no silhouette`() = runTest {
        val repository: MovementSessionRepository =
            SqlDelightMovementSessionRepository(inMemoryHazloSanoDatabase())

        save(repository, preview = emptyList())

        val stored = repository.getAllSessions().first().single()
        assertTrue(stored.previewPoints.isEmpty())
        // Y sus puntos siguen ahí: la salida se puede abrir aunque su silueta no se conozca.
        assertEquals(3, repository.getSessionPoints(stored.id).first().size)
    }
}
