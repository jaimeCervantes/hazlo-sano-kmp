package com.hazlosano.domain.repository

import com.hazlosano.domain.model.SleepContent

interface SleepRepository {
    suspend fun getSleepContent(): SleepContent
}
