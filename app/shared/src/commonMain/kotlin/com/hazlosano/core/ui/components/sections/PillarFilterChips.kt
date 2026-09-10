package com.hazlosano.core.ui.components.sections

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.hazlosano.core.ui.model.palette
import com.hazlosano.core.ui.model.pillarShortLabel
import com.hazlosano.core.ui.theme.HazloSpaces
import com.hazlosano.domain.model.PillarType
import hazlosano.app.shared.generated.resources.Res
import hazlosano.app.shared.generated.resources.publication_filter_all
import org.jetbrains.compose.resources.stringResource

/** Etiquetas de prueba: los chips se afirman por aquí y no por su redacción. */
object PillarFilterTags {
    const val ROW: String = "pillar_filter_row"

    fun chip(pillar: PillarType?): String = "pillar_filter_${pillar?.key ?: "all"}"
}

/**
 * Los chips que eligen pilar, más «Todos».
 *
 * «Todos» no es un pilar más disfrazado: es el filtro ausente, y por eso su valor es `null` — lo
 * mismo que el sitio entiende por no mandar el parámetro. Un `PillarType.ALL` inventado habría
 * obligado a cada sitio que recorre los pilares a acordarse de saltárselo.
 *
 * Lo usan las dos listas de publicaciones —el feed del Inicio y los resultados de la búsqueda—
 * acotando cosas distintas con el mismo gesto.
 */
@Composable
fun PillarFilterChips(
    selected: PillarType?,
    onSelect: (PillarType?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = HazloSpaces.gutter)
            .testTag(PillarFilterTags.ROW),
        horizontalArrangement = Arrangement.spacedBy(HazloSpaces.sm),
    ) {
        PillarFilterChip(
            pillar = null,
            label = stringResource(Res.string.publication_filter_all),
            selected = selected == null,
            onSelect = onSelect,
        )
        PillarType.entries.forEach { pillar ->
            PillarFilterChip(
                pillar = pillar,
                label = pillarShortLabel(pillar),
                selected = selected == pillar,
                onSelect = onSelect,
            )
        }
    }
}

@Composable
private fun PillarFilterChip(
    pillar: PillarType?,
    label: String,
    selected: Boolean,
    onSelect: (PillarType?) -> Unit,
) {
    // Cada pilar se pinta con el suyo; «Todos» con el color primario, porque no es de ninguno.
    val accent = pillar?.palette()?.solid ?: MaterialTheme.colorScheme.primary

    FilterChip(
        selected = selected,
        onClick = { onSelect(pillar) },
        label = { Text(label) },
        modifier = Modifier.testTag(PillarFilterTags.chip(pillar)),
        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = accent),
    )
}
