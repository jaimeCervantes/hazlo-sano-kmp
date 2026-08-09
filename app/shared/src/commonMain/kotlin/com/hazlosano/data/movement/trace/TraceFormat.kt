package com.hazlosano.data.movement.trace

import com.hazlosano.domain.feature.movement.filter.DiscardReason
import com.hazlosano.domain.feature.movement.filter.TraceRecord
import com.hazlosano.domain.feature.movement.model.UserLocation

/**
 * Serializes a captured trace as CSV, one reading per line.
 *
 * CSV rather than the database on purpose. The diagnosis is per session, so it would naturally be
 * columns on an existing table — and adding a column is exactly what this project's schema handling
 * cannot do to an install that already exists. A file also happens to be the format that can be
 * pulled off the phone and replayed, which is the whole point of capturing it.
 *
 * Kotlin renders numbers the same way on every locale and target, so a trace captured on a phone
 * parses identically in a JVM test.
 */
object TraceFormat {

    const val HEADER: String =
        "timestamp,latitude,longitude,altitude,accuracy,bearing,verdict,verticalAccuracy"

    private const val ACCEPTED = "ACCEPTED"
    private const val ABSENT = ""

    /** The shape written before the receiver's own altitude quality was captured. */
    private const val COLUMNS_WITHOUT_ALTITUDE_QUALITY = 7
    private const val COLUMNS = 8

    fun row(record: TraceRecord): String = with(record.reading) {
        listOf(
            timestamp.toString(),
            latitude.toString(),
            longitude.toString(),
            altitude?.toString() ?: ABSENT,
            accuracy.toString(),
            bearing.toString(),
            record.discardReason?.name ?: ACCEPTED,
            verticalAccuracy?.toString() ?: ABSENT,
        ).joinToString(",")
    }

    /**
     * Reads back what [row] wrote, skipping anything that does not parse.
     *
     * Being lenient is a requirement, not sloppiness: a trace whose recording was killed by the
     * system ends mid-line, and that trace is the one worth having — it is the outing with the
     * screen off. Losing its last reading is acceptable; refusing to read the whole file is not.
     */
    fun parse(lines: List<String>): List<TraceRecord> = lines.mapNotNull(::parseRow)

    private fun parseRow(line: String): TraceRecord? {
        val fields = line.split(',')
        // A trace captured before the altitude quality was recorded is still worth replaying: it
        // simply says nothing about how good its altitudes were.
        if (fields.size != COLUMNS && fields.size != COLUMNS_WITHOUT_ALTITUDE_QUALITY) return null

        val timestamp = fields[0].toLongOrNull() ?: return null
        val latitude = fields[1].toDoubleOrNull() ?: return null
        val longitude = fields[2].toDoubleOrNull() ?: return null
        val altitudeField = fields[3]
        // Blank means the receiver reported no altitude; unparseable means a broken row.
        val altitude =
            if (altitudeField.isBlank()) null else altitudeField.toDoubleOrNull() ?: return null
        val accuracy = fields[4].toFloatOrNull() ?: return null
        val bearing = fields[5].toFloatOrNull() ?: return null
        val verdict = fields[6]
        val verticalAccuracy = fields.getOrNull(7)?.takeIf { it.isNotBlank() }?.toFloatOrNull()

        val discardReason = when (verdict) {
            ACCEPTED -> null
            else -> DiscardReason.entries.firstOrNull { it.name == verdict } ?: return null
        }

        return TraceRecord(
            reading = UserLocation(
                latitude = latitude,
                longitude = longitude,
                altitude = altitude,
                accuracy = accuracy,
                verticalAccuracy = verticalAccuracy,
                bearing = bearing,
                timestamp = timestamp,
            ),
            discardReason = discardReason,
        )
    }
}
