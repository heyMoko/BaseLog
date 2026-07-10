package com.mokostudio.baselog.core.user

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

@OptIn(ExperimentalCoroutinesApi::class)
class FirebaseUserProfileRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth?,
    private val firestore: FirebaseFirestore?
) : UserProfileRepository {
    override fun observeProfileCompleted(): Flow<Boolean> {
        val auth = firebaseAuth ?: return flowOf(false)
        val db = firestore ?: return flowOf(false)

        return auth.observeCurrentUserId()
            .flatMapLatest { userId ->
                if (userId == null) {
                    flowOf(false)
                } else {
                    db.collection(USERS_COLLECTION)
                        .document(userId)
                        .observeCompletionState()
                }
            }
            .distinctUntilChanged()
    }

    override suspend fun saveProfile(profile: UserProfileDraft): Result<Unit> {
        val auth = firebaseAuth ?: return Result.failure(
            IllegalStateException("You need to be signed in before creating a profile.")
        )
        val db = firestore ?: return Result.failure(
            IllegalStateException("Firestore is not configured. Check Firebase setup before onboarding users.")
        )
        val user = auth.currentUser ?: return Result.failure(
            IllegalStateException("You need to be signed in before creating a profile.")
        )

        val document = db.collection(USERS_COLLECTION).document(user.uid)
        val existingSnapshot = document.awaitGet()
        val profileData = hashMapOf<String, Any>(
            "nickname" to profile.nickname.trim(),
            "favoriteTeamId" to profile.favoriteTeam.id,
            "favoriteTeamName" to profile.favoriteTeam.displayName,
            "bio" to profile.bio.trim(),
            "email" to user.email.orEmpty(),
            "photoUrl" to user.photoUrl?.toString().orEmpty(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        if (!existingSnapshot.exists()) {
            profileData["createdAt"] = FieldValue.serverTimestamp()
        }

        return document.awaitSet(profileData, SetOptions.merge())
    }
}

private fun FirebaseAuth.observeCurrentUserId(): Flow<String?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser?.uid)
    }

    trySend(currentUser?.uid)
    addAuthStateListener(listener)

    awaitClose {
        removeAuthStateListener(listener)
    }
}

private fun DocumentReference.observeCompletionState(): Flow<Boolean> = callbackFlow {
    val registration = addSnapshotListener { snapshot, _ ->
        trySend(snapshot.isProfileCompleted())
    }

    awaitClose {
        registration.remove()
    }
}

private fun DocumentSnapshot?.isProfileCompleted(): Boolean {
    val snapshot = this ?: return false
    if (!snapshot.exists()) return false

    val nickname = snapshot.getString("nickname").orEmpty().trim()
    val favoriteTeamId = snapshot.getString("favoriteTeamId").orEmpty().trim()
    return nickname.isNotBlank() && favoriteTeamId.isNotBlank()
}

private suspend fun DocumentReference.awaitGet(): DocumentSnapshot =
    suspendCancellableCoroutine { continuation ->
        get()
            .addOnSuccessListener { snapshot ->
                if (continuation.isActive) {
                    continuation.resume(snapshot)
                }
            }
            .addOnFailureListener { error ->
                if (continuation.isActive) {
                    continuation.resumeWith(Result.failure(error))
                }
            }
    }

private suspend fun DocumentReference.awaitSet(
    data: Map<String, Any>,
    options: SetOptions
): Result<Unit> = suspendCancellableCoroutine { continuation ->
    set(data, options)
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

private const val USERS_COLLECTION = "users"
