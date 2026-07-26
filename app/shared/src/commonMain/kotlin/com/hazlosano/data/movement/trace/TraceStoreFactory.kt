package com.hazlosano.data.movement.trace

/**
 * Creates the platform [TraceStore]. Only Android records against a real receiver today, so it is
 * the only target that keeps traces; everywhere else this is a no-op.
 */
expect fun createTraceStore(): TraceStore
