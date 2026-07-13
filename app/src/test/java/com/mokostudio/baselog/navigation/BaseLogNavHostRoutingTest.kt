package com.mokostudio.baselog.navigation

import com.mokostudio.baselog.core.startup.StartupDestination
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BaseLogNavHostRoutingTest {
    @Test
    fun homeDestination_allowsEditProfileRoute() {
        val allowedRoutes = StartupDestination.Home.allowedRoutes()

        assertTrue(BaseLogDestination.Home.route in allowedRoutes)
        assertTrue(BaseLogDestination.EditProfile.route in allowedRoutes)
        assertTrue(BaseLogDestination.EditLog.route in allowedRoutes)
    }

    @Test
    fun onboardingDestination_allowsOnlyOnboardingRoute() {
        val allowedRoutes = StartupDestination.Onboarding.allowedRoutes()

        assertEquals(setOf(BaseLogDestination.Onboarding.route), allowedRoutes)
    }

    @Test
    fun primaryRoute_matchesMainDestination() {
        assertEquals(BaseLogDestination.Login.route, StartupDestination.Login.primaryRoute())
        assertEquals(
            BaseLogDestination.Onboarding.route,
            StartupDestination.Onboarding.primaryRoute()
        )
        assertEquals(BaseLogDestination.Home.route, StartupDestination.Home.primaryRoute())
    }
}
