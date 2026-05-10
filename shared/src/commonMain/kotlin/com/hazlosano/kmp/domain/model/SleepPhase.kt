package com.hazlosano.kmp.domain.model

enum class SleepPhase(val label: String) {
    UNKNOWN("Desconocido"),
    AWAKE("Despierto"),
    ASLEEP("Dormido"),
    LIGHT("Sueño ligero"),
    DEEP("Sueño profundo"),
    REM("REM"),
}
