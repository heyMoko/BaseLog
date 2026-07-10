package com.mokostudio.baselog.feature.auth.login

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import javax.inject.Inject

class GoogleIdTokenProvider @Inject constructor() {
    suspend fun requestIdToken(
        context: Context,
        serverClientId: String
    ): Result<String> {
        if (serverClientId.isBlank()) {
            return Result.failure(
                IllegalStateException("Google sign-in is not configured. Check google-services.json and Firebase setup.")
            )
        }

        val credentialManager = CredentialManager.create(context)
        return runCatching {
            val credential = requestCredential(
                credentialManager = credentialManager,
                context = context,
                serverClientId = serverClientId,
                filterByAuthorizedAccounts = true
            ).credential

            extractGoogleIdToken(credential)
        }.recoverCatching { throwable ->
            if (throwable is NoCredentialException) {
                val credential = requestCredential(
                    credentialManager = credentialManager,
                    context = context,
                    serverClientId = serverClientId,
                    filterByAuthorizedAccounts = false
                ).credential
                extractGoogleIdToken(credential)
            } else {
                throw throwable
            }
        }
    }

    private suspend fun requestCredential(
        credentialManager: CredentialManager,
        context: Context,
        serverClientId: String,
        filterByAuthorizedAccounts: Boolean
    ): GetCredentialResponse {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setServerClientId(serverClientId)
            .setFilterByAuthorizedAccounts(filterByAuthorizedAccounts)
            .setAutoSelectEnabled(true)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return credentialManager.getCredential(
            context = context,
            request = request
        )
    }

    private fun extractGoogleIdToken(credential: androidx.credentials.Credential): String {
        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            return try {
                GoogleIdTokenCredential.createFrom(credential.data).idToken
            } catch (error: GoogleIdTokenParsingException) {
                throw IllegalStateException("Received an invalid Google ID token.", error)
            }
        }

        throw IllegalStateException("Google sign-in returned an unsupported credential.")
    }
}
