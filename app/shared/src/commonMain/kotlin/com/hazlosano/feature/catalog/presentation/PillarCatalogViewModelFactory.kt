package com.hazlosano.feature.catalog.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.hazlosano.data.catalog.catalogRepository
import com.hazlosano.data.location.readVisitorLocation
import com.hazlosano.domain.model.PillarType
import com.hazlosano.domain.usecase.GetPillarCatalogUseCase

/**
 * Arma el catálogo de un pilar con lo que este target tenga.
 *
 * Igual que el de rutas: no hay contenedor de inyección en el proyecto, así que la composición pasa
 * aquí en vez de repartirse por los Composables.
 *
 * La clave del `remember` es el pilar, para que cambiar de pestaña no reutilice el ViewModel de la
 * anterior y enseñe publicaciones de otro pilar mientras carga.
 */
@Composable
fun rememberPillarCatalogViewModel(pillar: PillarType): PillarCatalogViewModel =
    remember(pillar) {
        PillarCatalogViewModel(
            pillar = pillar,
            getPillarCatalog = GetPillarCatalogUseCase(catalogRepository()),
            readLocation = ::readVisitorLocation,
        )
    }
