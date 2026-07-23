package com.hazlosano.domain.time

fun interface TimeProvider {
    fun nowMillis(): Long
}
