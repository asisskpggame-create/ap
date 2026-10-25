package com.example.data.firebase

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.BuildConfig
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private const val FALLBACK_PROJECT_ID = "gen-lang-client-0280793534"
private const val FALLBACK_APP_ID = "1:957078935606:android:925e0574c700d106c9a097"
private const val FALLBACK_API_KEY = "AIzaSyDniF4amC-qH1eL1NqLKOvo6EgZdqhvda8"
private const val FALLBACK_STORAGE_BUCKET = "gen-lang-client-0280793534.firebasestorage.app"
private const val FALLBACK_WEB_CLIENT_ID = "957078935606-nfbplhuqu6k4g2j1r5rm83m4e04u8s8t.apps.googleusercontent.com"

/**
 * Ensures default FirebaseApp is initialized even if google-services.json was stripped
 * during GitHub Actions or standalone APK builds. Never throws.
 */
fun ensureFirebaseInitialized(context: Context): Boolean {
    return try {
        val appContext = context.applicationContext ?: context
        if (FirebaseApp.getApps(appContext).isNotEmpty()) {
            true
        } else {
            val defaultApp = FirebaseApp.initializeApp(appContext)
            if (defaultApp != null) {
                true
            } else {
                val fallbackOptions = FirebaseOptions.Builder()
                    .setProjectId(FALLBACK_PROJECT_ID)
                    .setApplicationId(FALLBACK_APP_ID)
                    .setApiKey(FALLBACK_API_KEY)
                    .setStorageBucket(FALLBACK_STORAGE_BUCKET)
                    .build()
                FirebaseApp.initializeApp(appContext, fallbackOptions) != null
            }
        }
    } catch (t: Throwable) {
        Log.w("AppCheck", "Firebase initialization fallback note: ${t.message}")
        false
    }
}

/**
 * Configures Firebase App Check before setContent or any Firebase AI / Firestore call.
 * Supplies FIREBASE_APPCHECK_DEBUG_TOKEN from Activity Intent extras if provided in debug/test environments.
 */
fun configureAppCheck(context: Context, intent: Intent?) {
    try {
        if (!ensureFirebaseInitialized(context)) return
        val debugTokenFromIntent = intent?.getStringExtra("FIREBASE_APPCHECK_DEBUG_TOKEN")
        if (!debugTokenFromIntent.isNullOrBlank()) {
            System.setProperty("firebase.appcheck.debug.secret", debugTokenFromIntent)
            val prefs = context.getSharedPreferences(
                "com.google.firebase.appcheck.debug.store.${FirebaseApp.getInstance().persistenceKey}",
                Context.MODE_PRIVATE
            )
            prefs.edit().putString("com.google.firebase.appcheck.debug.DEBUG_SECRET", debugTokenFromIntent).apply()
        }
        if (BuildConfig.DEBUG || !debugTokenFromIntent.isNullOrBlank()) {
            val firebaseAppCheck = FirebaseAppCheck.getInstance()
            firebaseAppCheck.installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance()
            )
        }
    } catch (t: Throwable) {
        Log.w("AppCheck", "App Check initialization note: ${t.message}")
    }
}

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

fun safeCurrentUser(context: Context? = null): FirebaseUser? {
    return try {
        if (context != null) ensureFirebaseInitialized(context)
        Firebase.auth.currentUser
    } catch (_: Throwable) {
        null
    }
}

fun safeAuthStateFlow(context: Context? = null): Flow<FirebaseUser?> {
    return try {
        if (context != null) ensureFirebaseInitialized(context)
        Firebase.auth.authStateFlow()
    } catch (_: Throwable) {
        flowOf(null)
    }
}

private fun getDefaultWebClientId(context: Context): String {
    return try {
        val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        if (resId != 0) {
            context.getString(resId).ifBlank { FALLBACK_WEB_CLIENT_ID }
        } else {
            FALLBACK_WEB_CLIENT_ID
        }
    } catch (_: Throwable) {
        FALLBACK_WEB_CLIENT_ID
    }
}

/**
 * 1. Silent Auto-Sign-In on App Startup (One option per request: GetGoogleIdOption)
 */
fun attemptAutoSignIn(
    context: Context,
    credentialManager: CredentialManager?,
    onAuthSuccess: () -> Unit,
    onUnauthenticated: () -> Unit,
    scope: CoroutineScope
) {
    try {
        ensureFirebaseInitialized(context)
        if (Firebase.auth.currentUser != null) {
            onAuthSuccess()
            return
        }
    } catch (_: Throwable) {
        onUnauthenticated()
        return
    }

    if (credentialManager == null) {
        onUnauthenticated()
        return
    }

    val clientId = getDefaultWebClientId(context)
    if (clientId.isEmpty()) {
        onUnauthenticated()
        return
    }

    scope.launch {
        try {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(true)
                .setServerClientId(clientId)
                .setAutoSelectEnabled(true)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(context, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                Firebase.auth.signInWithCredential(authCredential).await()
                onAuthSuccess()
            } else {
                onUnauthenticated()
            }
        } catch (_: Throwable) {
            onUnauthenticated()
        }
    }
}

/**
 * 2. Interactive Google Sign-In (One option per request: GetSignInWithGoogleOption)
 */
fun onGoogleSignInClicked(
    context: Context,
    credentialManager: CredentialManager?,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope,
    onAuthCancelled: () -> Unit = {}
) {
    if (credentialManager == null) {
        onAuthError("سرویس مدیریت حساب گوگل روی این دستگاه در دسترس نیست")
        return
    }
    val clientId = getDefaultWebClientId(context)
    if (clientId.isEmpty()) {
        onAuthError("پیکربندی ورود با گوگل یافت نشد")
        return
    }

    scope.launch {
        try {
            ensureFirebaseInitialized(context)
            val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(signInOption)
                .build()

            val activityContext = context as? Activity ?: context
            val result = credentialManager.getCredential(activityContext, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                Firebase.auth.signInWithCredential(authCredential).await()
                onAuthSuccess()
            } else {
                onAuthError("نوع اعتبارنامه نامعتبر است")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w("Auth", "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
            onAuthCancelled()
        } catch (t: Throwable) {
            Log.e("Auth", "Google Sign-In failed", t)
            onAuthError(t.localizedMessage ?: "خطا در ورود با حساب گوگل")
        }
    }
}

/**
 * 3. Sign-Out Flow (Must sign out of Firebase AND clear Credential Manager state)
 */
fun signOut(
    context: Context,
    credentialManager: CredentialManager?,
    onSignOutComplete: () -> Unit,
    scope: CoroutineScope
) {
    try {
        ensureFirebaseInitialized(context)
        Firebase.auth.signOut()
    } catch (_: Throwable) {
    }
    scope.launch {
        try {
            credentialManager?.clearCredentialState(ClearCredentialStateRequest())
        } catch (t: Throwable) {
            Log.e("Auth", "Failed to clear credential state", t)
        } finally {
            onSignOutComplete()
        }
    }
}
