package com.hazlosano.feature.home.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hazlosano.core.ui.components.atomic.HazloSkeleton
import com.hazlosano.core.ui.components.atomic.SleepSummaryCardDefaults
import com.hazlosano.core.ui.components.sections.HazloSectionTitleSkeleton
import com.hazlosano.core.ui.components.sections.publicationListSkeleton
import com.hazlosano.core.ui.theme.HazloShapes
import com.hazlosano.core.ui.theme.HazloSpaces

/** Etiquetas de prueba de los huecos de Inicio. */
object HomeSkeletonTags {
    const val SLEEP: String = "skeleton_home_sleep"
    const val PILLARS: String = "skeleton_home_pillars"
    const val FEED: String = "skeleton_home_feed"
}

/**
 * El hueco del resumen de anoche, **del alto exacto de la tarjeta**.
 *
 * Lee `SleepSummaryCardDefaults.height`, la misma constante que la tarjeta aplica como alto mínimo,
 * así que no pueden desincronizarse. Sin este hueco, el resumen no ocupaba nada mientras se leía de
 * la base de datos y al llegar empujaba hacia abajo los cuatro pilares y todo lo demás.
 */
fun LazyListScope.sleepSummarySkeleton() {
    item {
        HazloSkeleton(
            modifier = Modifier
                .padding(horizontal = HazloSpaces.gutter)
                .fillMaxWidth()
                .height(SleepSummaryCardDefaults.height)
                .testTag(HomeSkeletonTags.SLEEP),
            shape = CARD_SHAPE,
        )
    }
}

/**
 * El hueco de la rejilla de pilares: una tarjeta ancha arriba y dos cuadradas debajo, que es la
 * forma que tiene cuando llega.
 *
 * Vive en la pantalla de Inicio y no en `core/ui/components/` porque esa rejilla es de aquí: ninguna
 * otra pantalla la pinta. El día que una segunda la quiera, se promueve.
 */
fun LazyListScope.homePillarsSkeleton() {
    item {
        Column(
            modifier = Modifier
                .padding(horizontal = HazloSpaces.gutter)
                .testTag(HomeSkeletonTags.PILLARS),
        ) {
            HazloSkeleton(
                modifier = Modifier.fillMaxWidth().height(LARGE_PILLAR_HEIGHT),
                shape = CARD_SHAPE,
            )
            Spacer(modifier = Modifier.height(HazloSpaces.sm))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(HazloSpaces.sm),
            ) {
                repeat(SMALL_PILLARS) {
                    HazloSkeleton(
                        modifier = Modifier.weight(1f).aspectRatio(1f),
                        shape = CARD_SHAPE,
                    )
                }
            }
        }
    }
    item { Spacer(modifier = Modifier.height(HazloSpaces.lg)) }
}

/**
 * El hueco del feed: su encabezado, y las publicaciones que dibuja la lista.
 *
 * **Las filas ya no se dibujan aquí.** Dibujaba dos tarjetas de ancho completo de 320 dp mientras el
 * feed llevaba meses yendo de dos en dos: un hueco con otra forma que la lista que espera es un
 * brinco con pasos intermedios, no un brinco menos. Ahora las pone `publicationListSkeleton`, la
 * misma que usan los resultados de la búsqueda, así que sólo hay una forma que mantener.
 */
fun LazyListScope.homeFeedSkeleton() {
    item {
        Column(modifier = Modifier.testTag(HomeSkeletonTags.FEED)) {
            HazloSectionTitleSkeleton(modifier = Modifier.padding(horizontal = HazloSpaces.gutter))
            Spacer(modifier = Modifier.height(HazloSpaces.sm))
        }
    }
    publicationListSkeleton()
}

private val CARD_SHAPE = RoundedCornerShape(HazloShapes.card)
private val LARGE_PILLAR_HEIGHT: Dp = 140.dp
private const val SMALL_PILLARS = 2
