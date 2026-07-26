package com.hazlosano.domain.feature.movement.usecase

import com.hazlosano.domain.feature.movement.model.SessionDetail
import com.hazlosano.domain.feature.movement.repository.MovementSessionRepository
import kotlin.math.abs

/**
 * Brings the summary the history lists a session by back in line with what its route actually shows.
 *
 * The detail measures the route every time it is opened, so it is always current. The history cannot
 * do that — it would have to read every point of every session — and reads a stored summary instead.
 * Opening a session is therefore what heals that summary, which is what stops the list and the
 * detail from disagreeing after the measurement improves.
 */
class RefreshSessionSummaryUseCase(
    private val repository: MovementSessionRepository,
) {
    suspend operator fun invoke(detail: SessionDetail) {
        // A session that stored no points measures zero, which is the absence of a route rather
        // than a journey of no length: overwriting its summary with that would destroy the only
        // record left of how far it went.
        if (detail.path.isEmpty()) return
        if (abs(detail.distanceMeters - detail.session.distanceTraveled) < TOLERANCE_METERS) return

        repository.updateDistance(detail.session.id, detail.distanceMeters)
    }

    private companion object {
        /** Below this the difference is floating point noise, not a better measurement. */
        const val TOLERANCE_METERS = 0.5
    }
}
