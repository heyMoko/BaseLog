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

    override fun observeLog(logId: String): Flow<BaseballLogEntry?> {
        val auth = firebaseAuth ?: return flowOf(null)
        val db = firestore ?: return flowOf(null)

        return auth.observeCurrentUserId()
            .flatMapLatest { userId ->
                if (userId == null) {
                    flowOf(null)
                } else {
                    db.collection(USERS_COLLECTION)
                        .document(userId)
                        .collection(LOGS_COLLECTION)
                        .document(logId)
                        .observeLog()
                }
            }
            .distinctUntilChanged()
    }

    override suspend fun createLog(log: BaseballLogDraft): Result<Unit> {
        val document = resolveLogsCollection(firebaseAuth, firestore)
            .map { it.document() }
            .getOrElse { return Result.failure(it) }

        return document.awaitSet(log.toDocumentData(includeCreatedAt = true))
    }

    override suspend fun updateLog(
        logId: String,
        log: BaseballLogDraft
    ): Result<Unit> {
        val document = resolveLogsCollection(firebaseAuth, firestore)
            .map { it.document(logId) }
            .getOrElse { return Result.failure(it) }

        return document.awaitUpdate(log.toDocumentData(includeCreatedAt = false))
    }

    override suspend fun deleteLog(logId: String): Result<Unit> {
        val document = resolveLogsCollection(firebaseAuth, firestore)
            .map { it.document(logId) }
            .getOrElse { return Result.failure(it) }

        return document.awaitDelete()
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

private fun DocumentReference.observeLog(): Flow<BaseballLogEntry?> = callbackFlow {
    val registration = addSnapshotListener { snapshot, _ ->
        trySend(snapshot?.toBaseballLogEntry())
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

private suspend fun DocumentReference.awaitUpdate(
    data: Map<String, Any>
): Result<Unit> = suspendCancellableCoroutine { continuation ->
    update(data)
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

private suspend fun DocumentReference.awaitDelete(): Result<Unit> = suspendCancellableCoroutine { continuation ->
    delete()
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

private fun BaseballLogDraft.toDocumentData(includeCreatedAt: Boolean): Map<String, Any> {
    return buildMap {
        put(FIELD_ATTENDED_DATE, attendedDate.toString())
        put(FIELD_ATTENDED_YEAR, attendedDate.year)
        put(FIELD_TEAM_ID, opponentTeam.id)
        put(FIELD_TEAM_NAME, opponentTeam.displayName)
        put(FIELD_RESULT, result.name)
        if (includeCreatedAt) {
            put(FIELD_CREATED_AT, FieldValue.serverTimestamp())
        }
        put(FIELD_UPDATED_AT, FieldValue.serverTimestamp())
    }
}

private fun resolveLogsCollection(
    firebaseAuth: FirebaseAuth?,
    firestore: FirebaseFirestore?
) = runCatching {
    val auth = firebaseAuth ?: throw IllegalStateException(
        "You need to be signed in before managing a game log."
    )
    val db = firestore ?: throw IllegalStateException(
        "Firestore is not configured. Check Firebase setup before managing logs."
    )
    val user = auth.currentUser ?: throw IllegalStateException(
        "You need to be signed in before managing a game log."
    )

    db.collection(USERS_COLLECTION)
        .document(user.uid)
        .collection(LOGS_COLLECTION)
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
