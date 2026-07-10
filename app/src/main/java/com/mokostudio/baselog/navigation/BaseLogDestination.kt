package com.mokostudio.baselog.navigation

sealed interface BaseLogDestination {
    val route: String

    data object Splash : BaseLogDestination {
        override val route = "splash"
    }

    data object Login : BaseLogDestination {
        override val route = "login"
    }

    data object Onboarding : BaseLogDestination {
        override val route = "onboarding"
    }

    data object EditProfile : BaseLogDestination {
        override val route = "profile/edit"
    }

    data object Home : BaseLogDestination {
        override val route = "home"
    }
}
