package com.hazlosano.data.movement

import com.hazlosano.data.db.HazloSanoDatabase
import com.hazlosano.data.db.inMemoryDriver
import com.hazlosano.data.db.inMemoryHazloSanoDatabase
import com.hazlosano.data.db.migrateFromVersionOne
import com.hazlosano.domain.feature.movement.model.Route
import com.hazlosano.domain.feature.movement.model.WayPoint
import com.hazlosano.domain.feature.movement.model.calculateFingerprint
import com.hazlosano.domain.feature.movement.model.calculateStats
import com.hazlosano.domain.feature.movement.parser.GpxFormat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Integration test against an in-memory SQLite database. */
class SqlDelightRouteRepositoryTest {

    private fun repository(clock: () -> Long = { 1_000L }) =
        SqlDelightRouteRepository(inMemoryHazloSanoDatabase(), now = clock)

    @Test
    fun `a route saves and reads back with its points in order`() = runTest {
        val repository = repository()

        val id = repository.saveRoute(route(name = "Subida al cerro"))

        val read = assertNotNull(repository.getRouteWithPoints(id).first())
        assertEquals("Subida al cerro", read.name)
        assertEquals(3, read.points.size)
        assertEquals(19.4300, read.points.first().latitude, 1e-9)
        assertEquals(19.4320, read.points.last().latitude, 1e-9)
    }

    @Test
    fun `a point with no altitude comes back with none`() = runTest {
        val repository = repository()
        val id = repository.saveRoute(
            route(
                name = "Sin altitud",
                points = listOf(
                    WayPoint(19.43, -99.13, altitude = null, timestamp = null),
                    WayPoint(19.44, -99.13, altitude = 0.0, timestamp = 5L),
                ),
            ),
        )

        val read = assertNotNull(repository.getRouteWithPoints(id).first())

        assertNull(read.points.first().altitude, "a missing altitude came back as a number")
        assertNull(read.points.first().timestamp)
        // Sea level is an altitude and has to survive as one.
        assertEquals(0.0, assertNotNull(read.points.last().altitude), 1e-9)
        assertEquals(5L, read.points.last().timestamp)
    }

    @Test
    fun `the list of routes carries no points`() = runTest {
        // Drawing the list must not read every point of every route.
        val repository = repository()
        repository.saveRoute(route(name = "Una"))

        val listed = repository.getAllRoutes().first().single()

        assertEquals("Una", listed.name)
        assertTrue(listed.points.isEmpty())
        assertTrue(listed.distance > 0.0, "the summary the list draws from was not stored")
    }

    @Test
    fun `saving over a route replaces its points instead of appending them`() = runTest {
        val repository = repository()
        val id = repository.saveRoute(route(name = "Original"))

        repository.saveRoute(
            route(
                name = "Corregida",
                points = listOf(WayPoint(1.0, 2.0), WayPoint(3.0, 4.0)),
            ).copy(id = id),
        )

        val read = assertNotNull(repository.getRouteWithPoints(id).first())
        assertEquals("Corregida", read.name)
        assertEquals(2, read.points.size, "the old points were left behind")
        assertEquals(1, repository.getAllRoutes().first().size)
    }

    @Test
    fun `a route is found by the track it is, not only by what it is called`() = runTest {
        val repository = repository()
        val stored = route(name = "Vuelta al lago")
        val fingerprint = assertNotNull(stored.fingerprint)
        repository.saveRoute(stored)

        val found = assertNotNull(repository.getRouteByFingerprint(fingerprint))

        assertEquals("Vuelta al lago", found.name)
        assertEquals(3, found.points.size)
    }

    @Test
    fun `an empty fingerprint matches nothing`() = runTest {
        // A route with no points has nothing to fingerprint. Matching on blank would collapse
        // every such route into whichever one was stored first.
        val repository = repository()
        repository.saveRoute(route(name = "Sin huella").copy(fingerprint = ""))

        assertNull(repository.getRouteByFingerprint(""))
    }

    @Test
    fun `renaming a route keeps its track`() = runTest {
        val repository = repository()
        val id = repository.saveRoute(route(name = "Nombre viejo"))

        repository.renameRoute(id, "Nombre nuevo")

        val read = assertNotNull(repository.getRouteWithPoints(id).first())
        assertEquals("Nombre nuevo", read.name)
        assertEquals(3, read.points.size)
    }

    @Test
    fun `deleting a route takes its points with it`() = runTest {
        // Foreign keys are enabled on no driver in this project, so the ON DELETE CASCADE cannot
        // be relied on: deleting only the route row would orphan its points forever.
        val database = inMemoryHazloSanoDatabase()
        val repository = SqlDelightRouteRepository(database)
        val id = repository.saveRoute(route(name = "Para borrar"))

        repository.deleteRoute(id)

        assertTrue(repository.getAllRoutes().first().isEmpty())
        assertTrue(database.routeQueries.selectPointsByRoute(id).executeAsList().isEmpty())
    }

    @Test
    fun `a database migrated from version one can store routes`() = runTest {
        val driver = inMemoryDriver()

        driver.migrateFromVersionOne()

        val repository = SqlDelightRouteRepository(HazloSanoDatabase(driver))
        val id = repository.saveRoute(route(name = "Tras migrar"))
        assertEquals("Tras migrar", assertNotNull(repository.getRouteWithPoints(id).first()).name)
    }

    @Test
    fun `a route imported from GPX survives being stored`() = runTest {
        val repository = repository()
        val imported = GpxFormat.parse(
            """
            <gpx><trk><name>Del reloj</name><trkseg>
              <trkpt lat="19.43" lon="-99.13"><ele>2200.5</ele><time>2026-08-09T06:00:00Z</time></trkpt>
              <trkpt lat="19.44" lon="-99.14"><ele>2215.0</ele></trkpt>
            </trkseg></trk></gpx>
            """.trimIndent(),
        )

        val id = repository.saveRoute(imported)

        val read = assertNotNull(repository.getRouteWithPoints(id).first())
        assertEquals("Del reloj", read.name)
        assertEquals(imported.points, read.points)
    }

    private fun route(
        name: String,
        points: List<WayPoint> = listOf(
            WayPoint(19.4300, -99.1300, altitude = 2_200.0, timestamp = 1_000L),
            WayPoint(19.4310, -99.1300, altitude = 2_210.0, timestamp = 3_000L),
            WayPoint(19.4320, -99.1300, altitude = 2_220.0, timestamp = 5_000L),
        ),
    ): Route {
        val (distance, elevationGain) = points.calculateStats()
        return Route(
            name = name,
            distance = distance,
            elevationGain = elevationGain,
            points = points,
        ).let { it.copy(fingerprint = it.calculateFingerprint()) }
    }
}
