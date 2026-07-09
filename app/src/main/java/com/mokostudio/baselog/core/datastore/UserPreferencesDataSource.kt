package com.mokostudio.baselog.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class UserPreferencesDataSource @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    val isOnboardingCompleted: Flow<Boolean> =
        dataStore.data.map { preferences ->
            preferences[BaseLogPreferences.OnboardingCompleted] ?: false
        }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { preferences ->
            preferences[BaseLogPreferences.OnboardingCompleted] = completed
        }
    }
}
