package com.hazlosano.data.movement.trace

import com.hazlosano.domain.feature.movement.filter.DiscardReason
import com.hazlosano.domain.feature.movement.filter.TraceRecord
import com.hazlosano.domain.feature.movement.model.UserLocation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TraceFormatTest {

    private fun reading(
        timestamp: Long,
        accuracy: Float = 8f,
    ): UserLocation = UserLocation(
        latitude = 19.4312345,
        longitude = -99.1367890,
        altitude = 2237.5,
        accuracy = accuracy,
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
    fun anUnknownVerdictIsSkippedRatherThanGuessed() {
        // A trace written by a future version that added a reason: dropping the line is honest,
        // calling it accepted would invent a diagnosis.
        val line = TraceFormat.row(TraceRecord(reading(1_000), null))
            .replaceAfterLast(',', "SOMETHING_NEW")

        assertTrue(TraceFormat.parse(listOf(line)).isEmpty())
    }
}
