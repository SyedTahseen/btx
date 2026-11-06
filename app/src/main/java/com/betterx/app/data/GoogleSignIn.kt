package com.betterx.app.data

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.betterx.app.BuildConfig
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException

sealed class GoogleResult {
    data class Token(val idToken: String) : GoogleResult()
    data object Cancelled : GoogleResult()
    data class Error(val message: String) : GoogleResult()
}

/** Shows the Google account picker and returns a Google ID token, which [Auth.signInWithGoogle] turns into a session. */
object GoogleSignIn {
    /** The button only appears when a Web client ID has been set in app/build.gradle.kts. */
    val available: Boolean get() = BuildConfig.GOOGLE_WEB_CLIENT_ID.isNotBlank()

    suspend fun getIdToken(context: Context): GoogleResult {
        return try {
            val option = GetGoogleIdOption.Builder()
                .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
                .setFilterByAuthorizedAccounts(false)
                .build()
            val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
            val credential = CredentialManager.create(context).getCredential(context, request).credential
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                GoogleResult.Token(GoogleIdTokenCredential.createFrom(credential.data).idToken)
            } else {
                GoogleResult.Error("Google sign-in failed. Please try again.")
            }
        } catch (e: GetCredentialCancellationException) {
            GoogleResult.Cancelled
        } catch (e: NoCredentialException) {
            GoogleResult.Error("No Google account is available on this phone. Add one in Settings and try again.")
        } catch (e: GetCredentialException) {
            GoogleResult.Error("Google sign-in failed. Please try again.")
        } catch (e: GoogleIdTokenParsingException) {
            GoogleResult.Error("Google sign-in failed. Please try again.")
        }
    }
}
