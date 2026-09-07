package com.hazlosano.data.movement.trace

import com.hazlosano.domain.feature.movement.filter.LocationFilter
import com.hazlosano.domain.feature.movement.filter.LocationFilterResult
import com.hazlosano.domain.feature.movement.model.RecordingState
import com.hazlosano.domain.feature.movement.model.RouteDeviation
import com.hazlosano.domain.feature.movement.model.RouteStanding
import com.hazlosano.domain.feature.movement.model.UserLocation
import com.hazlosano.domain.feature.movement.model.WayPoint
import com.hazlosano.domain.feature.movement.model.distanceToRouteMeters
import com.hazlosano.domain.feature.movement.model.isMoving
import com.hazlosano.domain.feature.movement.model.observed
import com.hazlosano.domain.feature.movement.model.recorded
import com.hazlosano.domain.feature.movement.model.started
import java.io.File
import kotlin.test.Test

/**
 * Una salida siguiendo una ruta de verdad, replayada contra el aviso de desvio.
 *
 * `RouteDeviationCalibration` mide el ruido que el umbral tiene que dejar pasar, usando el camino
 * aceptado de cada traza como su propia ruta. Eso responde a la mitad de la pregunta: los falsos
 * positivos. La otra mitad —si un desvio real se detecta, y a tiempo— necesita dos trazas: **la ruta
 * sale de una salida y el seguimiento de otra**.
 *
 * Se pasan por propiedad de sistema, porque cual es la ruta y cual el seguimiento no se puede
 * adivinar de un directorio:
 *
 *     .\gradlew.bat :app:shared:jvmTest --tests "*RouteFollowingReplay" ^
 *         -Droute=1788749302780 -Dtrace=1788751561052
 *
 * La ruta se reconstruye como la reconstruye la app: `SaveRouteFromSessionUseCase` guarda **todos**
 * los puntos de la sesion sin diezmar, y los puntos de la sesion son lo que el filtro acepto.
 *
 * No afirma nada: reporta, y el numero se elige leyendolo. Se salta en silencio sin trazas.
 */
class RouteFollowingReplay {

    @Test
    fun reportWhatTheDeviationWarningWouldHaveDone() {
        val routeTrace = routeTraceFile()
        if (routeTrace == null) {
            println("Sin -Droute=<traza> no hay ruta que seguir; nada que replayar.")
            return
        }
        val route = TraceFormat.parse(routeTrace.readLines())
            .map { it.reading }
            .acceptedByTheFilter()
            .map { WayPoint(latitude = it.latitude, longitude = it.longitude) }

        if (route.size < 2) {
            println("La traza ${routeTrace.name} no tiene recorrido aceptado: no hay ruta.")
            return
        }

        println("\n── Siguiendo una ruta: que habria hecho el aviso de desvio ──")
        println("   ruta: ${routeTrace.name} (${route.size} puntos aceptados)\n")

        traceFiles()
            .filter { it.name != routeTrace.name }
            .forEach { report(it, route) }
    }

    private fun report(file: File, route: List<WayPoint>) {
        val readings = TraceFormat.parse(file.readLines()).map { it.reading }
        if (readings.isEmpty()) return

        val startedAt = readings.first().timestamp
        println("── ${file.name}  (${readings.size} lecturas, ${(readings.last().timestamp - startedAt) / 1_000}s)")

        val distances = readings.mapNotNull { distanceToRouteMeters(it.latitude, it.longitude, route) }
        println(
            "   distancia al trazado: mediana ${fmt(distances.percentile(0.50))} m · " +
                "p90 ${fmt(distances.percentile(0.90))} m · maxima ${fmt(distances.max())} m",
        )
        println("   lecturas fuera de los 50 m: ${distances.count { it > RouteDeviation.TOLERANCE_METERS }}")

        printDepartures(readings, route, startedAt)
        printPersistenceSweep(readings, route, startedAt)
        println()
    }

    /** Los tramos en que la senal cruda se sale del umbral, se avise o no. */
    private fun printDepartures(readings: List<UserLocation>, route: List<WayPoint>, startedAt: Long) {
        val excursions = mutableListOf<Excursion>()
        var current: Excursion? = null
        readings.forEach { reading ->
            val meters = distanceToRouteMeters(reading.latitude, reading.longitude, route) ?: return@forEach
            if (meters > RouteDeviation.TOLERANCE_METERS) {
                current = current
                    ?.copy(endedAt = reading.timestamp, peakMeters = maxOf(current!!.peakMeters, meters))
                    ?: Excursion(reading.timestamp, reading.timestamp, meters)
            } else {
                current?.let { excursions += it }
                current = null
            }
        }
        current?.let { excursions += it }

        if (excursions.isEmpty()) {
            println("   ningun tramo fuera del umbral")
            return
        }
        println("   tramos fuera del umbral:")
        excursions.forEach {
            val from = (it.startedAt - startedAt) / 1_000
            val seconds = (it.endedAt - it.startedAt) / 1_000
            println("      a los ${from}s, ${seconds}s fuera, hasta ${fmt(it.peakMeters)} m")
        }
    }

