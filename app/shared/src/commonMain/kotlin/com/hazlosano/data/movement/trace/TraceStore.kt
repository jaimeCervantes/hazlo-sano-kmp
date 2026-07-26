package com.hazlosano.data.movement.trace

import com.hazlosano.domain.feature.movement.filter.TraceRecord

/** Collects the readings of one recording as they arrive. */
interface TraceSink {

    /**
     * Takes a reading in. Must not block the caller: this runs on whatever thread is collecting
     * locations, which on Android is the service's main thread.
     */
    fun append(record: TraceRecord)

    /**
     * Closes the trace once the recording has stopped.
     *
     * [firstPointTimestamp] is what ties the trace to the session it produced — the first reading
     * the filter accepted is also the session's first stored point, so the detail can find its own
     * trace without the sessions table growing a column it cannot grow. Null when the recording
     * saved no session, in which case the trace is kept but belongs to nothing.
     */
    suspend fun finish(firstPointTimestamp: Long?)
}

/** Where captured traces are kept on this platform. */
interface TraceStore {

    fun openTrace(startedAtMillis: Long): TraceSink

    /** The trace of the session whose first stored point has this timestamp, or null if none. */
    suspend fun readTrace(firstPointTimestamp: Long): List<TraceRecord>?
}

/**
 * Used where capturing traces makes no sense. Recording still works; there is simply nothing to
 * diagnose, which the detail reports as "no trace" rather than as a session that rejected nothing.
 */
object NoOpTraceStore : TraceStore {

    override fun openTrace(startedAtMillis: Long): TraceSink = NoOpTraceSink

    override suspend fun readTrace(firstPointTimestamp: Long): List<TraceRecord>? = null
}

private object NoOpTraceSink : TraceSink {
    override fun append(record: TraceRecord) = Unit
    override suspend fun finish(firstPointTimestamp: Long?) = Unit
}
