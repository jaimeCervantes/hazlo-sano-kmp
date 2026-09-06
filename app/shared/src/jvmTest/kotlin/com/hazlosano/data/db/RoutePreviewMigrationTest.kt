package com.hazlosano.data.db

import com.hazlosano.data.movement.SqlDelightRouteRepository
import com.hazlosano.domain.feature.movement.model.MAX_PREVIEW_POINTS
import com.hazlosano.domain.feature.movement.model.Route
import com.hazlosano.domain.feature.movement.model.WayPoint
import com.hazlosano.domain.feature.movement.repository.RouteRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * La silueta de una ruta, para que «Mis rutas» pueda dibujarla.
 *
 * Es el mismo problema que tenían las salidas y por el mismo motivo: `selectAllRoutes` devuelve las
 * filas **sin puntos** a propósito —la lista no puede leer el recorrido completo de todas las
 * rutas—, así que sin una columna no había nada que dibujar.
 */
class RoutePreviewMigrationTest {

    private fun at(latitude: Double, longitude: Double) =
        WayPoint(latitude = latitude, longitude = longitude)

    private val cañada = listOf(
        at(19.4300, -99.1300),
        at(19.4310, -99.1320),
        at(19.4320, -99.1310),
    )

    private fun repository(): RouteRepository =
        SqlDelightRouteRepository(inMemoryHazloSanoDatabase())

    private suspend fun save(repository: RouteRepository, points: List<WayPoint>): Long =
        repository.saveRoute(
            Route(
                name = "Cañada del Molino",
                distance = 5_400.0,
                elevationGain = 210.0,
                points = points,
            ),
        )

    @Test
    fun `an install that came from version one gets the preview column`() {
        val driver = inMemoryDriver()

        driver.migrateFromVersionOne()

        assertTrue(
            "previewPoints" in driver.columnsOf("RouteEntity"),
            "una instalación que se actualizó no tiene dónde guardar la silueta de una ruta",
        )
    }

    @Test
    fun `a fresh install and an upgraded one agree on the route table`() {
        val fresh = inMemoryDriver().also { HazloSanoDatabase.Schema.create(it) }
        val upgraded = inMemoryDriver().also { it.migrateFromVersionOne() }

        assertEquals(fresh.columnsOf("RouteEntity"), upgraded.columnsOf("RouteEntity"))
    }

    /** Lo que hace posible la lista: la fila trae su forma sin cargar el recorrido. */
    @Test
    fun `the list gets a silhouette without loading the route`() = runTest {
        val repository = repository()
        save(repository, cañada)

        val listed = repository.getAllRoutes().first().single()

        assertTrue(listed.points.isEmpty(), "la lista cargó el recorrido completo")
        assertEquals(
            cañada.map { it.latitude to it.longitude },
            listed.previewPoints.map { it.latitude to it.longitude },
        )
    }

    /** Una ruta larga se guarda muestreada: la columna no puede crecer sin límite. */
    @Test
    fun `a long route is stored sampled`() = runTest {
        val repository = repository()
        val long = List(2_000) { at(19.43 + it * 0.00001, -99.13 + it * 0.00001) }

        save(repository, long)

        val listed = repository.getAllRoutes().first().single()
        assertTrue(
            listed.previewPoints.size <= MAX_PREVIEW_POINTS,
            "se guardaron ${listed.previewPoints.size} puntos de silueta",
        )
        assertTrue(listed.previewPoints.size >= 2, "una ruta larga tiene forma que dibujar")
    }

    /** Abrir la ruta sigue trayendo el recorrido entero, que es lo que el mapa necesita. */
    @Test
    fun `opening a route still brings every point`() = runTest {
        val repository = repository()
        val id = save(repository, cañada)

        val opened = repository.getRouteWithPoints(id).first()!!

        assertEquals(3, opened.points.size)
    }

    /** Una ruta sin puntos no tiene forma, y no se inventa ninguna. */
    @Test
    fun `a route with no points has no silhouette`() = runTest {
        val repository = repository()
        save(repository, emptyList())

        assertTrue(repository.getAllRoutes().first().single().previewPoints.isEmpty())
    }
}
