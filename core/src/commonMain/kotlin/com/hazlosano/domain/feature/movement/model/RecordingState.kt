package com.hazlosano.domain.feature.movement.model

import com.hazlosano.domain.geo.haversineMeters

/**
 * What has been recorded of the session in progress.
 *
 * Elapsed time is derived from [startedAtMillis] rather than counted by a timer, so a recording that
 * runs with the screen off or the app in the background still reports the real duration instead of
 * however many ticks the system allowed.
 */
data class RecordingState(
    val isRecording: Boolean = false,
    val startedAtMillis: Long? = null,
    val traveledPoints: List<UserLocation> = emptyList(),
    val distanceMeters: Double = 0.0,
    val elapsedSeconds: Long = 0,
    /**
     * When the receiver last delivered a reading, whatever the filter made of it.
     *
     * Kept apart from the path because the difference between the two is the only way to tell a
     * phone that is not moving from a phone that is not being told anything.
     */
    val lastReadingAtMillis: Long? = null,
)

/** Begins a new recording, discarding anything recorded before. */
fun RecordingState.started(nowMillis: Long): RecordingState =
    RecordingState(isRecording = true, startedAtMillis = nowMillis)

/** Adds a location to the traveled path. Locations arriving while idle are ignored. */
fun RecordingState.recorded(location: UserLocation): RecordingState {
    if (!isRecording) return this

    val previous = traveledPoints.lastOrNull()
    val addedMeters = previous?.let {
        haversineMeters(it.latitude, it.longitude, location.latitude, location.longitude)
    } ?: 0.0

    return observed(location.timestamp).copy(
        traveledPoints = traveledPoints + location,
        distanceMeters = distanceMeters + addedMeters,
    )
}

/**
 * Notes that the receiver delivered a reading, including the ones the filter turned away. Those are
 * the evidence that the user is not moving, as opposed to the app not being told.
 */
fun RecordingState.observed(atMillis: Long): RecordingState {
    if (!isRecording) return this
    return copy(lastReadingAtMillis = maxOf(atMillis, lastReadingAtMillis ?: atMillis))
}

/**
 * How long the receiver has kept reporting without any of it extending the path, or null when that
 * cannot be told.
 *
 * **Silence is not stillness.** A recording that lost its signal in a tunnel and a recording lying
 * on a table both stop growing their path, and only one of them has stopped travelling. This counts
 * the gap between the last reading and the last recorded point, so it grows only while the receiver
 * is actually saying something — a phone that stops reporting freezes this instead of ageing.
 */
val RecordingState.secondsWithoutMoving: Long?
    get() {
        val lastPoint = traveledPoints.lastOrNull()?.timestamp ?: return null
        val lastReading = lastReadingAtMillis ?: return null
        if (lastReading <= lastPoint) return null
        return (lastReading - lastPoint) / MILLIS_PER_SECOND
    }

/**
 * How many whole minutes the recording has spent going nowhere, once that is long enough to be
 * worth saying out loud, and null while there is nothing to say.
 *
 * The threshold is long enough that no traffic light, shop or photo trips it, and short enough that
 * a recording left running is caught while the outing is still fresh. The one that produced this
 * rule ran for thirty-one minutes on a table.
 */
fun RecordingState.goneNowhereMinutes(): Long? =
    secondsWithoutMoving
        ?.takeIf { it >= SECONDS_GONE_NOWHERE_WORTH_SAYING }
        ?.let { it / SECONDS_PER_MINUTE }

/**
 * Si la grabacion esta avanzando ahora mismo.
 *
 * Se apoya en [secondsWithoutMoving], que crece solo mientras el receptor sigue diciendo algo: un
 * telefono que deja de reportar congela la cuenta en vez de envejecerla, asi que perder la senal no
 * cuenta como pararse.
 *
 * Lo pregunta el aviso de desvio de ruta, y por un motivo medido: con el telefono quieto bajo techo
 * la senal llega a colocarse a 291 m del sitio donde esta, asi que parado la distancia al trazado es
 * ruido y no se puede juzgar.
 */
val RecordingState.isMoving: Boolean
    get() = isRecording && (secondsWithoutMoving ?: 0L) < SECONDS_STILL_MOVING

/**
 * Cuanto puede llevar el recorrido sin crecer y seguir contando como movimiento.
 *
 * Es la ventana de confirmacion del filtro: hasta que pasa, un tramo real todavia puede estar
 * retenido esperando confirmarse, y darlo por parado seria callar el aviso justo cuando arranca.
 */
const val SECONDS_STILL_MOVING = 60L

const val SECONDS_GONE_NOWHERE_WORTH_SAYING = 300L

/** Refreshes the elapsed time of a recording in progress. */
fun RecordingState.elapsedAt(nowMillis: Long): RecordingState {
    val startedAt = startedAtMillis
    if (!isRecording || startedAt == null) return this
    return copy(elapsedSeconds = ((nowMillis - startedAt) / MILLIS_PER_SECOND).coerceAtLeast(0))
}

/**
 * Ends the recording, keeping what was recorded so it can be saved.
 *
 * **A session that stopped travelling long before anyone pressed stop ends where it last moved.**
 * The wait that followed is not part of the outing, and counting it turns a two-minute bike ride
 * into a 33-minute session at 55 min/km — which is what a real forgotten recording produced. Only
 * stillness the receiver actually witnessed can end an outing early (see [secondsWithoutMoving]),
 * so a recording that lost its signal keeps every second of its duration.
 */
fun RecordingState.stopped(nowMillis: Long): RecordingState {
    val lastMovedAt = traveledPoints.lastOrNull()?.timestamp
    val endedAt = if (goneNowhereMinutes() != null && lastMovedAt != null) lastMovedAt else nowMillis
    return elapsedAt(endedAt).copy(isRecording = false)
}

private const val MILLIS_PER_SECOND = 1_000L
private const val SECONDS_PER_MINUTE = 60L
