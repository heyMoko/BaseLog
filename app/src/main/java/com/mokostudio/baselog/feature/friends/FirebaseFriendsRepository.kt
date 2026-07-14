package com.mokostudio.baselog.feature.friends

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.mokostudio.baselog.core.user.BaseballTeam
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
class FirebaseFriendsRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth?,
    private val firestore: FirebaseFirestore?
) : FriendsRepository {
    override fun observeFriends(): Flow<List<FriendSummary>> {
        val auth = firebaseAuth ?: return flowOf(emptyList())
        val db = firestore ?: return flowOf(emptyList())

        return auth.observeCurrentUserId()
            .flatMapLatest { userId ->
                if (userId == null) {
                    flowOf(emptyList())
                } else {
                    db.collection(USERS_COLLECTION)
                        .document(userId)
                        .collection(FRIENDS_COLLECTION)
                        .orderBy(FIELD_NICKNAME, Query.Direction.ASCENDING)
                        .observeFriendSummaries()
                }
            }
            .distinctUntilChanged()
    }

    override fun observeIncomingRequests(): Flow<List<FriendRequest>> {
        val auth = firebaseAuth ?: return flowOf(emptyList())
        val db = firestore ?: return flowOf(emptyList())

        return auth.observeCurrentUserId()
            .flatMapLatest { userId ->
                if (userId == null) {
                    flowOf(emptyList())
                } else {
                    db.collection(FRIEND_REQUESTS_COLLECTION)
                        .whereEqualTo(FIELD_RECIPIENT_ID, userId)
                        .observeIncomingRequests()
                }
            }
            .distinctUntilChanged()
    }

    override fun observeOutgoingPendingRequestUserIds(): Flow<Set<String>> {
        val auth = firebaseAuth ?: return flowOf(emptySet())
        val db = firestore ?: return flowOf(emptySet())

        return auth.observeCurrentUserId()
            .flatMapLatest { userId ->
                if (userId == null) {
                    flowOf(emptySet())
                } else {
                    db.collection(FRIEND_REQUESTS_COLLECTION)
                        .whereEqualTo(FIELD_REQUESTER_ID, userId)
                        .observeOutgoingPendingIds()
                }
            }
            .distinctUntilChanged()
    }

    override suspend fun searchUsers(query: String): Result<List<FriendSummary>> = runCatching {
        val currentUserId = requireCurrentUserId(firebaseAuth)
        val db = requireFirestore(firestore)
        val trimmedQuery = query.trim()
        if (trimmedQuery.isBlank()) {
            return@runCatching emptyList()
        }

        db.collection(PUBLIC_PROFILES_COLLECTION)
            .orderBy(FIELD_NICKNAME)
            .startAt(trimmedQuery)
            .endAt(trimmedQuery + "\uf8ff")
            .limit(USER_SEARCH_LIMIT)
            .get()
            .awaitQuerySnapshot()
            .documents
            .mapNotNull { document ->
                document.toFriendSummary(userId = document.id)
            }
            .filterNot { summary -> summary.userId == currentUserId }
    }

    override suspend fun sendFriendRequest(user: FriendSummary): Result<Unit> = runCatching {
        val currentUserId = requireCurrentUserId(firebaseAuth)
        val db = requireFirestore(firestore)
        require(currentUserId != user.userId) {
            "You cannot send a friend request to yourself."
        }

        val alreadyFriends = db.collection(USERS_COLLECTION)
            .document(currentUserId)
            .collection(FRIENDS_COLLECTION)
            .document(user.userId)
            .get()
            .awaitDocumentSnapshot()
            .exists()
        if (alreadyFriends) {
            error("You are already friends with this user.")
        }

        val outgoingPendingRequest = db.collection(FRIEND_REQUESTS_COLLECTION)
            .whereEqualTo(FIELD_REQUESTER_ID, currentUserId)
            .whereEqualTo(FIELD_RECIPIENT_ID, user.userId)
            .limit(1)
            .get()
            .awaitQuerySnapshot()
            .documents
            .firstOrNull { document ->
                document.getString(FIELD_STATUS) == FRIEND_REQUEST_STATUS_PENDING
            }
        if (outgoingPendingRequest != null) {
            error("Friend request already sent.")
        }

        val incomingPendingRequest = db.collection(FRIEND_REQUESTS_COLLECTION)
            .whereEqualTo(FIELD_REQUESTER_ID, user.userId)
            .whereEqualTo(FIELD_RECIPIENT_ID, currentUserId)
            .limit(1)
            .get()
            .awaitQuerySnapshot()
            .documents
            .firstOrNull { document ->
                document.getString(FIELD_STATUS) == FRIEND_REQUEST_STATUS_PENDING
            }
        if (incomingPendingRequest != null) {
            error("This user has already sent you a friend request.")
        }

        val requester = db.collection(USERS_COLLECTION)
            .document(currentUserId)
            .get()
            .awaitDocumentSnapshot()
            .toFriendSummary(userId = currentUserId)
            ?: error("Your profile is incomplete. Finish onboarding before adding friends.")

        val pairRequestId = createFriendPairId(currentUserId, user.userId)
        db.collection(FRIEND_REQUESTS_COLLECTION)
            .document(pairRequestId)
            .set(
                mapOf(
                    FIELD_REQUESTER_ID to requester.userId,
                    FIELD_REQUESTER_NICKNAME to requester.nickname,
                    FIELD_REQUESTER_TEAM_ID to requester.favoriteTeam.id,
                    FIELD_REQUESTER_TEAM_NAME to requester.favoriteTeam.displayName,
                    FIELD_REQUESTER_BIO to requester.bio,
                    FIELD_REQUESTER_PHOTO_URL to requester.photoUrl,
                    FIELD_RECIPIENT_ID to user.userId,
                    FIELD_STATUS to FRIEND_REQUEST_STATUS_PENDING,
                    FIELD_CREATED_AT to FieldValue.serverTimestamp(),
                    FIELD_UPDATED_AT to FieldValue.serverTimestamp()
                )
            )
            .awaitWrite()
    }

    override suspend fun acceptFriendRequest(requestId: String): Result<Unit> = runCatching {
        val currentUserId = requireCurrentUserId(firebaseAuth)
        val db = requireFirestore(firestore)
        val requestDocument = db.collection(FRIEND_REQUESTS_COLLECTION)
            .document(requestId)
            .get()
            .awaitDocumentSnapshot()

        val request = requestDocument.toIncomingFriendRequest()
            ?: error("We couldn't find this friend request.")
        val recipientId = requestDocument.getString(FIELD_RECIPIENT_ID)
        val status = requestDocument.getString(FIELD_STATUS)
        if (recipientId != currentUserId || status != FRIEND_REQUEST_STATUS_PENDING) {
            error("You can only accept requests sent to you.")
        }

        val currentUser = db.collection(USERS_COLLECTION)
            .document(currentUserId)
            .get()
            .awaitDocumentSnapshot()
            .toFriendSummary(userId = currentUserId)
            ?: error("Your profile is incomplete. Finish onboarding before accepting requests.")

        val batch = db.batch()
        batch.set(
            db.collection(USERS_COLLECTION)
                .document(currentUserId)
                .collection(FRIENDS_COLLECTION)
                .document(request.requester.userId),
            request.requester.toDocumentData()
        )
        batch.set(
            db.collection(USERS_COLLECTION)
                .document(request.requester.userId)
                .collection(FRIENDS_COLLECTION)
                .document(currentUserId),
            currentUser.toDocumentData()
        )
        batch.update(
            db.collection(FRIEND_REQUESTS_COLLECTION).document(requestId),
            mapOf(
                FIELD_STATUS to FRIEND_REQUEST_STATUS_ACCEPTED,
                FIELD_UPDATED_AT to FieldValue.serverTimestamp()
            )
        )
        batch.awaitCommit()
    }

    override suspend fun rejectFriendRequest(requestId: String): Result<Unit> = runCatching {
        val currentUserId = requireCurrentUserId(firebaseAuth)
        val db = requireFirestore(firestore)
        val requestDocument = db.collection(FRIEND_REQUESTS_COLLECTION)
            .document(requestId)
            .get()
            .awaitDocumentSnapshot()

        val recipientId = requestDocument.getString(FIELD_RECIPIENT_ID)
        val status = requestDocument.getString(FIELD_STATUS)
        if (!requestDocument.exists() ||
            recipientId != currentUserId ||
            status != FRIEND_REQUEST_STATUS_PENDING
        ) {
            error("You can only reject requests sent to you.")
        }

        db.collection(FRIEND_REQUESTS_COLLECTION)
            .document(requestId)
            .update(
                mapOf(
                    FIELD_STATUS to FRIEND_REQUEST_STATUS_REJECTED,
                    FIELD_UPDATED_AT to FieldValue.serverTimestamp()
                )
            )
            .awaitWrite()
    }

    override suspend fun removeFriend(friendUserId: String): Result<Unit> = runCatching {
        val currentUserId = requireCurrentUserId(firebaseAuth)
        val db = requireFirestore(firestore)
        val batch = db.batch()

        batch.delete(
            db.collection(USERS_COLLECTION)
                .document(currentUserId)
                .collection(FRIENDS_COLLECTION)
                .document(friendUserId)
        )
        batch.delete(
            db.collection(USERS_COLLECTION)
                .document(friendUserId)
                .collection(FRIENDS_COLLECTION)
                .document(currentUserId)
        )
        batch.delete(
            db.collection(FRIEND_REQUESTS_COLLECTION)
                .document(createFriendPairId(currentUserId, friendUserId))
        )

        batch.awaitCommit()
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

private fun Query.observeFriendSummaries(): Flow<List<FriendSummary>> = callbackFlow {
    val registration = addSnapshotListener { snapshot, _ ->
        val friends = snapshot?.documents.orEmpty()
            .mapNotNull { document -> document.toFriendSummary(userId = document.id) }
        trySend(friends)
    }

    awaitClose {
        registration.remove()
    }
}

private fun Query.observeIncomingRequests(): Flow<List<FriendRequest>> = callbackFlow {
    val registration = addSnapshotListener { snapshot, _ ->
        val requests = snapshot?.documents.orEmpty()
            .filter { document ->
                document.getString(FIELD_STATUS) == FRIEND_REQUEST_STATUS_PENDING
            }
            .mapNotNull(DocumentSnapshot::toIncomingFriendRequest)
            .sortedByDescending { request -> request.requestId }
        trySend(requests)
    }

    awaitClose {
        registration.remove()
    }
}

private fun Query.observeOutgoingPendingIds(): Flow<Set<String>> = callbackFlow {
    val registration = addSnapshotListener { snapshot, _ ->
        val ids = snapshot?.documents.orEmpty()
            .filter { document ->
                document.getString(FIELD_STATUS) == FRIEND_REQUEST_STATUS_PENDING
            }
            .mapNotNull { document -> document.getString(FIELD_RECIPIENT_ID) }
            .toSet()
        trySend(ids)
    }

    awaitClose {
        registration.remove()
    }
}

private fun DocumentSnapshot.toIncomingFriendRequest(): FriendRequest? {
    val requesterId = getString(FIELD_REQUESTER_ID).orEmpty().trim()
    if (requesterId.isBlank()) return null

    val requesterTeam = getString(FIELD_REQUESTER_TEAM_ID)?.let(BaseballTeam::fromId)
        ?: getString(FIELD_REQUESTER_TEAM_NAME)?.let(BaseballTeam::fromDisplayName)
        ?: return null

    return FriendRequest(
        requestId = id,
        requester = FriendSummary(
            userId = requesterId,
            nickname = getString(FIELD_REQUESTER_NICKNAME).orEmpty().trim(),
            favoriteTeam = requesterTeam,
            bio = getString(FIELD_REQUESTER_BIO).orEmpty().trim(),
            photoUrl = getString(FIELD_REQUESTER_PHOTO_URL).orEmpty().trim()
        )
    )
}

private fun DocumentSnapshot.toFriendSummary(userId: String): FriendSummary? {
    val nickname = getString(FIELD_NICKNAME).orEmpty().trim()
    if (nickname.length < MIN_NICKNAME_LENGTH) return null

    val favoriteTeam = getString(FIELD_FAVORITE_TEAM_ID)?.let(BaseballTeam::fromId)
        ?: getString(FIELD_FAVORITE_TEAM_NAME)?.let(BaseballTeam::fromDisplayName)
        ?: return null

    return FriendSummary(
        userId = userId,
        nickname = nickname,
        favoriteTeam = favoriteTeam,
        bio = getString(FIELD_BIO).orEmpty().trim(),
        photoUrl = getString(FIELD_PHOTO_URL).orEmpty().trim()
    )
}

private fun FriendSummary.toDocumentData(): Map<String, Any> {
    return mapOf(
        FIELD_NICKNAME to nickname,
        FIELD_FAVORITE_TEAM_ID to favoriteTeam.id,
        FIELD_FAVORITE_TEAM_NAME to favoriteTeam.displayName,
        FIELD_BIO to bio,
        FIELD_PHOTO_URL to photoUrl,
        FIELD_UPDATED_AT to FieldValue.serverTimestamp()
    )
}

private suspend fun com.google.android.gms.tasks.Task<com.google.firebase.firestore.QuerySnapshot>.awaitQuerySnapshot() =
    suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { snapshot ->
            if (continuation.isActive) {
                continuation.resume(snapshot)
            }
        }.addOnFailureListener { error ->
            if (continuation.isActive) {
                continuation.resumeWith(Result.failure(error))
            }
        }
    }

private suspend fun com.google.android.gms.tasks.Task<DocumentSnapshot>.awaitDocumentSnapshot() =
    suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { snapshot ->
            if (continuation.isActive) {
                continuation.resume(snapshot)
            }
        }.addOnFailureListener { error ->
            if (continuation.isActive) {
                continuation.resumeWith(Result.failure(error))
            }
        }
    }

