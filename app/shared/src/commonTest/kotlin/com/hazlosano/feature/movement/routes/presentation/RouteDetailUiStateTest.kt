package com.hazlosano.feature.movement.routes.presentation

import com.hazlosano.domain.feature.movement.model.Route
import com.hazlosano.domain.feature.movement.model.WayPoint
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RouteDetailUiStateTest {

    private fun route(
        name: String = "Cañón del Sumidero",
        distance: Double = 8_420.0,
        elevationGain: Double? = 350.0,
        points: List<WayPoint> = listOf(
            WayPoint(16.7500, -93.0800, altitude = 520.0),
            WayPoint(16.7510, -93.0790, altitude = 545.0),
        ),
    ) = Route(
        id = 1,
        name = name,
        distance = distance,
        elevationGain = elevationGain,
        points = points,
    )

    @Test
    fun `a route that no longer exists is missing rather than empty`() {
        assertEquals(RouteDetailUiState.Missing, routeDetail(null))
    }

    @Test
    fun `the figures travel through as they were stored`() {
        val detail = assertIs<RouteDetailUiState.Detail>(routeDetail(route()))

        assertEquals("Cañón del Sumidero", detail.name)
        assertEquals(8_420.0, detail.distanceMeters)
        assertEquals(350.0, detail.elevationGainMeters)
        assertEquals(2, detail.pointCount)
    }

    @Test
    fun `an unknown climb stays unknown instead of becoming a zero`() {
        // Quién decide que un desnivel es desconocido es `calculateStats` al medir, y la migración
        // para lo ya guardado. Aquí sólo se comprueba que la pantalla no lo convierte en un cero
        // por el camino, que es lo que la lista llevaba haciendo.
        val detail = assertIs<RouteDetailUiState.Detail>(
            routeDetail(route(elevationGain = null)),
        )

        assertNull(detail.elevationGainMeters)
    }

    @Test
    fun `a measured zero is passed through as a zero`() {
        val detail = assertIs<RouteDetailUiState.Detail>(routeDetail(route(elevationGain = 0.0)))

        assertEquals(0.0, detail.elevationGainMeters)
    }

    @Test
    fun `the path keeps the order and the coordinates of the route`() {
        val detail = assertIs<RouteDetailUiState.Detail>(routeDetail(route()))

        assertEquals(2, detail.path.size)
        assertEquals(16.7500, detail.path.first().latitude)
        assertEquals(-93.0790, detail.path.last().longitude)
    }

    @Test
    fun `a point with no altitude stays without one on the path`() {
        val detail = assertIs<RouteDetailUiState.Detail>(
            routeDetail(
                route(
                    points = listOf(
                        WayPoint(16.7500, -93.0800, altitude = null),
                        WayPoint(16.7510, -93.0790, altitude = 545.0),
                    ),
                ),
            ),
        )

        assertNull(detail.path.first().altitude)
        assertEquals(545.0, detail.path.last().altitude)
    }

    @Test
    fun `a route with two points can be drawn`() {
        assertTrue(assertIs<RouteDetailUiState.Detail>(routeDetail(route())).hasPath)
    }

    @Test
    fun `a single point is a place and not a track`() {
        val onePoint = route(points = listOf(WayPoint(16.7500, -93.0800)))

        assertFalse(assertIs<RouteDetailUiState.Detail>(routeDetail(onePoint)).hasPath)
    }

    @Test
    fun `a route saved with no points has nothing to draw`() {
        val empty = route(points = emptyList())

        val detail = assertIs<RouteDetailUiState.Detail>(routeDetail(empty))
        assertFalse(detail.hasPath)
        assertEquals(0, detail.pointCount)
    }
}
