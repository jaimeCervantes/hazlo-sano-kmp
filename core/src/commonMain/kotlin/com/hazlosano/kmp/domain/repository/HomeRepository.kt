package com.hazlosano.kmp.domain.repository

import com.hazlosano.kmp.domain.model.HomeContent

interface HomeRepository {
    suspend fun getHomeContent(): HomeContent
}
