package com.mokostudio.baselog.feature.ranking

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.MilitaryTech
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mokostudio.baselog.R
import com.mokostudio.baselog.feature.friends.FriendLeaderboardUiState
import com.mokostudio.baselog.feature.friends.LeaderboardMetric
import com.mokostudio.baselog.feature.friends.RankedFriendLeaderboardEntry
import com.mokostudio.baselog.ui.theme.BaseLogTheme
import com.mokostudio.baselog.ui.theme.BorderLight
import com.mokostudio.baselog.ui.theme.Navy900
import com.mokostudio.baselog.ui.theme.Orange500
import com.mokostudio.baselog.ui.theme.SurfaceLight
import com.mokostudio.baselog.ui.theme.TextMuted
import com.mokostudio.baselog.ui.theme.White

@Composable
fun RankingRoute(
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    viewModel: RankingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    RankingScreen(
        contentPadding = contentPadding,
        modifier = modifier,
        uiState = uiState,
        onMetricSelected = viewModel::onMetricSelected,
        onYearSelected = viewModel::onYearSelected
    )
}

@Composable
internal fun RankingScreen(
    contentPadding: PaddingValues,
    uiState: RankingUiState,
    onMetricSelected: (LeaderboardMetric) -> Unit,
    onYearSelected: (Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFFFBF7),
                        SurfaceLight,
                        White
                    )
                )
            )
            .padding(contentPadding)
    ) {
        if (uiState.isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            return@Box
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                RankingHeader(
                    subtitle = stringResource(id = R.string.ranking_subtitle)
                )
            }

            item {
                RankingHeroCard(
                    entry = uiState.myEntry,
                    selectedMetric = uiState.leaderboard.selectedMetric,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                RankingFilterSection(
                    leaderboard = uiState.leaderboard,
                    onMetricSelected = onMetricSelected,
                    onYearSelected = onYearSelected
                )
            }

            uiState.leaderboard.errorMessage?.let { message ->
                item {
                    RankingErrorCard(
                        message = message,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            if (uiState.leaderboard.entries.isEmpty()) {
                item {
                    RankingEmptyCard(modifier = Modifier.fillMaxWidth())
                }
            } else {
                items(uiState.leaderboard.entries, key = { it.userId }) { entry ->
                    when {
                        entry.rank == 1 -> TopRankRow(
                            entry = entry,
                            selectedMetric = uiState.leaderboard.selectedMetric
                        )

                        entry.isCurrentUser -> MyRankRow(
                            entry = entry,
                            selectedMetric = uiState.leaderboard.selectedMetric
                        )

                        else -> LeaderboardRow(
                            entry = entry,
                            selectedMetric = uiState.leaderboard.selectedMetric
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RankingHeader(
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = stringResource(id = R.string.ranking_title),
            style = MaterialTheme.typography.displaySmall,
            color = Navy900,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.titleMedium,
            color = TextMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun RankingHeroCard(
    entry: RankedFriendLeaderboardEntry?,
    selectedMetric: LeaderboardMetric,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(28.dp)
    Box(
        modifier = modifier
            .shadow(
                elevation = 14.dp,
                shape = shape,
                ambientColor = Navy900.copy(alpha = 0.18f),
                spotColor = Navy900.copy(alpha = 0.18f)
            )
            .clip(shape)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF0A1B3C),
                        Color(0xFF132C57),
                        Color(0xFF09162C)
                    ),
                    start = Offset.Zero,
                    end = Offset(1000f, 600f)
                )
            )
            .border(
                width = 1.dp,
                color = White.copy(alpha = 0.08f),
                shape = shape
            )
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Orange500.copy(alpha = 0.14f),
                            Color.Transparent
                        ),
                        center = Offset(820f, 180f),
                        radius = 320f
                    )
                )
        )

        if (entry == null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                RankingLabelPill(
                    text = stringResource(id = R.string.ranking_hero_label),
                    containerColor = White.copy(alpha = 0.12f),
                    contentColor = White
                )
                Text(
                    text = stringResource(id = R.string.ranking_my_rank_empty),
                    style = MaterialTheme.typography.titleMedium,
                    color = White
                )
            }
            return
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 176.dp)
                .padding(horizontal = 22.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            RankingLabelPill(
                text = stringResource(id = R.string.ranking_hero_label),
                containerColor = White.copy(alpha = 0.12f),
                contentColor = White
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.ranking_hero_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFFFFD068),
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = entry.rank.toString(),
                            style = MaterialTheme.typography.displayLarge,
                            color = Color(0xFFFFB347),
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "위",
                            modifier = Modifier.padding(bottom = 10.dp),
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color(0xFFFFB347),
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .height(96.dp)
                        .width(1.dp)
                        .background(White.copy(alpha = 0.14f))
                )

                Column(
                    modifier = Modifier
                        .width(116.dp)
                        .padding(start = 18.dp),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = selectedMetric.icon(),
                            contentDescription = null,
                            tint = White.copy(alpha = 0.92f),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = selectedMetric.label(),
                            style = MaterialTheme.typography.labelLarge,
                            color = White.copy(alpha = 0.92f),
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = entry.value,
                        style = MaterialTheme.typography.headlineLarge,
                        color = Color(0xFFFFB347),
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = entry.supporting,
                        style = MaterialTheme.typography.bodyLarge,
                        color = White.copy(alpha = 0.9f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                TrophyMark(
                    modifier = Modifier
                        .padding(start = 10.dp)
                        .size(72.dp)
                )
            }
        }
    }
}

@Composable
private fun TrophyMark(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(999.dp))
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0x33FFE0A3),
                            Color.Transparent
                        )
                    )
                )
        )
        Icon(
            imageVector = Icons.Outlined.EmojiEvents,
            contentDescription = null,
            tint = Color(0xFFFFB43A),
            modifier = Modifier.size(52.dp)
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 10.dp, end = 8.dp)
                .size(7.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Color(0xFFFFA22E))
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(width = 7.dp, height = 18.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Color(0xCCFF922B))
        )
    }
}

