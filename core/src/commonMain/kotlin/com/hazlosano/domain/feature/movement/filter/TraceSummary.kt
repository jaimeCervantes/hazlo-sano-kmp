package com.hazlosano.domain.feature.movement.filter

/**
 * What the filter did over a whole recording, as the session detail reports it.
 *
 * The counts are the diagnosis: a distance that came out short with most readings rejected as
 * [DiscardReason.IMPLAUSIBLE_SPEED] points at the plausibility gate, while the same distance with
 * everything accepted points at the noise floor or at the sampling rate instead.
 */
data class TraceSummary(
    val readings: Int,
    val accepted: Int,
    /** Only the reasons that actually occurred; a reason absent from here never fired. */
    val discardedBy: Map<DiscardReason, Int>,
    /** Averaged over the readings that reported an accuracy at all. */
    val averageAccuracyMeters: Double,
    /** How often the receiver actually delivered, which is not what was asked of it. */
    val samplingIntervalSeconds: Double,
) {
    val discarded: Int
        get() = readings - accepted
}

fun List<TraceRecord>.summarize(): TraceSummary =
    TraceSummary(
        readings = size,
        accepted = count { it.wasAccepted },
        discardedBy = mapNotNull { it.discardReason }.groupingBy { it }.eachCount(),
        averageAccuracyMeters = averageReportedAccuracy(),
        samplingIntervalSeconds = medianIntervalSeconds(),
    )

/**
 * A reading without an accuracy is left out rather than counted as some assumed value: the point of
 * the diagnosis is to report what the receiver said, not what the filter had to assume.
 */
private fun List<TraceRecord>.averageReportedAccuracy(): Double {
    val reported = filter { it.reading.accuracy > 0f }
    if (reported.isEmpty()) return 0.0
    return reported.sumOf { it.reading.accuracy.toDouble() } / reported.size
}

/**
 * The median, not the mean: a recording that spent a while without a fix — a tunnel, the receiver
 * throttled with the screen off — leaves one enormous gap that would drag a mean far away from the
 * rate the rest of the session actually ran at.
 */
private fun List<TraceRecord>.medianIntervalSeconds(): Double {
    val intervals = zipWithNext { previous, next ->
        (next.reading.timestamp - previous.reading.timestamp) / MILLIS_PER_SECOND
    }
        .filter { it > 0.0 }
        .sorted()

    if (intervals.isEmpty()) return 0.0
    val middle = intervals.size / 2
    return if (intervals.size % 2 == 1) {
        intervals[middle]
    } else {
        (intervals[middle - 1] + intervals[middle]) / 2.0
    }
}

private const val MILLIS_PER_SECOND = 1_000.0
