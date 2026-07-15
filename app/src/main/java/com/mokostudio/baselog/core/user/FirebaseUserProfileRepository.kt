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

    override fun observeCurrentUserProfile(): Flow<UserProfile?> {
        val auth = firebaseAuth ?: return flowOf(null)
        val db = firestore ?: return flowOf(null)

        return auth.observeCurrentUserId()
            .flatMapLatest { userId ->
                if (userId == null) {
                    flowOf(null)
                } else {
                    db.collection(USERS_COLLECTION)
                        .document(userId)
                        .observeProfile()
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
        val publicProfileDocument = db.collection(PUBLIC_PROFILES_COLLECTION).document(user.uid)
        val existingSnapshot = document.awaitGet()
        val preservedPhotoUrl = existingSnapshot.getString(FIELD_PHOTO_URL).orEmpty().trim()
        val photoUrl = user.photoUrl?.toString().orEmpty().ifBlank { preservedPhotoUrl }
        val profileData = hashMapOf<String, Any>(
            FIELD_NICKNAME to profile.nickname.trim(),
            FIELD_FAVORITE_TEAM_ID to profile.favoriteTeam.id,
            FIELD_FAVORITE_TEAM_NAME to profile.favoriteTeam.displayName,
            FIELD_BIO to profile.bio.trim(),
            FIELD_EMAIL to user.email.orEmpty(),
            FIELD_PHOTO_URL to photoUrl,
            FIELD_UPDATED_AT to FieldValue.serverTimestamp()
        )
        val publicProfileData = hashMapOf<String, Any>(
            FIELD_NICKNAME to profile.nickname.trim(),
            FIELD_FAVORITE_TEAM_ID to profile.favoriteTeam.id,
            FIELD_FAVORITE_TEAM_NAME to profile.favoriteTeam.displayName,
            FIELD_BIO to profile.bio.trim(),
            FIELD_PHOTO_URL to photoUrl,
            FIELD_UPDATED_AT to FieldValue.serverTimestamp()
        )

        if (!existingSnapshot.exists()) {
            profileData[FIELD_CREATED_AT] = FieldValue.serverTimestamp()
            publicProfileData[FIELD_CREATED_AT] = FieldValue.serverTimestamp()
        }

        return db.batch()
            .apply {
                set(document, profileData, SetOptions.merge())
                set(publicProfileDocument, publicProfileData, SetOptions.merge())
            }
            .awaitCommit()
    }

    override suspend fun syncCurrentPublicProfile(): Result<Unit> {
        val auth = firebaseAuth ?: return Result.failure(
            IllegalStateException("You need to be signed in before syncing your public profile.")
        )
        val db = firestore ?: return Result.failure(
            IllegalStateException("Firestore is not configured. Check Firebase setup before syncing profiles.")
        )
        val user = auth.currentUser ?: return Result.failure(
            IllegalStateException("You need to be signed in before syncing your public profile.")
        )

        val userDocument = db.collection(USERS_COLLECTION).document(user.uid)
        val publicProfileDocument = db.collection(PUBLIC_PROFILES_COLLECTION).document(user.uid)
        val userSnapshot = userDocument.awaitGet()
        val userProfile = userSnapshot.toUserProfile()
            ?: return Result.success(Unit)
        val publicProfileSnapshot = publicProfileDocument.awaitGet()
        val publicProfileData = hashMapOf<String, Any>(
            FIELD_NICKNAME to userProfile.nickname,
            FIELD_FAVORITE_TEAM_ID to userProfile.favoriteTeam.id,
            FIELD_FAVORITE_TEAM_NAME to userProfile.favoriteTeam.displayName,
            FIELD_BIO to userProfile.bio,
            FIELD_PHOTO_URL to userProfile.photoUrl,
            FIELD_UPDATED_AT to FieldValue.serverTimestamp()
        )

        if (!publicProfileSnapshot.exists()) {
            publicProfileData[FIELD_CREATED_AT] = FieldValue.serverTimestamp()
        }

        return publicProfileDocument.awaitSet(
            data = publicProfileData,
            options = SetOptions.merge()
        )
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

private fun DocumentReference.observeProfile(): Flow<UserProfile?> = callbackFlow {
    val registration = addSnapshotListener { snapshot, _ ->
        trySend(snapshot.toUserProfile())
    }

    awaitClose {
        registration.remove()
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

    val nickname = snapshot.getString(FIELD_NICKNAME).orEmpty().trim()
    val favoriteTeam = snapshot.resolveFavoriteTeam()
    return nickname.length >= MIN_NICKNAME_LENGTH && favoriteTeam != null
}

private fun DocumentSnapshot?.toUserProfile(): UserProfile? {
    val snapshot = this ?: return null
    if (!snapshot.exists()) return null

    val nickname = snapshot.getString(FIELD_NICKNAME).orEmpty().trim()
    if (nickname.length < MIN_NICKNAME_LENGTH) return null

    val favoriteTeam = snapshot.resolveFavoriteTeam() ?: return null

    return UserProfile(
        nickname = nickname,
        favoriteTeam = favoriteTeam,
        bio = snapshot.getString(FIELD_BIO).orEmpty().trim(),
        email = snapshot.getString(FIELD_EMAIL).orEmpty().trim(),
        photoUrl = snapshot.getString(FIELD_PHOTO_URL).orEmpty().trim()
    )
}

private fun DocumentSnapshot.resolveFavoriteTeam(): BaseballTeam? {
    val favoriteTeamId = getString(FIELD_FAVORITE_TEAM_ID).orEmpty().trim()
    if (favoriteTeamId.isNotBlank()) {
        BaseballTeam.fromId(favoriteTeamId)?.let { return it }
    }

    val favoriteTeamName = getString(FIELD_FAVORITE_TEAM_NAME).orEmpty().trim()
    return BaseballTeam.fromDisplayName(favoriteTeamName)
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

private suspend fun com.google.firebase.firestore.WriteBatch.awaitCommit(): Result<Unit> =
    suspendCancellableCoroutine { continuation ->
        commit()
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
private const val PUBLIC_PROFILES_COLLECTION = "publicProfiles"
private const val FIELD_NICKNAME = "nickname"
private const val FIELD_FAVORITE_TEAM_ID = "favoriteTeamId"
private const val FIELD_FAVORITE_TEAM_NAME = "favoriteTeamName"
private const val FIELD_BIO = "bio"
private const val FIELD_EMAIL = "email"
private const val FIELD_PHOTO_URL = "photoUrl"
private const val FIELD_CREATED_AT = "createdAt"
private const val FIELD_UPDATED_AT = "updatedAt"
private const val MIN_NICKNAME_LENGTH = 2
