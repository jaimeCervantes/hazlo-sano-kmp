package com.hazlosano.feature.sleep.presentation

import com.hazlosano.domain.model.SleepAnalysis

sealed interface SleepUiState {
    data object Loading : SleepUiState

    /**
     * Lo único que esta pantalla carga por su cuenta es el análisis de la última noche. Los
     * campeones, los retos y el catálogo llegan por separado, cada uno con su propia espera.
     */
    data class Success(val sleepAnalysis: SleepAnalysis? = null) : SleepUiState

    data class Error(val message: String) : SleepUiState
}
