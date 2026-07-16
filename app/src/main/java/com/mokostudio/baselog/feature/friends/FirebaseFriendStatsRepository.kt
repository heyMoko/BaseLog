package com.mokostudio.baselog.feature.friends

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.mokostudio.baselog.core.user.BaseballTeam
import com.mokostudio.baselog.feature.log.BaseballGameResult
import com.mokostudio.baselog.feature.log.BaseballLogEntry
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

@OptIn(ExperimentalCoroutinesApi::class)
class FirebaseFriendStatsRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth?,
    private val firestore: FirebaseFirestore?
) : FriendStatsRepository {
    override fun observeFriendProfile(friendUserId: String): Flow<FriendProfileLoadState> {
        val auth = firebaseAuth ?: return flowOf(
            FriendProfileLoadState(errorMessage = FRIEND_PROFILE_LOAD_ERROR)
        )
        val db = firestore ?: return flowOf(
            FriendProfileLoadState(errorMessage = FRIEND_PROFILE_LOAD_ERROR)
        )

        return auth.observeCurrentUserId()
            .flatMapLatest { currentUserId ->
                if (currentUserId == null) {
                    flowOf(FriendProfileLoadState())
                } else {
                    db.collection(USERS_COLLECTION)
                        .document(currentUserId)
                        .collection(FRIENDS_COLLECTION)
                        .document(friendUserId)
                        .observeFriendSummary(friendUserId)
                }
            }
            .distinctUntilChanged()
    }

    override fun observeFriendLogs(friendUserId: String): Flow<FriendLogsLoadState> {
        val auth = firebaseAuth ?: return flowOf(
            FriendLogsLoadState(errorMessage = FRIEND_LOGS_LOAD_ERROR)
        )
        val db = firestore ?: return flowOf(
            FriendLogsLoadState(errorMessage = FRIEND_LOGS_LOAD_ERROR)
        )

        return auth.observeCurrentUserId()
            .flatMapLatest { currentUserId ->
                if (currentUserId == null) {
                    flowOf(FriendLogsLoadState())
                } else {
                    db.collection(USERS_COLLECTION)
                        .document(friendUserId)
                        .collection(LOGS_COLLECTION)
                        .orderBy(FIELD_ATTENDED_DATE, Query.Direction.DESCENDING)
                        .observeFriendLogs()
                }
            }
            .distinctUntilChanged()
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

private fun DocumentReference.observeFriendSummary(friendUserId: String): Flow<FriendProfileLoadState> = callbackFlow {
    val registration = addSnapshotListener { snapshot, error ->
        if (error != null) {
            trySend(
                FriendProfileLoadState(
                    errorMessage = error.toUiMessage(
                        defaultMessage = FRIEND_PROFILE_LOAD_ERROR,
                        permissionDeniedMessage = FRIEND_PROFILE_PERMISSION_ERROR
                    )
                )
            )
            return@addSnapshotListener
        }

        trySend(FriendProfileLoadState(profile = snapshot?.toFriendSummary(friendUserId)))
    }

    awaitClose {
        registration.remove()
    }
}

private fun Query.observeFriendLogs(): Flow<FriendLogsLoadState> = callbackFlow {
    val registration = addSnapshotListener { snapshot, error ->
        if (error != null) {
            trySend(
                FriendLogsLoadState(
                    errorMessage = error.toUiMessage(
                        defaultMessage = FRIEND_LOGS_LOAD_ERROR,
                        permissionDeniedMessage = FRIEND_LOGS_PERMISSION_ERROR
                    )
                )
            )
            return@addSnapshotListener
        }

        val logs = snapshot?.documents.orEmpty()
            .mapNotNull(DocumentSnapshot::toBaseballLogEntry)
        trySend(FriendLogsLoadState(logs = logs))
    }

    awaitClose {
        registration.remove()
    }
}

private fun FirebaseFirestoreException.toUiMessage(
    defaultMessage: String,
    permissionDeniedMessage: String
): String {
    return when (code) {
        FirebaseFirestoreException.Code.PERMISSION_DENIED -> permissionDeniedMessage
        else -> localizedMessage ?: message ?: defaultMessage
    }
}

private fun DocumentSnapshot.toFriendSummary(friendUserId: String): FriendSummary? {
    val nickname = getString(FIELD_NICKNAME).orEmpty().trim()
    if (nickname.length < MIN_NICKNAME_LENGTH) return null

    val favoriteTeam = getString(FIELD_FAVORITE_TEAM_ID)?.let(BaseballTeam::fromId)
        ?: getString(FIELD_FAVORITE_TEAM_NAME)?.let(BaseballTeam::fromDisplayName)
        ?: return null

    return FriendSummary(
        userId = friendUserId,
        nickname = nickname,
        favoriteTeam = favoriteTeam,
        bio = getString(FIELD_BIO).orEmpty().trim(),
        photoUrl = getString(FIELD_PHOTO_URL).orEmpty().trim()
    )
}

private fun DocumentSnapshot.toBaseballLogEntry(): BaseballLogEntry? {
    val attendedDate = getString(FIELD_ATTENDED_DATE)?.let(LocalDate::parse) ?: return null
    val opponentTeam = getString(FIELD_TEAM_ID)?.let(BaseballTeam::fromId)
        ?: getString(FIELD_TEAM_NAME)?.let(BaseballTeam::fromDisplayName)
        ?: return null
    val result = getString(FIELD_RESULT)
        ?.let { raw ->
            BaseballGameResult.entries.firstOrNull { result ->
                result.name.equals(raw, ignoreCase = true)
            }
        }
        ?: return null

    return BaseballLogEntry(
        id = id,
        attendedDate = attendedDate,
        opponentTeam = opponentTeam,
        result = result
    )
}

private const val USERS_COLLECTION = "users"
private const val FRIENDS_COLLECTION = "friends"
private const val LOGS_COLLECTION = "logs"
private const val FIELD_NICKNAME = "nickname"
private const val FIELD_FAVORITE_TEAM_ID = "favoriteTeamId"
private const val FIELD_FAVORITE_TEAM_NAME = "favoriteTeamName"
private const val FIELD_BIO = "bio"
private const val FIELD_PHOTO_URL = "photoUrl"
private const val FIELD_ATTENDED_DATE = "attendedDate"
private const val FIELD_TEAM_ID = "teamId"
private const val FIELD_TEAM_NAME = "teamName"
private const val FIELD_RESULT = "result"
private const val MIN_NICKNAME_LENGTH = 2
private const val FRIEND_PROFILE_LOAD_ERROR = "친구 프로필을 지금 불러오지 못했어요."
private const val FRIEND_PROFILE_PERMISSION_ERROR =
    "이 친구의 프로필이 Firestore rules에 의해 차단되어 있어요. Firebase Console의 최신 rules를 반영한 뒤 다시 시도해주세요."
private const val FRIEND_LOGS_LOAD_ERROR = "친구의 직관 기록을 지금 불러오지 못했어요."
private const val FRIEND_LOGS_PERMISSION_ERROR =
    "이 친구의 직관 기록이 Firestore rules에 의해 차단되어 있어요. Firebase Console의 최신 rules를 반영한 뒤 다시 시도해주세요."
