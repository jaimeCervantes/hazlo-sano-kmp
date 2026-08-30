package com.hazlosano.core.ui.components.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.hazlosano.core.ui.components.atomic.SectionHeader
import com.hazlosano.core.ui.components.cards.HazloChampionCard
import com.hazlosano.core.ui.theme.HazloSpaces
import com.hazlosano.domain.model.HazloChampion
import hazlosano.app.shared.generated.resources.Res
import hazlosano.app.shared.generated.resources.section_view_all
import hazlosano.app.shared.generated.resources.section_weekly_champions
import org.jetbrains.compose.resources.stringResource

@Composable
fun HazloChampionsSection(
    champions: List<HazloChampion>,
    modifier: Modifier = Modifier,
    title: String = stringResource(Res.string.section_weekly_champions),
    accentColor: Color = MaterialTheme.colorScheme.primary,
    actionText: String? = stringResource(Res.string.section_view_all),
    onActionClick: (() -> Unit)? = null,
) {
    Column(modifier = modifier) {
        SectionHeader(
            title = title,
            modifier = Modifier.padding(horizontal = HazloSpaces.gutter),
            actionText = actionText,
            accentColor = accentColor,
            onActionClick = onActionClick,
        )
        Spacer(modifier = Modifier.height(HazloSpaces.sm))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(HazloSpaces.md),
            contentPadding = PaddingValues(horizontal = HazloSpaces.gutter),
        ) {
            items(champions) { champion ->
                HazloChampionCard(
                    name = champion.name,
                    title = champion.title,
                    stat = champion.stat,
                    imageUrl = champion.imageUrl,
                    accentColor = accentColor,
                )
            }
        }
    }
}
