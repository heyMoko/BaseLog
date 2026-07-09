package com.mokostudio.baselog.core.startup

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

class FakeAuthStateDataSource @Inject constructor() : AuthStateDataSource {
    override fun observeAuthenticated(): Flow<Boolean> = flowOf(false)
}
