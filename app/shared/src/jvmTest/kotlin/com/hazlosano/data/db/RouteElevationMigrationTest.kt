package com.hazlosano.data.db

import app.cash.sqldelight.db.SqlDriver
import com.hazlosano.data.movement.SqlDelightRouteRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * La migración que deja de afirmar que una ruta sin altitudes es llana.
 *
 * Tiene dos puntos y los dos importan. El primero es el relleno: el 0.0 guardado vuelve a ser NULL
 * sólo donde la ruta no tiene ningún punto con altitud, que es la misma pregunta que hacía
 * `calculateStats`, hecha ahora sobre lo que ya estaba guardado.
 *
 * El segundo es la ruta en sí. Reconstruir `RouteEntity` significa soltarla, y `RoutePointEntity` la
 * referencia con ON DELETE CASCADE: con claves foráneas activadas, el DELETE implícito de SQLite se
 * llevaría por delante **todos los puntos de todas las rutas**. Este proyecto no las activa en
 * ningún driver, y eso es justo lo que se comprueba aquí en vez de darlo por razonado — igual que
 * `MovementSessionMigrationTest` hizo con las sesiones.
 */
class RouteElevationMigrationTest {

    @Test
    fun `a route with no altitudes stops claiming to be flat`() = runTest {
        val driver = inMemoryDriver()
        driver.createRouteSchemaWithNonNullElevation()
        driver.insertRouteTheOldWay(id = 1, name = "Sin altitudes", elevationGain = 0.0)
        driver.insertPoint(routeId = 1, seq = 0, altitude = null)
        driver.insertPoint(routeId = 1, seq = 1, altitude = null)

        driver.migrateFromVersionOne()

        val route = SqlDelightRouteRepository(HazloSanoDatabase(driver))
            .getRouteWithPoints(1).first()
        assertNull(assertNotNull(route).elevationGain)
    }

    @Test
    fun `a route that really is flat keeps its zero`() = runTest {
        // Cero medido y cero por no haber medido no son lo mismo, y la migración no puede
        // confundirlos: si algún punto trae altitud, hubo medición.
        val driver = inMemoryDriver()
        driver.createRouteSchemaWithNonNullElevation()
        driver.insertRouteTheOldWay(id = 1, name = "Llana de verdad", elevationGain = 0.0)
        driver.insertPoint(routeId = 1, seq = 0, altitude = 2_200.0)
        driver.insertPoint(routeId = 1, seq = 1, altitude = 2_200.0)

        driver.migrateFromVersionOne()

        val route = SqlDelightRouteRepository(HazloSanoDatabase(driver)).getRouteWithPoints(1).first()
        assertEquals(0.0, assertNotNull(assertNotNull(route).elevationGain), 0.001)
    }

    @Test
    fun `a measured climb survives untouched`() = runTest {
        val driver = inMemoryDriver()
        driver.createRouteSchemaWithNonNullElevation()
        driver.insertRouteTheOldWay(id = 1, name = "Subida al cerro", elevationGain = 350.0)
        driver.insertPoint(routeId = 1, seq = 0, altitude = 2_200.0)
        driver.insertPoint(routeId = 1, seq = 1, altitude = 2_550.0)

        driver.migrateFromVersionOne()

        val route = SqlDelightRouteRepository(HazloSanoDatabase(driver)).getRouteWithPoints(1).first()
        assertEquals(350.0, assertNotNull(assertNotNull(route).elevationGain), 0.001)
    }

    @Test
    fun `rebuilding the routes table does not take the points with it`() = runTest {
        val driver = inMemoryDriver()
        driver.createRouteSchemaWithNonNullElevation()
        driver.insertRouteTheOldWay(id = 1, name = "Con puntos", elevationGain = 120.0)
        repeat(5) { driver.insertPoint(routeId = 1, seq = it.toLong(), altitude = 2_200.0 + it) }

        driver.migrateFromVersionOne()

        val route = SqlDelightRouteRepository(HazloSanoDatabase(driver)).getRouteWithPoints(1).first()
        assertEquals(
            5,
            assertNotNull(route).points.size,
            "dropping RouteEntity took the points of every route with it",
        )
    }

