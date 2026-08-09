package com.hazlosano.data.movement.trace

import com.hazlosano.domain.feature.movement.filter.DiscardReason
import com.hazlosano.domain.feature.movement.filter.LocationFilter
import com.hazlosano.domain.feature.movement.filter.LocationFilterResult
import com.hazlosano.domain.feature.movement.filter.TraceRecord
import com.hazlosano.domain.feature.movement.filter.summarize
import com.hazlosano.domain.feature.movement.model.SessionStats
import com.hazlosano.domain.feature.movement.model.UserLocation
import com.hazlosano.domain.feature.movement.usecase.CalculateStatsUseCase
import com.hazlosano.domain.geo.haversineMeters
import java.io.File
import kotlin.test.Test

/**
 * Runs traces captured on a real phone back through the filter and reports what the app would have
 * recorded from them.
 *
 * This is the calibration loop the trace capture exists for: change a constant, run this, compare.
 * It asserts nothing, because there is nothing to assert until the numbers are set against what the
 * outing actually was — it reports, and the judgement is made by reading it.
 *
 * Skips silently when there are no traces to read, so it never fails on a machine that has none.
 */
class TraceReplayHarness {

    @Test
    fun reportWhatTheCapturedTracesWouldRecord() {
        val traces = traceFiles()
        if (traces.isEmpty()) {
            println("No traces in ${tracesDirectory().absolutePath}; nothing to replay.")
            return
        }

        traces.forEach { file ->
            val records = TraceFormat.parse(file.readLines())
            report(file.name, records)
        }
    }

    private fun report(name: String, records: List<TraceRecord>) {
        val summary = records.summarize()
        val replayed = records.map { it.reading }.replay()
        val elapsedSeconds = records.elapsedSeconds()
        val stats = CalculateStatsUseCase()(replayed.accepted, elapsedSeconds)

        println(
            """

            ── $name
               readings          ${summary.readings}  over ${elapsedSeconds}s (${elapsedSeconds / 60}m ${elapsedSeconds % 60}s)
               sampled every     ${summary.samplingIntervalSeconds} s (median)
               accuracy          ${"%.1f".format(summary.averageAccuracyMeters)} m average
               accepted          ${summary.accepted}
               rejected          ${summary.discarded} ${summary.discardedBy.pretty()}

               distance raw      ${"%.0f".format(records.map { it.reading }.pathMeters())} m   (every reading, unfiltered)
               distance recorded ${"%.0f".format(replayed.accepted.pathMeters())} m   <- what the app saves
               net displacement  ${"%.0f".format(records.netMeters())} m   (first reading to last)

               moving time       ${stats.movingTime ?: "—"} s of $elapsedSeconds s
               pace              ${stats.avgPace?.let { "%.2f".format(it) } ?: "—"} min/km
               climb / descent   ${stats.metres(SessionStats::totalAscent)} / ${stats.metres(SessionStats::totalDescent)}
               altitude min/max  ${stats.metres(SessionStats::minAltitude)} / ${stats.metres(SessionStats::maxAltitude)}
               altitude raw span ${"%.0f".format(records.altitudeSpan())} m   (before smoothing)
            """.trimIndent(),
        )
    }

    private data class Replayed(val accepted: List<UserLocation>)

    private fun List<UserLocation>.replay(): Replayed {
        var filter = LocationFilter()
        val accepted = mutableListOf<UserLocation>()
        forEach { reading ->
            val outcome = filter.accepting(reading)
            filter = outcome.filter
            if (outcome is LocationFilterResult.Accepted) accepted += outcome.location
        }
        return Replayed(accepted)
    }

    private fun List<UserLocation>.pathMeters(): Double =
        zipWithNext { from, to ->
            haversineMeters(from.latitude, from.longitude, to.latitude, to.longitude)
        }.sum()

    private fun List<TraceRecord>.netMeters(): Double {
        if (size < 2) return 0.0
        val from = first().reading
        val to = last().reading
        return haversineMeters(from.latitude, from.longitude, to.latitude, to.longitude)
    }

    private fun List<TraceRecord>.elapsedSeconds(): Long =
        if (size < 2) 0L else (last().reading.timestamp - first().reading.timestamp) / 1_000L

    private fun List<TraceRecord>.altitudeSpan(): Double {
        val altitudes = mapNotNull { it.reading.altitude }
        if (altitudes.isEmpty()) return 0.0
        return altitudes.max() - altitudes.min()
    }

    private fun SessionStats.metres(field: (SessionStats) -> Double?): String =
        field(this)?.let { "%.0f m".format(it) } ?: "—"

    private fun Map<DiscardReason, Int>.pretty(): String =
        if (isEmpty()) "" else entries.joinToString(", ", "(", ")") { "${it.key.name} ${it.value}" }

    private fun traceFiles(): List<File> =
        tracesDirectory().takeIf { it.isDirectory }
            ?.listFiles { file -> file.name.endsWith(".csv") }
            ?.sortedBy { it.name }
            .orEmpty()

    /** The repository root's `traces/`, reached from the module the test runs in. */
    private fun tracesDirectory(): File = File("../../traces")
}
