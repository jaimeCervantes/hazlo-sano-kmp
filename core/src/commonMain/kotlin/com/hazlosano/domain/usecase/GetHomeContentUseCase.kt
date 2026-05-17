package com.hazlosano.domain.usecase

import com.hazlosano.domain.model.HomeContent
import com.hazlosano.domain.repository.HomeRepository

class GetHomeContentUseCase(private val repository: HomeRepository) {
    suspend operator fun invoke(): HomeContent = repository.getHomeContent()
}
