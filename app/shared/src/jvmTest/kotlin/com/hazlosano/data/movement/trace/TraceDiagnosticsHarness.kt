package com.hazlosano.data.movement.trace

import com.hazlosano.domain.feature.movement.filter.LocationFilter
import com.hazlosano.domain.feature.movement.filter.LocationFilterResult
import com.hazlosano.domain.feature.movement.filter.TraceRecord
import com.hazlosano.domain.feature.movement.model.Elevation
import com.hazlosano.domain.feature.movement.model.UserLocation
import com.hazlosano.domain.geo.haversineMeters
import java.io.File
import kotlin.math.roundToInt
import kotlin.test.Test

/**
 * Pulls apart *where* a captured trace loses climb and loses time spent moving, so a constant is
 * changed against a measurement rather than a hunch.
 *
 * Climb is lost in two independent places and they need different fixes: the vertical smoothing can
 * lag behind a real slope, and the hysteresis can swallow what is left. Reporting the naive sum over
 * raw altitude, the naive sum over smoothed altitude and the hysteresis result side by side says
 * which one is doing the damage.
 *
 * Reports only; asserts nothing.
 */
class TraceDiagnosticsHarness {

    @Test
    fun reportWhereClimbAndMovingTimeAreLost() {
        traceFiles().forEach { file ->
            val records = TraceFormat.parse(file.readLines())
            val accepted = records.map { it.reading }.acceptedThroughFilter()
            if (accepted.size < 2) return@forEach

            println("\n══ ${file.name}")
            reportReleases(records)
            reportClimb(records, accepted)
            reportMovingTime(accepted)
            reportAltitudeProfile(records, accepted)
        }
    }

    /**
     * Where the recorded distance actually comes from: every time the filter releases a departure
     * into the path, how much it added and when. A phone that never moved should have none of these
     * after the first minute, so a session that reports kilometres from a table is read here.
     */
    private fun reportReleases(records: List<TraceRecord>) {
        val start = records.first().reading.timestamp
        var filter = LocationFilter()
        var previous: UserLocation? = null
        val releases = mutableListOf<Triple<Long, Int, Double>>()

        records.forEach { record ->
            val outcome = filter.accepting(record.reading)
            filter = outcome.filter
            if (outcome !is LocationFilterResult.Accepted) return@forEach
            var added = 0.0
            outcome.locations.forEach { point ->
                previous?.let {
                    added += haversineMeters(it.latitude, it.longitude, point.latitude, point.longitude)
                }
                previous = point
            }
            releases += Triple(
                (record.reading.timestamp - start) / 1_000L,
                outcome.locations.size,
                added,
            )
        }

        println(
            """
               releases into the path
                 how many                      ${releases.size}
                 metres from them              ${"%.0f".format(releases.sumOf { it.third })}
                 biggest five                  ${releases.sortedByDescending { it.third }.take(5)
                .joinToString(", ") { "t+${it.first}s ${it.second}pt ${"%.0f".format(it.third)}m" }}
                 when they happened            ${releases.take(24).joinToString(", ") { "${it.first}s" }}
            """.trimIndent(),
        )
    }

    private fun reportClimb(records: List<TraceRecord>, accepted: List<UserLocation>) {
        val raw = records.map { it.reading }
        println(
            """
               climb, three ways
                 naive over raw altitude       +${raw.naiveUp().fmt()} / -${raw.naiveDown().fmt()}
                 naive over smoothed altitude  +${accepted.naiveUp().fmt()} / -${accepted.naiveDown().fmt()}
                 what the app reports          +${accepted.withHysteresis().first.fmt()} / -${accepted.withHysteresis().second.fmt()}
                 threshold used                ${accepted.typicalThreshold().fmt()} per step
            """.trimIndent(),
        )
    }

