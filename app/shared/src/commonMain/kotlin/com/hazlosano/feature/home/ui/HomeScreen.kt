package com.hazlosano.feature.home.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hazlosano.core.ui.components.atomic.AsyncImageBackground
import com.hazlosano.core.ui.components.atomic.HazloAsyncImage
import com.hazlosano.core.ui.components.atomic.LeafCard
import com.hazlosano.core.ui.components.atomic.PillarBadge
import com.hazlosano.core.ui.components.atomic.SectionHeader
import com.hazlosano.core.ui.components.atomic.SleepSummaryCard
import com.hazlosano.core.ui.components.sections.HazloChampionsSection
import com.hazlosano.core.ui.model.toColor
import com.hazlosano.core.ui.theme.HazloShapes
import com.hazlosano.core.ui.theme.HazloSpaces
import com.hazlosano.core.ui.theme.PillarSleep
import com.hazlosano.domain.model.FeedPost
import com.hazlosano.domain.model.HomeContent
import com.hazlosano.domain.model.PillarAction
import com.hazlosano.domain.model.PillarOverview
import com.hazlosano.domain.model.PillarType
import com.hazlosano.domain.model.SleepAnalysis
import com.hazlosano.feature.home.presentation.HomeUiState
import com.hazlosano.feature.home.presentation.HomeViewModel

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    sleepAnalysis: SleepAnalysis? = null,
    onNavigateToMovement: () -> Unit = {},
    onNavigateToTracker: () -> Unit = {},
    onRefreshSleep: (() -> Unit)? = null,
    onSleepCardClick: (() -> Unit)? = null,
) {
    val uiState by viewModel.uiState.collectAsState()

    when (val state = uiState) {
        is HomeUiState.Loading -> LoadingContent()
        is HomeUiState.Error -> ErrorContent(state.message)
        is HomeUiState.Success -> HomeContent(
            content = state.content,
            sleepAnalysis = sleepAnalysis,
            onNavigateToMovement = onNavigateToMovement,
            onNavigateToTracker = onNavigateToTracker,
            onRefreshSleep = onRefreshSleep,
            onSleepCardClick = onSleepCardClick,
        )
    }
}

@Composable
private fun LoadingContent() {
    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun ErrorContent(message: String) {
    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
        Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun HomeContent(
    content: HomeContent,
    sleepAnalysis: SleepAnalysis?,
    onNavigateToMovement: () -> Unit,
    onNavigateToTracker: () -> Unit,
    onRefreshSleep: (() -> Unit)?,
    onSleepCardClick: (() -> Unit)?,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(top = HazloSpaces.default, bottom = HazloSpaces.xl),
    ) {
        if (sleepAnalysis != null) {
            item {
                SleepSummaryCard(
                    analysis = sleepAnalysis,
                    accentColor = PillarSleep,
                    modifier = Modifier.padding(horizontal = HazloSpaces.gutter),
                    onRefresh = onRefreshSleep,
                    onClick = onSleepCardClick,
                )
            }
            item { Spacer(modifier = Modifier.height(HazloSpaces.md)) }
        }

        item { PillarsOverviewSection(content.pillars, onNavigateToTracker) }
        item {
            HazloChampionsSection(
                champions = content.champions,
                accentColor = MaterialTheme.colorScheme.primary,
            )
        }
        item {
            Spacer(modifier = Modifier.height(HazloSpaces.lg))
            SectionHeader(
                title = "Feed de la Tribu",
                modifier = Modifier.padding(horizontal = HazloSpaces.gutter),
                actionText = null,
            )
            Spacer(modifier = Modifier.height(HazloSpaces.sm))
        }
        items(content.feedPosts.size) { index ->
            FeedPostCard(content.feedPosts[index])
        }
    }
}

@Composable
private fun HeaderSection(title: String, subtitle: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = HazloSpaces.gutter, vertical = HazloSpaces.gutter),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PillarsOverviewSection(pillars: List<PillarOverview>, onNavigateToTracker: () -> Unit) {
    val largePillars = pillars.take(2)
    val smallPillars = pillars.drop(2)

    Column(modifier = Modifier.padding(horizontal = HazloSpaces.gutter)) {
        for (pillar in largePillars) {
            LargePillarCard(pillar = pillar, onNavigateToTracker = onNavigateToTracker)
            Spacer(modifier = Modifier.height(HazloSpaces.sm))
        }
        if (smallPillars.isNotEmpty()) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(HazloSpaces.sm)) {
                for (pillar in smallPillars) {
                    SmallPillarCard(pillar = pillar, modifier = Modifier.weight(1f))
                }
            }
        }
        Spacer(modifier = Modifier.height(HazloSpaces.lg))
    }
}

