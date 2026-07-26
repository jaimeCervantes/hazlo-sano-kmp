package com.hazlosano.data.movement.trace

import com.hazlosano.domain.feature.movement.filter.TraceRecord

/**
 * A trace store that keeps everything in memory, so what a recording captured can be asserted
 * without a filesystem. Traces become readable under the key the sink was finished with, which is
 * the same rule the real store implements by renaming the file.
 */
class FakeTraceStore : TraceStore {

    val finishedTraces: MutableMap<Long, List<TraceRecord>> = mutableMapOf()
    var opensRequested: Int = 0
        private set
    var lastSink: FakeTraceSink? = null
        private set

    override fun openTrace(startedAtMillis: Long): TraceSink {
        opensRequested++
        return FakeTraceSink(finishedTraces).also { lastSink = it }
    }

    override suspend fun readTrace(firstPointTimestamp: Long): List<TraceRecord>? =
        finishedTraces[firstPointTimestamp]
}

class FakeTraceSink(private val finishedTraces: MutableMap<Long, List<TraceRecord>>) : TraceSink {

    val appended: MutableList<TraceRecord> = mutableListOf()
    var finished: Boolean = false
        private set
    var tiedTo: Long? = null
        private set

    override fun append(record: TraceRecord) {
        appended += record
    }

    override suspend fun finish(firstPointTimestamp: Long?) {
        finished = true
        tiedTo = firstPointTimestamp
        if (firstPointTimestamp != null) finishedTraces[firstPointTimestamp] = appended.toList()
    }
}
