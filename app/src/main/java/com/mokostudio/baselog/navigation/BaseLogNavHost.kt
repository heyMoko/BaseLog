package com.mokostudio.baselog.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mokostudio.baselog.core.startup.StartupDestination
import com.mokostudio.baselog.feature.auth.login.LoginRoute
import com.mokostudio.baselog.feature.home.HomeRoute
import com.mokostudio.baselog.feature.home.HomeViewModel
import com.mokostudio.baselog.feature.splash.SplashRoute

@Composable
fun BaseLogNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    val sessionViewModel: SessionViewModel = hiltViewModel()
    val isAuthenticated by sessionViewModel.isAuthenticated.collectAsStateWithLifecycle()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    LaunchedEffect(isAuthenticated, currentRoute, navController) {
        when {
            currentRoute == null || currentRoute == BaseLogDestination.Splash.route -> Unit
            isAuthenticated && currentRoute == BaseLogDestination.Login.route -> {
                navController.navigate(BaseLogDestination.Home.route) {
                    popUpTo(BaseLogDestination.Login.route) {
                        inclusive = true
                    }
                    launchSingleTop = true
                }
            }
            !isAuthenticated && currentRoute == BaseLogDestination.Home.route -> {
                navController.navigate(BaseLogDestination.Login.route) {
                    popUpTo(BaseLogDestination.Home.route) {
                        inclusive = true
                    }
                    launchSingleTop = true
                }
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

        composable(BaseLogDestination.Home.route) {
            val homeViewModel: HomeViewModel = hiltViewModel()
            HomeRoute(onSignOutClick = homeViewModel::signOut)
        }
    }
}
