package com.hazlosano.domain.feature.movement.repository

import com.hazlosano.domain.feature.movement.model.Route

interface OfflineMapRepository {
    suspend fun checkIfMapIsDownloaded(route: Route): Boolean
    fun downloadOfflineMap(
        route: Route,
        onProgress: (Int) -> Unit,
        onComplete: (Boolean) -> Unit
    )
}
