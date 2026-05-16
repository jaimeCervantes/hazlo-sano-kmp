package com.hazlosano.kmp.domain.repository

import com.hazlosano.kmp.domain.model.SleepContent

interface SleepRepository {
    suspend fun getSleepContent(): SleepContent
}
