package com.hazlosano.core.ui.components.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hazlosano.core.ui.components.atomic.HazloSkeleton
import com.hazlosano.core.ui.components.cards.HazloProductCardDefaults
import com.hazlosano.core.ui.theme.HazloShapes
import com.hazlosano.core.ui.theme.HazloSpaces

/** Etiquetas de prueba de los huecos del tablero. */
object PillarBoardSkeletonTags {
    const val SUMMARY: String = "skeleton_summary"
    const val CHAMPIONS: String = "skeleton_champions"
    const val CAROUSEL: String = "skeleton_carousel"
    const val GRID: String = "skeleton_grid"
    const val GRID_CARD: String = "skeleton_grid_card"
}

/**
 * El hueco del resumen del pilar.
 *
 * Las medidas imitan a `PillarSummaryCard` a propósito: un esqueleto más bajo que lo que llega hace
 * saltar la pantalla justo cuando la persona ya empezó a leer, que es peor que no haber puesto nada.
 */
fun LazyListScope.pillarSummarySkeleton() {
    item {
        HazloSkeleton(
            modifier = Modifier
                .padding(horizontal = HazloSpaces.gutter)
                .fillMaxWidth()
                .height(SUMMARY_HEIGHT)
                .testTag(PillarBoardSkeletonTags.SUMMARY),
            shape = RoundedCornerShape(HazloShapes.card),
        )
    }
}

/** El hueco de los campeones: un título y una fila de tarjetas del ancho de las de verdad. */
fun LazyListScope.pillarHighlightsSkeleton() {
    item { Spacer(modifier = Modifier.height(HazloSpaces.md)) }
    item {
        Column(modifier = Modifier.testTag(PillarBoardSkeletonTags.CHAMPIONS)) {
            HazloSectionTitleSkeleton(modifier = Modifier.padding(horizontal = HazloSpaces.gutter))
            Spacer(modifier = Modifier.height(HazloSpaces.sm))
            CardRowSkeleton(cardWidth = CHAMPION_CARD_WIDTH, cardHeight = CHAMPION_CARD_HEIGHT)
        }
    }
}

/**
 * El hueco del catálogo: un carrusel y la rejilla con su buscador.
 *
 * Se pinta un solo carrusel aunque el tablero pueda traer tres. Reservar sitio para secciones que
 * quizá no existan —un pilar sin eventos no pinta "Próximos eventos"— prometería más de lo que va a
 * llegar.
 */
fun LazyListScope.pillarCatalogSkeleton() {
    item { Spacer(modifier = Modifier.height(HazloSpaces.md)) }
    item {
        Column(modifier = Modifier.testTag(PillarBoardSkeletonTags.CAROUSEL)) {
            HazloSectionTitleSkeleton(modifier = Modifier.padding(horizontal = HazloSpaces.gutter))
            Spacer(modifier = Modifier.height(HazloSpaces.sm))
            CardRowSkeleton(cardWidth = CAROUSEL_CARD_WIDTH, cardHeight = PRODUCT_CARD_HEIGHT)
        }
    }

    item { Spacer(modifier = Modifier.height(HazloSpaces.md)) }
    item {
        Column(
            modifier = Modifier
                .padding(horizontal = HazloSpaces.gutter)
                .testTag(PillarBoardSkeletonTags.GRID),
        ) {
            HazloSectionTitleSkeleton()
            Spacer(modifier = Modifier.height(HazloSpaces.unit))
            HazloSkeleton(
                modifier = Modifier.fillMaxWidth().height(SEARCH_FIELD_HEIGHT),
                shape = RoundedCornerShape(HazloShapes.pill),
            )
            Spacer(modifier = Modifier.height(GRID_SPACING))

            repeat(GRID_ROWS) { row ->
                if (row > 0) Spacer(modifier = Modifier.height(GRID_SPACING))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(GRID_SPACING),
                ) {
                    repeat(GRID_COLUMNS) {
                        HazloSkeleton(
                            modifier = Modifier
                                .weight(1f)
                                .height(PRODUCT_CARD_HEIGHT)
                                .testTag(PillarBoardSkeletonTags.GRID_CARD),
                            shape = HazloProductCardDefaults.containerShape,
                        )
                    }
                }
            }
        }
    }
}

/**
 * El hueco de un encabezado de sección. Público porque lo usan también los huecos de Inicio: un
 * título ausente se ve igual en cualquier pantalla.
 */
@Composable
fun HazloSectionTitleSkeleton(modifier: Modifier = Modifier) {
    HazloSkeleton(
        modifier = modifier.width(SECTION_TITLE_WIDTH).height(SECTION_TITLE_HEIGHT),
        shape = RoundedCornerShape(HazloShapes.chip),
    )
}

/**
 * Una fila de tarjetas que se sale por el borde, como el carrusel de verdad.
 *
 * Es una `Row` y no una `LazyRow` porque no hay nada que desplazar: son dos cajas que sólo están
 * ocupando sitio, y una lista perezosa dentro de otra sólo añadiría trabajo de medida.
 */
@Composable
private fun CardRowSkeleton(cardWidth: Dp, cardHeight: Dp) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = HazloSpaces.gutter),
        horizontalArrangement = Arrangement.spacedBy(HazloSpaces.md),
    ) {
        repeat(ROW_CARDS) {
            HazloSkeleton(modifier = Modifier.width(cardWidth).height(cardHeight))
        }
    }
}

private val SUMMARY_HEIGHT: Dp = 160.dp
private val SECTION_TITLE_WIDTH: Dp = 200.dp
private val SECTION_TITLE_HEIGHT: Dp = 24.dp
private val CHAMPION_CARD_WIDTH: Dp = 200.dp
private val CHAMPION_CARD_HEIGHT: Dp = 120.dp
private val CAROUSEL_CARD_WIDTH: Dp = 170.dp
private val PRODUCT_CARD_HEIGHT: Dp = 228.dp
private val SEARCH_FIELD_HEIGHT: Dp = 56.dp
private val GRID_SPACING: Dp = 16.dp
private const val GRID_COLUMNS = 2
private const val GRID_ROWS = 2
private const val ROW_CARDS = 2
