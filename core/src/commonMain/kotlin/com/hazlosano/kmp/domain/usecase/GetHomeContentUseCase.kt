package com.hazlosano.kmp.domain.usecase

import com.hazlosano.kmp.domain.model.HomeContent
import com.hazlosano.kmp.domain.repository.HomeRepository

class GetHomeContentUseCase(private val repository: HomeRepository) {
    suspend operator fun invoke(): HomeContent = repository.getHomeContent()
}
