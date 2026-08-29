package com.hazlosano.domain.model

/**
 * Los cuatro pilares.
 *
 * [key] es el valor que entiende la API del sitio (`?pillar=`) y **la clave estable** con la que la
 * UI busca su rótulo traducido. `core` no puede alcanzar los recursos de Compose, así que aquí vive
 * la clave y el texto vive en el catálogo de cadenas.
 *
 * [label] es deuda anterior a esa regla: sigue aquí porque hay pantallas que todavía lo leen, y se
 * retira cuando se pase la última. **No lo uses en código nuevo** — pide el rótulo por [key].
 */
enum class PillarType(val key: String, val label: String) {
    SLEEP("sleep", "Sueño"),
    MOVEMENT("movement", "Movimiento"),
    NUTRITION("nutrition", "Nutrición"),
    MIND("mindSpirit", "Mente"),
    ;

    companion object {
        fun fromKey(key: String?): PillarType? = entries.firstOrNull { it.key == key }
    }
}