@Composable
private fun RankingFilterSection(
    leaderboard: FriendLeaderboardUiState,
    onMetricSelected: (LeaderboardMetric) -> Unit,
    onYearSelected: (Int?) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        FlowRow(
            maxItemsInEachRow = 3,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            LeaderboardMetric.entries.forEach { metric ->
                RankingFilterChip(
                    label = metric.label(),
                    leadingIcon = metric.icon(),
                    isSelected = leaderboard.selectedMetric == metric,
                    modifier = Modifier.weight(1f),
                    onClick = { onMetricSelected(metric) }
                )
            }
        }

        FlowRow(
            maxItemsInEachRow = 3,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            RankingFilterChip(
                label = stringResource(id = R.string.logbook_filter_all),
                leadingIcon = Icons.Outlined.CalendarMonth,
                isSelected = leaderboard.selectedYear == null,
                modifier = Modifier.weight(1f),
                onClick = { onYearSelected(null) }
            )
            leaderboard.availableYears.forEach { year ->
                RankingFilterChip(
                    label = year.toString(),
                    leadingIcon = Icons.Outlined.CalendarMonth,
                    isSelected = leaderboard.selectedYear == year,
                    modifier = Modifier.weight(1f),
                    onClick = { onYearSelected(year) }
                )
            }
        }
    }
}