private suspend fun com.google.android.gms.tasks.Task<Void>.awaitWrite() =
    suspendCancellableCoroutine<Result<Unit>> { continuation ->
        addOnSuccessListener {
            if (continuation.isActive) {
                continuation.resume(Result.success(Unit))
            }
        }.addOnFailureListener { error ->
            if (continuation.isActive) {
                continuation.resume(Result.failure(error))
            }
        }
    }.getOrThrow()

private suspend fun com.google.firebase.firestore.WriteBatch.awaitCommit() =
    suspendCancellableCoroutine<Result<Unit>> { continuation ->
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
    }.getOrThrow()

private fun requireCurrentUserId(firebaseAuth: FirebaseAuth?): String {
    val auth = firebaseAuth ?: error("Firebase is not configured. Check Firebase setup before managing friends.")
    return auth.currentUser?.uid
        ?: error("You need to be signed in before managing friends.")
}

private fun requireFirestore(firestore: FirebaseFirestore?): FirebaseFirestore {
    return firestore ?: error("Firestore is not configured. Check Firebase setup before managing friends.")
}

private fun createFriendPairId(firstUserId: String, secondUserId: String): String {
    return listOf(firstUserId, secondUserId).sorted().joinToString("_")
}

private const val USERS_COLLECTION = "users"
private const val PUBLIC_PROFILES_COLLECTION = "publicProfiles"
private const val FRIENDS_COLLECTION = "friends"
private const val FRIEND_REQUESTS_COLLECTION = "friendRequests"
private const val FIELD_REQUESTER_ID = "requesterId"
private const val FIELD_REQUESTER_NICKNAME = "requesterNickname"
private const val FIELD_REQUESTER_TEAM_ID = "requesterFavoriteTeamId"
private const val FIELD_REQUESTER_TEAM_NAME = "requesterFavoriteTeamName"
private const val FIELD_REQUESTER_BIO = "requesterBio"
private const val FIELD_REQUESTER_PHOTO_URL = "requesterPhotoUrl"
private const val FIELD_RECIPIENT_ID = "recipientId"
private const val FIELD_STATUS = "status"
private const val FIELD_CREATED_AT = "createdAt"
private const val FIELD_UPDATED_AT = "updatedAt"
private const val FIELD_NICKNAME = "nickname"
private const val FIELD_FAVORITE_TEAM_ID = "favoriteTeamId"
private const val FIELD_FAVORITE_TEAM_NAME = "favoriteTeamName"
private const val FIELD_BIO = "bio"
private const val FIELD_PHOTO_URL = "photoUrl"
private const val USER_SEARCH_LIMIT = 20L
private const val MIN_NICKNAME_LENGTH = 2
private const val FRIEND_REQUEST_STATUS_PENDING = "PENDING"
private const val FRIEND_REQUEST_STATUS_ACCEPTED = "ACCEPTED"
private const val FRIEND_REQUEST_STATUS_REJECTED = "REJECTED"
