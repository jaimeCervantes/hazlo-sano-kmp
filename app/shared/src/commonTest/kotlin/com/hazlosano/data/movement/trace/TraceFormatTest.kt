package com.hazlosano.data.movement.trace

import com.hazlosano.domain.feature.movement.filter.DiscardReason
import com.hazlosano.domain.feature.movement.filter.TraceRecord
import com.hazlosano.domain.feature.movement.model.UserLocation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val VERDICT_COLUMN = 6

class TraceFormatTest {

    private fun reading(
        timestamp: Long,
        accuracy: Float = 8f,
    ): UserLocation = UserLocation(
        latitude = 19.4312345,
        longitude = -99.1367890,
        altitude = 2237.5,
        accuracy = accuracy,
        verticalAccuracy = 11.5f,
        bearing = 143.25f,
        timestamp = timestamp,
    )

    @Test
    fun aWrittenTraceReadsBackUnchanged() {
        val records = listOf(
            TraceRecord(reading(1_000), null),
            TraceRecord(reading(3_000, accuracy = 62f), DiscardReason.POOR_ACCURACY),
            TraceRecord(reading(5_000), DiscardReason.WITHIN_NOISE),
            TraceRecord(reading(7_000), DiscardReason.IMPLAUSIBLE_SPEED),
        )

        val written = listOf(TraceFormat.HEADER) + records.map(TraceFormat::row)

        assertEquals(records, TraceFormat.parse(written))
    }

    @Test
    fun everyDiscardReasonSurvivesTheRoundTrip() {
        // A reason that did not survive would silently become a different diagnosis.
        val records = DiscardReason.entries.mapIndexed { index, reason ->
            TraceRecord(reading(index * 2_000L), reason)
        }

        assertEquals(records, TraceFormat.parse(records.map(TraceFormat::row)))
    }

    @Test
    fun theColumnHeaderIsNotMistakenForAReading() {
        assertEquals(emptyList(), TraceFormat.parse(listOf(TraceFormat.HEADER)))
    }

    @Test
    fun aTraceCutOffMidLineStillYieldsEverythingBeforeIt() {
        // The outing worth diagnosing is the one the system killed with the screen off, and that
        // file ends wherever the process died.
        val complete = listOf(
            TraceRecord(reading(1_000), null),
            TraceRecord(reading(3_000), null),
        )
        val truncated = listOf(TraceFormat.HEADER) +
            complete.map(TraceFormat::row) +
            "5000,19.431234,-99.13"

        val parsed = TraceFormat.parse(truncated)

        assertEquals(complete, parsed)
    }

    @Test
    fun garbageInTheMiddleDoesNotDiscardTheRestOfTheTrace() {
        val records = listOf(
            TraceRecord(reading(1_000), null),
            TraceRecord(reading(5_000), null),
        )
        val lines = listOf(
            TraceFormat.row(records[0]),
            "not,a,reading,at,all,really,ACCEPTED",
            TraceFormat.row(records[1]),
        )

        assertEquals(records, TraceFormat.parse(lines))
    }

    @Test
    fun aReadingWithoutAnAltitudeSaysSoRatherThanClaimingSeaLevel() {
        val records = listOf(
            TraceRecord(reading(1_000).copy(altitude = null, verticalAccuracy = null), null),
            TraceRecord(reading(3_000).copy(altitude = 0.0), null),
        )

        val parsed = TraceFormat.parse(records.map(TraceFormat::row))

        assertEquals(records, parsed)
        assertNull(parsed[0].reading.altitude)
        // Zero is an altitude, and has to survive as one.
        assertEquals(0.0, parsed[1].reading.altitude)
    }

    @Test
    fun aTraceCapturedBeforeAltitudeQualityWasRecordedStillReads() {
        // The three traces from the first field calibration are in this shape. They have to keep
        // replaying, saying nothing about their vertical accuracy rather than failing to parse.
        val old = listOf(
            "timestamp,latitude,longitude,altitude,accuracy,bearing,verdict",
            "1785123280584,18.5964318,-96.6906914,197.8000030517578,20.9,0.0,ACCEPTED",
            "1785123286523,18.5964323,-96.6906911,197.8000030517578,26.4,0.0,WITHIN_NOISE",
        )

        val parsed = TraceFormat.parse(old)

        assertEquals(2, parsed.size)
        assertEquals(197.8000030517578, parsed.first().reading.altitude)
        assertEquals(20.9f, parsed.first().reading.accuracy)
        assertNull(parsed.first().reading.verticalAccuracy)
        assertEquals(DiscardReason.WITHIN_NOISE, parsed.last().discardReason)
    }

    @Test
    fun anUnknownVerdictIsSkippedRatherThanGuessed() {
        // A trace written by a future version that added a reason: dropping the line is honest,
        // calling it accepted would invent a diagnosis.
        val line = TraceFormat.row(TraceRecord(reading(1_000), null))
            .split(',')
            .toMutableList()
            .also { it[VERDICT_COLUMN] = "SOMETHING_NEW" }
            .joinToString(",")

        assertTrue(TraceFormat.parse(listOf(line)).isEmpty())
    }
}
