package com.hazlosano.data.movement

import com.hazlosano.data.db.inMemoryHazloSanoDatabase
import com.hazlosano.domain.feature.movement.model.Route
import com.hazlosano.domain.feature.movement.model.WayPoint
import com.hazlosano.domain.feature.movement.model.calculateFingerprint
import com.hazlosano.domain.feature.movement.model.calculateStats
import com.hazlosano.domain.feature.movement.model.distanceToRouteMeters
import com.hazlosano.domain.feature.movement.parser.GpxFormat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.system.measureTimeMillis
import kotlin.test.Test

/**
 * Cuanto tarda de verdad importar un GPX, por fases.
 *
 * Existe para contestar una pregunta de producto antes de maquetar nada: **si la importacion tarda
 * decimas de segundo, un indicador que parpadea estorba mas que ayuda; si tarda segundos, hace
 * falta**. Y si hace falta, dice ademas **que fase** se lleva el tiempo, porque un porcentaje solo
 * puede repartirse sobre trabajo que de verdad avanza.
 *
 * Mide tambien el coste por lectura del aviso de desvio: `distanceToRouteMeters` recorre todos los
 * segmentos de la ruta, y se llama con cada posicion que llega. Una ruta de miles de puntos lo paga
 * cada dos segundos durante toda la salida.
 *
 * No afirma nada: reporta, como los arneses de traza. Los tiempos de una maquina no son los de un
 * telefono, asi que lo que se lee aqui son **ordenes de magnitud y proporciones entre fases**, no
 * milisegundos que prometer.
 */
class GpxImportBenchmark {

    @Test
    fun reportHowLongImportingAGpxTakes() = runTest {
        println("\n── Importar un GPX, por fases ──")
        println("   (una maquina de escritorio; lo que importa es la proporcion, no el milisegundo)\n")
        println("   puntos    tamano     parsear    medir   guardar    releer     TOTAL")

        listOf(500, 2_000, 5_000, 10_000, 20_000).forEach { pointCount ->
            report(pointCount)
        }

        reportDeviationCostPerReading()
    }

    private suspend fun report(pointCount: Int) {
        val gpx = gpxWith(pointCount)
        val bytes = gpx.encodeToByteArray()
        // Una vuelta en vacio antes de medir: la primera pasada paga la compilacion JIT de las
        // expresiones regulares, y eso no es lo que un telefono paga en cada importacion.
        GpxFormat.parse(gpx)

        var parsed: Route
        val parseMillis = measureTimeMillis { parsed = GpxFormat.parse(gpx) }

        var measured: Route
        val statsMillis = measureTimeMillis {
            val (distance, elevation) = parsed.points.calculateStats()
            measured = parsed.copy(distance = distance, elevationGain = elevation)
                .let { it.copy(fingerprint = it.calculateFingerprint()) }
        }

        val repository = SqlDelightRouteRepository(inMemoryHazloSanoDatabase())
        var id = 0L
        val saveMillis = measureTimeMillis { id = repository.saveRoute(measured) }
        val readMillis = measureTimeMillis { repository.getRouteWithPoints(id).first() }

        println(
            "  %7d  %7s  %8s %8s  %8s  %8s  %8s".format(
                pointCount,
                "${bytes.size / 1024} KB",
                "$parseMillis ms",
                "$statsMillis ms",
                "$saveMillis ms",
                "$readMillis ms",
                "${parseMillis + statsMillis + saveMillis + readMillis} ms",
            ),
        )
    }

    /**
     * Lo que cuesta preguntar «me he salido» una vez, segun lo larga que sea la ruta cargada.
     *
     * Se paga con cada lectura del receptor —cada dos segundos en las trazas de campo— durante toda
     * la salida, asi que aqui un milisegundo no es un milisegundo: son 1.800 por hora de bici.
     */
    private fun reportDeviationCostPerReading() {
        println("\n── Coste de una comprobacion de desvio, segun el tamano de la ruta ──\n")
        println("   puntos     por lectura   por hora de salida (a 1 lectura/2 s)")

        listOf(500, 2_000, 5_000, 10_000, 20_000).forEach { pointCount ->
            val route = wayPoints(pointCount)
            val repeats = 200
            // Un punto fuera del trazado obliga a recorrer todos los segmentos, que es el peor caso
            // y el unico que importa: el aviso no puede permitirse ser lento justo cuando hace falta.
            val elapsed = measureTimeMillis {
                repeat(repeats) { distanceToRouteMeters(19.5000, -99.2000, route) }
            }
            val perReading = elapsed.toDouble() / repeats
            println(
                "  %7d   %9.3f ms   %8.1f s".format(
                    pointCount,
                    perReading,
                    perReading * 1_800 / 1_000,
                ),
            )
        }
        println()
    }

    /** Un GPX con la forma que escribe un receptor: un punto por segundo, con altitud y hora. */
    private fun gpxWith(pointCount: Int): String = buildString {
        append("""<?xml version="1.0" encoding="UTF-8"?>""").append('\n')
        append("""<gpx version="1.1" creator="benchmark">""").append('\n')
        append("  <trk>\n    <name>Ruta de prueba</name>\n    <trkseg>\n")
        wayPoints(pointCount).forEachIndexed { index, point ->
            append("      <trkpt lat=\"").append(point.latitude)
            append("\" lon=\"").append(point.longitude).append("\">\n")
            append("        <ele>").append(point.altitude).append("</ele>\n")
            append("        <time>2026-09-06T12:")
            append("%02d:%02d".format(index / 60 % 60, index % 60))
            append("Z</time>\n")
            append("      </trkpt>\n")
        }
        append("    </trkseg>\n  </trk>\n</gpx>\n")
    }

    /** Un trazado que avanza y sube, para que medir la distancia y el desnivel tenga trabajo real. */
    private fun wayPoints(count: Int): List<WayPoint> = List(count) { index ->
        WayPoint(
            latitude = 19.4300 + index * 0.00001,
            longitude = -99.1300 + index * 0.00001,
            altitude = 2_200.0 + index % 50,
        )
    }
}
