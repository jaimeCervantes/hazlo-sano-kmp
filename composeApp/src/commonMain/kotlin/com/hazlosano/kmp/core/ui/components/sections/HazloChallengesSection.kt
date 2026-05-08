package com.hazlosano.kmp.core.ui.components.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.hazlosano.kmp.core.ui.components.atomic.SectionHeader
import com.hazlosano.kmp.core.ui.components.cards.HazloChallengeCard
import com.hazlosano.kmp.core.ui.theme.HazloSpaces
import com.hazlosano.kmp.domain.model.HazloChallenge

@Composable
fun HazloChallengesSection(
    challenges: List<HazloChallenge>,
    modifier: Modifier = Modifier,
    title: String = HazloSectionDefaults.activeChallengesTitle,
    challengeCardWidth: Dp = HazloSectionDefaults.challengeCardWidth,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    onChallengeClick: (HazloChallenge) -> Unit = {},
) {
    Column(modifier = modifier) {
        SectionHeader(
            title = title,
            modifier = Modifier.padding(horizontal = HazloSpaces.gutter),
            accentColor = accentColor,
        )
        Spacer(modifier = Modifier.height(HazloSpaces.sm))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(HazloSpaces.md),
            contentPadding = PaddingValues(horizontal = HazloSpaces.gutter),
        ) {
            items(challenges) { challenge ->
                HazloChallengeCard(
                    title = challenge.title,
                    description = challenge.description,
                    progressText = challenge.progressText,
                    progress = challenge.progress,
                    imageUrl = challenge.imageUrl,
                    accentColor = accentColor,
                    onClick = { onChallengeClick(challenge) },
                    modifier = Modifier.width(challengeCardWidth),
                )
            }
        }
    }
}