    private fun reportMovingTime(accepted: List<UserLocation>) {
        val segments = accepted.zipWithNext { from, to ->
            val meters = haversineMeters(from.latitude, from.longitude, to.latitude, to.longitude)
            val seconds = (to.timestamp - from.timestamp) / 1_000.0
            Triple(meters, seconds, if (seconds > 0) meters / seconds else 0.0)
        }
        val still = segments.filter { it.third < MIN_TRAVELLING_SPEED_MPS }
        val stillSeconds = still.sumOf { it.second }
        val longestStill = still.maxByOrNull { it.second }

        println(
            """
               time counted as standing still
                 total                         ${stillSeconds.roundToInt()} s over ${still.size} segments
                 longest single segment        ${longestStill?.second?.roundToInt() ?: 0} s
                 segments under 0.5 m/s        ${still.size} of ${segments.size}
                 their speeds                  ${still.take(12).joinToString(", ") { "%.2f".format(it.third) }}
            """.trimIndent(),
        )
    }

    /** A coarse altitude profile, so the shape of the slopes is visible rather than inferred. */
    private fun reportAltitudeProfile(records: List<TraceRecord>, accepted: List<UserLocation>) {
        val raw = records.map { it.reading }
        val start = raw.first().timestamp
        println("               altitude over time (raw -> smoothed), every ~30 s")
        val buckets = raw.groupBy { ((it.timestamp - start) / 30_000L).toInt() }
        buckets.toSortedMap().forEach { (bucket, readings) ->
            val smoothed = accepted.filter { ((it.timestamp - start) / 30_000L).toInt() == bucket }
            val rawAvg = readings.mapNotNull { it.altitude }.averageOrNull()
            val smoothAvg = smoothed.mapNotNull { it.altitude }.averageOrNull()
            println(
                "                 %3ds  %s -> %s".format(
                    bucket * 30,
                    rawAvg.orDash(),
                    smoothAvg.orDash(),
                ),
            )
        }
    }

    private fun List<UserLocation>.naiveUp(): Double =
        zipWithNext { a, b -> climbBetween(a, b).coerceAtLeast(0.0) }.sum()

    private fun List<UserLocation>.naiveDown(): Double =
        zipWithNext { a, b -> (-climbBetween(a, b)).coerceAtLeast(0.0) }.sum()

    private fun climbBetween(from: UserLocation, to: UserLocation): Double {
        val here = to.altitude ?: return 0.0
        val there = from.altitude ?: return 0.0
        return here - there
    }

    /** Reproduces what CalculateStatsUseCase accumulates, with the same threshold rule. */
    private fun List<UserLocation>.withHysteresis(): Pair<Double, Double> {
        var elevation = Elevation()
        forEach { point ->
            point.altitude?.let { elevation = elevation.accumulating(it, point.threshold()) }
        }
        return elevation.ascentMeters to elevation.descentMeters
    }

    private fun List<Double>.averageOrNull(): Double? = if (isEmpty()) null else average()

    private fun Double?.orDash(): String = this?.let { "%6.1f".format(it) } ?: "     —"

    private fun List<UserLocation>.typicalThreshold(): Double =
        map { it.threshold() }.sorted()[size / 2]

    private fun UserLocation.threshold(): Double {
        val vertical = verticalAccuracy?.takeIf { it > 0f }?.toDouble()
            ?: ((if (accuracy > 0f) accuracy.toDouble() else 10.0) * VERTICAL_ACCURACY_RATIO)
        return (vertical * ELEVATION_THRESHOLD_FACTOR).coerceIn(3.0, 12.0)
    }

    private fun List<UserLocation>.acceptedThroughFilter(): List<UserLocation> {
        var filter = LocationFilter()
        val accepted = mutableListOf<UserLocation>()
        forEach { reading ->
            val outcome = filter.accepting(reading)
            filter = outcome.filter
            if (outcome is LocationFilterResult.Accepted) accepted += outcome.locations
        }
        return accepted
    }

    private fun Double.fmt(): String = "%.1f m".format(this)

    private fun traceFiles(): List<File> =
        File("../../traces").takeIf { it.isDirectory }
            ?.listFiles { file -> file.name.endsWith(".csv") }
            ?.sortedBy { it.name }
            .orEmpty()

    private companion object {
        const val MIN_TRAVELLING_SPEED_MPS = 0.5
        const val VERTICAL_ACCURACY_RATIO = 2.0
        const val ELEVATION_THRESHOLD_FACTOR = 0.6
    }
}
