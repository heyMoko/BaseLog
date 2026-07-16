package com.mokostudio.baselog.feature.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.automirrored.outlined.StickyNote2
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.InsertChart
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mokostudio.baselog.R
import com.mokostudio.baselog.feature.log.BaseballGameResult
import com.mokostudio.baselog.ui.theme.BaseLogTheme

@Composable
fun HomeRoute(
    contentPadding: PaddingValues,
    onAddLogClick: () -> Unit,
    onViewLogsClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    HomeScreen(
        modifier = modifier.padding(contentPadding),
        uiState = uiState,
        onAddLogClick = onAddLogClick,
        onViewLogsClick = onViewLogsClick
    )
}

@Composable
internal fun HomeScreen(
    uiState: HomeUiState,
    onAddLogClick: () -> Unit,
    onViewLogsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(start = 24.dp, end = 24.dp, top = 20.dp)
    ) {
        if (uiState.isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            return@Box
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            BaseLogBrandHeader(modifier = Modifier.fillMaxWidth())

            WinRateGaugeCard(
                summary = uiState.logSummary,
                modifier = Modifier.fillMaxWidth()
            )

            RecentGameLogsCard(
                logs = uiState.logSummary.recentLogs,
                onViewLogsClick = onViewLogsClick,
                modifier = Modifier.fillMaxWidth()
            )

            HomeCallToActionCard(
                onAddLogClick = onAddLogClick,
                modifier = Modifier.fillMaxWidth()
            )

            if (uiState.isProfileUnavailable) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = stringResource(id = R.string.home_profile_unavailable_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(id = R.string.home_profile_unavailable_body),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BaseLogBrandHeader(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(top = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(48.dp),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFFFFF3EA),
            shadowElevation = 3.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Image(
                    painter = painterResource(id = R.drawable.baselog_mark),
                    contentDescription = null,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = stringResource(id = R.string.app_name),
            style = MaterialTheme.typography.displaySmall,
            color = Color(0xFF081321),
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun HomeCallToActionCard(
    onAddLogClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = stringResource(id = R.string.home_cta_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = stringResource(id = R.string.home_cta_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Button(
                onClick = onAddLogClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(id = R.string.home_cta_action))
            }
        }
    }
}

@Composable
private fun WinRateGaugeCard(
    summary: HomeLogSummary,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            DashboardCardHeader()
            GaugeSection(summary = summary)
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            StatsRow(summary = summary)
            SummaryMessageBanner(
                message = summary.overallMessage
                    ?: stringResource(id = R.string.home_dashboard_total_caption)
            )
        }
    }
}

@Composable
private fun DashboardCardHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HomeHeaderIcon(
            icon = {
                Icon(
                    imageVector = Icons.Outlined.InsertChart,
                    contentDescription = null,
                    tint = Color(0xFF6A5AE0)
                )
            }
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(id = R.string.home_dashboard_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = stringResource(id = R.string.home_dashboard_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun GaugeSection(summary: HomeLogSummary) {
    val progressTarget = (summary.overallWinRatePercent ?: 0) / 100f
    val animatedProgress by animateFloatAsState(
        targetValue = progressTarget,
        animationSpec = tween(durationMillis = 900),
        label = "homeGauge"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        WinRateGauge(
            progress = animatedProgress,
            modifier = Modifier.size(292.dp)
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(id = R.string.home_dashboard_overall),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            Text(
                text = summary.overallWinRatePercent?.let { "$it%" }
                    ?: stringResource(id = R.string.home_dashboard_empty_value),
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = summary.overallRecord ?: stringResource(id = R.string.home_dashboard_empty_record),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun WinRateGauge(
    progress: Float,
    modifier: Modifier = Modifier
) {
    val trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    val activeColor = MaterialTheme.colorScheme.primary

    Canvas(modifier = modifier) {
        val strokeWidth = 22.dp.toPx()
        val diameter = size.minDimension - strokeWidth
        val topLeft = Offset(
            x = (size.width - diameter) / 2f,
            y = (size.height - diameter) / 2f
        )
        val arcSize = Size(diameter, diameter)

        drawArc(
            color = trackColor,
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
        drawArc(
            color = activeColor,
            startAngle = -90f,
            sweepAngle = 360f * progress.coerceIn(0f, 1f),
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
    }
}

@Composable
private fun StatsRow(summary: HomeLogSummary) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        HomeStatItem(
            modifier = Modifier.weight(1f),
            icon = {
                Icon(
                    imageVector = Icons.Outlined.EmojiEvents,
                    contentDescription = null,
                    tint = Color(0xFF6A5AE0)
                )
            },
            title = stringResource(id = R.string.home_stats_overall_record),
            value = summary.overallRecord ?: stringResource(id = R.string.home_dashboard_empty_record),
            emphasize = false
        )
        VerticalStatDivider()
        HomeStatItem(
            modifier = Modifier.weight(1f),
            icon = {
                Icon(
                    imageVector = Icons.Outlined.CalendarMonth,
                    contentDescription = null,
                    tint = Color(0xFF6A5AE0)
                )
            },
            title = stringResource(id = R.string.home_dashboard_year, summary.currentYear),
            value = summary.currentYearWinRatePercent?.let { "$it%" }
                ?: stringResource(id = R.string.home_dashboard_empty_value),
            emphasize = true
        )
        VerticalStatDivider()
        HomeStatItem(
            modifier = Modifier.weight(1f),
            icon = {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.StickyNote2,
                    contentDescription = null,
                    tint = Color(0xFF6A5AE0)
                )
            },
            title = stringResource(id = R.string.home_dashboard_total_games),
            value = summary.totalGames.toString(),
            emphasize = true
        )
    }
}

@Composable
private fun VerticalStatDivider() {
    Spacer(
        modifier = Modifier
            .padding(top = 10.dp)
            .height(88.dp)
            .width(1.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant)
    )
}

@Composable
private fun HomeStatItem(
    title: String,
    value: String,
    emphasize: Boolean,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        HomeHeaderIcon(icon = icon, size = 40.dp)
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = if (emphasize) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SummaryMessageBanner(message: String) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.background,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HomeHeaderIcon(
                icon = {
                    Icon(
                        imageVector = Icons.Outlined.StarOutline,
                        contentDescription = null,
                        tint = Color(0xFF6A5AE0)
                    )
                },
                size = 44.dp
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(id = R.string.home_dashboard_total_caption),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun RecentGameLogsCard(
    logs: List<HomeRecentLog>,
    onViewLogsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HomeHeaderIcon(
                    icon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.Article,
                            contentDescription = null,
                            tint = Color(0xFF6A5AE0)
                        )
                    }
                )
                Spacer(modifier = Modifier.width(14.dp))
                Text(
                    text = stringResource(id = R.string.home_recent_logs_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (logs.isEmpty()) {
                Text(
                    text = stringResource(id = R.string.home_recent_logs_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                logs.take(2).forEachIndexed { index, log ->
                    RecentLogRow(log = log)
                    if (index < logs.take(2).lastIndex) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    }
                }
            }

            OutlinedButton(
                onClick = onViewLogsClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(999.dp)
            ) {
                Text(text = stringResource(id = R.string.home_view_logs))
            }
        }
    }
}

@Composable
private fun RecentLogRow(log: HomeRecentLog) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = log.attendedDate,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = stringResource(id = R.string.home_recent_logs_opponent, log.opponentTeamName),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        ResultPill(
            label = log.resultLabel,
            color = log.resultColor(),
            containerColor = log.resultContainerColor()
        )
    }
}

@Composable
private fun ResultPill(
    label: String,
    color: Color,
    containerColor: Color
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = containerColor
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = color,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}

@Composable
private fun HomeHeaderIcon(
    icon: @Composable () -> Unit,
    size: Dp = 52.dp
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(Color(0xFFF1ECFF)),
        contentAlignment = Alignment.Center
    ) {
        icon()
    }
}

private val HomeRecentLog.resultLabel: String
    get() = when (result) {
        BaseballGameResult.Win -> "승"
        BaseballGameResult.Loss -> "패"
        BaseballGameResult.Draw -> "무"
    }

@Composable
private fun HomeRecentLog.resultColor(): Color = when (result) {
    BaseballGameResult.Win -> Color(0xFF16A34A)
    BaseballGameResult.Loss -> Color(0xFFDC2626)
    BaseballGameResult.Draw -> MaterialTheme.colorScheme.onSurfaceVariant
}

@Composable
private fun HomeRecentLog.resultContainerColor(): Color = when (result) {
    BaseballGameResult.Win -> Color(0xFFE7F7EB)
    BaseballGameResult.Loss -> Color(0xFFFBE8EA)
    BaseballGameResult.Draw -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    BaseLogTheme {
        HomeScreen(
            uiState = HomeUiState(
                profile = HomeProfileSummary(
                    nickname = "komo",
                    favoriteTeamName = "한화 이글스"
                ),
                logSummary = HomeLogSummary(
                    totalGames = 2,
                    overallWinRatePercent = 50,
                    overallRecord = "1승 1패 0무",
                    overallMessage = "나쁘지 않은 직관 감각이에요.",
                    currentYear = 2026,
                    currentYearWinRatePercent = 50,
                    currentYearRecord = "1승 1패 0무",
                    recentLogs = listOf(
                        HomeRecentLog(
                            id = "1",
                            attendedDate = "2026-07-14",
                            opponentTeamName = "LG 트윈스",
                            result = BaseballGameResult.Win
                        ),
                        HomeRecentLog(
                            id = "2",
                            attendedDate = "2026-07-08",
                            opponentTeamName = "두산 베어스",
                            result = BaseballGameResult.Loss
                        )
                    ),
                    hasLogs = true
                )
            ),
            onAddLogClick = {},
            onViewLogsClick = {}
        )
    }
}
