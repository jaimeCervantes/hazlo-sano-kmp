package com.hazlosano.feature.home.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.hazlosano.data.catalog.catalogRepository
import com.hazlosano.domain.usecase.GetCatalogUseCase

/**
 * Arma el feed del Inicio con lo que este target tenga.
 *
 * **Sin clave en el `remember`, y a propósito.** El filtro por pilar vive dentro del ViewModel, no
 * fuera: si la clave fuera el filtro, cambiar de chip construiría uno nuevo y tiraría lo cargado —
 * que es lo que se quiere al cambiar de filtro, sí, pero también tiraría el filtro anterior al
 * volver a él. Reiniciar es decisión de `select`, que sabe cuándo hace falta y cuándo no.
 */
@Composable
fun rememberHomeFeedViewModel(): HomeFeedViewModel = remember {
    HomeFeedViewModel(getCatalog = GetCatalogUseCase(catalogRepository()))
}
