package com.mokostudio.baselog.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.StickyNote2
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mokostudio.baselog.R
import com.mokostudio.baselog.feature.friends.FriendsRoute
import com.mokostudio.baselog.feature.home.HomeRoute
import com.mokostudio.baselog.feature.log.LogbookRoute
import com.mokostudio.baselog.feature.mypage.MyPageRoute
import com.mokostudio.baselog.feature.ranking.RankingRoute

@Composable
fun MainRoute(
    startTab: MainTab,
    onAddLogClick: () -> Unit,
    onEditLogClick: (String) -> Unit,
    onFriendClick: (String) -> Unit,
    onEditProfileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val mainNavController = rememberNavController()
    val backStackEntry by mainNavController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val selectedTab = MainTab.entries.firstOrNull { it.route == currentRoute } ?: startTab

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar {
                MainTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = {
                            if (selectedTab != tab) {
                                mainNavController.navigate(tab.route) {
                                    popUpTo(mainNavController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = tab.icon(),
                                contentDescription = tab.label()
                            )
                        },
                        label = {
                            Text(text = tab.label())
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        MainNavHost(
            navController = mainNavController,
            startTab = startTab,
            contentPadding = innerPadding,
            onAddLogClick = onAddLogClick,
            onEditLogClick = onEditLogClick,
            onFriendClick = onFriendClick,
            onEditProfileClick = onEditProfileClick
        )
    }
}

@Composable
private fun MainNavHost(
    navController: NavHostController,
    startTab: MainTab,
    contentPadding: PaddingValues,
    onAddLogClick: () -> Unit,
    onEditLogClick: (String) -> Unit,
    onFriendClick: (String) -> Unit,
    onEditProfileClick: () -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = startTab.route
    ) {
        composable(MainTab.Home.route) {
            HomeRoute(
                contentPadding = contentPadding,
                onAddLogClick = onAddLogClick,
                onViewLogsClick = {
                    navController.navigate(MainTab.Record.route) {
                        launchSingleTop = true
                    }
                },
                onViewRankingClick = {
                    navController.navigate(MainTab.Ranking.route) {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(MainTab.Record.route) {
            LogbookRoute(
                contentPadding = contentPadding,
                onAddLogClick = onAddLogClick,
                onEditLogClick = onEditLogClick
            )
        }

        composable(MainTab.Friends.route) {
            FriendsRoute(
                contentPadding = contentPadding,
                onFriendClick = onFriendClick
            )
        }

        composable(MainTab.Ranking.route) {
            RankingRoute(contentPadding = contentPadding)
        }

        composable(MainTab.MyPage.route) {
            MyPageRoute(
                contentPadding = contentPadding,
                onEditProfileClick = onEditProfileClick
            )
        }
    }
}

@Composable
private fun MainTab.label(): String = when (this) {
    MainTab.Home -> stringResource(id = R.string.main_tab_home)
    MainTab.Record -> stringResource(id = R.string.main_tab_record)
    MainTab.Friends -> stringResource(id = R.string.main_tab_friends)
    MainTab.Ranking -> stringResource(id = R.string.main_tab_ranking)
    MainTab.MyPage -> stringResource(id = R.string.main_tab_my)
}

@Composable
private fun MainTab.icon() = when (this) {
    MainTab.Home -> Icons.Outlined.Home
    MainTab.Record -> Icons.AutoMirrored.Outlined.StickyNote2
    MainTab.Friends -> Icons.Outlined.People
    MainTab.Ranking -> Icons.Outlined.BarChart
    MainTab.MyPage -> Icons.Outlined.Person
}
