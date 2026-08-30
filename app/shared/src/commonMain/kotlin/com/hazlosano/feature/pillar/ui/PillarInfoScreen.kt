package com.hazlosano.feature.pillar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hazlosano.core.ui.components.atomic.HazloTopAppBar
import com.hazlosano.core.ui.components.atomic.LeafCard
import com.hazlosano.core.ui.model.pillarIcon
import com.hazlosano.core.ui.model.pillarLabel
import com.hazlosano.core.ui.model.toColor
import com.hazlosano.core.ui.theme.HazloSpaces
import com.hazlosano.domain.model.PillarType
import hazlosano.app.shared.generated.resources.Res
import hazlosano.app.shared.generated.resources.pillar_info_mind_body
import hazlosano.app.shared.generated.resources.pillar_info_mind_practice
import hazlosano.app.shared.generated.resources.pillar_info_mind_tagline
import hazlosano.app.shared.generated.resources.pillar_info_mind_title
import hazlosano.app.shared.generated.resources.pillar_info_movement_body
import hazlosano.app.shared.generated.resources.pillar_info_movement_practice
import hazlosano.app.shared.generated.resources.pillar_info_movement_tagline
import hazlosano.app.shared.generated.resources.pillar_info_movement_title
import hazlosano.app.shared.generated.resources.pillar_info_nutrition_body
import hazlosano.app.shared.generated.resources.pillar_info_nutrition_practice
import hazlosano.app.shared.generated.resources.pillar_info_nutrition_tagline
import hazlosano.app.shared.generated.resources.pillar_info_nutrition_title
import hazlosano.app.shared.generated.resources.pillar_info_practice_label
import hazlosano.app.shared.generated.resources.pillar_info_sleep_body
import hazlosano.app.shared.generated.resources.pillar_info_sleep_practice
import hazlosano.app.shared.generated.resources.pillar_info_sleep_tagline
import hazlosano.app.shared.generated.resources.pillar_info_sleep_title
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** Etiquetas de prueba de la pantalla del pilar. */
object PillarInfoTags {
    const val HERO: String = "pillar_info_hero"
    const val BODY: String = "pillar_info_body"
    const val PRACTICE: String = "pillar_info_practice"
}

/**
 * Qué es un pilar y qué propone.
 *
 * Todo lo que se lee aquí sale del catálogo de recursos, con el texto de hazlosano.com/pilares. No
 * hay repositorio ni ViewModel porque no hay nada que ir a buscar: es contenido editorial fijo, y
 * meterlo en un modelo de dominio sólo habría alejado la copia del único sitio donde se puede
 * traducir.
 */
@Composable
fun PillarInfoScreen(
    pillar: PillarType,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = pillar.toColor()
    val text = pillarInfoText(pillar)

    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        HazloTopAppBar(
            title = pillarLabel(pillar),
            showBackButton = true,
            onBackClick = onBack,
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = HazloSpaces.md,
                bottom = HazloSpaces.xl,
            ),
        ) {
            item {
                PillarInfoHero(
                    pillar = pillar,
                    title = stringResource(text.title),
                    tagline = stringResource(text.tagline),
                )
            }

            item { Spacer(modifier = Modifier.height(HazloSpaces.md)) }

            item {
                Text(
                    text = stringResource(text.body),
                    modifier = Modifier
                        .padding(horizontal = HazloSpaces.gutter)
                        .testTag(PillarInfoTags.BODY),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            item { Spacer(modifier = Modifier.height(HazloSpaces.md)) }

            item {
                PillarPracticeCard(
                    label = stringResource(Res.string.pillar_info_practice_label),
                    practice = stringResource(text.practice),
                    accent = accent,
                )
            }
        }
    }
}

@Composable
private fun PillarInfoHero(
    pillar: PillarType,
    title: String,
    tagline: String,
) {
    val accent = pillar.toColor()

    Column(
        modifier = Modifier
            .padding(horizontal = HazloSpaces.gutter)
            .testTag(PillarInfoTags.HERO),
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = BADGE_ALPHA)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = pillarIcon(pillar),
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(32.dp),
            )
        }

        Spacer(modifier = Modifier.height(HazloSpaces.sm))

        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Spacer(modifier = Modifier.height(HazloSpaces.xs))

        Text(
            text = tagline,
            style = MaterialTheme.typography.titleMedium,
            color = accent,
        )
    }
}

/** La práctica que el pilar propone, en su propia tarjeta: es lo accionable de la pantalla. */
@Composable
private fun PillarPracticeCard(
    label: String,
    practice: String,
    accent: androidx.compose.ui.graphics.Color,
) {
    LeafCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = HazloSpaces.gutter)
            .testTag(PillarInfoTags.PRACTICE),
        tonalElevation = 2.dp,
    ) {
        Column(modifier = Modifier.padding(HazloSpaces.md)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = accent,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(HazloSpaces.xs))
            Text(
                text = practice,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

/**
 * Qué cadenas le tocan a cada pilar.
 *
 * Un solo `when` con las cuatro filas: cuando entre un quinto pilar, el compilador señala este sitio
 * y no cuatro repartidos por la pantalla.
 */
private data class PillarInfoText(
    val title: StringResource,
    val tagline: StringResource,
    val body: StringResource,
    val practice: StringResource,
)

private fun pillarInfoText(pillar: PillarType): PillarInfoText = when (pillar) {
    PillarType.SLEEP -> PillarInfoText(
        title = Res.string.pillar_info_sleep_title,
        tagline = Res.string.pillar_info_sleep_tagline,
        body = Res.string.pillar_info_sleep_body,
        practice = Res.string.pillar_info_sleep_practice,
    )

    PillarType.NUTRITION -> PillarInfoText(
        title = Res.string.pillar_info_nutrition_title,
        tagline = Res.string.pillar_info_nutrition_tagline,
        body = Res.string.pillar_info_nutrition_body,
        practice = Res.string.pillar_info_nutrition_practice,
    )

    PillarType.MOVEMENT -> PillarInfoText(
        title = Res.string.pillar_info_movement_title,
        tagline = Res.string.pillar_info_movement_tagline,
        body = Res.string.pillar_info_movement_body,
        practice = Res.string.pillar_info_movement_practice,
    )

    PillarType.MIND -> PillarInfoText(
        title = Res.string.pillar_info_mind_title,
        tagline = Res.string.pillar_info_mind_tagline,
        body = Res.string.pillar_info_mind_body,
        practice = Res.string.pillar_info_mind_practice,
    )
}

private const val BADGE_ALPHA = 0.25f
