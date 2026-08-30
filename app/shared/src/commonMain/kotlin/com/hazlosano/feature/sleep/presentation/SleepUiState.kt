package com.hazlosano.feature.sleep.presentation

import com.hazlosano.domain.model.SleepAnalysis

sealed interface SleepUiState {
    data object Loading : SleepUiState

    /**
     * Lo único que esta pantalla carga por su cuenta es el análisis de la última noche. Los
     * campeones, los retos y el catálogo llegan por separado, cada uno con su propia espera.
     */
    data class Success(val sleepAnalysis: SleepAnalysis? = null) : SleepUiState

    /**
     * Sin texto redactado: la palabra la elige la UI leyendo el catálogo de recursos. El `message`
     * de una excepción no es copia que nadie quiera leer, y encima no se puede traducir.
     */
    data object Failed : SleepUiState
}
