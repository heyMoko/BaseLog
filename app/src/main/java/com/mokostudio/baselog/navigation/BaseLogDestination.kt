package com.mokostudio.baselog.navigation

import com.mokostudio.baselog.feature.log.LOG_ID_NAV_ARG

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

    data object Friends : BaseLogDestination {
        override val route = "friends"
    }

    data object Logbook : BaseLogDestination {
        override val route = "logbook"
    }

    data object CreateLog : BaseLogDestination {
        override val route = "log/create"
    }

    data object EditLog : BaseLogDestination {
        override val route = "log/edit/{$LOG_ID_NAV_ARG}"

        fun createRoute(logId: String): String = "log/edit/$logId"
    }
}
