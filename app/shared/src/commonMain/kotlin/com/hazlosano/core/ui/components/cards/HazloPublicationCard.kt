package com.hazlosano.core.ui.components.cards

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import com.hazlosano.core.ui.components.atomic.HazloAsyncImage
import com.hazlosano.domain.model.HazloProduct
import com.hazlosano.domain.model.PublicationKind
import hazlosano.app.shared.generated.resources.Res
import hazlosano.app.shared.generated.resources.publication_price_free
import org.jetbrains.compose.resources.stringResource

/**
 * Una publicación del sitio, pintada como tarjeta.
 *
 * `HazloProductCard` es atómica y no sabe qué es una publicación: recibe título, precio y una imagen
 * ya resuelta. Traducir un [HazloProduct] a esos parámetros es conocimiento del catálogo, y estaba
 * escrito dentro de `HazloExploreProductsSection` — donde sólo lo alcanzaba esa rejilla.
 *
 * Sale con nombre propio al aparecer el segundo sitio que pinta publicaciones: el feed del Inicio.
 * Copiar las veinticinco líneas habría sido el segundo componente casi idéntico que `AGENTS.md`
 * llama fallo de diseño, y con él dos sitios donde arreglar el mismo detalle.
 */
@Composable
fun HazloPublicationCard(
    publication: HazloProduct,
    onFavoriteClick: () -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    /**
     * Qué decir encima del título: cuánto dura un servicio, cuándo ocurre un evento, nada en lo
     * demás. Lo resuelve quien llama, porque depende de la pantalla y no de la publicación.
     */
    overlineLabel: String? = null,
) {
    // Un evento sin precio es gratis y se dice; un anuncio sin precio simplemente no se vende, y
    // ahí la línea se queda vacía en lugar de anunciar un "Gratis" que no significa nada.
    val freeLabel = stringResource(Res.string.publication_price_free)

    HazloProductCard(
        title = publication.name,
        description = publication.description,
        price = publication.price,
        priceFallbackLabel = freeLabel.takeIf { publication.kind == PublicationKind.EVENT },
        overlineLabel = overlineLabel,
        isFavorite = publication.isFavorite,
        distanceMeters = publication.distanceMeters,
        onFavoriteClick = onFavoriteClick,
        accentColor = accentColor,
        modifier = modifier,
        imageContent = {
            if (publication.imageUrl.isBlank()) {
                HazloProductCardImagePlaceholder(
                    title = publication.name,
                    accentColor = accentColor,
                )
            } else {
                HazloAsyncImage(
                    model = publication.imageUrl,
                    contentDescription = publication.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        },
    )
}
