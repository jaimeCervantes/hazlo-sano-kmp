package com.hazlosano.data.movement.trace

import android.content.Context
import com.hazlosano.domain.feature.movement.filter.TraceRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Keeps traces as CSV files the phone can hand over without special access.
 *
 * A file being recorded is named after the moment the recording started, and is renamed after the
 * first point of the session it produced once that session is on disk. Until then it stays as a
 * partial: a recording the system killed leaves its readings behind under a name that belongs to no
 * session, which is honest — there is no session either.
 */
internal class FileTraceStore(context: Context) : TraceStore {

    private val appContext = context.applicationContext

    override fun openTrace(startedAtMillis: Long): TraceSink =
        FileTraceSink(directory(), startedAtMillis)

    override suspend fun readTrace(firstPointTimestamp: Long): List<TraceRecord>? =
        withContext(Dispatchers.IO) {
            val file = File(directory(), traceName(firstPointTimestamp))
            if (!file.exists()) return@withContext null
            TraceFormat.parse(file.readLines())
        }

    /**
     * The app's own folder on external storage, so a trace can be copied off with a plain `adb
     * pull`. Falls back to internal storage on the rare device that reports none — the capture
     * still works there, it just needs `run-as` to retrieve.
     */
    private fun directory(): File {
        val root = appContext.getExternalFilesDir(null) ?: appContext.filesDir
        return File(root, TRACE_DIRECTORY)
    }

    private companion object {
        const val TRACE_DIRECTORY = "traces"
    }
}

internal fun traceName(firstPointTimestamp: Long): String = "trace-$firstPointTimestamp.csv"

private class FileTraceSink(
    private val directory: File,
    startedAtMillis: Long,
) : TraceSink {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val records = Channel<TraceRecord>(Channel.UNLIMITED)
    private val partial = File(directory, "partial-$startedAtMillis.csv")

    private val writing = scope.launch {
        directory.mkdirs()
        partial.bufferedWriter().use { out ->
            out.appendLine(TraceFormat.HEADER)
            out.flush()
            for (record in records) {
                out.appendLine(TraceFormat.row(record))
                // Flushed per reading rather than buffered: one short line every couple of seconds
                // costs nothing, and it is what makes the trace of a recording the system killed
                // worth having at all.
                out.flush()
            }
        }
    }

    // Never blocks the caller and never drops a reading: the channel is unbounded and the writing
    // happens on IO.
    override fun append(record: TraceRecord) {
        records.trySend(record)
    }

    override suspend fun finish(firstPointTimestamp: Long?) {
        records.close()
        writing.join()
        if (firstPointTimestamp != null) {
            partial.renameTo(File(directory, traceName(firstPointTimestamp)))
        }
        scope.cancel()
    }
}
