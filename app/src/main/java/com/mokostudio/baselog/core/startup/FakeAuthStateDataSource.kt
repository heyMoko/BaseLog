package com.mokostudio.baselog.core.startup

import com.mokostudio.baselog.core.session.AuthSessionStore
import javax.inject.Inject

class FakeAuthStateDataSource @Inject constructor(
    private val authSessionStore: AuthSessionStore
) : AuthStateDataSource {
    override fun observeAuthenticated() = authSessionStore.observeAuthenticated()
}
