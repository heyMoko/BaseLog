package com.mokostudio.baselog.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.compose.rememberNavController
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mokostudio.baselog.core.startup.StartupDestination
import com.mokostudio.baselog.feature.auth.login.LoginRoute
import com.mokostudio.baselog.feature.friends.FriendProfileRoute
import com.mokostudio.baselog.feature.friends.FriendsRoute
import com.mokostudio.baselog.feature.home.HomeRoute
import com.mokostudio.baselog.feature.log.LOG_ID_NAV_ARG
import com.mokostudio.baselog.feature.log.LogEditorRoute
import com.mokostudio.baselog.feature.log.LogbookRoute
import com.mokostudio.baselog.feature.onboarding.OnboardingMode
import com.mokostudio.baselog.feature.onboarding.OnboardingRoute
import com.mokostudio.baselog.feature.splash.SplashRoute

@Composable
fun BaseLogNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    val sessionViewModel: SessionViewModel = hiltViewModel()
    val startupDestination by sessionViewModel.startupDestination.collectAsStateWithLifecycle()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    LaunchedEffect(startupDestination, currentRoute, navController) {
        if (currentRoute == null || currentRoute == BaseLogDestination.Splash.route) {
            return@LaunchedEffect
        }

        val allowedRoutes = startupDestination.allowedRoutes()
        if (currentRoute in allowedRoutes) {
            return@LaunchedEffect
        }

        val targetRoute = startupDestination.primaryRoute()

        if (currentRoute != targetRoute) {
            navController.navigate(targetRoute) {
                popUpTo(currentRoute) {
                    inclusive = true
                }
                launchSingleTop = true
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = BaseLogDestination.Splash.route,
        modifier = modifier
    ) {
        composable(BaseLogDestination.Splash.route) {
            SplashRoute(
                onNavigationReady = { destination ->
                    val route = when (destination) {
                        StartupDestination.Login -> BaseLogDestination.Login.route
                        StartupDestination.Onboarding -> BaseLogDestination.Onboarding.route
                        StartupDestination.Home -> BaseLogDestination.Home.route
                    }
                    navController.navigate(route) {
                        popUpTo(BaseLogDestination.Splash.route) {
                            inclusive = true
                        }
                    }
                }
            )
        }

        composable(BaseLogDestination.Login.route) {
            LoginRoute()
        }

        composable(BaseLogDestination.Onboarding.route) {
            OnboardingRoute(mode = OnboardingMode.Create)
        }

        composable(BaseLogDestination.EditProfile.route) {
            OnboardingRoute(
                mode = OnboardingMode.Edit,
                onBackClick = navController::popBackStack,
                onProfileSaved = {
                    navController.navigate(BaseLogDestination.Home.route) {
                        popUpTo(BaseLogDestination.Home.route) {
                            inclusive = false
                        }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(BaseLogDestination.Home.route) {
            HomeRoute(
                onEditProfileClick = {
                    navController.navigate(BaseLogDestination.EditProfile.route)
                },
                onViewFriendsClick = {
                    navController.navigate(BaseLogDestination.Friends.route)
                },
                onViewLogsClick = {
                    navController.navigate(BaseLogDestination.Logbook.route)
                },
                onAddLogClick = {
                    navController.navigate(BaseLogDestination.CreateLog.route)
                }
            )
        }

        composable(BaseLogDestination.Logbook.route) {
            LogbookRoute(
                onBackClick = navController::popBackStack,
                onAddLogClick = {
                    navController.navigate(BaseLogDestination.CreateLog.route)
                },
                onEditLogClick = { logId ->
                    navController.navigate(BaseLogDestination.EditLog.createRoute(logId))
                }
            )
        }

        composable(BaseLogDestination.Friends.route) {
            FriendsRoute(
                onBackClick = navController::popBackStack,
                onFriendClick = { friendUserId ->
                    navController.navigate(BaseLogDestination.FriendProfile.createRoute(friendUserId))
                }
            )
        }

        composable(
            route = BaseLogDestination.FriendProfile.route,
            arguments = listOf(
                navArgument(FRIEND_USER_ID_NAV_ARG) {
                    type = NavType.StringType
                }
            )
        ) {
            FriendProfileRoute(onBackClick = navController::popBackStack)
        }

        composable(BaseLogDestination.CreateLog.route) {
            LogEditorRoute(
                onBackClick = navController::popBackStack,
                onSaved = {
                    navController.navigate(BaseLogDestination.Logbook.route) {
                        popUpTo(BaseLogDestination.CreateLog.route) {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                },
                onDeleted = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = BaseLogDestination.EditLog.route,
            arguments = listOf(
                navArgument(LOG_ID_NAV_ARG) {
                    type = NavType.StringType
                }
            )
        ) {
            LogEditorRoute(
                onBackClick = navController::popBackStack,
                onSaved = {
                    navController.popBackStack()
                },
                onDeleted = {
                    navController.navigate(BaseLogDestination.Logbook.route) {
                        popUpTo(BaseLogDestination.EditLog.route) {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}

internal fun StartupDestination.primaryRoute(): String = when (this) {
    StartupDestination.Login -> BaseLogDestination.Login.route
    StartupDestination.Onboarding -> BaseLogDestination.Onboarding.route
    StartupDestination.Home -> BaseLogDestination.Home.route
}

internal fun StartupDestination.allowedRoutes(): Set<String> = when (this) {
    StartupDestination.Login -> setOf(BaseLogDestination.Login.route)
    StartupDestination.Onboarding -> setOf(BaseLogDestination.Onboarding.route)
    StartupDestination.Home -> setOf(
        BaseLogDestination.Home.route,
        BaseLogDestination.Friends.route,
        BaseLogDestination.FriendProfile.route,
        BaseLogDestination.EditProfile.route,
        BaseLogDestination.Logbook.route,
        BaseLogDestination.CreateLog.route,
        BaseLogDestination.EditLog.route
    )
}
