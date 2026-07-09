package com.mokostudio.baselog.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mokostudio.baselog.core.startup.StartupDestination
import com.mokostudio.baselog.feature.auth.login.LoginRoute
import com.mokostudio.baselog.feature.home.HomeRoute
import com.mokostudio.baselog.feature.splash.SplashRoute

@Composable
fun BaseLogNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
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
            LoginRoute(
                onContinueClick = {
                    navController.navigate(BaseLogDestination.Home.route) {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(BaseLogDestination.Home.route) {
            HomeRoute()
        }
    }
}
