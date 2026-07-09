package com.mokostudio.baselog.core.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import com.mokostudio.baselog.core.startup.AppStartupRepository
import com.mokostudio.baselog.core.startup.DefaultAppStartupRepository
import com.mokostudio.baselog.core.startup.FakeAuthStateDataSource
import com.mokostudio.baselog.core.startup.AuthStateDataSource
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppBindModule {
    @Binds
    @Singleton
    abstract fun bindAppStartupRepository(
        impl: DefaultAppStartupRepository
    ): AppStartupRepository

    @Binds
    @Singleton
    abstract fun bindAuthStateDataSource(
        impl: FakeAuthStateDataSource
    ): AuthStateDataSource
}

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun providePreferencesDataStore(
        @ApplicationContext context: Context
    ): DataStore<Preferences> = PreferenceDataStoreFactory.create {
        context.preferencesDataStoreFile("baselog.preferences_pb")
    }
}
