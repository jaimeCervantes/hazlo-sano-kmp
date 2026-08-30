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
import com.hazlosano.core.ui.components.sections.HazloSectionTitleSkeleton
import com.hazlosano.core.ui.theme.HazloShapes
import com.hazlosano.core.ui.theme.HazloSpaces

/** Etiquetas de prueba de los huecos de Inicio. */
object HomeSkeletonTags {
    const val PILLARS: String = "skeleton_home_pillars"
    const val FEED: String = "skeleton_home_feed"
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

/** El hueco del feed: su encabezado y un par de publicaciones. */
fun LazyListScope.homeFeedSkeleton() {
    item {
        Column(modifier = Modifier.testTag(HomeSkeletonTags.FEED)) {
            HazloSectionTitleSkeleton(modifier = Modifier.padding(horizontal = HazloSpaces.gutter))
            Spacer(modifier = Modifier.height(HazloSpaces.sm))
            repeat(FEED_POSTS) {
                HazloSkeleton(
                    modifier = Modifier
                        .padding(horizontal = HazloSpaces.gutter, vertical = HazloSpaces.unit)
                        .fillMaxWidth()
                        .height(FEED_POST_HEIGHT),
                    shape = CARD_SHAPE,
                )
            }
        }
    }
}

private val CARD_SHAPE = RoundedCornerShape(HazloShapes.lg)
private val LARGE_PILLAR_HEIGHT: Dp = 140.dp
private val FEED_POST_HEIGHT: Dp = 320.dp
private const val SMALL_PILLARS = 2
private const val FEED_POSTS = 2
