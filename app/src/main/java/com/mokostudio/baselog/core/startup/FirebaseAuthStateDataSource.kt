package com.mokostudio.baselog.core.startup

import com.google.firebase.auth.FirebaseAuth
import javax.inject.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class FirebaseAuthStateDataSource @Inject constructor(
    private val firebaseAuth: FirebaseAuth?
) : AuthStateDataSource {
    override fun observeAuthenticated(): Flow<Boolean> = callbackFlow {
        if (firebaseAuth == null) {
            trySend(false)
            close()
            return@callbackFlow
        }

        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser != null)
        }

        trySend(firebaseAuth.currentUser != null)
        firebaseAuth.addAuthStateListener(listener)

        awaitClose {
            firebaseAuth.removeAuthStateListener(listener)
        }
    }
}
