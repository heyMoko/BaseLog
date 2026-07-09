package com.mokostudio.baselog.navigation

sealed interface BaseLogDestination {
    val route: String

    data object Splash : BaseLogDestination {
        override val route = "splash"
    }

    data object Login : BaseLogDestination {
        override val route = "login"
    }

    data object Home : BaseLogDestination {
        override val route = "home"
    }
}
