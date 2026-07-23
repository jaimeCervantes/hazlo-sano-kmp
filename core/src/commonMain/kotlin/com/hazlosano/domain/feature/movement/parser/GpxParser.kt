package com.hazlosano.domain.feature.movement.parser

import com.hazlosano.domain.feature.movement.model.Route

interface GpxParser {
    fun parse(data: ByteArray): Route
}
