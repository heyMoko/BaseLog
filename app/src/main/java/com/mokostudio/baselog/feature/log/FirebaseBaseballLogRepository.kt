package com.mokostudio.baselog.feature.log

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.mokostudio.baselog.core.user.BaseballTeam
import java.time.LocalDate
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
class FirebaseBaseballLogRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth?,
    private val firestore: FirebaseFirestore?
) : BaseballLogRepository {
    override fun observeLogs(): Flow<List<BaseballLogEntry>> {
        val auth = firebaseAuth ?: return flowOf(emptyList())
        val db = firestore ?: return flowOf(emptyList())

        return auth.observeCurrentUserId()
            .flatMapLatest { userId ->
                if (userId == null) {
                    flowOf(emptyList())
                } else {
                    db.collection(USERS_COLLECTION)
                        .document(userId)
                        .collection(LOGS_COLLECTION)
                        .orderBy(FIELD_ATTENDED_DATE, Query.Direction.DESCENDING)
                        .observeLogs()
                }
            }
            .distinctUntilChanged()
    }

    override suspend fun saveLog(log: BaseballLogDraft): Result<Unit> {
        val auth = firebaseAuth ?: return Result.failure(
            IllegalStateException("You need to be signed in before saving a game log.")
        )
        val db = firestore ?: return Result.failure(
            IllegalStateException("Firestore is not configured. Check Firebase setup before saving logs.")
        )
        val user = auth.currentUser ?: return Result.failure(
            IllegalStateException("You need to be signed in before saving a game log.")
        )

        val document = db.collection(USERS_COLLECTION)
            .document(user.uid)
            .collection(LOGS_COLLECTION)
            .document()

        val logData = hashMapOf<String, Any>(
            FIELD_ATTENDED_DATE to log.attendedDate.toString(),
            FIELD_ATTENDED_YEAR to log.attendedDate.year,
            FIELD_TEAM_ID to log.opponentTeam.id,
            FIELD_TEAM_NAME to log.opponentTeam.displayName,
            FIELD_RESULT to log.result.name,
            FIELD_CREATED_AT to FieldValue.serverTimestamp(),
            FIELD_UPDATED_AT to FieldValue.serverTimestamp()
        )

        return document.awaitSet(logData)
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

private fun Query.observeLogs(): Flow<List<BaseballLogEntry>> = callbackFlow {
    val registration = addSnapshotListener { snapshot, _ ->
        val logs = snapshot?.documents.orEmpty()
            .mapNotNull { it.toBaseballLogEntry() }
        trySend(logs)
    }

    awaitClose {
        registration.remove()
    }
}

private fun DocumentSnapshot.toBaseballLogEntry(): BaseballLogEntry? {
    val id = id
    val attendedDate = getString(FIELD_ATTENDED_DATE)
        ?.let(LocalDate::parse)
        ?: return null
    val opponentTeam = resolveTeam() ?: return null
    val result = getString(FIELD_RESULT)
        ?.let(BaseballGameResult::fromStorageValue)
        ?: return null

    return BaseballLogEntry(
        id = id,
        attendedDate = attendedDate,
        opponentTeam = opponentTeam,
        result = result
    )
}

private fun DocumentSnapshot.resolveTeam() =
    getString(FIELD_TEAM_ID)?.let(BaseballTeam::fromId)
        ?: getString(FIELD_TEAM_NAME)?.let(BaseballTeam::fromDisplayName)

private suspend fun DocumentReference.awaitSet(
    data: Map<String, Any>
): Result<Unit> = suspendCancellableCoroutine { continuation ->
    set(data)
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

private fun BaseballGameResult.Companion.fromStorageValue(value: String): BaseballGameResult? {
    return BaseballGameResult.entries.firstOrNull { result ->
        result.name.equals(value, ignoreCase = true)
    }
}

private const val USERS_COLLECTION = "users"
private const val LOGS_COLLECTION = "logs"
private const val FIELD_ATTENDED_DATE = "attendedDate"
private const val FIELD_ATTENDED_YEAR = "attendedYear"
private const val FIELD_TEAM_ID = "teamId"
private const val FIELD_TEAM_NAME = "teamName"
private const val FIELD_RESULT = "result"
private const val FIELD_CREATED_AT = "createdAt"
private const val FIELD_UPDATED_AT = "updatedAt"
