package com.hazlosano.domain.usecase

import com.hazlosano.domain.model.SleepContent
import com.hazlosano.domain.repository.SleepRepository

class GetSleepContentUseCase(private val repository: SleepRepository) {
    suspend operator fun invoke(): SleepContent = repository.getSleepContent()
}
