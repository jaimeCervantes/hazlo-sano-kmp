package com.hazlosano.domain.feature.movement.usecase

import com.hazlosano.domain.feature.movement.model.Route
import com.hazlosano.domain.feature.movement.model.WayPoint
import com.hazlosano.domain.feature.movement.parser.GpxParser
import com.hazlosano.domain.feature.movement.repository.RouteRepository
import kotlinx.coroutines.CoroutineDispatcher
// El de kotlinx y no el implícito de Java: `Runnable` a secas resuelve en JVM y no existe en los
// demás targets, así que el test compilaba sólo en la mitad del proyecto.
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.coroutines.CoroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ImportRouteUseCaseTest {

    /**
     * Corre el bloque en el momento, pero deja constancia de que pasó por aquí.
     *
     * Comprobar "está en otro hilo" no serviría en los cinco targets —en web no hay más que uno—,
     * así que lo que se comprueba es lo que sí es común: que la lectura del archivo sale por el
     * dispatcher de trabajo y no se queda en el del llamante.
     */
    private class RecordingDispatcher : CoroutineDispatcher() {
        var dispatches: Int = 0

        override fun dispatch(context: CoroutineContext, block: Runnable) {
            dispatches++
            block.run()
        }
    }

    @Test
    fun `reading the file leaves the caller's dispatcher`() = runTest {
        // En Android el llamante es `viewModelScope`, o sea el hilo principal, y un GPX de miles de
        // puntos parseado ahí es un tirón en la interfaz.
        val dispatcher = RecordingDispatcher()
        val useCase = ImportRouteUseCase(
            repository = FakeRouteRepository(),
            parser = StaticGpxParser(route(name = "Ruta Test")),
            workDispatcher = dispatcher,
        )

        useCase(ByteArray(0))

        assertTrue(dispatcher.dispatches > 0, "el archivo se leyó en el hilo de quien llamó")
    }

    @Test
    fun `when a route is imported then stats are calculated and route is saved`() = runTest {
        val repository = FakeRouteRepository()
        val useCase = ImportRouteUseCase(
            repository = repository,
            parser = StaticGpxParser(route(name = "Ruta Test"))
        )

        val result = useCase(ByteArray(0))

        assertTrue(result is ImportRouteUseCase.Result.Success)
        assertEquals(1, repository.savedRoutes.size)
        val savedRoute = repository.savedRoutes.single()
        assertEquals("Ruta Test", savedRoute.name)
        assertTrue(savedRoute.distance > 1_000.0)
        assertEquals(10.0, assertNotNull(savedRoute.elevationGain), 0.1)
        assertTrue(savedRoute.fingerprint?.isNotBlank() == true)
    }

    @Test
    fun `when a route with the same name exists then AlreadyExists is returned`() = runTest {
        val existingRoute = route(id = 1, name = "Repetida")
        val repository = FakeRouteRepository(existingRouteByName = existingRoute)
        val useCase = ImportRouteUseCase(repository, StaticGpxParser(route(name = "Repetida")))

        val result = useCase(ByteArray(0))

        assertTrue(result is ImportRouteUseCase.Result.AlreadyExists)
        assertEquals(existingRoute, result.existingRoute)
        assertTrue(repository.savedRoutes.isEmpty())
    }

    @Test
    fun `when force overwrite is enabled then existing route id is preserved`() = runTest {
        val existingRoute = route(id = 10, name = "Repetida")
        val repository = FakeRouteRepository(existingRouteByName = existingRoute)
        val useCase = ImportRouteUseCase(repository, StaticGpxParser(route(name = "Repetida")))

        val result = useCase(ByteArray(0), forceOverwrite = true)

        assertTrue(result is ImportRouteUseCase.Result.Success)
        assertEquals(1, repository.savedRoutes.size)
        assertEquals(10L, repository.savedRoutes.single().id)
    }

    @Test
    fun `when fingerprint matches another route then duplicate is detected`() = runTest {
        val existingRoute = route(id = 5, name = "Ruta Antigua")
        val repository = FakeRouteRepository(existingRouteByFingerprint = existingRoute)
        val useCase = ImportRouteUseCase(repository, StaticGpxParser(route(name = "Nuevo Nombre")))

        val result = useCase(ByteArray(0))

        assertTrue(result is ImportRouteUseCase.Result.AlreadyExists)
        assertEquals("Ruta Antigua", result.existingRoute.name)
        assertTrue(repository.savedRoutes.isEmpty())
    }

    private class StaticGpxParser(private val parsedRoute: Route) : GpxParser {
        override fun parse(data: ByteArray): Route = parsedRoute
    }

    private class FakeRouteRepository(
        private val existingRouteByName: Route? = null,
        private val existingRouteByFingerprint: Route? = null
    ) : RouteRepository {
        val savedRoutes = mutableListOf<Route>()

        override fun getAllRoutes(): Flow<List<Route>> = flowOf(savedRoutes)

        override fun getRouteWithPoints(routeId: Long): Flow<Route?> {
            return flowOf(savedRoutes.firstOrNull { it.id == routeId })
        }

        override suspend fun getRouteByName(name: String): Route? = existingRouteByName

        override suspend fun getRouteByFingerprint(fingerprint: String): Route? {
            return existingRouteByFingerprint
        }

        override suspend fun saveRoute(route: Route): Long {
            savedRoutes += route
            return if (route.id != 0L) route.id else savedRoutes.size.toLong()
        }

        override suspend fun renameRoute(routeId: Long, name: String) {
            val index = savedRoutes.indexOfFirst { it.id == routeId }
            if (index >= 0) savedRoutes[index] = savedRoutes[index].copy(name = name)
        }

        override suspend fun deleteRoute(routeId: Long) {
            savedRoutes.removeAll { it.id == routeId }
        }
    }

    private fun route(
        id: Long = 0,
        name: String,
        points: List<WayPoint> = listOf(
            WayPoint(latitude = 40.0, longitude = -3.0, altitude = 100.0),
            WayPoint(latitude = 40.01, longitude = -3.0, altitude = 110.0)
        )
    ): Route {
        return Route(
            id = id,
            name = name,
            distance = 0.0,
            elevationGain = 0.0,
            points = points
        )
    }
}
