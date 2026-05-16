package com.hazlosano.kmp.domain.usecase

import com.hazlosano.kmp.domain.model.SleepContent
import com.hazlosano.kmp.domain.repository.SleepRepository

class GetSleepContentUseCase(private val repository: SleepRepository) {
    suspend operator fun invoke(): SleepContent = repository.getSleepContent()
}
