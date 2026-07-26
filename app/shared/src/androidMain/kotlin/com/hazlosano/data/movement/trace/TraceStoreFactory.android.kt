package com.hazlosano.data.movement.trace

import com.hazlosano.data.movement.MovementServiceLocator

actual fun createTraceStore(): TraceStore =
    FileTraceStore(MovementServiceLocator.requireContext())
