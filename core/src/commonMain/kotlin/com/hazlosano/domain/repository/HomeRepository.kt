package com.hazlosano.domain.repository

import com.hazlosano.domain.model.HomeContent

interface HomeRepository {
    suspend fun getHomeContent(): HomeContent
}
