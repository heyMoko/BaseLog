package com.mokostudio.baselog.feature.onboarding

import com.mokostudio.baselog.core.user.BaseballTeam
import com.mokostudio.baselog.core.user.UserProfile
import com.mokostudio.baselog.core.user.UserProfileDraft
import com.mokostudio.baselog.core.user.UserProfileRepository
import com.mokostudio.baselog.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun saveProfile_setsValidationErrorWhenNicknameTooShort() {
        val viewModel = OnboardingViewModel(userProfileRepository = FakeUserProfileRepository())

        viewModel.onNicknameChanged("A")
        viewModel.saveProfile()

        assertEquals(
            "Nickname must be at least 2 characters.",
            viewModel.uiState.value.errorMessage
        )
    }

    @Test
    fun saveProfile_setsValidationErrorWhenTeamMissing() {
        val viewModel = OnboardingViewModel(userProfileRepository = FakeUserProfileRepository())

        viewModel.onNicknameChanged("Moko")
        viewModel.saveProfile()

        assertEquals(
            "Choose your favorite team to continue.",
            viewModel.uiState.value.errorMessage
        )
    }

    @Test
    fun saveProfile_sendsTrimmedProfile() = runTest {
        val repository = FakeUserProfileRepository()
        val viewModel = OnboardingViewModel(userProfileRepository = repository)

        viewModel.onNicknameChanged("  Moko  ")
        viewModel.onTeamSelected(BaseballTeam.LgTwins)
        viewModel.onBioChanged("  Loves weekday games.  ")
        viewModel.saveProfile()
        advanceUntilIdle()

        assertEquals(
            UserProfileDraft(
                nickname = "Moko",
                favoriteTeam = BaseballTeam.LgTwins,
                bio = "Loves weekday games."
            ),
            repository.savedProfile
        )
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun saveProfile_surfacesRepositoryFailure() = runTest {
        val repository = FakeUserProfileRepository(
            saveResult = Result.failure(IllegalStateException("Save failed"))
        )
        val viewModel = OnboardingViewModel(userProfileRepository = repository)

        viewModel.onNicknameChanged("Moko")
        viewModel.onTeamSelected(BaseballTeam.LgTwins)
        viewModel.saveProfile()
        advanceUntilIdle()

        assertEquals("Save failed", viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isSaving)
    }

    @Test
    fun submitEnabled_dependsOnRequiredFields() {
        val viewModel = OnboardingViewModel(userProfileRepository = FakeUserProfileRepository())
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isSubmitEnabled)

        viewModel.onNicknameChanged("Moko")
        viewModel.onTeamSelected(BaseballTeam.LgTwins)

        assertTrue(viewModel.uiState.value.isSubmitEnabled)
    }

    @Test
    fun init_prefillsExistingProfile() = runTest {
        val repository = FakeUserProfileRepository()
        repository.profile.value = UserProfile(
            nickname = "Moko",
            favoriteTeam = BaseballTeam.LgTwins,
            bio = "Ballpark regular.",
            email = "moko@example.com",
            photoUrl = ""
        )

        val viewModel = OnboardingViewModel(userProfileRepository = repository)
        advanceUntilIdle()

        assertEquals("Moko", viewModel.uiState.value.nickname)
        assertEquals(BaseballTeam.LgTwins, viewModel.uiState.value.selectedTeam)
        assertEquals("Ballpark regular.", viewModel.uiState.value.bio)
        assertFalse(viewModel.uiState.value.isLoadingProfile)
    }

    private class FakeUserProfileRepository(
        private val saveResult: Result<Unit> = Result.success(Unit)
    ) : UserProfileRepository {
        var savedProfile: UserProfileDraft? = null
        val profile = MutableStateFlow<UserProfile?>(null)

        override fun observeProfileCompleted(): Flow<Boolean> = MutableStateFlow(false)

        override fun observeCurrentUserProfile(): Flow<UserProfile?> = profile

        override suspend fun saveProfile(profile: UserProfileDraft): Result<Unit> {
            savedProfile = profile
            return saveResult
        }
    }
}