    @Test
    fun `the name distance and fingerprint come across unchanged`() = runTest {
        val driver = inMemoryDriver()
        driver.createRouteSchemaWithNonNullElevation()
        driver.insertRouteTheOldWay(
            id = 1,
            name = "Cañón del Sumidero",
            elevationGain = 350.0,
            distance = 8_420.0,
            fingerprint = "8420-350-16.75--93.08-16.76--93.07",
        )
        driver.insertPoint(routeId = 1, seq = 0, altitude = 520.0)

        driver.migrateFromVersionOne()

        val route = assertNotNull(
            SqlDelightRouteRepository(HazloSanoDatabase(driver)).getRouteWithPoints(1).first(),
        )
        assertEquals("Cañón del Sumidero", route.name)
        assertEquals(8_420.0, route.distance, 0.001)
        assertEquals("8420-350-16.75--93.08-16.76--93.07", route.fingerprint)
    }

    @Test
    fun `several routes are backfilled independently of each other`() = runTest {
        val driver = inMemoryDriver()
        driver.createRouteSchemaWithNonNullElevation()
        driver.insertRouteTheOldWay(id = 1, name = "Sin altitudes", elevationGain = 0.0)
        driver.insertPoint(routeId = 1, seq = 0, altitude = null)
        driver.insertRouteTheOldWay(id = 2, name = "Medida", elevationGain = 80.0)
        driver.insertPoint(routeId = 2, seq = 0, altitude = 2_200.0)

        driver.migrateFromVersionOne()

        val repository = SqlDelightRouteRepository(HazloSanoDatabase(driver))
        assertNull(assertNotNull(repository.getRouteWithPoints(1).first()).elevationGain)
        assertEquals(
            80.0,
            assertNotNull(assertNotNull(repository.getRouteWithPoints(2).first()).elevationGain),
            0.001,
        )
    }

    @Test
    fun `a route stored with no points at all is left unknown`() = runTest {
        val driver = inMemoryDriver()
        driver.createRouteSchemaWithNonNullElevation()
        driver.insertRouteTheOldWay(id = 1, name = "Vacía", elevationGain = 0.0)

        driver.migrateFromVersionOne()

        val route = SqlDelightRouteRepository(HazloSanoDatabase(driver)).getRouteWithPoints(1).first()
        assertNull(assertNotNull(route).elevationGain)
    }

    @Test
    fun `a database that never had routes still gets the nullable column`() = runTest {
        val driver = inMemoryDriver()

        driver.migrateFromVersionOne()

        // Guardar una ruta con desnivel desconocido es lo que prueba que la columna acepta nulos.
        val repository = SqlDelightRouteRepository(HazloSanoDatabase(driver))
        repository.saveRoute(
            com.hazlosano.domain.feature.movement.model.Route(
                name = "Nueva",
                distance = 100.0,
                elevationGain = null,
                points = listOf(
                    com.hazlosano.domain.feature.movement.model.WayPoint(19.43, -99.13),
                    com.hazlosano.domain.feature.movement.model.WayPoint(19.44, -99.13),
                ),
            ),
        )

        assertNull(repository.getAllRoutes().first().single().elevationGain)
    }
}

/** RouteEntity como era mientras el desnivel no podía ser nulo. */
private fun SqlDriver.createRouteSchemaWithNonNullElevation() {
    exec(
        """
        CREATE TABLE RouteEntity (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            name TEXT NOT NULL,
            distance REAL NOT NULL,
            elevationGain REAL NOT NULL,
            fingerprint TEXT,
            createdAt INTEGER NOT NULL
        )
        """.trimIndent(),
    )
    exec(
        """
        CREATE TABLE RoutePointEntity (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            routeId INTEGER NOT NULL,
            seq INTEGER NOT NULL,
            latitude REAL NOT NULL,
            longitude REAL NOT NULL,
            altitude REAL,
            timestamp INTEGER,
            FOREIGN KEY(routeId) REFERENCES RouteEntity(id) ON DELETE CASCADE
        )
        """.trimIndent(),
    )
}

private fun SqlDriver.insertRouteTheOldWay(
    id: Long,
    name: String,
    elevationGain: Double,
    distance: Double = 1_000.0,
    fingerprint: String = "fp-$id",
) {
    exec(
        """
        INSERT INTO RouteEntity(id, name, distance, elevationGain, fingerprint, createdAt)
        VALUES ($id, '$name', $distance, $elevationGain, '$fingerprint', 1735689600000)
        """.trimIndent(),
    )
}

private fun SqlDriver.insertPoint(routeId: Long, seq: Long, altitude: Double?) {
    val altitudeSql = altitude?.toString() ?: "NULL"
    exec(
        """
        INSERT INTO RoutePointEntity(routeId, seq, latitude, longitude, altitude, timestamp)
        VALUES ($routeId, $seq, 19.43, -99.13, $altitudeSql, NULL)
        """.trimIndent(),
    )
}
