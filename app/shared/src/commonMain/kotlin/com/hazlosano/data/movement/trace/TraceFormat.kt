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

    const val HEADER: String = "timestamp,latitude,longitude,altitude,accuracy,bearing,verdict"

    private const val ACCEPTED = "ACCEPTED"
    private const val COLUMNS = 7

    fun row(record: TraceRecord): String = with(record.reading) {
        "$timestamp,$latitude,$longitude,$altitude,$accuracy,$bearing," +
            (record.discardReason?.name ?: ACCEPTED)
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
        if (fields.size != COLUMNS) return null

        val timestamp = fields[0].toLongOrNull() ?: return null
        val latitude = fields[1].toDoubleOrNull() ?: return null
        val longitude = fields[2].toDoubleOrNull() ?: return null
        val altitude = fields[3].toDoubleOrNull() ?: return null
        val accuracy = fields[4].toFloatOrNull() ?: return null
        val bearing = fields[5].toFloatOrNull() ?: return null
        val verdict = fields[6]

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
                bearing = bearing,
                timestamp = timestamp,
            ),
            discardReason = discardReason,
        )
    }
}