@Composable
private fun LargePillarCard(pillar: PillarOverview, onNavigateToTracker: () -> Unit) {
    val pillarColor = pillar.pillarType.toColor()
    val onClick: (() -> Unit)? = when (pillar.action) {
        PillarAction.TRACKER -> onNavigateToTracker
        null -> null
    }

    LeafCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(140.dp),
        tonalElevation = 4.dp,
    ) {
        AsyncImageBackground(imageUrl = pillar.imageUrl, contentDescription = null) {
            Row(
                modifier = Modifier.fillMaxSize().padding(HazloSpaces.md),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(pillar.title, style = MaterialTheme.typography.titleMedium, color = pillarColor, fontWeight = FontWeight.Bold)
                    Text(pillar.stat, style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Bold)
                    Text(pillar.subtitle, style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.8f))
                }
                if (pillar.action != null) {
                    Box(
                        modifier = Modifier.size(48.dp).clip(RoundedCornerShape(HazloShapes.lg)).background(pillarColor),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.DirectionsRun, contentDescription = null, tint = Color.White)
                    }
                } else {
                    val icon = pillarIcon(pillar.pillarType)
                    Icon(imageVector = icon, contentDescription = null, tint = pillarColor, modifier = Modifier.size(48.dp))
                }
            }
        }
    }
}

@Composable
private fun SmallPillarCard(pillar: PillarOverview, modifier: Modifier = Modifier) {
    val pillarColor = pillar.pillarType.toColor()

    LeafCard(modifier = modifier.aspectRatio(1f), tonalElevation = 2.dp) {
        AsyncImageBackground(imageUrl = pillar.imageUrl, contentDescription = pillar.title) {
            Column(
                modifier = Modifier.fillMaxSize().padding(HazloSpaces.md),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Icon(
                    imageVector = pillarIcon(pillar.pillarType),
                    contentDescription = pillar.title,
                    tint = pillarColor,
                    modifier = Modifier.size(28.dp),
                )
                Column {
                    Text(text = pillar.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = pillarColor)
                    Text(text = pillar.subtitle, style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.9f))
                }
            }
        }
    }
}

@Composable
private fun FeedPostCard(post: FeedPost) {
    val pillarColor = post.pillarType.toColor()

    LeafCard(
        modifier = Modifier.fillMaxWidth().padding(horizontal = HazloSpaces.gutter, vertical = HazloSpaces.unit),
        tonalElevation = 1.dp,
    ) {
        Column(modifier = Modifier.padding(HazloSpaces.md)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    HazloAsyncImage(
                        model = "https://api.dicebear.com/7.x/notionists/png?seed=${post.avatarSeed}&backgroundColor=transparent",
                        contentDescription = null,
                        modifier = Modifier.size(48.dp)
                            .clip(RoundedCornerShape(HazloShapes.xl))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                    )
                    Spacer(modifier = Modifier.width(HazloSpaces.sm))
                    Column {
                        Text(post.userName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(post.timeAgo, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                PillarBadge(label = post.pillarType.label, color = pillarColor)
            }
            Spacer(modifier = Modifier.height(HazloSpaces.md))
            Text(text = post.content, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            if (post.imageUrl != null) {
                Spacer(modifier = Modifier.height(HazloSpaces.md))
                HazloAsyncImage(
                    model = post.imageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(HazloShapes.lg)),
                    contentScale = ContentScale.Crop,
                )
            }
            Spacer(modifier = Modifier.height(HazloSpaces.md))
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(HazloSpaces.sm))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(HazloSpaces.md)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Favorite,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(HazloSpaces.xs))
                    Text(text = post.likes.toString(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.ChatBubble,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(HazloSpaces.xs))
                    Text(text = post.comments.toString(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    imageVector = Icons.Filled.Share,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun pillarIcon(type: PillarType): ImageVector = when (type) {
    PillarType.SLEEP -> Icons.Filled.Bedtime
    PillarType.MOVEMENT -> Icons.AutoMirrored.Filled.DirectionsRun
    PillarType.NUTRITION -> Icons.Filled.Restaurant
    PillarType.MIND -> Icons.Filled.SelfImprovement
}
