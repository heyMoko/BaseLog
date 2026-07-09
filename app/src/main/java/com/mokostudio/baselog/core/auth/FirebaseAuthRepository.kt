package com.mokostudio.baselog.core.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import javax.inject.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class FirebaseAuthRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth?
) : AuthRepository {
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

    override suspend fun signInWithGoogleIdToken(idToken: String): Result<Unit> {
        val auth = firebaseAuth ?: return Result.failure(
            IllegalStateException("Firebase is not configured. Add google-services.json to app/ and enable Google sign-in in Firebase.")
        )

        val credential = GoogleAuthProvider.getCredential(idToken, null)
        return auth.signInWithCredentialResult(credential)
    }

    override suspend fun signOut() {
        firebaseAuth?.signOut()
    }
}

private suspend fun FirebaseAuth.signInWithCredentialResult(
    credential: com.google.firebase.auth.AuthCredential
): Result<Unit> = suspendCancellableCoroutine { continuation ->
    signInWithCredential(credential)
        .addOnSuccessListener {
            if (continuation.isActive) {
                continuation.resume(Result.success(Unit))
            }
        }
        .addOnFailureListener { error ->
            if (continuation.isActive) {
                continuation.resume(Result.failure(error))
            }
        }
}