@Composable
private fun RankingFilterChip(
    label: String,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(22.dp)
    val containerBrush = if (isSelected) {
        Brush.horizontalGradient(
            colors = listOf(
                Color(0xFFFF941A),
                Orange500
            )
        )
    } else {
        Brush.horizontalGradient(
            colors = listOf(White, Color(0xFFFAFBFF))
        )
    }

    val contentColor = if (isSelected) White else TextMuted
    val borderColor = if (isSelected) Color.Transparent else BorderLight

    Surface(
        modifier = modifier
            .height(54.dp)
            .shadow(
                elevation = if (isSelected) 8.dp else 0.dp,
                shape = shape,
                ambientColor = Orange500.copy(alpha = 0.18f),
                spotColor = Orange500.copy(alpha = 0.18f)
            )
            .clip(shape)
            .clickable(onClick = onClick),
        color = Color.Transparent,
        shape = shape
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(containerBrush)
                .border(
                    width = if (isSelected) 0.dp else 1.dp,
                    color = borderColor,
                    shape = shape
                )
                .padding(horizontal = 12.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = contentColor,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun TopRankRow(
    entry: RankedFriendLeaderboardEntry,
    selectedMetric: LeaderboardMetric,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(26.dp)
    RowCardBase(
        modifier = modifier,
        shape = shape,
        brush = Brush.linearGradient(
            colors = listOf(
                Color(0xFF3A2B14),
                Color(0xFF1B1E24),
                Color(0xFF131720)
            )
        ),
        border = BorderStroke(2.dp, Color(0xFFFFC54F)),
        shadowColor = Color(0x26FFB833)
    ) {
        LeaderboardContent(
            entry = entry,
            selectedMetric = selectedMetric,
            rankBadgeBrush = Brush.linearGradient(
                colors = listOf(Color(0xFFFFD87A), Color(0xFFB37C1C))
            ),
            rankBadgeBorderColor = Color(0xFFFFE8A6),
            rankTextColor = Color(0xFF5A3C00),
            nameColor = White,
            teamColor = White.copy(alpha = 0.86f),
            supportingColor = White.copy(alpha = 0.82f),
            metricChipContainer = Color(0xFF9D6A16),
            metricChipContent = Color(0xFFFFF0CC),
            valueColor = Color(0xFFFFC54F)
        )
    }
}

@Composable
private fun MyRankRow(
    entry: RankedFriendLeaderboardEntry,
    selectedMetric: LeaderboardMetric,
    modifier: Modifier = Modifier
) {
    RowCardBase(
        modifier = modifier,
        shape = RoundedCornerShape(26.dp),
        brush = Brush.linearGradient(
            colors = listOf(
                Color(0xFFFBF9FF),
                Color(0xFFF4EFFF)
            )
        ),
        border = BorderStroke(2.dp, Color(0xFFC9B4FF)),
        shadowColor = Color(0x24A785FF)
    ) {
        LeaderboardContent(
            entry = entry,
            selectedMetric = selectedMetric,
            rankBadgeBrush = Brush.linearGradient(
                colors = listOf(Color(0xFFC9D6FF), Color(0xFF7487D4))
            ),
            rankBadgeBorderColor = Color(0xFFE7ECFF),
            rankTextColor = Color(0xFF32499A),
            nameColor = Navy900,
            teamColor = TextMuted,
            supportingColor = TextMuted,
            metricChipContainer = Color(0xFFD9CCFF),
            metricChipContent = Color(0xFF5F3DF0),
            valueColor = Color(0xFF5136E9)
        )
    }
}

@Composable
private fun LeaderboardRow(
    entry: RankedFriendLeaderboardEntry,
    selectedMetric: LeaderboardMetric,
    modifier: Modifier = Modifier
) {
    RowCardBase(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        brush = Brush.linearGradient(
            colors = listOf(White, Color(0xFFFCFBFF))
        ),
        border = BorderStroke(1.dp, BorderLight.copy(alpha = 0.9f)),
        shadowColor = Color(0x0F101828)
    ) {
        LeaderboardContent(
            entry = entry,
            selectedMetric = selectedMetric,
            rankBadgeBrush = when (entry.rank) {
                2 -> Brush.linearGradient(
                    colors = listOf(Color(0xFFD7DEF0), Color(0xFF8998B8))
                )

                3 -> Brush.linearGradient(
                    colors = listOf(Color(0xFFE5C6B4), Color(0xFF99674D))
                )

                else -> Brush.linearGradient(
                    colors = listOf(Color(0xFFE2E7F3), Color(0xFF95A0B6))
                )
            },
            rankBadgeBorderColor = when (entry.rank) {
                2 -> Color(0xFFF4F6FF)
                3 -> Color(0xFFF2DED2)
                else -> Color(0xFFF5F7FC)
            },
            rankTextColor = when (entry.rank) {
                2 -> Color(0xFF50618D)
                3 -> Color(0xFF6D4630)
                else -> Color(0xFF5C6A84)
            },
            nameColor = Navy900,
            teamColor = TextMuted,
            supportingColor = TextMuted,
            metricChipContainer = Color(0xFFF5E5D9),
            metricChipContent = Color(0xFF9A5A31),
            valueColor = Color(0xFF8C4D29)
        )
    }
}

@Composable
private fun RowCardBase(
    shape: Shape,
    brush: Brush,
    border: BorderStroke,
    shadowColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = shape,
                ambientColor = shadowColor,
                spotColor = shadowColor
            )
            .clip(shape)
            .background(brush)
            .border(border = border, shape = shape)
            .heightIn(min = 112.dp)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        content = content
    )
}

@Composable
private fun LeaderboardContent(
    entry: RankedFriendLeaderboardEntry,
    selectedMetric: LeaderboardMetric,
    rankBadgeBrush: Brush,
    rankBadgeBorderColor: Color,
    rankTextColor: Color,
    nameColor: Color,
    teamColor: Color,
    supportingColor: Color,
    metricChipContainer: Color,
    metricChipContent: Color,
    valueColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RankBadge(
            rank = entry.rank,
            containerBrush = rankBadgeBrush,
            borderColor = rankBadgeBorderColor,
            textColor = rankTextColor
        )
        Spacer(modifier = Modifier.width(14.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = if (entry.isCurrentUser) {
                    stringResource(id = R.string.friends_leaderboard_me, entry.nickname)
                } else {
                    entry.nickname
                },
                style = MaterialTheme.typography.titleLarge,
                color = nameColor,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = entry.favoriteTeamName,
                style = MaterialTheme.typography.bodyLarge,
                color = teamColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = entry.supporting,
                style = MaterialTheme.typography.bodyMedium,
                color = supportingColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Column(
            modifier = Modifier.width(78.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            RankingLabelPill(
                text = selectedMetric.label(),
                containerColor = metricChipContainer,
                contentColor = metricChipContent
            )
            Text(
                text = entry.value,
                style = MaterialTheme.typography.headlineMedium,
                color = valueColor,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun RankBadge(
    rank: Int,
    containerBrush: Brush,
    borderColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    val shape = HexBadgeShape
    Box(
        modifier = modifier
            .size(56.dp)
            .shadow(
                elevation = 6.dp,
                shape = shape,
                ambientColor = borderColor.copy(alpha = 0.18f),
                spotColor = borderColor.copy(alpha = 0.18f)
            )
            .clip(shape)
            .background(containerBrush)
            .border(2.dp, borderColor, shape),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(shape)
                .border(
                    width = 1.dp,
                    color = White.copy(alpha = 0.45f),
                    shape = shape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = rank.toString(),
                style = MaterialTheme.typography.headlineSmall,
                color = textColor,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
private fun RankingLabelPill(
    text: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = containerColor
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelLarge,
            color = contentColor,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun RankingErrorCard(
    message: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        color = Color(0xFFFFF2F0),
        border = BorderStroke(1.dp, Color(0xFFFFD6CF))
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error
        )
    }
}

@Composable
private fun RankingEmptyCard(
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(id = R.string.ranking_title),
                style = MaterialTheme.typography.titleMedium,
                color = Navy900,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(id = R.string.ranking_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted
            )
        }
    }
}

@Composable
private fun LeaderboardMetric.label(): String = when (this) {
    LeaderboardMetric.WinRate -> stringResource(id = R.string.friends_leaderboard_metric_win_rate)
    LeaderboardMetric.TotalGames -> stringResource(id = R.string.friends_leaderboard_metric_total_games)
    LeaderboardMetric.Wins -> stringResource(id = R.string.friends_leaderboard_metric_wins)
}

@Composable
private fun LeaderboardMetric.icon() = when (this) {
    LeaderboardMetric.WinRate -> Icons.Outlined.BarChart
    LeaderboardMetric.TotalGames -> Icons.Outlined.CalendarMonth
    LeaderboardMetric.Wins -> Icons.Outlined.MilitaryTech
}

private val HexBadgeShape = GenericShape { size, _ ->
    moveTo(size.width * 0.5f, 0f)
    lineTo(size.width, size.height * 0.24f)
    lineTo(size.width, size.height * 0.76f)
    lineTo(size.width * 0.5f, size.height)
    lineTo(0f, size.height * 0.76f)
    lineTo(0f, size.height * 0.24f)
    close()
}

@Preview(showBackground = true, backgroundColor = 0xFFF7F8FA)
@Composable
private fun RankingScreenPreview() {
    BaseLogTheme {
        RankingScreen(
            contentPadding = PaddingValues(),
            uiState = RankingUiState(
                leaderboard = FriendLeaderboardUiState(
                    entries = listOf(
                        RankedFriendLeaderboardEntry(
                            rank = 1,
                            userId = "u1",
                            nickname = "moko",
                            favoriteTeamName = "KT Wiz",
                            value = "100%",
                            supporting = "4승 0패 1무",
                            isCurrentUser = false
                        ),
                        RankedFriendLeaderboardEntry(
                            rank = 2,
                            userId = "u2",
                            nickname = "koma",
                            favoriteTeamName = "Hanwha Eagles",
                            value = "50%",
                            supporting = "1승 1패 0무",
                            isCurrentUser = true
                        ),
                        RankedFriendLeaderboardEntry(
                            rank = 3,
                            userId = "u3",
                            nickname = "player3",
                            favoriteTeamName = "LG Twins",
                            value = "60%",
                            supporting = "3승 2패 0무",
                            isCurrentUser = false
                        ),
                        RankedFriendLeaderboardEntry(
                            rank = 4,
                            userId = "u4",
                            nickname = "slayer7",
                            favoriteTeamName = "Samsung Lions",
                            value = "50%",
                            supporting = "3승 3패 0무",
                            isCurrentUser = false
                        )
                    ),
                    availableYears = listOf(2026, 2025),
                    selectedMetric = LeaderboardMetric.WinRate
                ),
                myEntry = RankedFriendLeaderboardEntry(
                    rank = 2,
                    userId = "u2",
                    nickname = "koma",
                    favoriteTeamName = "Hanwha Eagles",
                    value = "50%",
                    supporting = "1승 1패 0무",
                    isCurrentUser = true
                )
            ),
            onMetricSelected = {},
            onYearSelected = {}
        )
    }
}
