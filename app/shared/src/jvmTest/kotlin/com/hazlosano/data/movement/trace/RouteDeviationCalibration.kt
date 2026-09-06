package com.hazlosano.data.movement.trace

import com.hazlosano.domain.feature.movement.filter.LocationFilter
import com.hazlosano.domain.feature.movement.filter.LocationFilterResult
import com.hazlosano.domain.feature.movement.filter.TraceRecord
import com.hazlosano.domain.feature.movement.model.UserLocation
import com.hazlosano.domain.feature.movement.model.WayPoint
import com.hazlosano.domain.feature.movement.model.distanceToRouteMeters
import java.io.File
import kotlin.test.Test

/**
 * Cuánto se aleja la señal cruda del camino que de verdad se recorrió.
 *
 * Es la cifra que decide el umbral de «te has salido de la ruta». La pregunta que responde es la del
 * **falso positivo**: si el receptor puede colocarte a 60 m del sitio por el que estás pasando,
 * avisar a los 50 m es avisar de un desvío que no existe.
 *
 * **Cómo se mide sin tener una traza de alguien siguiendo una ruta.** No hay ninguna: las cuatro
 * capturas son salidas libres. Pero el recorrido que el filtro acepta de una traza **es** el camino
 * que se recorrió, así que se usa como ruta y se mide contra él la señal cruda de esa misma traza.
 * Lo que sale es exactamente el ruido que un umbral tiene que dejar pasar.
 *
 * No afirma nada: reporta, y el número se elige leyéndolo. Como `TraceReplayHarness`, se salta en
 * silencio donde no hay trazas — `traces/` está en `.gitignore`.
 */
class RouteDeviationCalibration {

    @Test
    fun reportHowFarTheRawSignalStraysFromTheRealPath() {
        val traces = traceFiles()
        if (traces.isEmpty()) {
            println("No traces in ${tracesDirectory().absolutePath}; nothing to calibrate against.")
            return
        }

        println("\n── Desvío de la señal cruda respecto al camino recorrido ──")
        println("   (el umbral de «te has salido» tiene que quedar por encima de esto)\n")

        traces.sortedBy { it.name }.forEach { file ->
            report(file, TraceFormat.parse(file.readLines()))
        }
    }

    private fun report(file: File, records: List<TraceRecord>) {
        val readings = records.map { it.reading }
        val route = readings.acceptedByTheFilter().asRoute()

        if (route.size < 2) {
            println("── ${file.name}\n   sin recorrido aceptado: no hay ruta contra la que medir\n")
            return
        }

        val deviations = readings
            .mapNotNull { distanceToRouteMeters(it.latitude, it.longitude, route) }
            .sorted()

        if (deviations.isEmpty()) return

        println(
            """
            ── ${file.name}
               lecturas          ${deviations.size}
               ruta (aceptado)   ${route.size} puntos
               desvío mediano    ${fmt(deviations.percentile(0.50))} m
               desvío p90        ${fmt(deviations.percentile(0.90))} m
               desvío p99        ${fmt(deviations.percentile(0.99))} m
               desvío máximo     ${fmt(deviations.last())} m
               por encima de 50 m ${deviations.count { it > 50 }} lecturas
               por encima de 75 m ${deviations.count { it > 75 }} lecturas
               por encima de 100 m ${deviations.count { it > 100 }} lecturas
            """.trimIndent(),
        )
        println()
    }

    /** El recorrido que el filtro acepta: el camino que de verdad se hizo. */
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

    private fun List<UserLocation>.asRoute(): List<WayPoint> =
        map { WayPoint(latitude = it.latitude, longitude = it.longitude) }

    private fun List<Double>.percentile(fraction: Double): Double {
        if (isEmpty()) return 0.0
        val index = ((size - 1) * fraction).toInt().coerceIn(0, size - 1)
        return this[index]
    }

    private fun fmt(value: Double): String = "%.1f".format(value)
}