    /**
     * Lo que este arnes existe para contestar: si los 30 s de persistencia sobran o faltan.
     *
     * Se replaya la misma traza con varias persistencias y se mira cuando habla el aviso. Un numero
     * que avisa igual de bien con menos retraso es mejor; uno que empieza a avisar de tramos que no
     * son desvios es peor.
     */
    private fun printPersistenceSweep(readings: List<UserLocation>, route: List<WayPoint>, startedAt: Long) {
        println("   persistencia   avisos   primer aviso   retraso   metros al avisar")
        listOf(0L, 10_000L, 15_000L, 20_000L, 30_000L, 45_000L, 60_000L).forEach { persistence ->
            val warnings = warningsWith(readings, route, persistence)
            val first = warnings.firstOrNull()
            val line = if (first == null) {
                "%9ss %8d   %-14s %-9s %s".format(persistence / 1_000, 0, "—", "—", "—")
            } else {
                "%9ss %8d   %-14s %-9s %s".format(
                    persistence / 1_000,
                    warnings.size,
                    "${(first.atMillis - startedAt) / 1_000}s",
                    "${(first.atMillis - first.strayingSince) / 1_000}s",
                    "${fmt(first.meters)} m",
                )
            }
            println("   $line")
        }
    }

    /**
     * Replaya la traza como la vive el tracker: el estado de grabacion lo construye el flujo filtrado
     * y el desvio se juzga sobre la posicion cruda.
     *
     * `isMoving` se lee **antes** de plegar la lectura actual, que es como llega en la app: el aviso
     * y la grabacion son dos colectores del mismo flujo, asi que el estado que el aviso ve es el que
     * dejo la lectura anterior.
     */
    private fun warningsWith(
        readings: List<UserLocation>,
        route: List<WayPoint>,
        persistenceMillis: Long,
    ): List<Warning> {
        val deviation = RouteDeviation(route, persistenceMillis = persistenceMillis)
        var filter = LocationFilter()
        var recording = RecordingState().started(readings.first().timestamp)
        var wasOff = false
        val warnings = mutableListOf<Warning>()
        var strayingSince: Long? = null

        readings.forEach { reading ->
            val meters = distanceToRouteMeters(reading.latitude, reading.longitude, route)
            val moving = recording.isMoving
            if (meters != null && meters > RouteDeviation.TOLERANCE_METERS && moving) {
                if (strayingSince == null) strayingSince = reading.timestamp
            } else {
                strayingSince = null
            }

            val standing = deviation.standing(reading, moving = moving)
            if (standing is RouteStanding.OffRoute && !wasOff) {
                warnings += Warning(
                    atMillis = reading.timestamp,
                    strayingSince = strayingSince ?: reading.timestamp,
                    meters = standing.metersFromRoute,
                )
            }
            wasOff = standing is RouteStanding.OffRoute

            recording = recording.observed(reading.timestamp)
            when (val outcome = filter.accepting(reading)) {
                is LocationFilterResult.Accepted -> {
                    filter = outcome.filter
                    outcome.locations.forEach { recording = recording.recorded(it) }
                }

                is LocationFilterResult.Discarded -> filter = outcome.filter
            }
        }
        return warnings
    }

    private fun List<UserLocation>.acceptedByTheFilter(): List<UserLocation> {
        var filter = LocationFilter()
        val accepted = mutableListOf<UserLocation>()
        forEach { reading ->
            when (val outcome = filter.accepting(reading)) {
                is LocationFilterResult.Accepted -> {
                    filter = outcome.filter
                    accepted += outcome.locations
                }

                is LocationFilterResult.Discarded -> filter = outcome.filter
            }
        }
        return accepted
    }

    private data class Excursion(val startedAt: Long, val endedAt: Long, val peakMeters: Double)

    private data class Warning(val atMillis: Long, val strayingSince: Long, val meters: Double)

    private fun routeTraceFile(): File? {
        val wanted = System.getProperty("route")?.takeIf { it.isNotBlank() } ?: return null
        return tracesDirectory().takeIf { it.isDirectory }
            ?.listFiles { file -> file.name.endsWith(".csv") }
            ?.firstOrNull { it.name.contains(wanted) }
    }

    private fun List<Double>.percentile(fraction: Double): Double {
        if (isEmpty()) return 0.0
        val ordered = sorted()
        val index = ((size - 1) * fraction).toInt().coerceIn(0, size - 1)
        return ordered[index]
    }

    private fun fmt(value: Double): String = "%.1f".format(value)
}
